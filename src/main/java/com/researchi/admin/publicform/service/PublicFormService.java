package com.researchi.admin.publicform.service;

import com.researchi.admin.blacklist.service.BlacklistModePolicy;
import com.researchi.admin.form.domain.FormFieldDetail;
import com.researchi.admin.form.domain.FormFieldOption;
import com.researchi.admin.form.domain.FormFieldType;
import com.researchi.admin.form.service.FormFieldService;
import com.researchi.admin.job.domain.AdminJobMeta;
import com.researchi.admin.job.domain.JobDetail;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.job.support.ApplicationFormNoticeItem;
import com.researchi.admin.job.support.ApplicationFormNoticeParser;
import com.researchi.admin.job.support.ApplicationFormNoticeOption;
import com.researchi.admin.keyword.service.KeywordExtractionService;
import com.researchi.admin.publicform.domain.AdminApplicationDuplicateLog;
import com.researchi.admin.publicform.domain.AdminBlacklist;
import com.researchi.admin.publicform.domain.AdminBlacklistMatchLog;
import com.researchi.admin.publicform.domain.AdminFormSubmissionAnswer;
import com.researchi.admin.publicform.domain.AdminJobApplication;
import com.researchi.admin.publicform.domain.AdminJobApplicationExtraAnswer;
import com.researchi.admin.publicform.domain.AdminPrivacyConsent;
import com.researchi.admin.publicform.domain.PublicFormAvailability;
import com.researchi.admin.publicform.domain.PublicFormPage;
import com.researchi.admin.publicform.domain.PublicFormSubmissionResult;
import com.researchi.admin.publicform.domain.PublicFormSubmissionStatus;
import com.researchi.admin.publicform.domain.PublicFormUnavailableException;
import com.researchi.admin.publicform.domain.PublicFormValidationException;
import com.researchi.admin.publicform.mapper.AdminApplicationDuplicateLogMapper;
import com.researchi.admin.publicform.mapper.AdminBlacklistMapper;
import com.researchi.admin.publicform.mapper.AdminBlacklistMatchLogMapper;
import com.researchi.admin.publicform.mapper.AdminFormSubmissionAnswerMapper;
import com.researchi.admin.publicform.mapper.AdminJobApplicationMapper;
import com.researchi.admin.publicform.mapper.AdminJobApplicationExtraAnswerMapper;
import com.researchi.admin.publicform.mapper.AdminPrivacyConsentMapper;
import com.researchi.admin.publicform.web.PublicApplicationForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class PublicFormService {

    private final JobService jobService;
    private final FormFieldService formFieldService;
    private final AdminJobApplicationMapper adminJobApplicationMapper;
    private final AdminFormSubmissionAnswerMapper adminFormSubmissionAnswerMapper;
    private final AdminApplicationDuplicateLogMapper adminApplicationDuplicateLogMapper;
    private final AdminBlacklistMapper adminBlacklistMapper;
    private final AdminBlacklistMatchLogMapper adminBlacklistMatchLogMapper;
    private final AdminPrivacyConsentMapper adminPrivacyConsentMapper;
    private final AdminJobApplicationExtraAnswerMapper adminJobApplicationExtraAnswerMapper;
    private final PublicFormProtectionService protectionService;
    private final KeywordExtractionService keywordExtractionService;

    public PublicFormService(
            JobService jobService,
            FormFieldService formFieldService,
            AdminJobApplicationMapper adminJobApplicationMapper,
            AdminFormSubmissionAnswerMapper adminFormSubmissionAnswerMapper,
            AdminApplicationDuplicateLogMapper adminApplicationDuplicateLogMapper,
            AdminBlacklistMapper adminBlacklistMapper,
            AdminBlacklistMatchLogMapper adminBlacklistMatchLogMapper,
            AdminPrivacyConsentMapper adminPrivacyConsentMapper,
            AdminJobApplicationExtraAnswerMapper adminJobApplicationExtraAnswerMapper,
            PublicFormProtectionService protectionService,
            KeywordExtractionService keywordExtractionService
    ) {
        this.jobService = jobService;
        this.formFieldService = formFieldService;
        this.adminJobApplicationMapper = adminJobApplicationMapper;
        this.adminFormSubmissionAnswerMapper = adminFormSubmissionAnswerMapper;
        this.adminApplicationDuplicateLogMapper = adminApplicationDuplicateLogMapper;
        this.adminBlacklistMapper = adminBlacklistMapper;
        this.adminBlacklistMatchLogMapper = adminBlacklistMatchLogMapper;
        this.adminPrivacyConsentMapper = adminPrivacyConsentMapper;
        this.adminJobApplicationExtraAnswerMapper = adminJobApplicationExtraAnswerMapper;
        this.protectionService = protectionService;
        this.keywordExtractionService = keywordExtractionService;
    }

    public PublicFormPage getPage(Long documentSrl, HttpSession session) {
        JobDetail jobDetail = requireOpenJob(documentSrl);
        return new PublicFormPage(
                jobDetail,
                activeFields(documentSrl),
                protectionService.ensureCaptchaQuestion(session),
                protectionService.isCaptchaEnabled()
        );
    }

    public PublicFormAvailability getAvailability(Long documentSrl) {
        try {
            JobDetail jobDetail = jobService.getJob(documentSrl);
            String unavailableReason = unavailableReason(jobDetail);
            return unavailableReason == null
                    ? new PublicFormAvailability(true, "현재 신청 가능한 공고입니다.")
                    : new PublicFormAvailability(false, unavailableReason);
        } catch (IllegalArgumentException ex) {
            return new PublicFormAvailability(false, "존재하지 않는 공고이거나 신청서 주소가 잘못되었습니다.");
        }
    }

    public Map<Long, List<String>> extractDynamicValues(Long documentSrl, HttpServletRequest request) {
        Map<Long, List<String>> values = new LinkedHashMap<>();
        for (FormFieldDetail field : activeFields(documentSrl)) {
            String[] parameterValues = request.getParameterValues(parameterName(field.id()));
            if (parameterValues == null || parameterValues.length == 0) {
                values.put(field.id(), List.of());
                continue;
            }
            List<String> fieldValues = new ArrayList<>();
            for (String current : parameterValues) {
                String normalized = trimToNull(current);
                if (normalized != null) {
                    fieldValues.add(normalized);
                }
            }
            values.put(field.id(), List.copyOf(fieldValues));
        }
        return values;
    }

    public String parameterName(Long fieldId) {
        return "field_" + fieldId;
    }

    @Transactional("adminTransactionManager")
    public PublicFormSubmissionResult submit(
            Long documentSrl,
            PublicApplicationForm form,
            Map<Long, List<String>> dynamicValues,
            HttpServletRequest request
    ) {
        requireOpenJob(documentSrl);
        String rateLimitKey = documentSrl + ":" + clientIp(request);
        if (!protectionService.tryAcquireRateLimitSlot(rateLimitKey)) {
            return new PublicFormSubmissionResult(PublicFormSubmissionStatus.BLOCKED, null);
        }

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        Map<Long, String> dynamicFieldErrors = new LinkedHashMap<>();
        validateBusinessRules(form, request.getSession(), fieldErrors);
        List<ApplicationFormNoticeItem> additionalItems = additionalItems(documentSrl);
        List<String> normalizedExtraAnswers = validateAdditionalAnswers(additionalItems, form.getExtraAnswers(), fieldErrors);

        String normalizedMobile = protectionService.normalizePhone(form.getMobilePhone());
        String normalizedTel = protectionService.normalizePhone(form.getTelPhone());
        String normalizedEmail = protectionService.normalizeEmail(form.getEmailAddress());
        if (normalizedMobile == null || normalizedMobile.length() < 10) {
            fieldErrors.put("mobilePhone", "올바른 휴대전화 번호를 입력해 주세요.");
        }
        if (normalizedTel != null && normalizedTel.length() < 9) {
            fieldErrors.put("telPhone", "올바른 전화번호를 입력해 주세요.");
        }
        if (normalizedEmail != null && !normalizedEmail.contains("@")) {
            fieldErrors.put("emailAddress", "올바른 이메일 주소를 입력해 주세요.");
        }
        if (Boolean.TRUE.equals(form.getNotifyEmailYn()) && normalizedEmail == null) {
            fieldErrors.put("emailAddress", "이메일 알림을 받으려면 이메일 주소를 입력해 주세요.");
        }

        Map<Long, List<String>> normalizedDynamicValues = validateDynamicValues(activeFields(documentSrl), dynamicValues, dynamicFieldErrors);
        if (!fieldErrors.isEmpty() || !dynamicFieldErrors.isEmpty()) {
            throw new PublicFormValidationException(fieldErrors, dynamicFieldErrors, null);
        }

        String applicantName = trimToNull(form.getApplicantName());
        String genderCode = normalizeGender(form.getGenderCode());
        LocalDate birthDate = form.getBirthDate();
        String mobilePhoneHash = protectionService.phoneHash(normalizedMobile);
        List<String> mobilePhoneHashes = phoneHashCandidates(normalizedMobile);

        AdminApplicationDuplicateLog existing = adminApplicationDuplicateLogMapper.findLatestByDocumentSrlAndMobilePhoneHashes(documentSrl, mobilePhoneHashes);
        if (existing != null && existing.getMatchedApplicationId() != null) {
            adminApplicationDuplicateLogMapper.insert(duplicateLog(documentSrl, applicantName, genderCode, birthDate, mobilePhoneHash, true, existing.getMatchedApplicationId()));
            return new PublicFormSubmissionResult(PublicFormSubmissionStatus.DUPLICATE, existing.getMatchedApplicationId());
        }

        List<AdminBlacklist> blacklistMatches = adminBlacklistMapper.findActiveMatches(applicantName, birthDate, mobilePhoneHashes);
        AdminBlacklist effectiveBlacklist = BlacklistModePolicy.effectiveMatch(blacklistMatches);
        AdminJobApplication application = toApplication(
                documentSrl,
                form,
                additionalItems.stream().map(ApplicationFormNoticeItem::label).toList(),
                normalizedExtraAnswers,
                normalizedMobile,
                normalizedTel,
                normalizedEmail,
                mobilePhoneHash,
                effectiveBlacklist
        );
        adminJobApplicationMapper.insert(application);

        adminApplicationDuplicateLogMapper.insert(duplicateLog(documentSrl, applicantName, genderCode, birthDate, mobilePhoneHash, false, application.getId()));

        saveAnswers(application.getId(), normalizedDynamicValues);
        saveExtraAnswers(application.getId(), additionalItems.stream().map(ApplicationFormNoticeItem::label).toList(), normalizedExtraAnswers);
        saveConsents(application.getId(), form, clientIp(request));
        saveBlacklistMatches(application.getId(), blacklistMatches);
        keywordExtractionService.syncApplicationKeywords(application.getId());

        return new PublicFormSubmissionResult(
                blacklistMatches.isEmpty() ? PublicFormSubmissionStatus.COMPLETE : PublicFormSubmissionStatus.BLOCKED,
                application.getId()
        );
    }

    private List<ApplicationFormNoticeItem> additionalItems(Long documentSrl) {
        JobDetail jobDetail = jobService.getJob(documentSrl);
        AdminJobMeta meta = jobDetail.getMeta();
        if (meta == null || meta.getApplicationFormNotice() == null || meta.getApplicationFormNotice().isBlank()) {
            return List.of();
        }
        return ApplicationFormNoticeParser.parseDetails(meta.getApplicationFormNotice());
    }

    private void validateBusinessRules(
            PublicApplicationForm form,
            HttpSession session,
            Map<String, String> fieldErrors
    ) {
        if (form.getWebsite() != null && !form.getWebsite().isBlank()) {
            throw new PublicFormUnavailableException("차단된 요청입니다.");
        }
        if (!protectionService.validateCaptcha(session, form.getCaptchaAnswer())) {
            fieldErrors.put("captchaAnswer", "자동 입력 방지 답변이 올바르지 않습니다.");
        }
        if (form.getBirthDate() != null) {
            if (form.getBirthDate().isAfter(LocalDate.now())) {
                fieldErrors.put("birthDate", "생년월일은 미래 날짜일 수 없습니다.");
            }
            if (form.getBirthDate().isBefore(LocalDate.of(1900, 1, 1))) {
                fieldErrors.put("birthDate", "생년월일이 너무 이릅니다.");
            }
        }
    }

    private Map<Long, List<String>> validateDynamicValues(
            List<FormFieldDetail> fields,
            Map<Long, List<String>> dynamicValues,
            Map<Long, String> dynamicFieldErrors
    ) {
        Map<Long, List<String>> normalized = new LinkedHashMap<>();
        for (FormFieldDetail field : fields) {
            List<String> rawValues = dynamicValues.getOrDefault(field.id(), List.of());
            List<String> currentValues = rawValues.stream()
                    .map(this::trimToNull)
                    .filter(Objects::nonNull)
                    .toList();

            if (field.required() && currentValues.isEmpty()) {
                dynamicFieldErrors.put(field.id(), "필수 항목입니다.");
                normalized.put(field.id(), List.of());
                continue;
            }

            if (currentValues.isEmpty()) {
                normalized.put(field.id(), List.of());
                continue;
            }

            FormFieldType fieldType = FormFieldType.valueOf(field.fieldType().toUpperCase(Locale.ROOT));
            switch (fieldType) {
                case TEXT, TEXTAREA -> {
                    if (currentValues.size() > 1) {
                        dynamicFieldErrors.put(field.id(), "하나의 값만 입력해 주세요.");
                    }
                }
                case NUMBER -> {
                    if (currentValues.size() > 1 || !currentValues.get(0).matches("-?\\d+(\\.\\d+)?")) {
                        dynamicFieldErrors.put(field.id(), "올바른 숫자를 입력해 주세요.");
                    }
                }
                case DATE -> {
                    if (currentValues.size() > 1) {
                        dynamicFieldErrors.put(field.id(), "올바른 날짜를 입력해 주세요.");
                    } else {
                        try {
                            LocalDate.parse(currentValues.get(0));
                        } catch (RuntimeException ex) {
                            dynamicFieldErrors.put(field.id(), "올바른 날짜를 입력해 주세요.");
                        }
                    }
                }
                case RADIO, SELECT -> {
                    if (currentValues.size() != 1 || !isAllowedOption(field.options(), currentValues.get(0))) {
                        dynamicFieldErrors.put(field.id(), "제공된 옵션 중 하나를 선택해 주세요.");
                    }
                }
                case CHECKBOX -> {
                    boolean invalid = currentValues.stream().anyMatch(value -> !isAllowedOption(field.options(), value));
                    if (invalid) {
                        dynamicFieldErrors.put(field.id(), "제공된 옵션만 선택해 주세요.");
                    }
                }
            }
            normalized.put(field.id(), currentValues);
        }
        return normalized;
    }

    private AdminJobApplication toApplication(
            Long documentSrl,
            PublicApplicationForm form,
            List<String> additionalItems,
            List<String> normalizedExtraAnswers,
            String normalizedMobile,
            String normalizedTel,
            String normalizedEmail,
            String mobilePhoneHash,
            AdminBlacklist effectiveBlacklist
    ) {
        AdminJobApplication application = new AdminJobApplication();
        application.setDocumentSrl(documentSrl);
        application.setApplicantName(trimToNull(form.getApplicantName()));
        application.setGenderCode(normalizeGender(form.getGenderCode()));
        application.setBirthDate(form.getBirthDate());
        application.setAgeText(trimToNull(form.getAgeText()));
        application.setJobText(trimToNull(form.getJobText()));
        application.setOrganizationText(trimToNull(form.getOrganizationText()));
        application.setMobilePhoneEnc(protectionService.encrypt(normalizedMobile));
        application.setMobilePhoneMasked(normalizedMobile);
        application.setTelPhoneEnc(protectionService.encrypt(normalizedTel));
        application.setTelPhoneMasked(normalizedTel);
        application.setRegionText(trimToNull(form.getRegionText()) != null ? trimToNull(form.getRegionText()) : trimToNull(form.getAddress()));
        String address = trimToNull(form.getAddress());
        application.setAddressEnc(protectionService.encrypt(address));
        application.setAddressMasked(address);
        application.setExtraComment(buildExtraCommentSummary(additionalItems, normalizedExtraAnswers));
        application.setPriorResearchText(trimToNull(form.getPriorResearchText()));
        application.setEmailAddressEnc(protectionService.encrypt(normalizedEmail));
        application.setEmailAddressMasked(normalizedEmail);
        application.setNotifyEmailYn(toYn(form.getNotifyEmailYn()));
        application.setNotifySmsYn(toYn(form.getNotifySmsYn()));
        application.setNotifyKeywordYn(toYn(hasRecommendationChannel(form)));
        application.setApplicationStatus(effectiveBlacklist == null ? "RECEIVED" : BlacklistModePolicy.applicationStatus(effectiveBlacklist.getBlackMode()));
        application.setIsNewApplicant(adminApplicationDuplicateLogMapper.countPrimaryByMobilePhoneHashes(phoneHashCandidates(normalizedMobile)) == 0 ? "Y" : "N");
        application.setIsBlacklisted(effectiveBlacklist == null ? "N" : "Y");
        application.setBlackModeApplied(effectiveBlacklist == null ? null : BlacklistModePolicy.normalize(effectiveBlacklist.getBlackMode()));
        application.setProvideYn("Y");
        application.setDeliveryStatus("PENDING");
        return application;
    }

    private List<String> validateAdditionalAnswers(
            List<ApplicationFormNoticeItem> additionalItems,
            List<String> extraAnswers,
            Map<String, String> fieldErrors
    ) {
        if (additionalItems.isEmpty()) {
            return List.of();
        }
        List<String> normalizedAnswers = new ArrayList<>();
        for (int index = 0; index < additionalItems.size(); index++) {
            ApplicationFormNoticeItem item = additionalItems.get(index);
            String value = extraAnswers != null && extraAnswers.size() > index ? trimToNull(extraAnswers.get(index)) : null;
            if (value == null) {
                fieldErrors.put("extraAnswers[" + index + "]", "추가기재사항 답변을 입력해주세요.");
                normalizedAnswers.add("");
                continue;
            }
            if (!isValidAdditionalAnswer(item, value)) {
                fieldErrors.put("extraAnswers[" + index + "]", additionalAnswerErrorMessage(item));
            }
            normalizedAnswers.add(displayAdditionalAnswer(item, value));
        }
        return List.copyOf(normalizedAnswers);
    }

    private String displayAdditionalAnswer(ApplicationFormNoticeItem item, String value) {
        return switch (item.type()) {
            case "RADIO", "SELECT" -> item.options().stream()
                    .filter(option -> option.value().equals(value))
                    .map(ApplicationFormNoticeOption::label)
                    .findFirst()
                    .orElse(value);
            case "CHECKBOX" -> splitMultiValue(value).stream()
                    .map(selectedValue -> item.options().stream()
                            .filter(option -> option.value().equals(selectedValue))
                            .map(ApplicationFormNoticeOption::label)
                            .findFirst()
                            .orElse(selectedValue))
                    .reduce((left, right) -> left + ", " + right)
                    .orElse(value);
            default -> value;
        };
    }

    private boolean isValidAdditionalAnswer(ApplicationFormNoticeItem item, String value) {
        return switch (item.type()) {
            case "NUMBER" -> value.matches("-?\\d+(\\.\\d+)?");
            case "DATE" -> {
                try {
                    LocalDate.parse(value);
                    yield true;
                } catch (RuntimeException ex) {
                    yield false;
                }
            }
            case "RADIO", "SELECT" -> item.options().stream().anyMatch(option -> option.value().equals(value));
            case "CHECKBOX" -> {
                List<String> selected = splitMultiValue(value);
                yield !selected.isEmpty() && selected.stream().allMatch(selectedValue -> isAllowedAdditionalOption(item.options(), selectedValue));
            }
            default -> true;
        };
    }

    private String additionalAnswerErrorMessage(ApplicationFormNoticeItem item) {
        return switch (item.type()) {
            case "NUMBER" -> "올바른 숫자를 입력해 주세요.";
            case "DATE" -> "올바른 날짜를 입력해 주세요.";
            case "RADIO", "SELECT" -> "제공된 옵션 중 하나를 선택해 주세요.";
            case "CHECKBOX" -> "제공된 옵션만 선택해 주세요.";
            default -> "추가기재사항 답변을 입력해주세요.";
        };
    }

    private boolean isAllowedAdditionalOption(List<ApplicationFormNoticeOption> options, String value) {
        return options.stream().anyMatch(option -> option.value().equals(value));
    }

    private List<String> splitMultiValue(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(current -> !current.isBlank())
                .toList();
    }

    private String buildExtraCommentSummary(List<String> additionalItems, List<String> normalizedExtraAnswers) {
        if (additionalItems.isEmpty() || normalizedExtraAnswers.isEmpty()) {
            return null;
        }
        List<String> summaryLines = new ArrayList<>();
        for (int index = 0; index < Math.min(additionalItems.size(), normalizedExtraAnswers.size()); index++) {
            String answer = trimToNull(normalizedExtraAnswers.get(index));
            if (answer == null) {
                continue;
            }
            summaryLines.add(additionalItems.get(index) + ": " + answer);
        }
        return summaryLines.isEmpty() ? null : String.join("\n", summaryLines);
    }

    private void saveAnswers(Long applicationId, Map<Long, List<String>> normalizedDynamicValues) {
        for (Map.Entry<Long, List<String>> entry : normalizedDynamicValues.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            AdminFormSubmissionAnswer answer = new AdminFormSubmissionAnswer();
            answer.setApplicationId(applicationId);
            answer.setFieldId(entry.getKey());
            if (entry.getValue().size() == 1) {
                answer.setAnswerText(entry.getValue().get(0));
            } else {
                answer.setAnswerText(String.join(", ", entry.getValue()));
                answer.setAnswerJson(toJsonArray(entry.getValue()));
            }
            adminFormSubmissionAnswerMapper.insert(answer);
        }
    }

    private void saveExtraAnswers(Long applicationId, List<String> additionalItems, List<String> normalizedExtraAnswers) {
        for (int index = 0; index < Math.min(additionalItems.size(), normalizedExtraAnswers.size()); index++) {
            String answerText = trimToNull(normalizedExtraAnswers.get(index));
            if (answerText == null) {
                continue;
            }
            AdminJobApplicationExtraAnswer answer = new AdminJobApplicationExtraAnswer();
            answer.setApplicationId(applicationId);
            answer.setAnswerOrder(index + 1);
            answer.setQuestionLabel(additionalItems.get(index));
            answer.setAnswerText(answerText);
            adminJobApplicationExtraAnswerMapper.insert(answer);
        }
    }

    private void saveConsents(Long applicationId, PublicApplicationForm form, String ipAddress) {
        adminPrivacyConsentMapper.insert(consent(applicationId, "PRIVACY_COLLECTION", "v1", "Y", ipAddress));
        adminPrivacyConsentMapper.insert(consent(applicationId, "KEYWORD_NOTIFICATION", "v1", toYn(form.getNotifyKeywordYn()), ipAddress));
    }

    private void saveBlacklistMatches(Long applicationId, List<AdminBlacklist> blacklistMatches) {
        for (AdminBlacklist match : blacklistMatches) {
            AdminBlacklistMatchLog matchLog = new AdminBlacklistMatchLog();
            matchLog.setApplicationId(applicationId);
            matchLog.setBlacklistId(match.getId());
            matchLog.setMatchType(match.getMatchType());
            matchLog.setActionTaken(BlacklistModePolicy.actionTaken(match.getBlackMode()));
            adminBlacklistMatchLogMapper.insert(matchLog);
        }
    }

    private AdminPrivacyConsent consent(Long applicationId, String consentType, String version, String agreedYn, String ipAddress) {
        AdminPrivacyConsent consent = new AdminPrivacyConsent();
        consent.setApplicationId(applicationId);
        consent.setConsentType(consentType);
        consent.setConsentVersion(version);
        consent.setAgreedYn(agreedYn);
        consent.setIpAddress(ipAddress);
        return consent;
    }

    private AdminApplicationDuplicateLog duplicateLog(
            Long documentSrl,
            String applicantName,
            String genderCode,
            LocalDate birthDate,
            String mobilePhoneHash,
            boolean duplicateFound,
            Long matchedApplicationId
    ) {
        AdminApplicationDuplicateLog duplicateLog = new AdminApplicationDuplicateLog();
        duplicateLog.setDocumentSrl(documentSrl);
        duplicateLog.setApplicantName(applicantName);
        duplicateLog.setGenderCode(genderCode);
        duplicateLog.setBirthDate(birthDate);
        duplicateLog.setMobilePhoneHash(mobilePhoneHash);
        duplicateLog.setDuplicateFound(duplicateFound ? "Y" : "N");
        duplicateLog.setMatchedApplicationId(matchedApplicationId);
        return duplicateLog;
    }

    private JobDetail requireOpenJob(Long documentSrl) {
        JobDetail jobDetail;
        try {
            jobDetail = jobService.getJob(documentSrl);
        } catch (IllegalArgumentException ex) {
            throw new PublicFormUnavailableException("존재하지 않는 공고이거나 신청서 주소가 잘못되었습니다.");
        }
        String unavailableReason = unavailableReason(jobDetail);
        if (unavailableReason != null) {
            throw new PublicFormUnavailableException(unavailableReason);
        }
        return jobDetail;
    }

    private String unavailableReason(JobDetail jobDetail) {
        if (!"PUBLIC".equals(jobDetail.getDocument().getStatus())) {
            return "공고가 아직 공개 상태가 아니어서 신청서를 열 수 없습니다.";
        }

        AdminJobMeta meta = jobDetail.getMeta();
        if (meta == null) {
            return "공고 메타 정보가 아직 준비되지 않아 신청서를 열 수 없습니다.";
        }
        if (!"RECRUITING".equals(meta.getRecruitStatus())) {
            return "현재 모집중 상태가 아니어서 신청서를 열 수 없습니다.";
        }
        if (!"Y".equals(meta.getApplicationEnabled())) {
            return "이 공고는 현재 지원서 접수가 비활성화되어 있습니다.";
        }
        if (meta.getCloseDate() != null && meta.getCloseDate().isBefore(LocalDate.now())) {
            return "마감일이 지나 신청서를 열 수 없습니다.";
        }
        return null;
    }

    private List<FormFieldDetail> activeFields(Long documentSrl) {
        return formFieldService.getFields(documentSrl).stream()
                .filter(FormFieldDetail::active)
                .toList();
    }

    private boolean isAllowedOption(List<FormFieldOption> options, String value) {
        return options.stream().anyMatch(option -> option.value().equals(value));
    }

    private String normalizeGender(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        String compact = normalized.replace(" ", "");
        return switch (compact.toUpperCase(Locale.ROOT)) {
            case "남", "남성", "남자", "M", "MALE" -> "M";
            case "여", "여성", "여자", "F", "FEMALE" -> "F";
            default -> normalized.toUpperCase(Locale.ROOT);
        };
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String toYn(Boolean value) {
        return Boolean.TRUE.equals(value) ? "Y" : "N";
    }

    private boolean hasRecommendationChannel(PublicApplicationForm form) {
        return Boolean.TRUE.equals(form.getNotifyEmailYn()) || Boolean.TRUE.equals(form.getNotifySmsYn());
    }

    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private List<String> phoneHashCandidates(String normalizedPhone) {
        String currentHash = protectionService.phoneHash(normalizedPhone);
        String legacyHash = protectionService.legacyPhoneHash(normalizedPhone);
        if (currentHash == null) {
            return List.of();
        }
        if (currentHash.equals(legacyHash)) {
            return List.of(currentHash);
        }
        return List.of(currentHash, legacyHash);
    }

    private String toJsonArray(List<String> values) {
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append('"').append(values.get(index).replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
        }
        return builder.append(']').toString();
    }
}
