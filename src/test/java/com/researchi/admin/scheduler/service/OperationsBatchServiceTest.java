package com.researchi.admin.scheduler.service;

import com.researchi.admin.blacklist.service.BlacklistService;
import com.researchi.admin.job.domain.AdminJobMeta;
import com.researchi.admin.job.domain.JobListItem;
import com.researchi.admin.job.mapper.AdminJobMetaMapper;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.mailing.domain.AdminMailSendJob;
import com.researchi.admin.mailing.mapper.AdminMailSendJobMapper;
import com.researchi.admin.mailing.service.MailingService;
import com.researchi.admin.matching.service.MatchingService;
import com.researchi.admin.notification.service.NotificationService;
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
import static org.mockito.ArgumentMatchers.eq;
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
    private MailingService mailingService;
    @Mock
    private AdminJobMetaMapper adminJobMetaMapper;
    @Mock
    private OperationsCleanupMapper operationsCleanupMapper;
    @Mock
    private BlacklistService blacklistService;
    @Mock
    private MatchingService matchingService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private JobService jobService;

    @InjectMocks
    private OperationsBatchService operationsBatchService;

    @Test
    void scheduledSendBatchExecutesOnlySuccessfulJobs() {
        AdminMailSendJob first = new AdminMailSendJob();
        first.setId(1L);
        AdminMailSendJob second = new AdminMailSendJob();
        second.setId(2L);
        when(adminMailSendJobMapper.findDueScheduled(any())).thenReturn(List.of(first, second));
        when(mailingService.executeScheduledSend(1L)).thenReturn(true);
        when(mailingService.executeScheduledSend(2L)).thenReturn(false);

        int executed = operationsBatchService.runScheduledSendBatch();

        assertThat(executed).isEqualTo(1);
    }

    @Test
    void scheduledSendBatchContinuesWhenOneJobThrows() {
        AdminMailSendJob first = new AdminMailSendJob();
        first.setId(1L);
        AdminMailSendJob second = new AdminMailSendJob();
        second.setId(2L);
        when(adminMailSendJobMapper.findDueScheduled(any())).thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("broken scheduled job")).when(mailingService).executeScheduledSend(1L);
        when(mailingService.executeScheduledSend(2L)).thenReturn(true);

        int executed = operationsBatchService.runScheduledSendBatch();

        assertThat(executed).isEqualTo(1);
        verify(mailingService).executeScheduledSend(1L);
        verify(mailingService).executeScheduledSend(2L);
    }

    @Test
    void thresholdBatchTriggersOnlyEligibleJobs() {
        AdminJobMeta first = new AdminJobMeta();
        first.setDocumentSrl(9L);
        AdminJobMeta second = new AdminJobMeta();
        second.setDocumentSrl(10L);
        when(adminJobMetaMapper.findAll()).thenReturn(List.of(first, second));
        when(mailingService.triggerThresholdAutomatically(9L)).thenReturn(true);
        when(mailingService.triggerThresholdAutomatically(10L)).thenReturn(false);

        int executed = operationsBatchService.runThresholdVerificationBatch();

        assertThat(executed).isEqualTo(1);
    }

    @Test
    void thresholdBatchContinuesWhenOneJobThrows() {
        AdminJobMeta first = new AdminJobMeta();
        first.setDocumentSrl(9L);
        AdminJobMeta second = new AdminJobMeta();
        second.setDocumentSrl(10L);
        when(adminJobMetaMapper.findAll()).thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("broken threshold job")).when(mailingService).triggerThresholdAutomatically(9L);
        when(mailingService.triggerThresholdAutomatically(10L)).thenReturn(true);

        int executed = operationsBatchService.runThresholdVerificationBatch();

        assertThat(executed).isEqualTo(1);
        verify(mailingService).triggerThresholdAutomatically(9L);
        verify(mailingService).triggerThresholdAutomatically(10L);
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

        int deleted = operationsBatchService.runSixMonthCleanupBatch();

        assertThat(deleted).isEqualTo(45);
    }

    @Test
    void blacklistExpiryBatchDelegatesToBlacklistService() {
        when(blacklistService.expireExpiredEntries(any())).thenReturn(2);

        int expired = operationsBatchService.runBlacklistExpiryBatch();

        assertThat(expired).isEqualTo(2);
    }

    @Test
    void keywordMatchBatchRunsOnlyRecruitingJobsWithApplicationsEnabled() {
        AdminJobMeta recruiting = new AdminJobMeta();
        recruiting.setApplicationEnabled("Y");
        recruiting.setRecruitStatus("RECRUITING");
        AdminJobMeta closed = new AdminJobMeta();
        closed.setApplicationEnabled("Y");
        closed.setRecruitStatus("CLOSED");
        when(jobService.getJobs()).thenReturn(List.of(
                new JobListItem(9L, "Job 9", "", "PUBLIC", "", "", recruiting, "newjob"),
                new JobListItem(10L, "Job 10", "", "CLOSED", "", "", closed, "newjob")
        ));
        when(matchingService.runScheduled(9L)).thenReturn(44L);

        int executed = operationsBatchService.runKeywordMatchBatch();

        assertThat(executed).isEqualTo(1);
        verify(matchingService).runScheduled(9L);
        verify(notificationService).sendEmailNotifications(eq(9L), eq(44L), any(), eq(null));
        verify(notificationService).sendSmsNotifications(eq(9L), eq(44L), any(), eq(null));
    }
}
