package com.researchi.admin.legacy.application.service;

import com.researchi.admin.legacy.application.support.ApplicationFormNoticeItem;
import com.researchi.admin.legacy.application.support.ApplicationFormNoticeParser;
import com.researchi.admin.legacy.application.domain.LegacyApplicationExtraAnswer;
import com.researchi.admin.legacy.application.mapper.LegacyApplicationExtraAnswerMapper;
import com.researchi.admin.legacy.application.mapper.LegacyApplicationSearchIndexMapper;
import com.researchi.admin.legacy.matching.service.LegacyMatchingService;
import com.researchi.admin.legacy.research.domain.ResearchApplication;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.legacy.research.mapper.ResearchApplicationMapper;
import com.researchi.admin.legacy.research.service.ResearchMasterService;
import com.researchi.admin.publicform.domain.PublicFormUnavailableException;
import com.researchi.admin.publicform.domain.PublicFormValidationException;
import com.researchi.admin.publicform.service.PublicFormProtectionService;
import com.researchi.admin.publicform.web.PublicApplicationForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LegacyPublicApplicationService {

    private static final Logger log = LoggerFactory.getLogger(LegacyPublicApplicationService.class);
    private static final DateTimeFormatter BIRTH_DATE = DateTimeFormatter.ofPattern("yyMMdd");
    private static final String KOREAN_NAME_PATTERN = "[가-힣\\s]+";
    private static final String ENGLISH_NAME_PATTERN = "[A-Za-z\\s]+";
    private static final int OLD_APP_AGE_MAX_LENGTH = 3;
    private static final int OLD_APP_JOB_MAX_LENGTH = 20;
    private static final int OLD_APP_COMPANY_MAX_LENGTH = 200;
    private static final int OLD_APP_PHONE_MAX_LENGTH = 15;
    private static final int OLD_APP_ADDR_MAX_LENGTH = 100;
    private static final int OLD_ATTEND_RESEARCH_MAX_LENGTH = 200;
    private static final int OLD_ADD_COMMENT_SAFE_MAX_LENGTH = 15000;
    private static final int STRUCTURED_QUESTION_GROUP_MAX_LENGTH = 255;
    private static final int STRUCTURED_QUESTION_LABEL_MAX_LENGTH = 500;

    private final ResearchMasterService researchMasterService;
    private final ResearchApplicationMapper researchApplicationMapper;
    private final LegacyApplicationExtraAnswerMapper legacyApplicationExtraAnswerMapper;
    private final LegacyApplicationSearchIndexMapper legacyApplicationSearchIndexMapper;
    private final PublicFormProtectionService protectionService;
    private final LegacyMatchingService legacyMatchingService;
    private final LegacyApplicationConsentService legacyApplicationConsentService;

    public LegacyPublicApplicationService(
            ResearchMasterService researchMasterService,
            ResearchApplicationMapper researchApplicationMapper,
            LegacyApplicationExtraAnswerMapper legacyApplicationExtraAnswerMapper,
            LegacyApplicationSearchIndexMapper legacyApplicationSearchIndexMapper,
            PublicFormProtectionService protectionService,
            LegacyMatchingService legacyMatchingService,
            LegacyApplicationConsentService legacyApplicationConsentService
    ) {
        this.researchMasterService = researchMasterService;
        this.researchApplicationMapper = researchApplicationMapper;
        this.legacyApplicationExtraAnswerMapper = legacyApplicationExtraAnswerMapper;
        this.legacyApplicationSearchIndexMapper = legacyApplicationSearchIndexMapper;
        this.protectionService = protectionService;
        this.legacyMatchingService = legacyMatchingService;
        this.legacyApplicationConsentService = legacyApplicationConsentService;
    }

    public ResearchMaster getOpenResearch(Long researchNo) {
        ResearchMaster researchMaster = researchMasterService.getResearchMaster(researchNo);
        if (researchMaster == null) {
            throw new PublicFormUnavailableException("존재하지 않는 좌담회/설문입니다.");
        }
        if (isClosed(researchMaster.getCloseDate())) {
            throw new PublicFormUnavailableException("모집이 마감된 좌담회/설문입니다.");
        }
        return researchMaster;
    }

    public List<ApplicationFormNoticeItem> additionalItems(Long researchNo) {
        return ApplicationFormNoticeParser.parseDetails(getOpenResearch(researchNo).getAddComment());
    }

    public boolean isCaptchaEnabled() {
        return protectionService.isCaptchaEnabled();
    }

    public String ensureCaptchaQuestion(HttpSession session) {
        return protectionService.ensureCaptchaQuestion(session);
    }

    @Transactional("oldAdminTransactionManager")
    public synchronized Long submit(Long researchNo, PublicApplicationForm form, HttpServletRequest request) {
        ResearchMaster researchMaster = getOpenResearch(researchNo);
        String rateLimitKey = "legacy:" + researchNo + ":" + clientIp(request);
        if (!protectionService.tryAcquireRateLimitSlot(rateLimitKey)) {
            throw new PublicFormUnavailableException("잠시 후 다시 신청해 주세요.");
        }

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        validateBusinessRules(form, request.getSession(), fieldErrors);
        List<ApplicationFormNoticeItem> additionalItems = ApplicationFormNoticeParser.parseDetails(researchMaster.getAddComment());
        List<String> normalizedExtraAnswers = validateAdditionalAnswers(additionalItems, form, fieldErrors);
        if (!fieldErrors.isEmpty()) {
            throw new PublicFormValidationException(fieldErrors, Map.of(), null);
        }

        String applicantName = trimToNull(form.getApplicantName());
        String appHphone = normalizePhoneForDisplay(form.getMobilePhone());
        Long duplicateSeq = researchApplicationMapper.findDuplicateSeqByNameAndPhone(researchNo, applicantName, appHphone);
        if (duplicateSeq != null) {
            throw new PublicFormValidationException(
                    Map.of(),
                    Map.of(),
                    "이미 같은 이름과 휴대폰 번호로 접수된 신청서가 있습니다."
            );
        }

        Long researchAppSeq = researchApplicationMapper.findNextResearchAppSeq();
        ResearchApplication application = toResearchApplication(researchNo, researchAppSeq, form, normalizedExtraAnswers);
        researchApplicationMapper.insert(application);
        researchApplicationMapper.incrementCounts(researchNo);
        saveStructuredExtraAnswers(researchNo, researchAppSeq, additionalItems, normalizedExtraAnswers, form.getSelectedExtraGroup(), application.getAddComment());
        legacyApplicationConsentService.recordSubmissionConsent(researchNo, researchAppSeq, form);
        indexSubmittedApplication(researchNo, researchAppSeq, form.getEmailAddress());
        return researchAppSeq;
    }

    private void indexSubmittedApplication(Long researchNo, Long researchAppSeq, String emailAddress) {
        try {
            ResearchApplication savedApplication = researchApplicationMapper.findByResearchNoAndSeq(researchNo, researchAppSeq);
            if (savedApplication != null) {
                savedApplication.setAppEmail(trimToNull(emailAddress));
                legacyApplicationSearchIndexMapper.upsert(savedApplication);
                legacyMatchingService.indexApplicationIfIdle(savedApplication);
            }
        } catch (Exception ex) {
            log.warn(
                    "Failed to index submitted legacy application. researchNo={}, researchAppSeq={}",
                    researchNo,
                    researchAppSeq,
                    ex
            );
        }
    }

    private void saveStructuredExtraAnswers(
            Long researchNo,
            Long researchAppSeq,
            List<ApplicationFormNoticeItem> additionalItems,
            List<String> normalizedExtraAnswers,
            String selectedExtraGroup,
            String rawAnswerText
    ) {
        String selectedGroup = resolveSelectedGroup(additionalItems, selectedExtraGroup);
        if (selectedGroup != null) {
            String safeSelectedGroup = limitLength(selectedGroup, STRUCTURED_QUESTION_GROUP_MAX_LENGTH);
            LegacyApplicationExtraAnswer marker = new LegacyApplicationExtraAnswer();
            marker.setResearchNo(researchNo);
            marker.setResearchAppSeq(researchAppSeq);
            marker.setAnswerOrder(0);
            marker.setQuestionGroup(safeSelectedGroup);
            marker.setQuestionLabel(LegacyApplicationExtraAnswerFormatter.GROUP_MARKER_LABEL);
            marker.setAnswerText(safeSelectedGroup);
            marker.setRawAnswerText(rawAnswerText);
            legacyApplicationExtraAnswerMapper.insert(marker);
        }
        for (int index = 0; index < Math.min(additionalItems.size(), normalizedExtraAnswers.size()); index++) {
            String answerText = trimToNull(normalizedExtraAnswers.get(index));
            if (answerText == null) {
                continue;
            }
            LegacyApplicationExtraAnswer answer = new LegacyApplicationExtraAnswer();
            answer.setResearchNo(researchNo);
            answer.setResearchAppSeq(researchAppSeq);
            answer.setAnswerOrder(index + 1);
            answer.setQuestionGroup(limitLength(additionalItems.get(index).groupLabel(), STRUCTURED_QUESTION_GROUP_MAX_LENGTH));
            answer.setQuestionLabel(limitLength(additionalItems.get(index).label(), STRUCTURED_QUESTION_LABEL_MAX_LENGTH));
            answer.setAnswerText(answerText);
            answer.setRawAnswerText(rawAnswerText);
            legacyApplicationExtraAnswerMapper.insert(answer);
        }
    }

    private String resolveSelectedGroup(List<ApplicationFormNoticeItem> additionalItems, String selectedExtraGroup) {
        String selectedGroup = trimToNull(selectedExtraGroup);
        if (selectedGroup != null) {
            return selectedGroup;
        }
        List<String> groups = additionalItems.stream()
                .map(ApplicationFormNoticeItem::groupLabel)
                .filter(group -> group != null && !group.isBlank())
                .distinct()
                .toList();
        return groups.size() == 1 ? groups.get(0) : null;
    }

    private ResearchApplication toResearchApplication(
            Long researchNo,
            Long researchAppSeq,
            PublicApplicationForm form,
            List<String> normalizedExtraAnswers
    ) {
        ResearchApplication application = new ResearchApplication();
        application.setResearchNo(researchNo);
        application.setResearchAppSeq(researchAppSeq);
        application.setAppName(trimToNull(form.getApplicantName()));
        application.setAppSex(toOldSexCode(form.getGenderCode()));
        application.setAppBirth(toOldBirthDate(form.getBirthDate()));
        application.setAppAge(trimToNull(form.getAgeText()));
        application.setAppJob(trimToNull(form.getJobText()));
        application.setAppCompany(trimToNull(form.getOrganizationText()));
        application.setAppHphone(normalizePhoneForDisplay(form.getMobilePhone()));
        application.setAppTele(normalizePhoneForDisplay(form.getTelPhone()));
        application.setAppAddr(trimToNull(form.getAddress()));
        application.setAddComment(buildRawAdditionalAnswer(normalizedExtraAnswers));
        application.setAttendResearch(trimToNull(form.getPriorResearchText()));
        application.setProvideYn("N");
        return application;
    }

    private String toOldBirthDate(LocalDate birthDate) {
        return birthDate == null ? null : birthDate.format(BIRTH_DATE);
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
        if (!Boolean.TRUE.equals(form.getProvideYn())) {
            fieldErrors.put("provideYnAccepted", "개인정보 수집 및 이용 동의가 필요합니다.");
        }
        if (!form.isFutureRecruitmentChannelAccepted()) {
            fieldErrors.put(
                    "futureRecruitmentChannelAccepted",
                    "향후 모집에 동의하는 경우 SMS 또는 이메일 중 최소 한 가지 수신 방법을 선택해 주세요."
            );
        }
        String applicantNameError = validateApplicantName(form.getApplicantName());
        if (applicantNameError != null) {
            fieldErrors.put("applicantName", applicantNameError);
        }
        if (toOldSexCode(form.getGenderCode()) == null) {
            fieldErrors.put("genderCode", "성별을 입력해 주세요.");
        }
        if (form.getBirthDate() == null || form.getBirthDate().isAfter(LocalDate.now())) {
            fieldErrors.put("birthDate", "생년월일을 입력해 주세요.");
        }
        if (trimToNull(form.getAgeText()) == null) {
            fieldErrors.put("ageText", "나이를 입력해 주세요.");
        }
        if (trimToNull(form.getJobText()) == null) {
            fieldErrors.put("jobText", "직업을 입력해 주세요.");
        }
        if (trimToNull(form.getOrganizationText()) == null) {
            fieldErrors.put("organizationText", "회사/학교명을 입력해 주세요.");
        }
        String normalizedMobile = protectionService.normalizePhone(form.getMobilePhone());
        if (normalizedMobile == null || normalizedMobile.length() < 10) {
            fieldErrors.put("mobilePhone", "올바른 휴대폰 번호를 입력해 주세요.");
        }
        if (trimToNull(form.getAddress()) == null) {
            fieldErrors.put("address", "주소를 입력해 주세요.");
        }
        if (trimToNull(form.getEmailAddress()) == null) {
            fieldErrors.put("emailAddress", "이메일 주소를 입력해 주세요.");
        }
        rejectIfTooLong(fieldErrors, "ageText", form.getAgeText(), OLD_APP_AGE_MAX_LENGTH, "나이는 " + OLD_APP_AGE_MAX_LENGTH + "자 이하로 입력해 주세요.");
        rejectIfTooLong(fieldErrors, "jobText", form.getJobText(), OLD_APP_JOB_MAX_LENGTH, "직업은 " + OLD_APP_JOB_MAX_LENGTH + "자 이하로 입력해 주세요.");
        rejectIfTooLong(fieldErrors, "organizationText", form.getOrganizationText(), OLD_APP_COMPANY_MAX_LENGTH, "회사/학교명은 " + OLD_APP_COMPANY_MAX_LENGTH + "자 이하로 입력해 주세요.");
        rejectIfTooLong(fieldErrors, "mobilePhone", normalizePhoneForDisplay(form.getMobilePhone()), OLD_APP_PHONE_MAX_LENGTH, "휴대폰 번호는 " + OLD_APP_PHONE_MAX_LENGTH + "자 이하로 입력해 주세요.");
        rejectIfTooLong(fieldErrors, "telPhone", normalizePhoneForDisplay(form.getTelPhone()), OLD_APP_PHONE_MAX_LENGTH, "전화번호는 " + OLD_APP_PHONE_MAX_LENGTH + "자 이하로 입력해 주세요.");
        rejectIfTooLong(fieldErrors, "address", form.getAddress(), OLD_APP_ADDR_MAX_LENGTH, "주소는 " + OLD_APP_ADDR_MAX_LENGTH + "자 이하로 입력해 주세요.");
        rejectIfTooLong(fieldErrors, "priorResearchText", form.getPriorResearchText(), OLD_ATTEND_RESEARCH_MAX_LENGTH, "최근 참여 이력은 " + OLD_ATTEND_RESEARCH_MAX_LENGTH + "자 이하로 입력해 주세요.");
    }

    private List<String> validateAdditionalAnswers(
            List<ApplicationFormNoticeItem> additionalItems,
            PublicApplicationForm form,
            Map<String, String> fieldErrors
    ) {
        if (additionalItems.isEmpty()) {
            return List.of();
        }
        List<String> groups = additionalItems.stream()
                .map(ApplicationFormNoticeItem::groupLabel)
                .filter(group -> group != null && !group.isBlank())
                .distinct()
                .toList();
        String selectedGroup = trimToNull(form.getSelectedExtraGroup());
        if (groups.size() > 1 && (selectedGroup == null || !groups.contains(selectedGroup))) {
            fieldErrors.put("selectedExtraGroup", "답변할 추가기재사항 그룹을 선택해 주세요.");
        }
        List<String> extraAnswers = form.getExtraAnswers();
        List<String> normalizedAnswers = new ArrayList<>();
        for (int index = 0; index < additionalItems.size(); index++) {
            if (!isApplicableAdditionalItem(additionalItems.get(index), groups, selectedGroup)) {
                normalizedAnswers.add("");
                continue;
            }
            String value = extraAnswers != null && extraAnswers.size() > index ? trimToNull(extraAnswers.get(index)) : null;
            if (value == null) {
                fieldErrors.put("extraAnswers[" + index + "]", "추가기재사항을 입력해 주세요.");
                normalizedAnswers.add("");
                continue;
            }
            if (value.length() > OLD_ADD_COMMENT_SAFE_MAX_LENGTH) {
                fieldErrors.put("extraAnswers[" + index + "]", "추가기재사항 답변은 " + OLD_ADD_COMMENT_SAFE_MAX_LENGTH + "자 이하로 입력해 주세요.");
            }
            normalizedAnswers.add(value);
        }
        String rawAdditionalAnswer = buildRawAdditionalAnswer(normalizedAnswers);
        if (rawAdditionalAnswer != null && rawAdditionalAnswer.length() > OLD_ADD_COMMENT_SAFE_MAX_LENGTH) {
            fieldErrors.putIfAbsent("extraAnswers[0]", "추가기재사항 전체 답변이 너무 깁니다. 내용을 줄여서 입력해 주세요.");
        }
        return List.copyOf(normalizedAnswers);
    }

    private boolean isApplicableAdditionalItem(ApplicationFormNoticeItem item, List<String> groups, String selectedGroup) {
        if (groups.size() <= 1) {
            return true;
        }
        return selectedGroup != null && selectedGroup.equals(item.groupLabel());
    }

    private String buildRawAdditionalAnswer(List<String> normalizedExtraAnswers) {
        List<String> answers = normalizedExtraAnswers.stream()
                .map(this::trimToNull)
                .filter(value -> value != null)
                .toList();
        return answers.isEmpty() ? null : String.join(" / ", answers);
    }

    private String toOldSexCode(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        String compact = normalized.replace(" ", "").toUpperCase();
        return switch (compact) {
            case "1", "M", "MALE", "남", "남자", "남성" -> "1";
            case "2", "F", "FEMALE", "여", "여자", "여성" -> "2";
            default -> null;
        };
    }

    private String normalizePhoneForDisplay(String value) {
        String formatted = protectionService.formatPhoneForDisplay(value);
        return formatted == null ? trimToNull(value) : formatted;
    }

    private boolean isClosed(String closeDate) {
        LocalDate parsedCloseDate = parseCloseDate(closeDate);
        return parsedCloseDate != null && LocalDate.now().isAfter(parsedCloseDate);
    }

    private LocalDate parseCloseDate(String closeDate) {
        String value = trimToNull(closeDate);
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() < 8) {
            return null;
        }
        try {
            return LocalDate.parse(digits.substring(0, 8), DateTimeFormatter.BASIC_ISO_DATE);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String validateApplicantName(String value) {
        String name = trimToNull(value);
        if (name == null) {
            return "이름을 입력해주세요.";
        }
        String compact = name.replaceAll("\\s+", "");
        if (compact.length() < 2) {
            return "이름은 2자 이상 입력해주세요.";
        }
        if (name.length() > 30) {
            return "이름은 30자 이하로 입력해주세요.";
        }
        if (!name.matches(KOREAN_NAME_PATTERN) && !name.matches(ENGLISH_NAME_PATTERN)) {
            return "이름은 한글 또는 영문으로 2자 이상 입력해주세요.";
        }
        return null;
    }

    private void rejectIfTooLong(
            Map<String, String> fieldErrors,
            String field,
            String value,
            int maxLength,
            String message
    ) {
        String normalized = trimToNull(value);
        if (normalized != null && normalized.length() > maxLength) {
            fieldErrors.putIfAbsent(field, message);
        }
    }

    private String limitLength(String value, int maxLength) {
        String normalized = trimToNull(value);
        if (normalized == null || normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }
}
