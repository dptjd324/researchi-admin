package com.researchi.admin.application.service;

import com.researchi.admin.application.domain.ApplicationAnswerItem;
import com.researchi.admin.application.domain.ApplicationDetail;
import com.researchi.admin.application.domain.ApplicationExtraAnswerItem;
import com.researchi.admin.application.domain.ApplicationJobCount;
import com.researchi.admin.application.domain.ApplicationJobFilter;
import com.researchi.admin.application.domain.ApplicationRecord;
import com.researchi.admin.application.mapper.AdminApplicationQueryMapper;
import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.job.domain.JobDetail;
import com.researchi.admin.job.domain.JobListItem;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.publicform.mapper.AdminJobApplicationExtraAnswerMapper;
import com.researchi.admin.publicform.service.PublicFormProtectionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ApplicationService {

    private static final List<String> ALLOWED_STATUSES = List.of(
            "RECEIVED",
            "REVIEWING",
            "APPROVED",
            "REJECTED",
            "BLOCKED"
    );

    private final AdminApplicationQueryMapper adminApplicationQueryMapper;
    private final JobService jobService;
    private final AdminActionLogService adminActionLogService;
    private final AdminJobApplicationExtraAnswerMapper adminJobApplicationExtraAnswerMapper;
    private final PublicFormProtectionService protectionService;

    public ApplicationService(
            AdminApplicationQueryMapper adminApplicationQueryMapper,
            JobService jobService,
            AdminActionLogService adminActionLogService,
            AdminJobApplicationExtraAnswerMapper adminJobApplicationExtraAnswerMapper,
            PublicFormProtectionService protectionService
    ) {
        this.adminApplicationQueryMapper = adminApplicationQueryMapper;
        this.jobService = jobService;
        this.adminActionLogService = adminActionLogService;
        this.adminJobApplicationExtraAnswerMapper = adminJobApplicationExtraAnswerMapper;
        this.protectionService = protectionService;
    }

    public List<ApplicationRecord> getApplications(Long documentSrl, String keyword) {
        List<ApplicationRecord> applications = documentSrl == null
                ? adminApplicationQueryMapper.findAll()
                : adminApplicationQueryMapper.findByDocumentSrl(documentSrl);
        Map<Long, JobListItem> jobsByDocumentSrl = jobsByDocumentSrl(applications);

        return applications.stream()
                .map(application -> enrich(application, jobsByDocumentSrl))
                .peek(this::populatePersonalInfoDisplay)
                .filter(application -> matchesKeyword(application, keyword))
                .sorted(Comparator.comparing(ApplicationRecord::getAppliedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
    }

    public int countApplications(Long documentSrl) {
        return adminApplicationQueryMapper.count(documentSrl);
    }

    public int countApplications(Long documentSrl, String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword == null) {
            return countApplications(documentSrl);
        }
        return adminApplicationQueryMapper.countSearch(documentSrl, normalizedKeyword, matchingJobDocumentSrls(normalizedKeyword));
    }

    public List<ApplicationRecord> getApplicationPage(Long documentSrl, int limit, int offset) {
        List<ApplicationRecord> page = adminApplicationQueryMapper.findPage(documentSrl, limit, offset);
        Map<Long, JobListItem> jobsByDocumentSrl = jobsByDocumentSrl(page);
        return page.stream()
                .map(application -> enrich(application, jobsByDocumentSrl))
                .peek(this::populatePersonalInfoDisplay)
                .toList();
    }

    public List<ApplicationRecord> getApplicationPage(Long documentSrl, String keyword, int limit, int offset) {
        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword == null) {
            return getApplicationPage(documentSrl, limit, offset);
        }
        List<ApplicationRecord> page = adminApplicationQueryMapper.findSearchPage(
                        documentSrl,
                        normalizedKeyword,
                        matchingJobDocumentSrls(normalizedKeyword),
                        limit,
                        offset
                );
        Map<Long, JobListItem> jobsByDocumentSrl = jobsByDocumentSrl(page);
        return page.stream()
                .map(application -> enrich(application, jobsByDocumentSrl))
                .peek(this::populatePersonalInfoDisplay)
                .toList();
    }

    public List<ApplicationJobFilter> getJobFilters() {
        List<ApplicationJobCount> counts = adminApplicationQueryMapper.countByDocumentSrl();
        Map<Long, JobListItem> jobsByDocumentSrl = jobsByDocumentSrlFromDocumentSrls(
                counts.stream().map(ApplicationJobCount::getDocumentSrl).toList()
        );
        return counts.stream()
                .map(count -> new ApplicationJobFilter(
                        count.getDocumentSrl(),
                        resolveJobTitle(count.getDocumentSrl(), jobsByDocumentSrl),
                        count.getApplicationCount()
                ))
                .sorted(Comparator.comparing(ApplicationJobFilter::documentSrl).reversed())
                .toList();
    }

    public ApplicationDetail getApplicationDetail(Long id) {
        ApplicationRecord application = adminApplicationQueryMapper.findById(id);
        if (application == null) {
            throw new IllegalArgumentException("지원서를 찾을 수 없습니다.");
        }

        JobDetail jobDetail = jobService.getJob(application.getDocumentSrl());
        application.setJobTitle(jobDetail.getDocument().getTitle());
        application.setJobType(jobDetail.getJobType());
        populatePersonalInfoDisplay(application);

        List<ApplicationAnswerItem> answers = adminApplicationQueryMapper.findAnswersByApplicationId(id).stream()
                .map(this::normalizeAnswer)
                .sorted(Comparator.comparing(ApplicationAnswerItem::getFieldOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(ApplicationAnswerItem::getFieldId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();

        List<ApplicationExtraAnswerItem> extraAnswers = adminJobApplicationExtraAnswerMapper.findByApplicationId(id).stream()
                .map(item -> {
                    ApplicationExtraAnswerItem extraAnswerItem = new ApplicationExtraAnswerItem();
                    extraAnswerItem.setAnswerOrder(item.getAnswerOrder());
                    extraAnswerItem.setQuestionLabel(item.getQuestionLabel());
                    extraAnswerItem.setAnswerText(item.getAnswerText());
                    return extraAnswerItem;
                })
                .toList();

        return new ApplicationDetail(application, jobDetail, answers, extraAnswers);
    }

    public List<String> getAllowedStatuses() {
        return ALLOWED_STATUSES;
    }

    @Transactional("adminTransactionManager")
    public void updateStatus(
            Long id,
            String applicationStatus,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        String normalizedStatus = normalizeStatus(applicationStatus);
        ApplicationRecord application = adminApplicationQueryMapper.findById(id);
        if (application == null) {
            throw new IllegalArgumentException("지원서를 찾을 수 없습니다.");
        }
        if (normalizedStatus.equals(application.getApplicationStatus())) {
            return;
        }

        int updated = adminApplicationQueryMapper.updateStatus(id, normalizedStatus);
        if (updated != 1) {
            throw new IllegalStateException("지원서 상태를 변경하지 못했습니다.");
        }

        adminActionLogService.log(
                principal.getId(),
                "APPLICATION_STATUS_UPDATE",
                "APPLICATION",
                String.valueOf(id),
                "지원서 상태 변경: " + normalizedStatus,
                request
        );
    }

    @Transactional("adminTransactionManager")
    public void clearBlacklist(
            Long id,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        ApplicationRecord application = adminApplicationQueryMapper.findById(id);
        if (application == null) {
            throw new IllegalArgumentException("지원서를 찾을 수 없습니다.");
        }
        if (!"Y".equals(application.getIsBlacklisted())) {
            return;
        }

        int updated = adminApplicationQueryMapper.clearBlacklistState(id);
        if (updated != 1) {
            throw new IllegalStateException("지원서 블랙리스트 상태를 해제하지 못했습니다.");
        }

        adminActionLogService.log(
                principal.getId(),
                "APPLICATION_BLACKLIST_CLEAR",
                "APPLICATION",
                String.valueOf(id),
                "지원서 블랙리스트 해제",
                request
        );
    }

    private Map<Long, JobListItem> jobsByDocumentSrl(List<ApplicationRecord> applications) {
        return jobsByDocumentSrlFromDocumentSrls(
                applications.stream()
                        .map(ApplicationRecord::getDocumentSrl)
                        .toList()
        );
    }

    private Map<Long, JobListItem> jobsByDocumentSrlFromDocumentSrls(List<Long> documentSrls) {
        return jobService.getJobsByDocumentSrls(documentSrls).stream()
                .collect(LinkedHashMap::new, (map, job) -> map.put(job.getDocumentSrl(), job), Map::putAll);
    }

    private ApplicationRecord enrich(ApplicationRecord application, Map<Long, JobListItem> jobsByDocumentSrl) {
        JobListItem job = jobsByDocumentSrl.get(application.getDocumentSrl());
        if (job == null) {
            application.setJobTitle("Job #" + application.getDocumentSrl());
            application.setJobType(null);
            return application;
        }
        application.setJobTitle(job.getTitle());
        application.setJobType(job.getJobType());
        return application;
    }

    private void populatePersonalInfoDisplay(ApplicationRecord application) {
        application.setMobilePhoneDisplay(decryptOrFallback(application.getMobilePhoneEnc(), application.getMobilePhoneMasked()));
        application.setTelPhoneDisplay(decryptOrFallback(application.getTelPhoneEnc(), application.getTelPhoneMasked()));
        application.setEmailAddressDisplay(decryptOrFallback(application.getEmailAddressEnc(), application.getEmailAddressMasked()));
        application.setAddressDisplay(decryptOrFallback(application.getAddressEnc(), application.getAddressMasked()));
    }

    private String decryptOrFallback(String encryptedValue, String fallback) {
        if (encryptedValue == null || encryptedValue.isBlank()) {
            return fallback;
        }
        return protectionService.decrypt(encryptedValue);
    }

    private boolean matchesKeyword(ApplicationRecord application, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalizedKeyword = keyword.trim().toLowerCase(Locale.ROOT);
        return contains(application.getApplicantName(), normalizedKeyword)
                || contains(application.getMobilePhoneDisplay(), normalizedKeyword)
                || contains(application.getEmailAddressDisplay(), normalizedKeyword)
                || contains(application.getRegionText(), normalizedKeyword)
                || contains(application.getJobText(), normalizedKeyword)
                || contains(application.getOrganizationText(), normalizedKeyword)
                || contains(application.getApplicationStatus(), normalizedKeyword)
                || contains(application.getDeliveryStatus(), normalizedKeyword)
                || contains(application.getJobTitle(), normalizedKeyword);
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private List<Long> matchingJobDocumentSrls(String normalizedKeyword) {
        if (normalizedKeyword == null) {
            return List.of();
        }
        return jobService.findApplicationDocumentSrlsByTitle(normalizedKeyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String resolveJobTitle(Long documentSrl, Map<Long, JobListItem> jobsByDocumentSrl) {
        JobListItem job = jobsByDocumentSrl.get(documentSrl);
        return job == null ? "Job #" + documentSrl : job.getTitle();
    }

    private ApplicationAnswerItem normalizeAnswer(ApplicationAnswerItem item) {
        item.setDisplayAnswer(toDisplayAnswer(item.getAnswerText(), item.getAnswerJson()));
        return item;
    }

    private String toDisplayAnswer(String answerText, String answerJson) {
        if (answerJson == null || answerJson.isBlank()) {
            return answerText;
        }
        String trimmed = answerJson.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return answerText;
        }
        String body = trimmed.substring(1, trimmed.length() - 1).trim();
        if (body.isEmpty()) {
            return answerText;
        }

        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean escaping = false;
        for (int index = 0; index < body.length(); index++) {
            char currentChar = body.charAt(index);
            if (escaping) {
                current.append(currentChar);
                escaping = false;
                continue;
            }
            if (currentChar == '\\') {
                escaping = true;
                continue;
            }
            if (currentChar == '"') {
                inQuotes = !inQuotes;
                if (!inQuotes) {
                    values.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }
            if (inQuotes) {
                current.append(currentChar);
            }
        }
        return values.isEmpty() ? answerText : String.join(", ", values);
    }

    private String normalizeStatus(String applicationStatus) {
        String normalized = applicationStatus == null ? "" : applicationStatus.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("지원하지 않는 지원서 상태입니다.");
        }
        return normalized;
    }
}
