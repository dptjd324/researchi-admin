package com.researchi.admin.scheduler.service;

import com.researchi.admin.legacy.research.service.LegacyResearchMailService;
import com.researchi.admin.legacy.matching.service.LegacyMatchingService;
import com.researchi.admin.mailing.domain.AdminMailSendJob;
import com.researchi.admin.mailing.mapper.AdminMailSendJobMapper;
import com.researchi.admin.scheduler.config.SchedulerProperties;
import com.researchi.admin.scheduler.mapper.OperationsCleanupMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OperationsBatchServiceTest {

    @Mock
    private SchedulerProperties schedulerProperties;
    @Mock
    private AdminMailSendJobMapper adminMailSendJobMapper;
    @Mock
    private LegacyResearchMailService legacyResearchMailService;
    @Mock
    private LegacyMatchingService legacyMatchingService;
    @Mock
    private OperationsCleanupMapper operationsCleanupMapper;

    @InjectMocks
    private OperationsBatchService operationsBatchService;

    @Test
    void scheduledSendBatchExecutesOnlySuccessfulJobs() {
        AdminMailSendJob first = new AdminMailSendJob();
        first.setId(1L);
        first.setTriggerType("LEGACY_SCHEDULED");
        AdminMailSendJob second = new AdminMailSendJob();
        second.setId(2L);
        second.setTriggerType("LEGACY_SCHEDULED_DAILY");
        when(adminMailSendJobMapper.findDueScheduled(any())).thenReturn(List.of(first, second));
        when(legacyResearchMailService.executeScheduledSend(1L)).thenReturn(true);
        when(legacyResearchMailService.executeScheduledSend(2L)).thenReturn(false);

        int executed = operationsBatchService.runScheduledSendBatch();

        assertThat(executed).isEqualTo(1);
    }

    @Test
    void scheduledSendBatchContinuesWhenOneJobThrows() {
        AdminMailSendJob first = new AdminMailSendJob();
        first.setId(1L);
        first.setTriggerType("LEGACY_SCHEDULED");
        AdminMailSendJob second = new AdminMailSendJob();
        second.setId(2L);
        second.setTriggerType("LEGACY_SCHEDULED");
        when(adminMailSendJobMapper.findDueScheduled(any())).thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("broken scheduled job")).when(legacyResearchMailService).executeScheduledSend(1L);
        when(legacyResearchMailService.executeScheduledSend(2L)).thenReturn(true);

        int executed = operationsBatchService.runScheduledSendBatch();

        assertThat(executed).isEqualTo(1);
        verify(legacyResearchMailService).executeScheduledSend(1L);
        verify(legacyResearchMailService).executeScheduledSend(2L);
    }

    @Test
    void thresholdBatchTriggersOnlyEligibleLegacyResearch() {
        when(legacyResearchMailService.getEnabledThresholdResearchNos()).thenReturn(List.of(9L, 10L));
        when(legacyResearchMailService.triggerThresholdAutomatically(9L)).thenReturn(true);
        when(legacyResearchMailService.triggerThresholdAutomatically(10L)).thenReturn(false);

        int executed = operationsBatchService.runThresholdVerificationBatch();

        assertThat(executed).isEqualTo(1);
    }

    @Test
    void thresholdBatchContinuesWhenOneLegacyResearchThrows() {
        when(legacyResearchMailService.getEnabledThresholdResearchNos()).thenReturn(List.of(9L, 10L));
        doThrow(new IllegalStateException("broken threshold job")).when(legacyResearchMailService).triggerThresholdAutomatically(9L);
        when(legacyResearchMailService.triggerThresholdAutomatically(10L)).thenReturn(true);

        int executed = operationsBatchService.runThresholdVerificationBatch();

        assertThat(executed).isEqualTo(1);
        verify(legacyResearchMailService).triggerThresholdAutomatically(9L);
        verify(legacyResearchMailService).triggerThresholdAutomatically(10L);
    }

    @Test
    void cleanupBatchAggregatesDeletedRows() {
        when(schedulerProperties.getRetentionMonths()).thenReturn(6);
        when(operationsCleanupMapper.deleteDuplicateLogsBefore(any())).thenReturn(1);
        when(operationsCleanupMapper.deleteBlacklistMatchLogsForExpiredApplications(any())).thenReturn(2);
        when(operationsCleanupMapper.deletePrivacyConsentsForExpiredApplications(any())).thenReturn(3);
        when(operationsCleanupMapper.deleteFormAnswersForExpiredApplications(any())).thenReturn(4);
        when(operationsCleanupMapper.deleteApplicationKeywordsForExpiredApplications(any())).thenReturn(5);
        when(operationsCleanupMapper.deleteNotificationLogsForExpiredApplications(any())).thenReturn(6);
        when(operationsCleanupMapper.deleteMailTargetsForExpiredApplications(any())).thenReturn(7);
        when(operationsCleanupMapper.deleteKeywordMatchTargetsForExpiredApplications(any())).thenReturn(8);
        when(operationsCleanupMapper.deleteApplicationsBefore(any())).thenReturn(9);
        when(legacyMatchingService.cleanupMatchingLogsAfterClosedDeadline()).thenReturn(10);

        int deleted = operationsBatchService.runSixMonthCleanupBatch();

        assertThat(deleted).isEqualTo(55);
    }

    @Test
    void blacklistExpiryBatchIsNoOpAfterLegacyBlacklistTransition() {
        int expired = operationsBatchService.runBlacklistExpiryBatch();

        assertThat(expired).isZero();
    }

    @Test
    void keywordMatchBatchIsNoOpAfterLegacyMatchingSchedulerTransition() {
        int executed = operationsBatchService.runKeywordMatchBatch();

        assertThat(executed).isZero();
    }
}
