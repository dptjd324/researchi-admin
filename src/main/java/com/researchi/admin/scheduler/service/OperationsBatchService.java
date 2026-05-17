package com.researchi.admin.scheduler.service;

import com.researchi.admin.legacy.research.service.LegacyResearchMailService;
import com.researchi.admin.legacy.matching.service.LegacyMatchingService;
import com.researchi.admin.mailing.domain.AdminMailSendJob;
import com.researchi.admin.mailing.mapper.AdminMailSendJobMapper;
import com.researchi.admin.scheduler.config.SchedulerProperties;
import com.researchi.admin.scheduler.mapper.OperationsCleanupMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OperationsBatchService {

    private final SchedulerProperties schedulerProperties;
    private final AdminMailSendJobMapper adminMailSendJobMapper;
    private final LegacyResearchMailService legacyResearchMailService;
    private final LegacyMatchingService legacyMatchingService;
    private final OperationsCleanupMapper operationsCleanupMapper;

    public OperationsBatchService(
            SchedulerProperties schedulerProperties,
            AdminMailSendJobMapper adminMailSendJobMapper,
            LegacyResearchMailService legacyResearchMailService,
            LegacyMatchingService legacyMatchingService,
            OperationsCleanupMapper operationsCleanupMapper
    ) {
        this.schedulerProperties = schedulerProperties;
        this.adminMailSendJobMapper = adminMailSendJobMapper;
        this.legacyResearchMailService = legacyResearchMailService;
        this.legacyMatchingService = legacyMatchingService;
        this.operationsCleanupMapper = operationsCleanupMapper;
    }

    public boolean isEnabled() {
        return schedulerProperties.isEnabled();
    }

    public int runScheduledSendBatch() {
        int executed = 0;
        for (AdminMailSendJob sendJob : adminMailSendJobMapper.findDueScheduled(LocalDateTime.now())) {
            if (!isLegacyScheduled(sendJob)) {
                continue;
            }
            try {
                if (legacyResearchMailService.executeScheduledSend(sendJob.getId())) {
                    executed++;
                }
            } catch (RuntimeException ignored) {
                // Continue processing later due jobs even if one scheduled mail job is broken.
            }
        }
        return executed;
    }

    private boolean isLegacyScheduled(AdminMailSendJob sendJob) {
        return sendJob != null
                && sendJob.getTriggerType() != null
                && sendJob.getTriggerType().startsWith("LEGACY_SCHEDULED");
    }

    public int runThresholdVerificationBatch() {
        int executed = 0;
        for (Long researchNo : legacyResearchMailService.getEnabledThresholdResearchNos()) {
            try {
                if (legacyResearchMailService.triggerThresholdAutomatically(researchNo)) {
                    executed++;
                }
            } catch (RuntimeException ignored) {
                // Keep legacy threshold jobs from blocking the existing threshold batch.
            }
        }
        for (Long ruleId : legacyResearchMailService.getEnabledThresholdRuleIds()) {
            try {
                if (legacyResearchMailService.triggerThresholdRuleAutomatically(ruleId)) {
                    executed++;
                }
            } catch (RuntimeException ignored) {
                // Keep additional legacy threshold rules from blocking the batch.
            }
        }
        return executed;
    }

    @Transactional("adminTransactionManager")
    public int runSixMonthCleanupBatch() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMonths(schedulerProperties.getRetentionMonths());
        int deleted = 0;
        deleted += operationsCleanupMapper.deleteDuplicateLogsBefore(cutoff);
        deleted += operationsCleanupMapper.deleteBlacklistMatchLogsForExpiredApplications(cutoff);
        deleted += operationsCleanupMapper.deletePrivacyConsentsForExpiredApplications(cutoff);
        deleted += operationsCleanupMapper.deleteFormAnswersForExpiredApplications(cutoff);
        deleted += operationsCleanupMapper.deleteApplicationKeywordsForExpiredApplications(cutoff);
        deleted += operationsCleanupMapper.deleteNotificationLogsForExpiredApplications(cutoff);
        deleted += operationsCleanupMapper.deleteMailTargetsForExpiredApplications(cutoff);
        deleted += operationsCleanupMapper.deleteKeywordMatchTargetsForExpiredApplications(cutoff);
        deleted += operationsCleanupMapper.deleteApplicationsBefore(cutoff);
        deleted += legacyMatchingService.cleanupMatchingLogsAfterClosedDeadline();
        return deleted;
    }

    @Transactional("adminTransactionManager")
    public int runBlacklistExpiryBatch() {
        return 0;
    }

    @Transactional("adminTransactionManager")
    public int runKeywordMatchBatch() {
        return 0;
    }
}
