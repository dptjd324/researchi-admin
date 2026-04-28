package com.researchi.admin.matching.service;

import com.researchi.admin.application.domain.ApplicationRecord;
import com.researchi.admin.application.service.ApplicationService;
import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.keyword.domain.KeywordCandidate;
import com.researchi.admin.keyword.service.KeywordExtractionService;
import com.researchi.admin.matching.domain.AdminKeywordMatchJob;
import com.researchi.admin.matching.domain.AdminKeywordMatchTarget;
import com.researchi.admin.matching.domain.MatchingOverview;
import com.researchi.admin.matching.domain.MatchingTargetView;
import com.researchi.admin.matching.mapper.AdminKeywordMatchJobMapper;
import com.researchi.admin.matching.mapper.AdminKeywordMatchTargetMapper;
import com.researchi.admin.notification.domain.AdminNotificationLog;
import com.researchi.admin.notification.service.NotificationService;
import com.researchi.admin.publicform.service.PublicFormProtectionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MatchingService {

    private final KeywordExtractionService keywordExtractionService;
    private final AdminKeywordMatchJobMapper adminKeywordMatchJobMapper;
    private final AdminKeywordMatchTargetMapper adminKeywordMatchTargetMapper;
    private final ApplicationService applicationService;
    private final JobService jobService;
    private final NotificationService notificationService;
    private final AdminActionLogService adminActionLogService;
    private final PublicFormProtectionService protectionService;

    public MatchingService(
            KeywordExtractionService keywordExtractionService,
            AdminKeywordMatchJobMapper adminKeywordMatchJobMapper,
            AdminKeywordMatchTargetMapper adminKeywordMatchTargetMapper,
            ApplicationService applicationService,
            JobService jobService,
            NotificationService notificationService,
            AdminActionLogService adminActionLogService,
            PublicFormProtectionService protectionService
    ) {
        this.keywordExtractionService = keywordExtractionService;
        this.adminKeywordMatchJobMapper = adminKeywordMatchJobMapper;
        this.adminKeywordMatchTargetMapper = adminKeywordMatchTargetMapper;
        this.applicationService = applicationService;
        this.jobService = jobService;
        this.notificationService = notificationService;
        this.adminActionLogService = adminActionLogService;
        this.protectionService = protectionService;
    }

    public MatchingOverview getOverview(Long documentSrl, Long selectedMatchJobId) {
        jobService.getJob(documentSrl);
        List<String> jobKeywords = keywordExtractionService.getJobKeywords(documentSrl).stream()
                .map(KeywordCandidate::keyword)
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .toList();
        List<AdminKeywordMatchJob> matchJobs = adminKeywordMatchJobMapper.findByDocumentSrl(documentSrl);
        Long activeMatchJobId = selectedMatchJobId != null
                ? selectedMatchJobId
                : matchJobs.stream().findFirst().map(AdminKeywordMatchJob::getId).orElse(null);
        List<MatchingTargetView> targets = activeMatchJobId == null
                ? List.of()
                : adminKeywordMatchTargetMapper.findViewsByMatchJobId(activeMatchJobId).stream()
                .peek(this::populatePersonalInfoDisplay)
                .toList();
        List<AdminNotificationLog> notificationLogs = notificationService.getNotificationLogs(documentSrl);
        return new MatchingOverview(jobKeywords, matchJobs, targets, notificationLogs);
    }

    @Transactional("adminTransactionManager")
    public Long run(Long documentSrl, AdminPrincipal principal, HttpServletRequest request) {
        return runInternal(documentSrl, principal, request);
    }

    @Transactional("adminTransactionManager")
    public Long runScheduled(Long documentSrl) {
        return runInternal(documentSrl, new AdminPrincipal(null, "scheduler", "", "Scheduler", "Y", null), null);
    }

    private Long runInternal(Long documentSrl, AdminPrincipal principal, HttpServletRequest request) {
        List<KeywordCandidate> jobKeywords = keywordExtractionService.syncJobKeywords(documentSrl);
        Set<String> jobKeywordSet = jobKeywords.stream()
                .map(KeywordCandidate::normalized)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);

        AdminKeywordMatchJob matchJob = new AdminKeywordMatchJob();
        matchJob.setDocumentSrl(documentSrl);
        matchJob.setMatchStatus("RUNNING");
        matchJob.setMatchedCount(0);
        adminKeywordMatchJobMapper.insert(matchJob);

        if (jobKeywordSet.isEmpty()) {
            matchJob.setMatchStatus("NO_KEYWORDS");
            matchJob.setCompletedAt(LocalDateTime.now());
            adminKeywordMatchJobMapper.update(matchJob);
            logRun(principal, request, documentSrl, matchJob.getId(), 0);
            return matchJob.getId();
        }

        List<ApplicationRecord> eligibleApplications = applicationService.getApplications(null, null).stream()
                .filter(application -> !"Y".equals(application.getIsBlacklisted()))
                .filter(application -> !"BLOCKED".equals(application.getApplicationStatus()))
                .filter(application -> !"N".equals(application.getProvideYn()))
                .filter(application -> "Y".equals(application.getNotifyKeywordYn()))
                .filter(application -> !documentSrl.equals(application.getDocumentSrl()))
                .sorted(Comparator.comparing(ApplicationRecord::getAppliedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<Long> applicationIds = new ArrayList<>();
        for (ApplicationRecord application : eligibleApplications) {
            keywordExtractionService.syncApplicationKeywords(application.getId());
            applicationIds.add(application.getId());
        }
        Map<Long, List<KeywordCandidate>> keywordMap = keywordExtractionService.getApplicationKeywords(applicationIds);
        int matchedCount = 0;

        for (ApplicationRecord application : eligibleApplications) {
            List<KeywordCandidate> applicationKeywords = keywordMap.getOrDefault(application.getId(), List.of());
            List<String> matchedKeywords = intersect(jobKeywordSet, applicationKeywords);
            if (matchedKeywords.isEmpty()) {
                continue;
            }
            AdminKeywordMatchTarget target = new AdminKeywordMatchTarget();
            target.setMatchJobId(matchJob.getId());
            target.setApplicationId(application.getId());
            target.setMatchedKeyword(joinKeywords(matchedKeywords));
            target.setMatchScore(matchedKeywords.size());
            target.setNotifyEmailYn(application.getNotifyEmailYn());
            target.setNotifySmsYn(application.getNotifySmsYn());
            target.setNotifyStatus(hasNotificationChannel(application) ? "PENDING" : "NO_CHANNEL");
            adminKeywordMatchTargetMapper.insert(target);
            matchedCount++;
        }

        matchJob.setMatchStatus(matchedCount == 0 ? "NO_MATCHES" : "COMPLETED");
        matchJob.setMatchedCount(matchedCount);
        matchJob.setCompletedAt(LocalDateTime.now());
        adminKeywordMatchJobMapper.update(matchJob);
        logRun(principal, request, documentSrl, matchJob.getId(), matchedCount);
        return matchJob.getId();
    }

    private void logRun(AdminPrincipal principal, HttpServletRequest request, Long documentSrl, Long matchJobId, int matchedCount) {
        adminActionLogService.log(
                principal.getId(),
                "KEYWORD_MATCH_RUN",
                "JOB",
                String.valueOf(documentSrl),
                "Keyword match job #" + matchJobId + " completed with " + matchedCount + " matches.",
                request
        );
    }

    private List<String> intersect(Set<String> jobKeywordSet, List<KeywordCandidate> applicationKeywords) {
        LinkedHashMap<String, String> matched = new LinkedHashMap<>();
        for (KeywordCandidate candidate : applicationKeywords) {
            if (jobKeywordSet.contains(candidate.normalized())) {
                matched.put(candidate.normalized(), candidate.keyword());
            }
        }
        return List.copyOf(matched.values());
    }

    private String joinKeywords(List<String> matchedKeywords) {
        return matchedKeywords.stream().limit(8).reduce((left, right) -> left + ", " + right).orElse("");
    }

    private boolean hasNotificationChannel(ApplicationRecord application) {
        return "Y".equals(application.getNotifyEmailYn()) || "Y".equals(application.getNotifySmsYn());
    }

    private void populatePersonalInfoDisplay(MatchingTargetView target) {
        target.setEmailAddressDisplay(decryptOrFallback(target.getEmailAddressEnc(), target.getEmailAddressMasked()));
        target.setMobilePhoneDisplay(decryptOrFallback(target.getMobilePhoneEnc(), target.getMobilePhoneMasked()));
    }

    private String decryptOrFallback(String encryptedValue, String fallback) {
        if (encryptedValue == null || encryptedValue.isBlank()) {
            return fallback;
        }
        return protectionService.decrypt(encryptedValue);
    }
}
