package com.researchi.admin.log.service;

import com.researchi.admin.auth.mapper.AdminActionLogMapper;
import com.researchi.admin.job.domain.JobListItem;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.log.domain.StatusBarSummary;
import com.researchi.admin.mailing.domain.AdminMailSendJob;
import com.researchi.admin.mailing.mapper.AdminMailSendJobMapper;
import com.researchi.admin.notification.mapper.AdminNotificationLogMapper;
import com.researchi.admin.search.mapper.AdminSearchLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminLogServiceTest {

    @Mock
    private AdminActionLogMapper adminActionLogMapper;
    @Mock
    private AdminMailSendJobMapper adminMailSendJobMapper;
    @Mock
    private AdminSearchLogMapper adminSearchLogMapper;
    @Mock
    private AdminNotificationLogMapper adminNotificationLogMapper;
    @Mock
    private JobService jobService;

    @InjectMocks
    private AdminLogService adminLogService;

    @Test
    void getMailLogsAddsJobTitlesAndStatusBarSummarizesCounts() {
        JobListItem job = new JobListItem(9L, "Survey Job", "", "PUBLIC", "", "", null, "newjob");
        when(jobService.getJobsByDocumentSrls(List.of(9L))).thenReturn(List.of(job));

        AdminMailSendJob mailJob = new AdminMailSendJob();
        mailJob.setId(71L);
        mailJob.setDocumentSrl(9L);
        mailJob.setSentAt(LocalDateTime.now());
        when(adminMailSendJobMapper.findAll()).thenReturn(List.of(mailJob));

        assertThat(adminLogService.getMailLogs()).extracting(AdminMailSendJob::getJobTitle).containsExactly("Survey Job");
    }

    @Test
    void getStatusBarSummaryUsesAggregateQueriesOnly() {
        LocalDateTime latestActionAt = LocalDateTime.now().minusMinutes(3);
        LocalDateTime latestMailAt = LocalDateTime.now().minusMinutes(2);
        LocalDateTime latestSearchAt = LocalDateTime.now().minusMinutes(1);
        LocalDateTime latestNotificationAt = LocalDateTime.now();

        when(adminActionLogMapper.countAll()).thenReturn(10L);
        when(adminMailSendJobMapper.countAll()).thenReturn(20L);
        when(adminSearchLogMapper.countAll()).thenReturn(30L);
        when(adminNotificationLogMapper.countAll()).thenReturn(40L);
        when(adminActionLogMapper.findLatestCreatedAt()).thenReturn(latestActionAt);
        when(adminMailSendJobMapper.findLatestActivityAt()).thenReturn(latestMailAt);
        when(adminSearchLogMapper.findLatestSearchedAt()).thenReturn(latestSearchAt);
        when(adminNotificationLogMapper.findLatestCreatedAt()).thenReturn(latestNotificationAt);

        StatusBarSummary summary = adminLogService.getStatusBarSummary();

        assertThat(summary.actionCount()).isEqualTo(10);
        assertThat(summary.mailCount()).isEqualTo(20);
        assertThat(summary.searchCount()).isEqualTo(30);
        assertThat(summary.notificationCount()).isEqualTo(40);
        assertThat(summary.latestActionAt()).isEqualTo(latestActionAt);
        assertThat(summary.latestMailAt()).isEqualTo(latestMailAt);
        assertThat(summary.latestSearchAt()).isEqualTo(latestSearchAt);
        assertThat(summary.latestNotificationAt()).isEqualTo(latestNotificationAt);
        verify(adminActionLogMapper, never()).findAll();
        verify(adminMailSendJobMapper, never()).findAll();
        verify(adminSearchLogMapper, never()).findAll();
        verify(adminNotificationLogMapper, never()).findAll();
    }
}
