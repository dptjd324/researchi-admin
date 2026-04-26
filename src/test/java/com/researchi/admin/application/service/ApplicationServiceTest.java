package com.researchi.admin.application.service;

import com.researchi.admin.application.domain.ApplicationAnswerItem;
import com.researchi.admin.application.domain.ApplicationDetail;
import com.researchi.admin.application.domain.ApplicationRecord;
import com.researchi.admin.application.mapper.AdminApplicationQueryMapper;
import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.job.domain.JobDetail;
import com.researchi.admin.job.domain.JobListItem;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.publicform.mapper.AdminJobApplicationExtraAnswerMapper;
import com.researchi.admin.xe.domain.XeJobDocument;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationServiceTest {

    @Mock
    private AdminApplicationQueryMapper adminApplicationQueryMapper;

    @Mock
    private JobService jobService;

    @Mock
    private AdminActionLogService adminActionLogService;

    @Mock
    private AdminJobApplicationExtraAnswerMapper adminJobApplicationExtraAnswerMapper;

    @InjectMocks
    private ApplicationService applicationService;

    @Test
    void getApplicationsUsesJobScopedQueryForPerJobList() {
        ApplicationRecord application = new ApplicationRecord();
        application.setId(12L);
        application.setDocumentSrl(9L);
        application.setApplicantName("Lee");
        application.setAppliedAt(LocalDateTime.of(2026, 4, 16, 10, 0));

        when(adminApplicationQueryMapper.findByDocumentSrl(9L)).thenReturn(List.of(application));
        when(jobService.getJobs()).thenReturn(List.of(new JobListItem(
                9L,
                "Scoped Job",
                "Body",
                "PUBLIC",
                "20260416",
                "20260416",
                null,
                "newjob"
        )));

        List<ApplicationRecord> applications = applicationService.getApplications(9L, null);

        assertThat(applications).hasSize(1);
        assertThat(applications.get(0).getDocumentSrl()).isEqualTo(9L);
        verify(adminApplicationQueryMapper).findByDocumentSrl(9L);
        verify(adminApplicationQueryMapper, never()).findAll();
    }

    @Test
    void getApplicationsFiltersUsingQuickSearchAcrossJobTitle() {
        ApplicationRecord application = new ApplicationRecord();
        application.setId(11L);
        application.setDocumentSrl(9L);
        application.setApplicantName("Kim");
        application.setApplicationStatus("RECEIVED");
        application.setAppliedAt(LocalDateTime.of(2026, 4, 16, 9, 0));

        when(adminApplicationQueryMapper.findAll()).thenReturn(List.of(application));
        when(jobService.getJobs()).thenReturn(List.of(new JobListItem(
                9L,
                "Seoul Research Survey",
                "Body",
                "PUBLIC",
                "20260416",
                "20260416",
                null,
                "newjob"
        )));

        List<ApplicationRecord> applications = applicationService.getApplications(null, "survey");

        assertThat(applications).hasSize(1);
        assertThat(applications.get(0).getJobTitle()).isEqualTo("Seoul Research Survey");
    }

    @Test
    void getApplicationsKeepsApplicantTypeAndStatusesForListDisplay() {
        ApplicationRecord application = new ApplicationRecord();
        application.setId(13L);
        application.setDocumentSrl(9L);
        application.setApplicantName("Park");
        application.setIsNewApplicant("Y");
        application.setApplicationStatus("RECEIVED");
        application.setDeliveryStatus("PENDING");
        application.setAppliedAt(LocalDateTime.of(2026, 4, 16, 11, 0));

        when(adminApplicationQueryMapper.findAll()).thenReturn(List.of(application));
        when(jobService.getJobs()).thenReturn(List.of(new JobListItem(
                9L,
                "Status Job",
                "Body",
                "PUBLIC",
                "20260416",
                "20260416",
                null,
                "newjob"
        )));

        ApplicationRecord result = applicationService.getApplications(null, null).get(0);

        assertThat(result.getIsNewApplicant()).isEqualTo("Y");
        assertThat(result.getApplicationStatus()).isEqualTo("RECEIVED");
        assertThat(result.getDeliveryStatus()).isEqualTo("PENDING");
    }

    @Test
    void getApplicationDetailNormalizesDynamicAnswerArray() {
        ApplicationRecord application = new ApplicationRecord();
        application.setId(11L);
        application.setDocumentSrl(9L);
        application.setApplicantName("Kim");

        ApplicationAnswerItem answer = new ApplicationAnswerItem();
        answer.setFieldId(3L);
        answer.setFieldLabel("Preferred brands");
        answer.setFieldType("CHECKBOX");
        answer.setAnswerText("A, B");
        answer.setAnswerJson("[\"A\",\"B\"]");

        when(adminApplicationQueryMapper.findById(11L)).thenReturn(application);
        when(adminApplicationQueryMapper.findAnswersByApplicationId(11L)).thenReturn(List.of(answer));
        when(adminJobApplicationExtraAnswerMapper.findByApplicationId(11L)).thenReturn(List.of());
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L, "Survey Job"));

        ApplicationDetail detail = applicationService.getApplicationDetail(11L);

        assertThat(detail.application().getJobTitle()).isEqualTo("Survey Job");
        assertThat(detail.answers()).singleElement().extracting(ApplicationAnswerItem::getDisplayAnswer).isEqualTo("A, B");
    }

    @Test
    void getJobFiltersBuildsSplitUiCountsPerJob() {
        ApplicationRecord first = new ApplicationRecord();
        first.setId(21L);
        first.setDocumentSrl(9L);
        ApplicationRecord second = new ApplicationRecord();
        second.setId(22L);
        second.setDocumentSrl(9L);
        ApplicationRecord third = new ApplicationRecord();
        third.setId(23L);
        third.setDocumentSrl(10L);

        when(adminApplicationQueryMapper.findAll()).thenReturn(List.of(first, second, third));
        when(jobService.getJobs()).thenReturn(List.of(
                new JobListItem(9L, "Job A", "Body", "PUBLIC", "20260416", "20260416", null, "newjob"),
                new JobListItem(10L, "Job B", "Body", "PUBLIC", "20260416", "20260416", null, "additional")
        ));

        assertThat(applicationService.getJobFilters())
                .extracting("documentSrl", "jobTitle", "applicationCount")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(10L, "Job B", 1L),
                        org.assertj.core.groups.Tuple.tuple(9L, "Job A", 2L)
                );
    }

    @Test
    void updateStatusWritesActionLog() {
        ApplicationRecord application = new ApplicationRecord();
        application.setId(11L);
        application.setDocumentSrl(9L);
        application.setApplicationStatus("RECEIVED");

        when(adminApplicationQueryMapper.findById(11L)).thenReturn(application);
        when(adminApplicationQueryMapper.updateStatus(11L, "REVIEWING")).thenReturn(1);
        doNothing().when(adminActionLogService).log(any(), any(), any(), any(), any(), any());

        applicationService.updateStatus(
                11L,
                "reviewing",
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new org.springframework.mock.web.MockHttpServletRequest()
        );

        verify(adminApplicationQueryMapper).updateStatus(11L, "REVIEWING");
        verify(adminActionLogService).log(eq(1L), eq("APPLICATION_STATUS_UPDATE"), eq("APPLICATION"), eq("11"), eq("지원서 상태 변경: REVIEWING"), any(HttpServletRequest.class));
    }

    @Test
    void updateStatusIgnoresNoOpChange() {
        ApplicationRecord application = new ApplicationRecord();
        application.setId(11L);
        application.setDocumentSrl(9L);
        application.setApplicationStatus("REVIEWING");

        when(adminApplicationQueryMapper.findById(11L)).thenReturn(application);

        applicationService.updateStatus(
                11L,
                "reviewing",
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new org.springframework.mock.web.MockHttpServletRequest()
        );

        verify(adminApplicationQueryMapper, never()).updateStatus(any(), any());
        verify(adminActionLogService, never()).log(any(), any(), any(), any(), any(), any());
    }

    private JobDetail jobDetail(Long documentSrl, String title) {
        XeJobDocument document = new XeJobDocument();
        document.setDocumentSrl(documentSrl);
        document.setTitle(title);
        document.setMid("newjob");
        document.setStatus("PUBLIC");
        return new JobDetail(document, null);
    }
}
