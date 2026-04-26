package com.researchi.admin.log.service;

import com.researchi.admin.auth.mapper.AdminActionLogMapper;
import com.researchi.admin.job.domain.JobListItem;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.log.domain.ActionLogItem;
import com.researchi.admin.log.domain.StatusBarSummary;
import com.researchi.admin.mailing.domain.AdminMailSendJob;
import com.researchi.admin.mailing.mapper.AdminMailSendJobMapper;
import com.researchi.admin.notification.domain.AdminNotificationLog;
import com.researchi.admin.notification.mapper.AdminNotificationLogMapper;
import com.researchi.admin.search.domain.SearchLogItem;
import com.researchi.admin.search.mapper.AdminSearchLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
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
        when(jobService.getJobs()).thenReturn(List.of(job));

        AdminMailSendJob mailJob = new AdminMailSendJob();
        mailJob.setId(71L);
        mailJob.setDocumentSrl(9L);
        mailJob.setSentAt(LocalDateTime.now());
        when(adminMailSendJobMapper.findAll()).thenReturn(List.of(mailJob));

        ActionLogItem actionLog = new ActionLogItem();
        actionLog.setCreatedAt(LocalDateTime.now().minusMinutes(3));
        when(adminActionLogMapper.findAll()).thenReturn(List.of(actionLog));

        SearchLogItem searchLog = new SearchLogItem();
        searchLog.setSearchedAt(LocalDateTime.now().minusMinutes(2));
        when(adminSearchLogMapper.findAll()).thenReturn(List.of(searchLog));

        AdminNotificationLog notificationLog = new AdminNotificationLog();
        notificationLog.setCreatedAt(LocalDateTime.now().minusMinutes(1));
        when(adminNotificationLogMapper.findAll()).thenReturn(List.of(notificationLog));

        assertThat(adminLogService.getMailLogs()).extracting(AdminMailSendJob::getJobTitle).containsExactly("Survey Job");

        StatusBarSummary summary = adminLogService.getStatusBarSummary();

        assertThat(summary.actionCount()).isEqualTo(1);
        assertThat(summary.mailCount()).isEqualTo(1);
        assertThat(summary.searchCount()).isEqualTo(1);
        assertThat(summary.notificationCount()).isEqualTo(1);
        assertThat(summary.latestMailAt()).isNotNull();
    }
}
