package com.researchi.admin.search.service;

import com.researchi.admin.application.domain.ApplicationRecord;
import com.researchi.admin.application.service.ApplicationService;
import com.researchi.admin.auth.mapper.AdminActionLogMapper;
import com.researchi.admin.job.domain.JobListItem;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.log.domain.ActionLogItem;
import com.researchi.admin.mailing.domain.AdminMailSendJob;
import com.researchi.admin.mailing.mapper.AdminMailSendJobMapper;
import com.researchi.admin.notification.domain.AdminNotificationLog;
import com.researchi.admin.notification.mapper.AdminNotificationLogMapper;
import com.researchi.admin.search.domain.AdminSearchLog;
import com.researchi.admin.search.domain.PeriodSearchForm;
import com.researchi.admin.search.domain.PeriodSearchResult;
import com.researchi.admin.search.mapper.AdminSearchLogMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class PeriodSearchService {

    private static final List<String> SUPPORTED_SCOPES = List.of("APPLICATION", "MAIL", "ACTION", "NOTIFICATION");

    private final ApplicationService applicationService;
    private final AdminMailSendJobMapper adminMailSendJobMapper;
    private final AdminActionLogMapper adminActionLogMapper;
    private final AdminNotificationLogMapper adminNotificationLogMapper;
    private final AdminSearchLogMapper adminSearchLogMapper;
    private final JobService jobService;

    public PeriodSearchService(
            ApplicationService applicationService,
            AdminMailSendJobMapper adminMailSendJobMapper,
            AdminActionLogMapper adminActionLogMapper,
            AdminNotificationLogMapper adminNotificationLogMapper,
            AdminSearchLogMapper adminSearchLogMapper,
            JobService jobService
    ) {
        this.applicationService = applicationService;
        this.adminMailSendJobMapper = adminMailSendJobMapper;
        this.adminActionLogMapper = adminActionLogMapper;
        this.adminNotificationLogMapper = adminNotificationLogMapper;
        this.adminSearchLogMapper = adminSearchLogMapper;
        this.jobService = jobService;
    }

    public PeriodSearchForm defaultForm() {
        return new PeriodSearchForm();
    }

    public List<JobListItem> getJobOptions() {
        return jobService.getJobs();
    }

    public List<String> getScopeOptions() {
        return SUPPORTED_SCOPES;
    }

    public List<String> getStatusOptions(String scope) {
        String normalizedScope = normalizeScope(scope);
        return switch (normalizedScope) {
            case "APPLICATION" -> applicationService.getAllowedStatuses();
            case "MAIL" -> List.of("PENDING", "SENT", "FAILED", "SCHEDULED", "NO_TARGETS");
            case "ACTION" -> List.of();
            case "NOTIFICATION" -> List.of("SENT", "FAILED", "SKIPPED_DUPLICATE");
            default -> List.of();
        };
    }

    @Transactional("adminTransactionManager")
    public PeriodSearchResult search(PeriodSearchForm form) {
        String scope = normalizeScope(form.getScope());
        DateRange dateRange = resolveDateRange(form);

        return switch (scope) {
            case "APPLICATION" -> searchApplications(form, scope, dateRange);
            case "MAIL" -> searchMail(form, scope, dateRange);
            case "ACTION" -> searchActions(form, scope, dateRange);
            case "NOTIFICATION" -> searchNotifications(form, scope, dateRange);
            default -> throw new IllegalArgumentException("지원하지 않는 검색 범위입니다.");
        };
    }

    private PeriodSearchResult searchApplications(PeriodSearchForm form, String scope, DateRange dateRange) {
        List<ApplicationRecord> results = applicationService.getApplications(form.getDocumentSrl(), form.getKeyword()).stream()
                .filter(item -> matchesStatus(item.getApplicationStatus(), form.getStatus()))
                .filter(item -> matchesDate(item.getAppliedAt(), dateRange))
                .toList();
        logSearch(scope, form, results.size(), dateRange);
        return new PeriodSearchResult(
                scope,
                "지원 시각",
                results.size(),
                resolveSingleDocumentSrl(results.stream().map(ApplicationRecord::getDocumentSrl).toList()),
                results,
                List.of(),
                List.of(),
                List.of()
        );
    }

    private PeriodSearchResult searchMail(PeriodSearchForm form, String scope, DateRange dateRange) {
        Map<Long, String> titlesByDocumentSrl = jobTitles();
        List<AdminMailSendJob> results = adminMailSendJobMapper.findAll().stream()
                .peek(job -> job.setJobTitle(titlesByDocumentSrl.getOrDefault(job.getDocumentSrl(), "Job #" + job.getDocumentSrl())))
                .filter(job -> matchesDocumentSrl(job.getDocumentSrl(), form.getDocumentSrl()))
                .filter(job -> matchesStatus(job.getSendStatus(), form.getStatus()))
                .filter(job -> matchesKeyword(job, form.getKeyword()))
                .filter(job -> matchesDate(resolveMailAt(job), dateRange))
                .toList();
        logSearch(scope, form, results.size(), dateRange);
        return new PeriodSearchResult(
                scope,
                "발송 시각",
                results.size(),
                resolveSingleDocumentSrl(results.stream().map(AdminMailSendJob::getDocumentSrl).toList()),
                List.of(),
                results,
                List.of(),
                List.of()
        );
    }

    private PeriodSearchResult searchActions(PeriodSearchForm form, String scope, DateRange dateRange) {
        List<ActionLogItem> results = adminActionLogMapper.findAll().stream()
                .filter(item -> matchesKeyword(item, form.getKeyword()))
                .filter(item -> matchesStatus(item.getActionType(), form.getStatus()))
                .filter(item -> matchesDate(item.getCreatedAt(), dateRange))
                .toList();
        logSearch(scope, form, results.size(), dateRange);
        return new PeriodSearchResult(
                scope,
                "생성 시각",
                results.size(),
                null,
                List.of(),
                List.of(),
                results,
                List.of()
        );
    }

    private PeriodSearchResult searchNotifications(PeriodSearchForm form, String scope, DateRange dateRange) {
        List<AdminNotificationLog> results = adminNotificationLogMapper.findAll().stream()
                .filter(item -> matchesDocumentSrl(item.getDocumentSrl(), form.getDocumentSrl()))
                .filter(item -> matchesStatus(item.getSendStatus(), form.getStatus()))
                .filter(item -> matchesKeyword(item, form.getKeyword()))
                .filter(item -> matchesDate(item.getCreatedAt(), dateRange))
                .toList();
        logSearch(scope, form, results.size(), dateRange);
        return new PeriodSearchResult(
                scope,
                "알림 시각",
                results.size(),
                resolveSingleDocumentSrl(results.stream().map(AdminNotificationLog::getDocumentSrl).toList()),
                List.of(),
                List.of(),
                List.of(),
                results
        );
    }

    private void logSearch(String scope, PeriodSearchForm form, int resultCount, DateRange dateRange) {
        AdminSearchLog log = new AdminSearchLog();
        log.setSearchType(scope);
        log.setKeywordText(blankToNull(form.getKeyword()));
        log.setConditionJson(buildConditionJson(form, scope, dateRange));
        log.setResultCount(resultCount);
        adminSearchLogMapper.insert(log);
    }

    private Map<Long, String> jobTitles() {
        Map<Long, String> titlesByDocumentSrl = new LinkedHashMap<>();
        for (JobListItem job : jobService.getJobs()) {
            titlesByDocumentSrl.put(job.getDocumentSrl(), job.getTitle());
        }
        return titlesByDocumentSrl;
    }

    private boolean matchesKeyword(ApplicationRecord item, String keyword) {
        String normalized = normalizeKeyword(keyword);
        if (normalized == null) {
            return true;
        }
        return contains(item.getApplicantName(), normalized)
                || contains(item.getMobilePhoneMasked(), normalized)
                || contains(item.getEmailAddressMasked(), normalized)
                || contains(item.getRegionText(), normalized)
                || contains(item.getAgeText(), normalized)
                || contains(item.getJobText(), normalized)
                || contains(item.getOrganizationText(), normalized)
                || contains(item.getApplicationStatus(), normalized)
                || contains(item.getJobTitle(), normalized);
    }

    private boolean matchesKeyword(AdminMailSendJob item, String keyword) {
        String normalized = normalizeKeyword(keyword);
        if (normalized == null) {
            return true;
        }
        return contains(item.getJobTitle(), normalized)
                || contains(item.getTemplateName(), normalized)
                || contains(item.getSendType(), normalized)
                || contains(item.getTriggerType(), normalized)
                || contains(item.getSendStatus(), normalized);
    }

    private boolean matchesKeyword(ActionLogItem item, String keyword) {
        String normalized = normalizeKeyword(keyword);
        if (normalized == null) {
            return true;
        }
        return contains(item.getLoginId(), normalized)
                || contains(item.getUserName(), normalized)
                || contains(item.getActionType(), normalized)
                || contains(item.getTargetType(), normalized)
                || contains(item.getTargetId(), normalized)
                || contains(item.getActionDetail(), normalized)
                || contains(item.getIpAddress(), normalized);
    }

    private boolean matchesKeyword(AdminNotificationLog item, String keyword) {
        String normalized = normalizeKeyword(keyword);
        if (normalized == null) {
            return true;
        }
        return contains(item.getTargetAddressMasked(), normalized)
                || contains(item.getChannelType(), normalized)
                || contains(item.getKeywordSummary(), normalized)
                || contains(item.getSendStatus(), normalized)
                || contains(item.getFailReason(), normalized);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private boolean matchesDocumentSrl(Long value, Long expected) {
        return expected == null || expected.equals(value);
    }

    private boolean matchesStatus(String value, String expected) {
        return expected == null || expected.isBlank() || expected.equalsIgnoreCase(value);
    }

    private boolean matchesDate(LocalDateTime value, DateRange range) {
        if (value == null) {
            return false;
        }
        if (range.from() != null && value.toLocalDate().isBefore(range.from())) {
            return false;
        }
        return range.to() == null || !value.toLocalDate().isAfter(range.to());
    }

    private LocalDateTime resolveMailAt(AdminMailSendJob job) {
        if (job.getSentAt() != null) {
            return job.getSentAt();
        }
        if (job.getScheduledAt() != null) {
            return job.getScheduledAt();
        }
        return job.getCreatedAt();
    }

    private String normalizeScope(String scope) {
        String normalized = scope == null ? "APPLICATION" : scope.trim().toUpperCase(Locale.ROOT);
        return SUPPORTED_SCOPES.contains(normalized) ? normalized : "APPLICATION";
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }

    private DateRange resolveDateRange(PeriodSearchForm form) {
        LocalDate today = LocalDate.now();
        String preset = form.getDatePreset() == null ? "TODAY" : form.getDatePreset().trim().toUpperCase(Locale.ROOT);
        return switch (preset) {
            case "THIS_WEEK" -> {
                LocalDate from = today.with(DayOfWeek.MONDAY);
                yield new DateRange(from, from.plusDays(6));
            }
            case "SPECIFIC_DAY" -> {
                LocalDate specific = form.getSpecificDate() == null ? today : form.getSpecificDate();
                yield new DateRange(specific, specific);
            }
            case "CUSTOM" -> new DateRange(form.getDateFrom(), form.getDateTo());
            case "TODAY" -> new DateRange(today, today);
            default -> new DateRange(today, today);
        };
    }

    private Long resolveSingleDocumentSrl(List<Long> documentSrls) {
        Set<Long> values = new LinkedHashSet<>();
        for (Long value : documentSrls) {
            if (value != null) {
                values.add(value);
            }
        }
        return values.size() == 1 ? values.iterator().next() : null;
    }

    private String buildConditionJson(PeriodSearchForm form, String scope, DateRange dateRange) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        appendJsonField(builder, "scope", scope);
        appendJsonField(builder, "keyword", blankToNull(form.getKeyword()));
        appendJsonField(builder, "documentSrl", form.getDocumentSrl() == null ? null : String.valueOf(form.getDocumentSrl()));
        appendJsonField(builder, "status", blankToNull(form.getStatus()));
        appendJsonField(builder, "datePreset", form.getDatePreset());
        appendJsonField(builder, "specificDate", form.getSpecificDate() == null ? null : form.getSpecificDate().toString());
        appendJsonField(builder, "dateFrom", dateRange.from() == null ? null : dateRange.from().toString());
        appendJsonField(builder, "dateTo", dateRange.to() == null ? null : dateRange.to().toString());
        builder.append("}");
        return builder.toString();
    }

    private void appendJsonField(StringBuilder builder, String key, String value) {
        if (builder.length() > 1) {
            builder.append(",");
        }
        builder.append("\"").append(escapeJson(key)).append("\":");
        if (value == null) {
            builder.append("null");
            return;
        }
        builder.append("\"").append(escapeJson(value)).append("\"");
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record DateRange(LocalDate from, LocalDate to) {
    }
}
