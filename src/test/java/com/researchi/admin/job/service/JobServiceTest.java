package com.researchi.admin.job.service;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.client.domain.ClientSummary;
import com.researchi.admin.client.service.ClientService;
import com.researchi.admin.job.domain.AdminJobMeta;
import com.researchi.admin.job.domain.JobListItem;
import com.researchi.admin.job.mapper.AdminJobMetaMapper;
import com.researchi.admin.job.web.JobForm;
import com.researchi.admin.keyword.service.KeywordExtractionService;
import com.researchi.admin.xe.domain.XeJobDocument;
import com.researchi.admin.xe.service.XeJobService;
import jakarta.servlet.http.HttpServletRequest;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private XeJobService xeJobService;

    @Mock
    private AdminJobMetaMapper adminJobMetaMapper;

    @Mock
    private AdminActionLogService adminActionLogService;

    @Mock
    private KeywordExtractionService keywordExtractionService;

    @Mock
    private ClientService clientService;

    @InjectMocks
    private JobService jobService;

    @Test
    void createJobCreatesXeDocumentMetaAndActionLog() {
        JobForm form = baseForm();
        AdminPrincipal principal = new AdminPrincipal(1L, "admin", "hash", "관리자", "Y", LocalDateTime.now().minusMinutes(1));
        when(xeJobService.createJobDocument(eq("newjob"), eq("신규 공고"), eq("본문"), eq("PUBLIC"), anyString())).thenReturn(321L);
        when(clientService.getClientSummary(7L)).thenReturn(new ClientSummary(7L, "클라이언트", "리서치팀", "담당자", "client@example.com", "reply@example.com", List.of("client@example.com", "client2@example.com"), true));
        doNothing().when(adminActionLogService).log(any(), any(), any(), any(), any(), any());

        Long documentSrl = jobService.createJob(form, principal, mockRequest());

        assertThat(documentSrl).isEqualTo(321L);
        ArgumentCaptor<AdminJobMeta> captor = ArgumentCaptor.forClass(AdminJobMeta.class);
        verify(adminJobMetaMapper).insert(captor.capture());
        assertThat(captor.getValue().getDocumentSrl()).isEqualTo(321L);
        assertThat(captor.getValue().getJobType()).isEqualTo("NEW");
        assertThat(captor.getValue().getRecruitStatus()).isEqualTo("RECRUITING");
        assertThat(captor.getValue().getClientId()).isEqualTo(7L);
        assertThat(captor.getValue().getClientName()).isEqualTo("클라이언트");
        assertThat(captor.getValue().getClientEmail()).isEqualTo("client@example.com");
        assertThat(captor.getValue().getClientEmails()).isEqualTo("client2@example.com");
        verify(keywordExtractionService).syncJobKeywords(321L);
        verify(adminActionLogService).log(eq(1L), eq("JOB_CREATE"), eq("JOB"), eq("321"), eq("공고 등록"), any(HttpServletRequest.class));
    }

    @Test
    void updateRecruitStatusUpdatesXeAndAdminMeta() {
        AdminPrincipal principal = new AdminPrincipal(1L, "admin", "hash", "관리자", "Y", LocalDateTime.now().minusMinutes(1));
        AdminJobMeta meta = new AdminJobMeta();
        meta.setDocumentSrl(15L);
        meta.setJobType("NEW");
        meta.setRecruitStatus("WAITING");
        meta.setApplicationEnabled("Y");
        meta.setAutoSendEnabled("N");
        meta.setAutoSendRepeatYn("N");

        when(adminJobMetaMapper.findByDocumentSrl(15L)).thenReturn(meta);
        doNothing().when(adminActionLogService).log(any(), any(), any(), any(), any(), any());

        jobService.updateRecruitStatus(15L, "CLOSED", principal, mockRequest());

        verify(xeJobService).updateJobStatus(15L, "CLOSED");
        assertThat(meta.getRecruitStatus()).isEqualTo("CLOSED");
        verify(adminJobMetaMapper).update(meta);
        verify(adminActionLogService).log(eq(1L), eq("JOB_STATUS_UPDATE"), eq("JOB"), eq("15"), eq("공고 상태 변경: CLOSED"), any(HttpServletRequest.class));
    }

    @Test
    void updateRecruitStatusCreatesDefaultMetaWhenMissing() {
        AdminPrincipal principal = new AdminPrincipal(1L, "admin", "hash", "관리자", "Y", LocalDateTime.now().minusMinutes(1));
        XeJobDocument document = new XeJobDocument();
        document.setDocumentSrl(15L);
        document.setMid("newjob");

        when(adminJobMetaMapper.findByDocumentSrl(15L)).thenReturn(null);
        when(xeJobService.getJobDocument(15L)).thenReturn(document);
        doNothing().when(adminActionLogService).log(any(), any(), any(), any(), any(), any());

        jobService.updateRecruitStatus(15L, "CLOSED", principal, mockRequest());

        verify(xeJobService).updateJobStatus(15L, "CLOSED");
        ArgumentCaptor<AdminJobMeta> captor = ArgumentCaptor.forClass(AdminJobMeta.class);
        verify(adminJobMetaMapper).insert(captor.capture());
        assertThat(captor.getValue().getDocumentSrl()).isEqualTo(15L);
        assertThat(captor.getValue().getJobType()).isEqualTo("NEW");
        assertThat(captor.getValue().getRecruitStatus()).isEqualTo("CLOSED");
        assertThat(captor.getValue().getApplicationEnabled()).isEqualTo("Y");
        assertThat(captor.getValue().getAutoSendEnabled()).isEqualTo("N");
        assertThat(captor.getValue().getAutoSendRepeatYn()).isEqualTo("N");
        assertThat(captor.getValue().getAutoSendAttachmentType()).isEqualTo("XLSX");
    }

    @Test
    void toFormFallsBackToXeStatusWhenMetaIsMissing() {
        XeJobDocument document = new XeJobDocument();
        document.setDocumentSrl(9L);
        document.setMid("additional");
        document.setTitle("추가 공고");
        document.setContent("본문");
        document.setStatus("PUBLIC");

        JobForm form = jobService.toForm(new com.researchi.admin.job.domain.JobDetail(document, null));

        assertThat(form.getJobType()).isEqualTo("ADDITIONAL");
        assertThat(form.getRecruitStatus()).isEqualTo("RECRUITING");
        assertThat(form.getApplicationEnabled()).isTrue();
        assertThat(form.getAutoSendEnabled()).isFalse();
    }

    @Test
    void getJobsIgnoresDuplicateMetaRowsForSameDocument() {
        XeJobDocument document = new XeJobDocument();
        document.setDocumentSrl(9L);
        document.setMid("newjob");
        document.setTitle("공고");
        document.setContent("본문");
        document.setStatus("PUBLIC");

        AdminJobMeta first = new AdminJobMeta();
        first.setDocumentSrl(9L);
        first.setJobType("NEW");
        first.setClientName("First");

        AdminJobMeta latest = new AdminJobMeta();
        latest.setDocumentSrl(9L);
        latest.setJobType("ADDITIONAL");
        latest.setClientName("Latest");

        when(xeJobService.getJobDocuments()).thenReturn(List.of(document));
        when(adminJobMetaMapper.findAll()).thenReturn(List.of(first, latest));

        List<JobListItem> jobs = jobService.getJobs();

        assertThat(jobs).hasSize(1);
        assertThat(jobs.get(0).getMeta()).isSameAs(latest);
        assertThat(jobs.get(0).getJobType()).isEqualTo("ADDITIONAL");
    }

    @Test
    void ensureJobMetaCreatesDefaultMetaForExistingXeJob() {
        XeJobDocument document = new XeJobDocument();
        document.setDocumentSrl(22L);
        document.setMid("newjob");
        document.setStatus("PUBLIC");

        when(adminJobMetaMapper.findByDocumentSrl(22L)).thenReturn(null);
        when(xeJobService.getJobDocument(22L)).thenReturn(document);

        AdminJobMeta meta = jobService.ensureJobMeta(22L);

        assertThat(meta.getDocumentSrl()).isEqualTo(22L);
        assertThat(meta.getJobType()).isEqualTo("NEW");
        assertThat(meta.getRecruitStatus()).isEqualTo("RECRUITING");
        assertThat(meta.getApplicationEnabled()).isEqualTo("Y");
        assertThat(meta.getAutoSendEnabled()).isEqualTo("N");
        assertThat(meta.getAutoSendAttachmentType()).isEqualTo("XLSX");
        verify(adminJobMetaMapper).insert(any(AdminJobMeta.class));
    }

    private JobForm baseForm() {
        JobForm form = new JobForm();
        form.setJobType("NEW");
        form.setTitle("신규 공고");
        form.setContent("본문");
        form.setRecruitStatus("RECRUITING");
        form.setRewardText("5만원");
        form.setPlaceText("서울");
        form.setCloseDate(LocalDate.of(2026, 4, 30));
        form.setClientId(7L);
        form.setApplicationEnabled(true);
        form.setAutoSendEnabled(false);
        form.setAutoSendRepeatYn("N");
        return form;
    }

    private HttpServletRequest mockRequest() {
        return new org.springframework.mock.web.MockHttpServletRequest();
    }
}
