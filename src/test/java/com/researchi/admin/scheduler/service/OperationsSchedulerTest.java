package com.researchi.admin.scheduler.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationsSchedulerTest {

    @Mock
    private OperationsBatchService operationsBatchService;

    @InjectMocks
    private OperationsScheduler operationsScheduler;

    @Test
    void scheduledMethodsDoNothingWhenSchedulerIsDisabled() {
        when(operationsBatchService.isEnabled()).thenReturn(false);

        operationsScheduler.scheduledSend();
        operationsScheduler.thresholdVerification();
        operationsScheduler.cleanup();
        operationsScheduler.blacklistExpiry();
        operationsScheduler.keywordMatch();

        verify(operationsBatchService, never()).runScheduledSendBatch();
        verify(operationsBatchService, never()).runThresholdVerificationBatch();
        verify(operationsBatchService, never()).runSixMonthCleanupBatch();
        verify(operationsBatchService, never()).runBlacklistExpiryBatch();
        verify(operationsBatchService, never()).runKeywordMatchBatch();
    }

    @Test
    void scheduledMethodsCallBatchesWhenSchedulerIsEnabled() {
        when(operationsBatchService.isEnabled()).thenReturn(true);

        operationsScheduler.scheduledSend();
        operationsScheduler.thresholdVerification();
        operationsScheduler.cleanup();
        operationsScheduler.blacklistExpiry();
        operationsScheduler.keywordMatch();

        verify(operationsBatchService).runScheduledSendBatch();
        verify(operationsBatchService).runThresholdVerificationBatch();
        verify(operationsBatchService).runSixMonthCleanupBatch();
        verify(operationsBatchService).runBlacklistExpiryBatch();
        verify(operationsBatchService).runKeywordMatchBatch();
    }
}
