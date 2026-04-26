package com.researchi.admin.scheduler.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OperationsScheduler {

    private final OperationsBatchService operationsBatchService;

    public OperationsScheduler(OperationsBatchService operationsBatchService) {
        this.operationsBatchService = operationsBatchService;
    }

    @Scheduled(cron = "${app.scheduler.scheduled-send-cron:0 * * * * *}")
    public void scheduledSend() {
        if (!operationsBatchService.isEnabled()) {
            return;
        }
        operationsBatchService.runScheduledSendBatch();
    }

    @Scheduled(cron = "${app.scheduler.threshold-cron:30 * * * * *}")
    public void thresholdVerification() {
        if (!operationsBatchService.isEnabled()) {
            return;
        }
        operationsBatchService.runThresholdVerificationBatch();
    }

    @Scheduled(cron = "${app.scheduler.cleanup-cron:0 0 3 * * *}")
    public void cleanup() {
        if (!operationsBatchService.isEnabled()) {
            return;
        }
        operationsBatchService.runSixMonthCleanupBatch();
    }

    @Scheduled(cron = "${app.scheduler.blacklist-expiry-cron:0 15 * * * *}")
    public void blacklistExpiry() {
        if (!operationsBatchService.isEnabled()) {
            return;
        }
        operationsBatchService.runBlacklistExpiryBatch();
    }

    @Scheduled(cron = "${app.scheduler.keyword-match-cron:0 30 * * * *}")
    public void keywordMatch() {
        if (!operationsBatchService.isEnabled()) {
            return;
        }
        operationsBatchService.runKeywordMatchBatch();
    }
}
