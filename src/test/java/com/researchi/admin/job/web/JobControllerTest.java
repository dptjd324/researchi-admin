package com.researchi.admin.job.web;

import com.researchi.admin.client.service.ClientService;
import com.researchi.admin.job.domain.AdminJobMeta;
import com.researchi.admin.job.domain.JobDetail;
import com.researchi.admin.job.domain.JobListItem;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.mailing.service.MailTemplateService;
import com.researchi.admin.notification.config.NotificationProperties;
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

    @InjectMocks
    private JobController jobController;

    @Test
    void jobsFiltersByKeywordWithinSelectedJobType() {
        when(jobService.getJobs()).thenReturn(List.of(
                jobListItem(9L, "신규 자동차 좌담회", "NEW"),
                jobListItem(10L, "추가 자동차 좌담회", "ADDITIONAL"),
                jobListItem(11L, "신규 식품 테스트", "NEW")
        ));

        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/jobs");

        String viewName = jobController.jobs(model, "NEW", "자동차 좌담", null, request, null);

        assertThat(viewName).isEqualTo("jobs/list");
        assertThat(model.get("keyword")).isEqualTo("자동차 좌담");
        assertThat(model.get("selectedJobType")).isEqualTo("NEW");
        @SuppressWarnings("unchecked")
        List<JobListItem> jobs = (List<JobListItem>) model.get("jobs");
        assertThat(jobs).extracting(JobListItem::getDocumentSrl).containsExactly(9L);
    }

    @Test
    void jobsMatchesSimilarTitleWhenKeywordSpacingDiffers() {
        when(jobService.getJobs()).thenReturn(List.of(
                jobListItem(9L, "자동차구매자 좌담회", "NEW"),
                jobListItem(10L, "식품 테스트", "NEW")
        ));

        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/jobs");

        String viewName = jobController.jobs(model, null, "자동차 구매자", null, request, null);

        assertThat(viewName).isEqualTo("jobs/list");
        @SuppressWarnings("unchecked")
        List<JobListItem> jobs = (List<JobListItem>) model.get("jobs");
        assertThat(jobs).extracting(JobListItem::getDocumentSrl).containsExactly(9L);
    }

    @Test
    void jobDetailPopulatesModel() {
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));
        when(publicFormService.getAvailability(9L)).thenReturn(new PublicFormAvailability(true, "현재 신청 가능한 공고입니다."));

        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/jobs/9");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8082);

        String viewName = jobController.jobDetail(9L, model, request);

        assertThat(viewName).isEqualTo("jobs/detail");
        assertThat(model.get("jobDetail")).isNotNull();
        assertThat(model.get("pageTitle")).isEqualTo("공고 상세");
        assertThat(model.get("publicApplyUrl")).isEqualTo("http://localhost:8082/apply/9");
        assertThat(model.get("publicApplyAvailable")).isEqualTo(true);
        assertThat(model.get("publicApplyMessage")).isEqualTo("현재 신청 가능한 공고입니다.");
        assertThat(model.get("applicationFormNoticeItems")).isEqualTo(List.of("결혼여부", "자녀유무", "알러지 유무"));
    }

    private JobDetail jobDetail(Long documentSrl) {
        XeJobDocument document = new XeJobDocument();
        document.setDocumentSrl(documentSrl);
        document.setTitle("Survey Job");
        document.setContent("Job content");
        document.setMid("newjob");
        document.setStatus("PUBLIC");

        AdminJobMeta meta = new AdminJobMeta();
        meta.setApplicationFormNotice("결혼여부/자녀유무\n알러지 유무");
        return new JobDetail(document, meta);
    }

    private JobListItem jobListItem(Long documentSrl, String title, String jobType) {
        AdminJobMeta meta = new AdminJobMeta();
        meta.setJobType(jobType);
        return new JobListItem(documentSrl, title, "content", "PUBLIC", "20260424120000", "20260424120000", meta, "NEW".equals(jobType) ? "newjob" : "additional");
    }
}
