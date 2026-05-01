package com.researchi.admin.scheduler.service;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.blacklist.service.BlacklistService;
import com.researchi.admin.job.domain.AdminJobMeta;
import com.researchi.admin.job.mapper.AdminJobMetaMapper;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.mailing.domain.AdminMailSendJob;
import com.researchi.admin.mailing.mapper.AdminMailSendJobMapper;
import com.researchi.admin.mailing.service.MailingService;
import com.researchi.admin.matching.service.MatchingService;
import com.researchi.admin.notification.service.NotificationService;
import com.researchi.admin.scheduler.config.SchedulerProperties;
import com.researchi.admin.scheduler.mapper.OperationsCleanupMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OperationsBatchService {

    private final SchedulerProperties schedulerProperties;
    private final AdminMailSendJobMapper adminMailSendJobMapper;
    private final MailingService mailingService;
    private final AdminJobMetaMapper adminJobMetaMapper;
    private final OperationsCleanupMapper operationsCleanupMapper;
    private final BlacklistService blacklistService;
    private final MatchingService matchingService;
    private final NotificationService notificationService;
    private final JobService jobService;

    public OperationsBatchService(
            SchedulerProperties schedulerProperties,
            AdminMailSendJobMapper adminMailSendJobMapper,
            MailingService mailingService,
            AdminJobMetaMapper adminJobMetaMapper,
            OperationsCleanupMapper operationsCleanupMapper,
            BlacklistService blacklistService,
            MatchingService matchingService,
            NotificationService notificationService,
            JobService jobService
    ) {
        this.schedulerProperties = schedulerProperties;
        this.adminMailSendJobMapper = adminMailSendJobMapper;
        this.mailingService = mailingService;
        this.adminJobMetaMapper = adminJobMetaMapper;
        this.operationsCleanupMapper = operationsCleanupMapper;
        this.blacklistService = blacklistService;
        this.matchingService = matchingService;
        this.notificationService = notificationService;
        this.jobService = jobService;
    }

    public boolean isEnabled() {
        return schedulerProperties.isEnabled();
    }

    public int runScheduledSendBatch() {
        int executed = 0;
        for (AdminMailSendJob sendJob : adminMailSendJobMapper.findDueScheduled(LocalDateTime.now())) {
            try {
                if (mailingService.executeScheduledSend(sendJob.getId())) {
                    executed++;
                }
            } catch (RuntimeException ignored) {
                // Continue processing later due jobs even if one scheduled mail job is broken.
            }
        }
        return executed;
    }

    public int runThresholdVerificationBatch() {
        int executed = 0;
        for (AdminJobMeta jobMeta : adminJobMetaMapper.findAll()) {
            try {
                if (mailingService.triggerThresholdAutomatically(jobMeta.getDocumentSrl())) {
                    executed++;
                }
            } catch (RuntimeException ignored) {
                // Keep later threshold jobs from being blocked by one broken job.
            }
        }
        return executed;
    }

    @Transactional("adminTransactionManager")
    public int runSixMonthCleanupBatch() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMonths(schedulerProperties.getRetentionMonths());
        int deleted = 0;
        deleted += jobService.permanentlyDeleteExpiredDeletedJobs(now);
        deleted += operationsCleanupMapper.deleteDuplicateLogsBefore(cutoff);
        deleted += operationsCleanupMapper.deleteBlacklistMatchLogsForExpiredApplications(cutoff);
        deleted += operationsCleanupMapper.deletePrivacyConsentsForExpiredApplications(cutoff);
        deleted += operationsCleanupMapper.deleteFormAnswersForExpiredApplications(cutoff);
        deleted += operationsCleanupMapper.deleteApplicationKeywordsForExpiredApplications(cutoff);
        deleted += operationsCleanupMapper.deleteNotificationLogsForExpiredApplications(cutoff);
        deleted += operationsCleanupMapper.deleteMailTargetsForExpiredApplications(cutoff);
        deleted += operationsCleanupMapper.deleteKeywordMatchTargetsForExpiredApplications(cutoff);
        deleted += operationsCleanupMapper.deleteApplicationsBefore(cutoff);
        return deleted;
    }

    @Transactional("adminTransactionManager")
    public int runBlacklistExpiryBatch() {
        return blacklistService.expireExpiredEntries(LocalDateTime.now());
    }

    @Transactional("adminTransactionManager")
    public int runKeywordMatchBatch() {
        int executed = 0;
        for (AdminJobMeta meta : adminJobMetaMapper.findEnabledRecruitingJobs()) {
            Long matchJobId = matchingService.runScheduled(meta.getDocumentSrl());
            AdminPrincipal scheduler = new AdminPrincipal(null, "scheduler", "", "Scheduler", "Y", null);
            notificationService.sendEmailNotifications(meta.getDocumentSrl(), matchJobId, scheduler, null);
            notificationService.sendSmsNotifications(meta.getDocumentSrl(), matchJobId, scheduler, null);
            executed++;
        }
        return executed;
    }
}
