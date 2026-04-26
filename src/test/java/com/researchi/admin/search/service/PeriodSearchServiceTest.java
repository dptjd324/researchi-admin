package com.researchi.admin.search.service;

import com.researchi.admin.application.domain.ApplicationRecord;
import com.researchi.admin.application.service.ApplicationService;
import com.researchi.admin.auth.mapper.AdminActionLogMapper;
import com.researchi.admin.job.domain.JobListItem;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.log.domain.ActionLogItem;
import com.researchi.admin.mailing.domain.AdminMailSendJob;
import com.researchi.admin.mailing.mapper.AdminMailSendJobMapper;
import com.researchi.admin.notification.mapper.AdminNotificationLogMapper;
import com.researchi.admin.search.domain.AdminSearchLog;
import com.researchi.admin.search.domain.PeriodSearchForm;
import com.researchi.admin.search.domain.PeriodSearchResult;
import com.researchi.admin.search.mapper.AdminSearchLogMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PeriodSearchServiceTest {

    @Mock
    private ApplicationService applicationService;
    @Mock
    private AdminMailSendJobMapper adminMailSendJobMapper;
    @Mock
    private AdminActionLogMapper adminActionLogMapper;
    @Mock
    private AdminNotificationLogMapper adminNotificationLogMapper;
    @Mock
    private AdminSearchLogMapper adminSearchLogMapper;
    @Mock
    private JobService jobService;

    @InjectMocks
    private PeriodSearchService periodSearchService;

    @Test
    void searchApplicationsAppliesFiltersAndStoresSearchLog() {
        ApplicationRecord matched = application(11L, 9L, "Kim", "RECEIVED", LocalDateTime.now());
        ApplicationRecord otherStatus = application(12L, 9L, "Kim", "REJECTED", LocalDateTime.now());
        when(applicationService.getApplications(9L, "kim")).thenReturn(List.of(matched, otherStatus));

        PeriodSearchForm form = new PeriodSearchForm();
        form.setScope("APPLICATION");
        form.setDocumentSrl(9L);
        form.setKeyword("kim");
        form.setStatus("RECEIVED");
        form.setDatePreset("SPECIFIC_DAY");
        form.setSpecificDate(LocalDate.now());

        PeriodSearchResult result = periodSearchService.search(form);

        assertThat(result.scope()).isEqualTo("APPLICATION");
        assertThat(result.resultCount()).isEqualTo(1);
        assertThat(result.sendDocumentSrl()).isEqualTo(9L);
        assertThat(result.applications()).containsExactly(matched);

        ArgumentCaptor<AdminSearchLog> captor = ArgumentCaptor.forClass(AdminSearchLog.class);
        verify(adminSearchLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getSearchType()).isEqualTo("APPLICATION");
        assertThat(captor.getValue().getKeywordText()).isEqualTo("kim");
        assertThat(captor.getValue().getResultCount()).isEqualTo(1);
        assertThat(captor.getValue().getConditionJson()).contains("\"documentSrl\":\"9\"");
    }

    @Test
    void searchMailUsesSendWindowAndSingleJobSendTarget() {
        AdminMailSendJob matched = mailJob(41L, 9L, "SENT", "Survey Mail", LocalDateTime.now());
        AdminMailSendJob otherJob = mailJob(42L, 10L, "SENT", "Other", LocalDateTime.now());
        when(adminMailSendJobMapper.findAll()).thenReturn(List.of(matched, otherJob));
        when(jobService.getJobs()).thenReturn(List.of(
                new JobListItem(9L, "Survey Mail", "", "PUBLIC", "", "", null, "newjob"),
                new JobListItem(10L, "Other Mail", "", "PUBLIC", "", "", null, "newjob")
        ));

        PeriodSearchForm form = new PeriodSearchForm();
        form.setScope("MAIL");
        form.setKeyword("survey");
        form.setStatus("SENT");
        form.setDatePreset("TODAY");

        PeriodSearchResult result = periodSearchService.search(form);

        assertThat(result.scope()).isEqualTo("MAIL");
        assertThat(result.resultCount()).isEqualTo(1);
        assertThat(result.sendDocumentSrl()).isEqualTo(9L);
        assertThat(result.mailJobs()).containsExactly(matched);
        verify(adminSearchLogMapper).insert(any(AdminSearchLog.class));
    }

    @Test
    void searchActionsSupportsThisWeekPresetAndMultipleFilters() {
        ActionLogItem matched = new ActionLogItem();
        matched.setActionType("LOGIN_SUCCESS");
        matched.setActionDetail("관리자 로그인 성공");
        matched.setCreatedAt(LocalDateTime.now().minusDays(1));

        ActionLogItem oldLog = new ActionLogItem();
        oldLog.setActionType("LOGIN_SUCCESS");
        oldLog.setActionDetail("관리자 로그인 성공");
        oldLog.setCreatedAt(LocalDateTime.now().minusWeeks(2));

        ActionLogItem wrongStatus = new ActionLogItem();
        wrongStatus.setActionType("PASSWORD_CHANGE");
        wrongStatus.setActionDetail("비밀번호 변경");
        wrongStatus.setCreatedAt(LocalDateTime.now());

        when(adminActionLogMapper.findAll()).thenReturn(List.of(matched, oldLog, wrongStatus));

        PeriodSearchForm form = new PeriodSearchForm();
        form.setScope("ACTION");
        form.setKeyword("로그인");
        form.setStatus("LOGIN_SUCCESS");
        form.setDatePreset("THIS_WEEK");

        PeriodSearchResult result = periodSearchService.search(form);

        assertThat(result.scope()).isEqualTo("ACTION");
        assertThat(result.resultCount()).isEqualTo(1);
        assertThat(result.actionLogs()).containsExactly(matched);
        verify(adminSearchLogMapper).insert(any(AdminSearchLog.class));
    }

    @Test
    void searchNotificationsSupportsCustomRangeAndDocumentFilter() {
        com.researchi.admin.notification.domain.AdminNotificationLog matched =
                new com.researchi.admin.notification.domain.AdminNotificationLog();
        matched.setDocumentSrl(9L);
        matched.setSendStatus("SENT");
        matched.setKeywordSummary("survey panel");
        matched.setCreatedAt(LocalDateTime.now().minusDays(1));

        com.researchi.admin.notification.domain.AdminNotificationLog otherJob =
                new com.researchi.admin.notification.domain.AdminNotificationLog();
        otherJob.setDocumentSrl(10L);
        otherJob.setSendStatus("SENT");
        otherJob.setKeywordSummary("survey panel");
        otherJob.setCreatedAt(LocalDateTime.now().minusDays(1));

        com.researchi.admin.notification.domain.AdminNotificationLog outOfRange =
                new com.researchi.admin.notification.domain.AdminNotificationLog();
        outOfRange.setDocumentSrl(9L);
        outOfRange.setSendStatus("SENT");
        outOfRange.setKeywordSummary("survey panel");
        outOfRange.setCreatedAt(LocalDateTime.now().minusDays(10));

        when(adminNotificationLogMapper.findAll()).thenReturn(List.of(matched, otherJob, outOfRange));

        PeriodSearchForm form = new PeriodSearchForm();
        form.setScope("NOTIFICATION");
        form.setDocumentSrl(9L);
        form.setKeyword("survey");
        form.setStatus("SENT");
        form.setDatePreset("CUSTOM");
        form.setDateFrom(LocalDate.now().minusDays(2));
        form.setDateTo(LocalDate.now());

        PeriodSearchResult result = periodSearchService.search(form);

        assertThat(result.scope()).isEqualTo("NOTIFICATION");
        assertThat(result.resultCount()).isEqualTo(1);
        assertThat(result.sendDocumentSrl()).isEqualTo(9L);
        assertThat(result.notificationLogs()).containsExactly(matched);
        verify(adminSearchLogMapper).insert(any(AdminSearchLog.class));
    }

    private ApplicationRecord application(Long id, Long documentSrl, String applicantName, String status, LocalDateTime appliedAt) {
        ApplicationRecord application = new ApplicationRecord();
        application.setId(id);
        application.setDocumentSrl(documentSrl);
        application.setApplicantName(applicantName);
        application.setApplicationStatus(status);
        application.setAppliedAt(appliedAt);
        application.setJobTitle("Survey Job");
        return application;
    }

    private AdminMailSendJob mailJob(Long id, Long documentSrl, String status, String jobTitle, LocalDateTime sentAt) {
        AdminMailSendJob mailSendJob = new AdminMailSendJob();
        mailSendJob.setId(id);
        mailSendJob.setDocumentSrl(documentSrl);
        mailSendJob.setSendStatus(status);
        mailSendJob.setJobTitle(jobTitle);
        mailSendJob.setSentAt(sentAt);
        return mailSendJob;
    }
}
