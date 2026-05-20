package com.researchi.admin.scheduler.service;

import com.researchi.admin.legacy.research.service.LegacyResearchMailService;
import com.researchi.admin.legacy.matching.service.LegacyMatchingService;
import com.researchi.admin.mailing.domain.AdminMailSendJob;
import com.researchi.admin.mailing.mapper.AdminMailSendJobMapper;
import com.researchi.admin.scheduler.config.SchedulerProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class OperationsBatchService {

    private final SchedulerProperties schedulerProperties;
    private final AdminMailSendJobMapper adminMailSendJobMapper;
    private final LegacyResearchMailService legacyResearchMailService;
    private final LegacyMatchingService legacyMatchingService;

    public OperationsBatchService(
            SchedulerProperties schedulerProperties,
            AdminMailSendJobMapper adminMailSendJobMapper,
            LegacyResearchMailService legacyResearchMailService,
            LegacyMatchingService legacyMatchingService
    ) {
        this.schedulerProperties = schedulerProperties;
        this.adminMailSendJobMapper = adminMailSendJobMapper;
        this.legacyResearchMailService = legacyResearchMailService;
        this.legacyMatchingService = legacyMatchingService;
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
        return legacyMatchingService.cleanupMatchingLogsAfterClosedDeadline();
    }
}
