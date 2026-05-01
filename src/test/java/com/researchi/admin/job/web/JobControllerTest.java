package com.researchi.admin.job.web;

import com.researchi.admin.client.service.ClientService;
import com.researchi.admin.job.domain.AdminJobMeta;
import com.researchi.admin.job.domain.BoardConfig;
import com.researchi.admin.job.domain.JobDetail;
import com.researchi.admin.job.domain.JobListItem;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.mailing.service.MailTemplateService;
import com.researchi.admin.matching.service.MatchingService;
import com.researchi.admin.notification.config.NotificationProperties;
import com.researchi.admin.notification.service.NotificationService;
import com.researchi.admin.publicform.domain.PublicFormAvailability;
import com.researchi.admin.publicform.service.PublicFormService;
import com.researchi.admin.xe.domain.XeJobDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobControllerTest {

    @Mock
    private JobService jobService;

    @Mock
    private MailTemplateService mailTemplateService;

    @Mock
    private ClientService clientService;

    @Mock
    private PublicFormService publicFormService;

    @Mock
    private NotificationProperties notificationProperties;

    @Mock
    private MatchingService matchingService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private JobController jobController;

    @Test
    void jobsFiltersByKeywordWithinSelectedJobTypeUsingDatabasePage() {
        when(jobService.countJobs("NEW", "car interview")).thenReturn(1);
        when(jobService.getJobPage("NEW", "car interview", 12, 0))
                .thenReturn(List.of(jobListItem(9L, "New Car Interview", "NEW")));

        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/jobs");

        String viewName = jobController.jobs(model, "NEW", "car interview", null, null, request, null);

        assertThat(viewName).isEqualTo("jobs/list");
        assertThat(model.get("keyword")).isEqualTo("car interview");
        assertThat(model.get("selectedJobType")).isEqualTo("NEW");
        @SuppressWarnings("unchecked")
        List<JobListItem> jobs = (List<JobListItem>) model.get("jobs");
        assertThat(jobs).extracting(JobListItem::getDocumentSrl).containsExactly(9L);
        verify(jobService).countJobs("NEW", "car interview");
        verify(jobService).getJobPage("NEW", "car interview", 12, 0);
    }

    @Test
    void jobsMatchesSimilarTitleWhenKeywordSpacingDiffersUsingDatabasePage() {
        when(jobService.countJobs(null, "car buyer")).thenReturn(1);
        when(jobService.getJobPage(null, "car buyer", 12, 0))
                .thenReturn(List.of(jobListItem(9L, "CarBuyer Interview", "NEW")));

        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/jobs");

        String viewName = jobController.jobs(model, null, "car buyer", null, null, request, null);

        assertThat(viewName).isEqualTo("jobs/list");
        @SuppressWarnings("unchecked")
        List<JobListItem> jobs = (List<JobListItem>) model.get("jobs");
        assertThat(jobs).extracting(JobListItem::getDocumentSrl).containsExactly(9L);
        verify(jobService).countJobs(null, "car buyer");
        verify(jobService).getJobPage(null, "car buyer", 12, 0);
    }

    @Test
    void jobsUsesDatabasePageWhenKeywordIsBlank() {
        when(jobService.countJobs("NEW", null)).thenReturn(25);
        when(jobService.getJobPage("NEW", null, 12, 12)).thenReturn(List.of(jobListItem(9L, "Paged Job", "NEW")));

        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/jobs");
        request.setQueryString("jobType=NEW&page=2");

        String viewName = jobController.jobs(model, "NEW", null, 2, null, request, null);

        assertThat(viewName).isEqualTo("jobs/list");
        @SuppressWarnings("unchecked")
        List<JobListItem> jobs = (List<JobListItem>) model.get("jobs");
        assertThat(jobs).extracting(JobListItem::getDocumentSrl).containsExactly(9L);
        assertThat(model.get("currentPage")).isEqualTo(2);
        assertThat(model.get("totalPages")).isEqualTo(3);
        verify(jobService).countJobs("NEW", null);
        verify(jobService).getJobPage("NEW", null, 12, 12);
    }

    @Test
    void jobsUsesCursorPageForNextNavigation() {
        when(jobService.countJobs("NEW", null)).thenReturn(36);
        when(jobService.getJobPageAfter("NEW", null, 90L, 12))
                .thenReturn(List.of(jobListItem(89L, "Cursor Job", "NEW"), jobListItem(78L, "Next Cursor", "NEW")));

        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/jobs");
        request.setParameter("jobType", "NEW");
        request.setParameter("page", "2");
        request.setParameter("cursor", "90");

        String viewName = jobController.jobs(model, "NEW", null, 2, 90L, request, null);

        assertThat(viewName).isEqualTo("jobs/list");
        @SuppressWarnings("unchecked")
        List<JobListItem> jobs = (List<JobListItem>) model.get("jobs");
        assertThat(jobs).extracting(JobListItem::getDocumentSrl).containsExactly(89L, 78L);
        assertThat(model.get("nextPageUrl").toString()).contains("page=3", "cursor=78");
        assertThat(model.get("pageLinks").toString()).doesNotContain("cursor=90");
        verify(jobService).getJobPageAfter("NEW", null, 90L, 12);
    }

    @Test
    void jobDetailPopulatesModel() {
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));
        when(publicFormService.getAvailability(9L)).thenReturn(new PublicFormAvailability(true, "Apply now"));

        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/jobs/9");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8082);

        String viewName = jobController.jobDetail(9L, model, request, null);

        assertThat(viewName).isEqualTo("jobs/detail");
        assertThat(model.get("jobDetail")).isNotNull();
        assertThat(model.get("pageTitle")).isEqualTo("공고 상세");
        assertThat(model.get("publicApplyUrl")).isEqualTo("http://localhost:8082/apply/9");
        assertThat(model.get("publicApplyAvailable")).isEqualTo(true);
        assertThat(model.get("publicApplyMessage")).isEqualTo("Apply now");
        assertThat(model.get("applicationFormNoticeItems")).isEqualTo(List.of("Marriage", "Medication", "Allergy"));
    }

    @Test
    void newJobAlwaysShowsAllApplicationBoardTypes() {
        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/jobs/new");

        String viewName = jobController.newJob(model, request, null);

        assertThat(viewName).isEqualTo("jobs/form");
        JobForm form = (JobForm) model.get("jobForm");
        assertThat(form.getJobType()).isEqualTo("NEW");
        assertThat(model.get("jobTypes")).isEqualTo(BoardConfig.applicationBoards());
    }

    @Test
    void createJobReportsMissingXeBoardWithoutHidingAnnouncementTypes() {
        when(jobService.hasBoardModule("FAST")).thenReturn(false);

        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/jobs");
        JobForm form = validJobForm();
        form.setJobType("FAST");
        org.springframework.validation.BeanPropertyBindingResult bindingResult =
                new org.springframework.validation.BeanPropertyBindingResult(form, "jobForm");

        String viewName = jobController.createJob(null, form, bindingResult, request, model);

        assertThat(viewName).isEqualTo("jobs/form");
        assertThat(bindingResult.hasFieldErrors("jobType")).isTrue();
        assertThat(bindingResult.getFieldError("jobType").getDefaultMessage()).contains("mid=fast");
        assertThat(model.get("jobTypes")).isEqualTo(BoardConfig.applicationBoards());
        assertThat(model.get("jobForm")).isSameAs(form);
    }

    @Test
    void deleteJobDelegatesAndRedirectsToList() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/jobs/9/delete");

        String viewName = jobController.deleteJob(9L, null, "중복 공고", request);

        assertThat(viewName).isEqualTo("redirect:/jobs?deleteScheduled");
        verify(jobService).deleteContentJob(9L, null, request, "중복 공고");
    }

    @Test
    void deleteJobRequiresReason() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/jobs/9/delete");

        String viewName = jobController.deleteJob(9L, null, " ", request);

        assertThat(viewName).isEqualTo("redirect:/jobs/9/edit?deleteReasonRequired");
    }

    private JobDetail jobDetail(Long documentSrl) {
        XeJobDocument document = new XeJobDocument();
        document.setDocumentSrl(documentSrl);
        document.setTitle("Survey Job");
        document.setContent("Job content");
        document.setMid("newjob");
        document.setStatus("PUBLIC");

        AdminJobMeta meta = new AdminJobMeta();
        meta.setApplicationFormNotice("Marriage/Medication\nAllergy");
        return new JobDetail(document, meta);
    }

    private JobListItem jobListItem(Long documentSrl, String title, String jobType) {
        AdminJobMeta meta = new AdminJobMeta();
        meta.setJobType(jobType);
        return new JobListItem(documentSrl, title, "content", "PUBLIC", "20260424120000", "20260424120000", meta, "NEW".equals(jobType) ? "newjob" : "additional");
    }

    private JobForm validJobForm() {
        JobForm form = new JobForm();
        form.setJobType("NEW");
        form.setTitle("공고");
        form.setContent("본문");
        form.setRecruitStatus("RECRUITING");
        form.setApplicationEnabled(true);
        form.setAutoSendEnabled(false);
        form.setAutoSendRepeatYn("N");
        form.setAutoSendAttachmentType("XLSX");
        return form;
    }
}
