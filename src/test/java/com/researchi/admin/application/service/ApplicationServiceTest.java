package com.researchi.admin.application.service;

import com.researchi.admin.application.domain.ApplicationAnswerItem;
import com.researchi.admin.application.domain.ApplicationDetail;
import com.researchi.admin.application.domain.ApplicationJobCount;
import com.researchi.admin.application.domain.ApplicationRecord;
import com.researchi.admin.application.mapper.AdminApplicationQueryMapper;
import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.job.domain.JobDetail;
import com.researchi.admin.job.domain.JobListItem;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.publicform.mapper.AdminJobApplicationExtraAnswerMapper;
import com.researchi.admin.publicform.service.PublicFormProtectionService;
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

    @Mock
    private PublicFormProtectionService protectionService;

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
        when(jobService.getJobsByDocumentSrls(List.of(9L))).thenReturn(List.of(new JobListItem(
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
        when(jobService.getJobsByDocumentSrls(List.of(9L))).thenReturn(List.of(new JobListItem(
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
        when(jobService.getJobsByDocumentSrls(List.of(9L))).thenReturn(List.of(new JobListItem(
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
    void getApplicationPageUsesMapperLimitAndOffset() {
        ApplicationRecord application = new ApplicationRecord();
        application.setId(31L);
        application.setDocumentSrl(9L);
        application.setApplicantName("Page User");
        application.setAppliedAt(LocalDateTime.of(2026, 4, 16, 12, 0));

        when(adminApplicationQueryMapper.findPage(9L, 12, 24)).thenReturn(List.of(application));
        when(jobService.getJobsByDocumentSrls(List.of(9L))).thenReturn(List.of(new JobListItem(
                9L,
                "Paged Job",
                "Body",
                "PUBLIC",
                "20260416",
                "20260416",
                null,
                "newjob"
        )));

        List<ApplicationRecord> applications = applicationService.getApplicationPage(9L, 12, 24);

        assertThat(applications).singleElement().extracting(ApplicationRecord::getJobTitle).isEqualTo("Paged Job");
        verify(adminApplicationQueryMapper).findPage(9L, 12, 24);
    }

    @Test
    void getApplicationSearchPageUsesDatabaseSearchAndJobTitleMatches() {
        ApplicationRecord application = new ApplicationRecord();
        application.setId(31L);
        application.setDocumentSrl(9L);
        application.setApplicantName("Page User");
        application.setAppliedAt(LocalDateTime.of(2026, 4, 16, 12, 0));

        when(jobService.findApplicationDocumentSrlsByTitle("survey")).thenReturn(List.of(9L));
        when(jobService.getJobsByDocumentSrls(List.of(9L))).thenReturn(List.of(
                new JobListItem(9L, "Seoul Survey", "Body", "PUBLIC", "20260416", "20260416", null, "newjob")
        ));
        when(adminApplicationQueryMapper.findSearchPage(null, "survey", List.of(9L), 12, 0))
                .thenReturn(List.of(application));

        List<ApplicationRecord> applications = applicationService.getApplicationPage(null, " Survey ", 12, 0);

        assertThat(applications).singleElement().extracting(ApplicationRecord::getJobTitle).isEqualTo("Seoul Survey");
        verify(adminApplicationQueryMapper).findSearchPage(null, "survey", List.of(9L), 12, 0);
    }

    @Test
    void countApplicationsUsesDatabaseSearch() {
        when(jobService.findApplicationDocumentSrlsByTitle("survey")).thenReturn(List.of(9L));
        when(adminApplicationQueryMapper.countSearch(null, "survey", List.of(9L))).thenReturn(3);

        assertThat(applicationService.countApplications(null, " Survey ")).isEqualTo(3);
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
    void getApplicationDetailShowsDecryptedPersonalInfo() {
        ApplicationRecord application = new ApplicationRecord();
        application.setId(11L);
        application.setDocumentSrl(9L);
        application.setApplicantName("Kim");
        application.setMobilePhoneEnc("mobile-enc");
        application.setTelPhoneEnc("tel-enc");
        application.setEmailAddressEnc("email-enc");
        application.setAddressEnc("address-enc");
        application.setMobilePhoneMasked("01012345678");
        application.setTelPhoneMasked("0212345678");
        application.setEmailAddressMasked("kim@example.com");
        application.setAddressMasked("Seoul Gangnam");

        when(adminApplicationQueryMapper.findById(11L)).thenReturn(application);
        when(adminApplicationQueryMapper.findAnswersByApplicationId(11L)).thenReturn(List.of());
        when(adminJobApplicationExtraAnswerMapper.findByApplicationId(11L)).thenReturn(List.of());
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L, "Survey Job"));
        when(protectionService.decrypt("mobile-enc")).thenReturn("01012345678");
        when(protectionService.decrypt("tel-enc")).thenReturn("0212345678");
        when(protectionService.decrypt("email-enc")).thenReturn("kim@example.com");
        when(protectionService.decrypt("address-enc")).thenReturn("Seoul Gangnam");

        ApplicationDetail detail = applicationService.getApplicationDetail(11L);

        assertThat(detail.application().getMobilePhoneDisplay()).isEqualTo("01012345678");
        assertThat(detail.application().getTelPhoneDisplay()).isEqualTo("0212345678");
        assertThat(detail.application().getEmailAddressDisplay()).isEqualTo("kim@example.com");
        assertThat(detail.application().getAddressDisplay()).isEqualTo("Seoul Gangnam");
    }

    @Test
    void getJobFiltersBuildsSplitUiCountsPerJob() {
        ApplicationJobCount firstCount = new ApplicationJobCount();
        firstCount.setDocumentSrl(9L);
        firstCount.setApplicationCount(2L);
        ApplicationJobCount secondCount = new ApplicationJobCount();
        secondCount.setDocumentSrl(10L);
        secondCount.setApplicationCount(1L);

        when(adminApplicationQueryMapper.countByDocumentSrl()).thenReturn(List.of(firstCount, secondCount));
        when(jobService.getJobsByDocumentSrls(List.of(9L, 10L))).thenReturn(List.of(
                new JobListItem(9L, "Job A", "Body", "PUBLIC", "20260416", "20260416", null, "newjob"),
                new JobListItem(10L, "Job B", "Body", "PUBLIC", "20260416", "20260416", null, "additional")
        ));

        assertThat(applicationService.getJobFilters())
                .extracting("documentSrl", "jobTitle", "applicationCount")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(10L, "Job B", 1L),
                        org.assertj.core.groups.Tuple.tuple(9L, "Job A", 2L)
                );
        verify(adminApplicationQueryMapper, never()).findAll();
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

    @Test
    void clearBlacklistClearsOnlyBlacklistFlags() {
        ApplicationRecord application = new ApplicationRecord();
        application.setId(11L);
        application.setDocumentSrl(9L);
        application.setApplicationStatus("APPROVED");
        application.setIsBlacklisted("Y");
        application.setBlackModeApplied("PERMANENT_BLOCK");

        when(adminApplicationQueryMapper.findById(11L)).thenReturn(application);
        when(adminApplicationQueryMapper.clearBlacklistState(11L)).thenReturn(1);

        applicationService.clearBlacklist(
                11L,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new org.springframework.mock.web.MockHttpServletRequest()
        );

        verify(adminApplicationQueryMapper).clearBlacklistState(11L);
        verify(adminApplicationQueryMapper, never()).updateStatus(any(), any());
        verify(adminActionLogService).log(eq(1L), eq("APPLICATION_BLACKLIST_CLEAR"), eq("APPLICATION"), eq("11"), eq("지원서 블랙리스트 해제"), any(HttpServletRequest.class));
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
