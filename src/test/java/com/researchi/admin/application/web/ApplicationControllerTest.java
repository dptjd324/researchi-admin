package com.researchi.admin.application.web;

import com.researchi.admin.application.domain.ApplicationDetail;
import com.researchi.admin.application.domain.ApplicationRecord;
import com.researchi.admin.application.service.ApplicationService;
import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.job.domain.JobDetail;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.xe.domain.XeJobDocument;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationControllerTest {

    @Mock
    private ApplicationService applicationService;

    @Mock
    private JobService jobService;

    @InjectMocks
    private ApplicationController applicationController;

    @Test
    void applicationsPopulatesAllListModel() {
        when(applicationService.countApplications(null, "kim")).thenReturn(1);
        when(applicationService.getApplicationPage(null, "kim", 12, 0)).thenReturn(List.of(applicationRecord(11L, 9L)));
        when(applicationService.getJobFilters()).thenReturn(List.of());
        when(applicationService.getAllowedStatuses()).thenReturn(List.of("RECEIVED", "REVIEWING"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/applications");
        request.setQueryString("keyword=kim");
        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = applicationController.applications(null, "kim", null, model, request, null);

        assertThat(viewName).isEqualTo("applications/list");
        assertThat(model.get("pageTitle")).isEqualTo("전체 지원서");
        assertThat(model.get("pageDescription")).isEqualTo("공고별 목록, 빠른 검색, 상태 변경, 응답 상세 확인을 지원하는 지원서 관리 화면입니다.");
        assertThat(model.get("selectedDocumentSrl")).isNull();
        assertThat(model.get("keyword")).isEqualTo("kim");
        assertThat(model.get("returnTo")).isEqualTo("/applications?keyword=kim");
        assertThat(model.get("currentPage")).isEqualTo(1);
        assertThat(model.get("totalPages")).isEqualTo(1);
        verify(applicationService).countApplications(null, "kim");
        verify(applicationService).getApplicationPage(null, "kim", 12, 0);
    }

    @Test
    void applicationsUsesDatabasePageWhenKeywordIsBlank() {
        when(applicationService.countApplications(null, null)).thenReturn(25);
        when(applicationService.getApplicationPage(null, null, 12, 12)).thenReturn(List.of(applicationRecord(12L, 9L)));
        when(applicationService.getJobFilters()).thenReturn(List.of());
        when(applicationService.getAllowedStatuses()).thenReturn(List.of("RECEIVED", "REVIEWING"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/applications");
        request.setQueryString("page=2");
        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = applicationController.applications(null, null, 2, model, request, null);

        assertThat(viewName).isEqualTo("applications/list");
        assertThat(model.get("applications")).asList().hasSize(1);
        assertThat(model.get("currentPage")).isEqualTo(2);
        assertThat(model.get("totalPages")).isEqualTo(3);
        assertThat(model.get("totalItemCount")).isEqualTo(25);
        verify(applicationService).countApplications(null, null);
        verify(applicationService).getApplicationPage(null, null, 12, 12);
    }

    @Test
    void applicationsByJobPopulatesScopedModel() {
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));
        when(applicationService.countApplications(9L, "kim")).thenReturn(1);
        when(applicationService.getApplicationPage(9L, "kim", 12, 0)).thenReturn(List.of(applicationRecord(11L, 9L)));
        when(applicationService.getJobFilters()).thenReturn(List.of());
        when(applicationService.getAllowedStatuses()).thenReturn(List.of("RECEIVED", "REVIEWING"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/jobs/9/applications");
        request.setQueryString("keyword=kim");
        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = applicationController.applicationsByJob(9L, "kim", null, model, request, null);

        assertThat(viewName).isEqualTo("applications/list");
        assertThat(model.get("pageTitle")).isEqualTo("공고별 지원서");
        assertThat(model.get("pageDescription")).isEqualTo("공고별 목록, 빠른 검색, 상태 변경, 응답 상세 확인을 지원하는 지원서 관리 화면입니다.");
        assertThat(model.get("selectedDocumentSrl")).isEqualTo(9L);
        assertThat(model.get("selectedJobDetail")).isNotNull();
        assertThat(model.get("returnTo")).isEqualTo("/jobs/9/applications?keyword=kim");
        assertThat(model.get("currentPage")).isEqualTo(1);
    }

    @Test
    void applicationsCanScopeByDocumentSrlQueryParam() {
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));
        when(applicationService.countApplications(9L, "kim")).thenReturn(1);
        when(applicationService.getApplicationPage(9L, "kim", 12, 0)).thenReturn(List.of(applicationRecord(11L, 9L)));
        when(applicationService.getJobFilters()).thenReturn(List.of());
        when(applicationService.getAllowedStatuses()).thenReturn(List.of("RECEIVED", "REVIEWING"));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/applications");
        request.setQueryString("documentSrl=9&keyword=kim");
        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = applicationController.applications(9L, "kim", null, model, request, null);

        assertThat(viewName).isEqualTo("applications/list");
        assertThat(model.get("selectedDocumentSrl")).isEqualTo(9L);
        assertThat(model.get("returnTo")).isEqualTo("/applications?documentSrl=9&keyword=kim");
        verify(jobService).requireApplicationBoard(9L);
        verify(jobService).getJob(9L);
        verify(applicationService).countApplications(9L, "kim");
        verify(applicationService).getApplicationPage(9L, "kim", 12, 0);
    }

    @Test
    void applicationDetailPopulatesModel() {
        ApplicationRecord application = applicationRecord(11L, 9L);
        when(applicationService.getApplicationDetail(11L)).thenReturn(new ApplicationDetail(application, jobDetail(9L), List.of(), List.of()));
        when(applicationService.getAllowedStatuses()).thenReturn(List.of("RECEIVED", "REVIEWING"));

        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = applicationController.applicationDetail(11L, model, new MockHttpServletRequest("GET", "/applications/11"), null);

        assertThat(viewName).isEqualTo("applications/detail");
        assertThat(model.get("pageTitle")).isEqualTo("지원서 상세");
        assertThat(model.get("pageDescription")).isEqualTo("단일 지원서와 동적 응답을 상세하게 확인합니다.");
        assertThat(model.get("detail")).isNotNull();
        assertThat(model.get("statusOptions")).isEqualTo(List.of("RECEIVED", "REVIEWING"));
    }

    @Test
    void updateStatusRedirectsBackToList() {
        String viewName = applicationController.updateStatus(
                11L,
                "REVIEWING",
                "/applications",
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        assertThat(viewName).isEqualTo("redirect:/applications?statusUpdated");
        verify(applicationService).updateStatus(eq(11L), eq("REVIEWING"), any(AdminPrincipal.class), any(HttpServletRequest.class));
    }

    private ApplicationRecord applicationRecord(Long id, Long documentSrl) {
        ApplicationRecord application = new ApplicationRecord();
        application.setId(id);
        application.setDocumentSrl(documentSrl);
        application.setApplicantName("Kim");
        application.setApplicationStatus("RECEIVED");
        return application;
    }

    private JobDetail jobDetail(Long documentSrl) {
        XeJobDocument document = new XeJobDocument();
        document.setDocumentSrl(documentSrl);
        document.setTitle("Survey Job");
        document.setMid("newjob");
        document.setStatus("PUBLIC");
        return new JobDetail(document, null);
    }
}
