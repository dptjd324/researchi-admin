package com.researchi.admin.mailing.service;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.client.domain.ClientSummary;
import com.researchi.admin.client.service.ClientService;
import com.researchi.admin.export.domain.ExportPayload;
import com.researchi.admin.export.mapper.AdminExportQueryMapper;
import com.researchi.admin.export.service.ExportService;
import com.researchi.admin.job.domain.AdminJobMeta;
import com.researchi.admin.job.domain.JobDetail;
import com.researchi.admin.job.domain.JobListItem;
import com.researchi.admin.job.mapper.AdminJobMetaMapper;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.mailing.domain.AdminMailSendJob;
import com.researchi.admin.mailing.domain.AdminMailSendTarget;
import com.researchi.admin.mailing.domain.AdminMailTemplate;
import com.researchi.admin.mailing.domain.MailingPreview;
import com.researchi.admin.mailing.web.MailThresholdSettingsForm;
import com.researchi.admin.mailing.mapper.AdminMailSendJobMapper;
import com.researchi.admin.mailing.mapper.AdminMailSendTargetMapper;
import com.researchi.admin.mailing.mapper.AdminMailTemplateMapper;
import com.researchi.admin.mailing.mapper.AdminMailingApplicationMapper;
import com.researchi.admin.mailing.web.MailSendManualForm;
import com.researchi.admin.publicform.config.PublicFormProperties;
import com.researchi.admin.publicform.service.PublicFormProtectionService;
import com.researchi.admin.xe.domain.XeJobDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailingServiceTest {

    @Mock
    private AdminMailTemplateMapper adminMailTemplateMapper;
    @Mock
    private AdminMailSendJobMapper adminMailSendJobMapper;
    @Mock
    private AdminMailSendTargetMapper adminMailSendTargetMapper;
    @Mock
    private AdminMailingApplicationMapper adminMailingApplicationMapper;
    @Mock
    private AdminJobMetaMapper adminJobMetaMapper;
    @Mock
    private AdminExportQueryMapper adminExportQueryMapper;
    @Mock
    private ExportService exportService;
    @Mock
    private JobService jobService;
    @Mock
    private ClientService clientService;
    @Mock
    private MailDispatchGateway mailDispatchGateway;
    @Mock
    private AdminActionLogService adminActionLogService;

    @InjectMocks
    private MailingService mailingService;

    @Test
    void historyShowsFullRecipientsWhenStoredSnapshotIsMasked() {
        AdminMailSendJob sendJob = new AdminMailSendJob();
        sendJob.setId(77L);
        sendJob.setDocumentSrl(9L);
        when(adminMailSendJobMapper.findByDocumentSrl(9L)).thenReturn(List.of(sendJob));
        when(jobService.getJobsByDocumentSrls(List.of(9L))).thenReturn(List.of(
                new JobListItem(9L, "Survey Job", "", "PUBLIC", null, null, null, "newjob")
        ));

        AdminMailSendTarget target = new AdminMailSendTarget();
        target.setSendJobId(77L);
        target.setTargetEmailMasked("dp***@naver.com");
        when(adminMailSendTargetMapper.findBySendJobIds(List.of(77L))).thenReturn(List.of(target));

        AdminJobMeta jobMeta = new AdminJobMeta();
        jobMeta.setDocumentSrl(9L);
        jobMeta.setClientName("Client A");
        jobMeta.setClientEmail("dptjd324@naver.com");
        when(jobService.ensureJobMeta(9L)).thenReturn(jobMeta);

        List<com.researchi.admin.mailing.domain.MailingHistoryItem> history = mailingService.getHistory(9L);

        assertThat(history).hasSize(1);
        assertThat(history.get(0).recipientAddresses()).containsExactly("dptjd324@naver.com");
        assertThat(history.get(0).recipientAddressesSummary()).isEqualTo("dptjd324@naver.com");
    }

    @Test
    void historyShowsCumulativeSentCountPerDocument() {
        AdminMailSendJob firstSent = historyJob(77L, 9L, "SENT", 4);
        firstSent.setSentAt(LocalDateTime.of(2026, 4, 28, 12, 30));
        AdminMailSendJob secondSent = historyJob(78L, 9L, "SENT", 3);
        secondSent.setSentAt(LocalDateTime.of(2026, 4, 30, 2, 8));
        AdminMailSendJob failed = historyJob(79L, 9L, "FAILED", 2);
        failed.setCreatedAt(LocalDateTime.of(2026, 5, 1, 9, 0));
        when(adminMailSendJobMapper.findByDocumentSrl(9L)).thenReturn(List.of(failed, secondSent, firstSent));
        when(jobService.getJobsByDocumentSrls(List.of(9L))).thenReturn(List.of(
                new JobListItem(9L, "Survey Job", "", "PUBLIC", null, null, null, "newjob")
        ));
        when(adminMailSendTargetMapper.findBySendJobIds(List.of(79L, 78L, 77L))).thenReturn(List.of());

        List<com.researchi.admin.mailing.domain.MailingHistoryItem> history = mailingService.getHistory(9L);

        assertThat(history).hasSize(3);
        assertThat(history).extracting(com.researchi.admin.mailing.domain.MailingHistoryItem::cumulativeSentCount)
                .containsExactly(7, 7, 4);
    }

    @Test
    void historyDisplaysNewestActivityFirstEvenWhenMapperReturnsOlderRowsFirst() {
        AdminMailSendJob firstSent = historyJob(77L, 9L, "SENT", 4);
        firstSent.setSentAt(LocalDateTime.of(2026, 4, 28, 12, 30));
        AdminMailSendJob secondSent = historyJob(78L, 9L, "SENT", 3);
        secondSent.setSentAt(LocalDateTime.of(2026, 4, 30, 2, 8));
        AdminMailSendJob scheduled = historyJob(79L, 9L, "SCHEDULED", 0);
        scheduled.setScheduledAt(LocalDateTime.of(2026, 5, 2, 10, 0));
        when(adminMailSendJobMapper.findByDocumentSrl(9L)).thenReturn(List.of(firstSent, secondSent, scheduled));
        when(jobService.getJobsByDocumentSrls(List.of(9L))).thenReturn(List.of(
                new JobListItem(9L, "Survey Job", "", "PUBLIC", null, null, null, "newjob")
        ));
        when(adminMailSendTargetMapper.findBySendJobIds(List.of(79L, 78L, 77L))).thenReturn(List.of());

        List<com.researchi.admin.mailing.domain.MailingHistoryItem> history = mailingService.getHistory(9L);

        assertThat(history).extracting(item -> item.sendJob().getId())
                .containsExactly(79L, 78L, 77L);
        assertThat(history).extracting(com.researchi.admin.mailing.domain.MailingHistoryItem::cumulativeSentCount)
                .containsExactly(7, 7, 4);
    }

    @Test
    void historySeparatesCumulativeSentCountByDocumentSrlNotTitle() {
        AdminMailSendJob firstDocument = historyJob(81L, 9L, "SENT", 4);
        firstDocument.setSentAt(LocalDateTime.of(2026, 4, 28, 12, 30));
        AdminMailSendJob sameTitleOtherDocument = historyJob(82L, 10L, "SENT", 3);
        sameTitleOtherDocument.setSentAt(LocalDateTime.of(2026, 4, 29, 12, 30));
        when(adminMailSendJobMapper.findAll()).thenReturn(List.of(sameTitleOtherDocument, firstDocument));
        when(jobService.getJobsByDocumentSrls(List.of(10L, 9L))).thenReturn(List.of(
                new JobListItem(9L, "Survey Job", "", "PUBLIC", null, null, null, "newjob"),
                new JobListItem(10L, "Survey Job", "", "PUBLIC", null, null, null, "newjob")
        ));
        when(adminMailSendTargetMapper.findBySendJobIds(List.of(82L, 81L))).thenReturn(List.of());

        List<com.researchi.admin.mailing.domain.MailingHistoryItem> history = mailingService.getHistory(null);

        assertThat(history).hasSize(2);
        assertThat(history).extracting(com.researchi.admin.mailing.domain.MailingHistoryItem::cumulativeSentCount)
                .containsExactly(3, 4);
    }

    @Test
    void updateThresholdSettingsStoresOnlyThresholdMailFields() {
        AdminJobMeta jobMeta = new AdminJobMeta();
        jobMeta.setDocumentSrl(9L);
        when(jobService.ensureJobMeta(9L)).thenReturn(jobMeta);

        AdminMailTemplate template = new AdminMailTemplate();
        template.setId(3L);
        template.setActiveYn("Y");
        when(adminMailTemplateMapper.findById(3L)).thenReturn(template);
        when(adminJobMetaMapper.updateThresholdMailSettings(9L, "Y", "THRESHOLD", 5, 3L, "TXT")).thenReturn(1);

        MailThresholdSettingsForm form = new MailThresholdSettingsForm();
        form.setDocumentSrl(9L);
        form.setAutoSendEnabled(true);
        form.setAutoSendThreshold(5);
        form.setAutoSendTemplateId(3L);
        form.setAutoSendAttachmentType("TXT");

        mailingService.updateThresholdSettings(
                form,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        verify(adminJobMetaMapper).updateThresholdMailSettings(9L, "Y", "THRESHOLD", 5, 3L, "TXT");
        verify(adminActionLogService).log(eq(1L), eq("MAIL_THRESHOLD_SETTINGS_UPDATE"), eq("JOB"), eq("9"), any(), any());
    }

    @Test
    void updateThresholdSettingsAllowsDirectContentWhenTemplateIsEmpty() {
        AdminJobMeta jobMeta = new AdminJobMeta();
        jobMeta.setDocumentSrl(9L);
        when(jobService.ensureJobMeta(9L)).thenReturn(jobMeta);
        when(adminJobMetaMapper.updateThresholdMailSettings(9L, "Y", "THRESHOLD", 5, null, "XLSX")).thenReturn(1);

        MailThresholdSettingsForm form = new MailThresholdSettingsForm();
        form.setDocumentSrl(9L);
        form.setAutoSendEnabled(true);
        form.setAutoSendThreshold(5);
        form.setAutoSendTemplateId(null);
        form.setAutoSendAttachmentType("XLSX");

        mailingService.updateThresholdSettings(
                form,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        verify(adminMailTemplateMapper, never()).findById(any());
        verify(adminJobMetaMapper).updateThresholdMailSettings(9L, "Y", "THRESHOLD", 5, null, "XLSX");
    }

    @Test
    void manualSendBuildsSnapshotExcludesBlacklistedAndUpdatesDeliveryStatus() throws Exception {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setEncryptionKey("test-encryption-key");
        properties.setCaptchaEnabled(false);
        PublicFormProtectionService protectionService = new PublicFormProtectionService(properties);
        mailingService = new MailingService(
                adminMailTemplateMapper,
                adminMailSendJobMapper,
                adminMailSendTargetMapper,
                adminMailingApplicationMapper,
                adminJobMetaMapper,
                adminExportQueryMapper,
                exportService,
                jobService,
                clientService,
                protectionService,
                mailDispatchGateway,
                adminActionLogService
        );

        AdminMailTemplate template = new AdminMailTemplate();
        template.setId(3L);
        template.setTemplateName("Client Send");
        template.setMailSubject("{{jobTitle}}");
        template.setMailBody("Count {{applicationCount}}");
        template.setActiveYn("Y");
        when(adminMailTemplateMapper.findById(3L)).thenReturn(template);

        AdminJobMeta jobMeta = new AdminJobMeta();
        jobMeta.setDocumentSrl(9L);
        jobMeta.setClientName("Client A");
        jobMeta.setClientEmail("client@example.com");
        jobMeta.setClientEmails("client2@example.com");
        when(jobService.ensureJobMeta(9L)).thenReturn(jobMeta);

        when(adminExportQueryMapper.findApplicationsByDocumentSrl(9L)).thenReturn(List.of(
                exportApplication(101L, "Y", "N"),
                exportApplication(102L, "Y", "Y")
        ));
        when(exportService.prepareXlsx(eq(9L), eq(List.of(101L))))
                .thenReturn(new ExportPayload("job-9.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2}, 1));
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));
        doAnswer(invocation -> {
            AdminMailSendJob sendJob = invocation.getArgument(0);
            sendJob.setId(55L);
            return null;
        }).when(adminMailSendJobMapper).insert(any(AdminMailSendJob.class));
        AdminMailSendJob persistedJob = new AdminMailSendJob();
        persistedJob.setId(55L);
        persistedJob.setSendStatus("SENT");
        when(adminMailSendJobMapper.findById(55L)).thenReturn(persistedJob);

        MailSendManualForm form = new MailSendManualForm();
        form.setDocumentSrl(9L);
        form.setTemplateId(3L);
        form.setAttachmentType("XLSX");

        Long sendJobId = mailingService.sendManual(
                form,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        assertThat(sendJobId).isEqualTo(55L);
        ArgumentCaptor<com.researchi.admin.mailing.domain.MailDispatchRequest> dispatchCaptor = ArgumentCaptor.forClass(com.researchi.admin.mailing.domain.MailDispatchRequest.class);
        ArgumentCaptor<com.researchi.admin.mailing.domain.AdminMailSendTarget> targetCaptor = ArgumentCaptor.forClass(com.researchi.admin.mailing.domain.AdminMailSendTarget.class);
        verify(adminMailSendJobMapper).insert(any(AdminMailSendJob.class));
        verify(mailDispatchGateway).dispatch(dispatchCaptor.capture());
        assertThat(dispatchCaptor.getValue().recipients()).containsExactly("client@example.com", "client2@example.com");
        verify(adminMailSendTargetMapper, times(2)).insert(targetCaptor.capture());
        assertThat(targetCaptor.getAllValues()).allSatisfy(target -> {
            assertThat(target.getApplicationId()).isEqualTo(101L);
            assertThat(target.getSendResult()).isEqualTo("SENT");
        });
        assertThat(targetCaptor.getAllValues())
                .extracting(com.researchi.admin.mailing.domain.AdminMailSendTarget::getTargetEmailMasked)
                .containsExactly("client@example.com", "client2@example.com");
        verify(adminMailingApplicationMapper).updateDeliveryStatus(eq(101L), eq("SENT"), eq(55L), any());
        verify(adminMailingApplicationMapper, times(1)).updateDeliveryStatus(any(), any(), any(), any());
    }

    @Test
    void manualSendThrowsWhenThereAreNoEligibleApplications() {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setEncryptionKey("test-encryption-key");
        properties.setCaptchaEnabled(false);
        PublicFormProtectionService protectionService = new PublicFormProtectionService(properties);
        mailingService = new MailingService(
                adminMailTemplateMapper,
                adminMailSendJobMapper,
                adminMailSendTargetMapper,
                adminMailingApplicationMapper,
                adminJobMetaMapper,
                adminExportQueryMapper,
                exportService,
                jobService,
                clientService,
                protectionService,
                mailDispatchGateway,
                adminActionLogService
        );

        AdminMailTemplate template = new AdminMailTemplate();
        template.setId(3L);
        template.setActiveYn("Y");
        when(adminMailTemplateMapper.findById(3L)).thenReturn(template);

        AdminJobMeta jobMeta = new AdminJobMeta();
        jobMeta.setDocumentSrl(9L);
        jobMeta.setClientName("Client A");
        jobMeta.setClientEmail("client@example.com");
        when(jobService.ensureJobMeta(9L)).thenReturn(jobMeta);

        when(adminExportQueryMapper.findApplicationsByDocumentSrl(9L)).thenReturn(List.of());
        doAnswer(invocation -> {
            AdminMailSendJob sendJob = invocation.getArgument(0);
            sendJob.setId(56L);
            return null;
        }).when(adminMailSendJobMapper).insert(any(AdminMailSendJob.class));
        AdminMailSendJob persistedJob = new AdminMailSendJob();
        persistedJob.setId(56L);
        persistedJob.setSendStatus("NO_TARGETS");
        when(adminMailSendJobMapper.findById(56L)).thenReturn(persistedJob);

        MailSendManualForm form = new MailSendManualForm();
        form.setDocumentSrl(9L);
        form.setTemplateId(3L);
        form.setAttachmentType("XLSX");

        assertThatThrownBy(() -> mailingService.sendManual(
                form,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        )).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void manualSendThrowsWhenDispatchFails() throws Exception {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setEncryptionKey("test-encryption-key");
        properties.setCaptchaEnabled(false);
        PublicFormProtectionService protectionService = new PublicFormProtectionService(properties);
        mailingService = new MailingService(
                adminMailTemplateMapper,
                adminMailSendJobMapper,
                adminMailSendTargetMapper,
                adminMailingApplicationMapper,
                adminJobMetaMapper,
                adminExportQueryMapper,
                exportService,
                jobService,
                clientService,
                protectionService,
                mailDispatchGateway,
                adminActionLogService
        );

        AdminMailTemplate template = new AdminMailTemplate();
        template.setId(3L);
        template.setTemplateName("Client Send");
        template.setMailSubject("{{jobTitle}}");
        template.setMailBody("Count {{applicationCount}}");
        template.setActiveYn("Y");
        when(adminMailTemplateMapper.findById(3L)).thenReturn(template);

        AdminJobMeta jobMeta = new AdminJobMeta();
        jobMeta.setDocumentSrl(9L);
        jobMeta.setClientName("Client A");
        jobMeta.setClientEmail("client@example.com");
        when(jobService.ensureJobMeta(9L)).thenReturn(jobMeta);

        when(adminExportQueryMapper.findApplicationsByDocumentSrl(9L)).thenReturn(List.of(
                exportApplication(101L, "Y", "N")
        ));
        when(exportService.prepareXlsx(eq(9L), eq(List.of(101L))))
                .thenReturn(new ExportPayload("job-9.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2}, 1));
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));
        doAnswer(invocation -> {
            AdminMailSendJob sendJob = invocation.getArgument(0);
            sendJob.setId(57L);
            return null;
        }).when(adminMailSendJobMapper).insert(any(AdminMailSendJob.class));
        doThrow(new IllegalStateException("SMTP authentication failed")).when(mailDispatchGateway).dispatch(any());
        AdminMailSendJob persistedJob = new AdminMailSendJob();
        persistedJob.setId(57L);
        persistedJob.setSendStatus("FAILED");
        when(adminMailSendJobMapper.findById(57L)).thenReturn(persistedJob);
        AdminMailSendTarget failedTarget = new AdminMailSendTarget();
        failedTarget.setFailReason("SMTP authentication failed");
        when(adminMailSendTargetMapper.findBySendJobId(57L)).thenReturn(List.of(failedTarget));

        MailSendManualForm form = new MailSendManualForm();
        form.setDocumentSrl(9L);
        form.setTemplateId(3L);
        form.setAttachmentType("XLSX");

        assertThatThrownBy(() -> mailingService.sendManual(
                form,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SMTP authentication failed");
    }

    @Test
    void scheduleStoresSnapshotTargetsAndScheduledDeliveryState() throws Exception {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setEncryptionKey("test-encryption-key");
        properties.setCaptchaEnabled(false);
        PublicFormProtectionService protectionService = new PublicFormProtectionService(properties);
        mailingService = new MailingService(
                adminMailTemplateMapper,
                adminMailSendJobMapper,
                adminMailSendTargetMapper,
                adminMailingApplicationMapper,
                adminJobMetaMapper,
                adminExportQueryMapper,
                exportService,
                jobService,
                clientService,
                protectionService,
                mailDispatchGateway,
                adminActionLogService
        );

        AdminMailTemplate template = new AdminMailTemplate();
        template.setId(3L);
        template.setActiveYn("Y");
        when(adminMailTemplateMapper.findById(3L)).thenReturn(template);

        AdminJobMeta jobMeta = new AdminJobMeta();
        jobMeta.setDocumentSrl(9L);
        jobMeta.setClientName("Client A");
        jobMeta.setClientEmail("client@example.com");
        when(jobService.ensureJobMeta(9L)).thenReturn(jobMeta);

        when(adminExportQueryMapper.findApplicationsByDocumentSrl(9L)).thenReturn(List.of(
                exportApplication(101L, "Y", "N")
        ));
        when(adminMailSendJobMapper.findByDuplicatePreventKey(any())).thenReturn(null);
        doAnswer(invocation -> {
            AdminMailSendJob sendJob = invocation.getArgument(0);
            sendJob.setId(77L);
            return null;
        }).when(adminMailSendJobMapper).insert(any(AdminMailSendJob.class));

        var form = new com.researchi.admin.mailing.web.MailScheduleForm();
        form.setDocumentSrl(9L);
        form.setTemplateId(3L);
        form.setAttachmentType("XLSX");
        form.setScheduledAt(LocalDateTime.now().plusHours(2).withSecond(0).withNano(0));

        Long sendJobId = mailingService.schedule(
                form,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        assertThat(sendJobId).isEqualTo(77L);
        ArgumentCaptor<com.researchi.admin.mailing.domain.AdminMailSendTarget> targetCaptor = ArgumentCaptor.forClass(com.researchi.admin.mailing.domain.AdminMailSendTarget.class);
        verify(adminMailSendJobMapper).insert(any(AdminMailSendJob.class));
        verify(adminMailSendTargetMapper).insert(targetCaptor.capture());
        assertThat(targetCaptor.getValue().getApplicationId()).isEqualTo(101L);
        assertThat(targetCaptor.getValue().getSendResult()).isEqualTo("PENDING");
        verify(adminMailingApplicationMapper).updateDeliveryStatus(eq(101L), eq("SCHEDULED"), eq(77L), eq(null));
        verify(mailDispatchGateway, times(0)).dispatch(any());
    }

    @Test
    void scheduleRejectsTimesThatAreNotAtLeastOneMinuteInTheFuture() throws Exception {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setEncryptionKey("test-encryption-key");
        properties.setCaptchaEnabled(false);
        PublicFormProtectionService protectionService = new PublicFormProtectionService(properties);
        mailingService = new MailingService(
                adminMailTemplateMapper,
                adminMailSendJobMapper,
                adminMailSendTargetMapper,
                adminMailingApplicationMapper,
                adminJobMetaMapper,
                adminExportQueryMapper,
                exportService,
                jobService,
                clientService,
                protectionService,
                mailDispatchGateway,
                adminActionLogService
        );

        var form = new com.researchi.admin.mailing.web.MailScheduleForm();
        form.setDocumentSrl(9L);
        form.setTemplateId(3L);
        form.setAttachmentType("XLSX");
        form.setScheduledAt(LocalDateTime.now().withSecond(0).withNano(0));

        assertThatThrownBy(() -> mailingService.schedule(
                form,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        )).isInstanceOf(IllegalArgumentException.class);

        verify(adminMailSendJobMapper, times(0)).insert(any(AdminMailSendJob.class));
        verify(mailDispatchGateway, times(0)).dispatch(any());
    }

    @Test
    void scheduleDailyRepeatCreatesDailyJobWithoutSnapshotTargets() throws Exception {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setEncryptionKey("test-encryption-key");
        properties.setCaptchaEnabled(false);
        PublicFormProtectionService protectionService = new PublicFormProtectionService(properties);
        mailingService = new MailingService(
                adminMailTemplateMapper,
                adminMailSendJobMapper,
                adminMailSendTargetMapper,
                adminMailingApplicationMapper,
                adminJobMetaMapper,
                adminExportQueryMapper,
                exportService,
                jobService,
                clientService,
                protectionService,
                mailDispatchGateway,
                adminActionLogService
        );

        AdminJobMeta jobMeta = new AdminJobMeta();
        jobMeta.setDocumentSrl(9L);
        jobMeta.setClientName("Client A");
        jobMeta.setClientEmail("client@example.com");
        when(jobService.ensureJobMeta(9L)).thenReturn(jobMeta);
        when(adminMailSendJobMapper.findByDuplicatePreventKey(any())).thenReturn(null);
        doAnswer(invocation -> {
            AdminMailSendJob sendJob = invocation.getArgument(0);
            sendJob.setId(93L);
            return null;
        }).when(adminMailSendJobMapper).insert(any(AdminMailSendJob.class));

        LocalTime sendTime = LocalTime.now().plusHours(2).truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
        var form = new com.researchi.admin.mailing.web.MailScheduleForm();
        form.setDocumentSrl(9L);
        form.setAttachmentType("XLSX");
        form.setDirectMailSubject("Daily subject");
        form.setDirectMailBody("Daily body");
        form.setDailyRepeat(true);
        form.setDailySendTime(sendTime);

        Long sendJobId = mailingService.schedule(
                form,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        assertThat(sendJobId).isEqualTo(93L);
        ArgumentCaptor<AdminMailSendJob> sendJobCaptor = ArgumentCaptor.forClass(AdminMailSendJob.class);
        verify(adminMailSendJobMapper).insert(sendJobCaptor.capture());
        AdminMailSendJob scheduledJob = sendJobCaptor.getValue();
        assertThat(scheduledJob.getTriggerType()).isEqualTo("SCHEDULED_DAILY");
        assertThat(scheduledJob.getSendStatus()).isEqualTo("SCHEDULED");
        assertThat(scheduledJob.getRepeatYn()).isEqualTo("Y");
        assertThat(scheduledJob.getRepeatUnit()).isEqualTo("DAILY");
        assertThat(scheduledJob.getTargetSnapshotCount()).isZero();
        assertThat(scheduledJob.getScheduledAt().toLocalTime()).isEqualTo(sendTime);
        verify(adminMailSendTargetMapper, never()).insert(any());
        verify(adminMailingApplicationMapper, never()).updateDeliveryStatus(any(), any(), any(), any());
        verify(mailDispatchGateway, never()).dispatch(any());
    }

    @Test
    void thresholdTriggerSendsWhenConfiguredThresholdIsReached() throws Exception {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setEncryptionKey("test-encryption-key");
        properties.setCaptchaEnabled(false);
        PublicFormProtectionService protectionService = new PublicFormProtectionService(properties);
        mailingService = new MailingService(
                adminMailTemplateMapper,
                adminMailSendJobMapper,
                adminMailSendTargetMapper,
                adminMailingApplicationMapper,
                adminJobMetaMapper,
                adminExportQueryMapper,
                exportService,
                jobService,
                clientService,
                protectionService,
                mailDispatchGateway,
                adminActionLogService
        );

        AdminMailTemplate template = new AdminMailTemplate();
        template.setId(3L);
        template.setTemplateName("Threshold Send");
        template.setMailSubject("{{jobTitle}}");
        template.setMailBody("Count {{applicationCount}}");
        template.setActiveYn("Y");
        when(adminMailTemplateMapper.findById(3L)).thenReturn(template);

        AdminJobMeta jobMeta = new AdminJobMeta();
        jobMeta.setDocumentSrl(9L);
        jobMeta.setClientName("Client A");
        jobMeta.setClientEmail("client@example.com");
        jobMeta.setAutoSendEnabled("Y");
        jobMeta.setAutoSendMode("THRESHOLD");
        jobMeta.setAutoSendThreshold(2);
        when(jobService.ensureJobMeta(9L)).thenReturn(jobMeta);

        when(adminExportQueryMapper.findApplicationsByDocumentSrl(9L)).thenReturn(List.of(
                exportApplication(101L, "Y", "N"),
                exportApplication(102L, "Y", "N")
        ));
        when(adminMailSendJobMapper.findByDuplicatePreventKey(any())).thenReturn(null);
        when(exportService.prepareXlsx(eq(9L), eq(List.of(101L, 102L))))
                .thenReturn(new ExportPayload("job-9.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2}, 2));
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));
        doAnswer(invocation -> {
            AdminMailSendJob sendJob = invocation.getArgument(0);
            sendJob.setId(88L);
            return null;
        }).when(adminMailSendJobMapper).insert(any(AdminMailSendJob.class));
        AdminMailSendJob persistedJob = new AdminMailSendJob();
        persistedJob.setId(88L);
        persistedJob.setSendStatus("SENT");
        when(adminMailSendJobMapper.findById(88L)).thenReturn(persistedJob);

        var form = new com.researchi.admin.mailing.web.MailThresholdTriggerForm();
        form.setDocumentSrl(9L);
        form.setTemplateId(3L);
        form.setAttachmentType("XLSX");

        Long sendJobId = mailingService.triggerThreshold(
                form,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        assertThat(sendJobId).isEqualTo(88L);
        verify(mailDispatchGateway).dispatch(any());
        verify(adminMailSendTargetMapper, times(2)).insert(any());
        verify(adminMailingApplicationMapper).updateDeliveryStatus(eq(101L), eq("SENT"), eq(88L), any());
        verify(adminMailingApplicationMapper).updateDeliveryStatus(eq(102L), eq("SENT"), eq(88L), any());
    }

    @Test
    void automaticThresholdUsesDefaultDirectContentWhenTemplateIsEmpty() throws Exception {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setEncryptionKey("test-encryption-key");
        properties.setCaptchaEnabled(false);
        PublicFormProtectionService protectionService = new PublicFormProtectionService(properties);
        mailingService = new MailingService(
                adminMailTemplateMapper,
                adminMailSendJobMapper,
                adminMailSendTargetMapper,
                adminMailingApplicationMapper,
                adminJobMetaMapper,
                adminExportQueryMapper,
                exportService,
                jobService,
                clientService,
                protectionService,
                mailDispatchGateway,
                adminActionLogService
        );

        AdminJobMeta jobMeta = new AdminJobMeta();
        jobMeta.setDocumentSrl(9L);
        jobMeta.setClientName("Client A");
        jobMeta.setClientEmail("client@example.com");
        jobMeta.setAutoSendEnabled("Y");
        jobMeta.setAutoSendMode("THRESHOLD");
        jobMeta.setAutoSendThreshold(2);
        jobMeta.setAutoSendTemplateId(null);
        jobMeta.setAutoSendAttachmentType("XLSX");
        when(jobService.ensureJobMeta(9L)).thenReturn(jobMeta);

        when(adminExportQueryMapper.findApplicationsByDocumentSrl(9L)).thenReturn(List.of(
                exportApplication(101L, "Y", "N"),
                exportApplication(102L, "Y", "N")
        ));
        when(adminMailSendJobMapper.findByDuplicatePreventKey(any())).thenReturn(null);
        when(exportService.prepareXlsx(eq(9L), eq(List.of(101L, 102L))))
                .thenReturn(new ExportPayload("job-9.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2}, 2));
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));
        doAnswer(invocation -> {
            AdminMailSendJob sendJob = invocation.getArgument(0);
            sendJob.setId(90L);
            return null;
        }).when(adminMailSendJobMapper).insert(any(AdminMailSendJob.class));

        boolean sent = mailingService.triggerThresholdAutomatically(9L);

        assertThat(sent).isTrue();
        ArgumentCaptor<AdminMailSendJob> sendJobCaptor = ArgumentCaptor.forClass(AdminMailSendJob.class);
        verify(adminMailSendJobMapper).insert(sendJobCaptor.capture());
        assertThat(sendJobCaptor.getValue().getTemplateId()).isNull();
        assertThat(sendJobCaptor.getValue().getMailSubjectSnapshot()).isNotBlank();
        assertThat(sendJobCaptor.getValue().getMailBodySnapshot()).isNotBlank();
        verify(mailDispatchGateway).dispatch(any());
    }

    @Test
    void automaticThresholdCountsOnlyApplicationsAfterLastSuccessfulThresholdSend() throws Exception {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setEncryptionKey("test-encryption-key");
        properties.setCaptchaEnabled(false);
        PublicFormProtectionService protectionService = new PublicFormProtectionService(properties);
        mailingService = new MailingService(
                adminMailTemplateMapper,
                adminMailSendJobMapper,
                adminMailSendTargetMapper,
                adminMailingApplicationMapper,
                adminJobMetaMapper,
                adminExportQueryMapper,
                exportService,
                jobService,
                clientService,
                protectionService,
                mailDispatchGateway,
                adminActionLogService
        );

        LocalDateTime lastThresholdSentAt = LocalDateTime.of(2026, 4, 30, 10, 0);
        AdminJobMeta jobMeta = new AdminJobMeta();
        jobMeta.setDocumentSrl(9L);
        jobMeta.setClientName("Client A");
        jobMeta.setClientEmail("client@example.com");
        jobMeta.setAutoSendEnabled("Y");
        jobMeta.setAutoSendMode("THRESHOLD");
        jobMeta.setAutoSendThreshold(2);
        jobMeta.setAutoSendAttachmentType("XLSX");
        when(jobService.ensureJobMeta(9L)).thenReturn(jobMeta);
        when(adminMailSendJobMapper.findLastSuccessfulThresholdSentAt(9L)).thenReturn(lastThresholdSentAt);

        when(adminExportQueryMapper.findApplicationsByDocumentSrl(9L)).thenReturn(List.of(
                exportApplication(101L, "Y", "N", lastThresholdSentAt.minusMinutes(1)),
                exportApplication(102L, "Y", "N", lastThresholdSentAt.plusMinutes(1)),
                exportApplication(103L, "Y", "N", lastThresholdSentAt.plusMinutes(2))
        ));
        when(adminMailSendJobMapper.findByDuplicatePreventKey(any())).thenReturn(null);
        when(exportService.prepareXlsx(eq(9L), eq(List.of(102L, 103L))))
                .thenReturn(new ExportPayload("job-9.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2}, 2));
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));
        doAnswer(invocation -> {
            AdminMailSendJob sendJob = invocation.getArgument(0);
            sendJob.setId(91L);
            return null;
        }).when(adminMailSendJobMapper).insert(any(AdminMailSendJob.class));

        boolean sent = mailingService.triggerThresholdAutomatically(9L);

        assertThat(sent).isTrue();
        ArgumentCaptor<AdminMailSendJob> sendJobCaptor = ArgumentCaptor.forClass(AdminMailSendJob.class);
        verify(adminMailSendJobMapper).insert(sendJobCaptor.capture());
        assertThat(sendJobCaptor.getValue().getTargetSnapshotCount()).isEqualTo(2);
        verify(exportService).prepareXlsx(9L, List.of(102L, 103L));
        verify(adminMailingApplicationMapper, never()).updateDeliveryStatus(eq(101L), any(), any(), any());
        verify(adminMailingApplicationMapper).updateDeliveryStatus(eq(102L), eq("SENT"), eq(91L), any());
        verify(adminMailingApplicationMapper).updateDeliveryStatus(eq(103L), eq("SENT"), eq(91L), any());
    }

    @Test
    void thresholdTriggerFallsBackToCurrentEligibleCountWhenThresholdIsMissing() throws Exception {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setEncryptionKey("test-encryption-key");
        properties.setCaptchaEnabled(false);
        PublicFormProtectionService protectionService = new PublicFormProtectionService(properties);
        mailingService = new MailingService(
                adminMailTemplateMapper,
                adminMailSendJobMapper,
                adminMailSendTargetMapper,
                adminMailingApplicationMapper,
                adminJobMetaMapper,
                adminExportQueryMapper,
                exportService,
                jobService,
                clientService,
                protectionService,
                mailDispatchGateway,
                adminActionLogService
        );

        AdminMailTemplate template = new AdminMailTemplate();
        template.setId(3L);
        template.setTemplateName("Threshold Send");
        template.setMailSubject("{{jobTitle}}");
        template.setMailBody("Count {{applicationCount}}");
        template.setActiveYn("Y");
        when(adminMailTemplateMapper.findById(3L)).thenReturn(template);

        AdminJobMeta jobMeta = new AdminJobMeta();
        jobMeta.setDocumentSrl(9L);
        jobMeta.setClientName("Client A");
        jobMeta.setClientEmail("client@example.com");
        when(jobService.ensureJobMeta(9L)).thenReturn(jobMeta);

        when(adminExportQueryMapper.findApplicationsByDocumentSrl(9L)).thenReturn(List.of(
                exportApplication(101L, "Y", "N")
        ));
        when(adminMailSendJobMapper.findByDuplicatePreventKey(any())).thenReturn(null);
        when(exportService.prepareXlsx(eq(9L), eq(List.of(101L))))
                .thenReturn(new ExportPayload("job-9.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2}, 1));
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));
        doAnswer(invocation -> {
            AdminMailSendJob sendJob = invocation.getArgument(0);
            sendJob.setId(89L);
            return null;
        }).when(adminMailSendJobMapper).insert(any(AdminMailSendJob.class));
        AdminMailSendJob persistedJob = new AdminMailSendJob();
        persistedJob.setId(89L);
        persistedJob.setSendStatus("SENT");
        when(adminMailSendJobMapper.findById(89L)).thenReturn(persistedJob);

        var form = new com.researchi.admin.mailing.web.MailThresholdTriggerForm();
        form.setDocumentSrl(9L);
        form.setTemplateId(3L);
        form.setAttachmentType("XLSX");

        Long sendJobId = mailingService.triggerThreshold(
                form,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        assertThat(sendJobId).isEqualTo(89L);
        ArgumentCaptor<AdminMailSendJob> sendJobCaptor = ArgumentCaptor.forClass(AdminMailSendJob.class);
        verify(adminMailSendJobMapper).insert(sendJobCaptor.capture());
        assertThat(sendJobCaptor.getValue().getThresholdSnapshot()).isEqualTo(1);
        verify(mailDispatchGateway).dispatch(any());
    }

    @Test
    void executeScheduledSendUsesStoredApplicationsAndUpdatesExistingTargets() throws Exception {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setEncryptionKey("test-encryption-key");
        properties.setCaptchaEnabled(false);
        PublicFormProtectionService protectionService = new PublicFormProtectionService(properties);
        mailingService = new MailingService(
                adminMailTemplateMapper,
                adminMailSendJobMapper,
                adminMailSendTargetMapper,
                adminMailingApplicationMapper,
                adminJobMetaMapper,
                adminExportQueryMapper,
                exportService,
                jobService,
                clientService,
                protectionService,
                mailDispatchGateway,
                adminActionLogService
        );

        AdminMailSendJob sendJob = new AdminMailSendJob();
        sendJob.setId(77L);
        sendJob.setDocumentSrl(9L);
        sendJob.setTemplateId(3L);
        sendJob.setAttachmentType("TXT");
        sendJob.setSendStatus("SCHEDULED");
        when(adminMailSendJobMapper.findById(77L)).thenReturn(sendJob);
        when(adminMailSendJobMapper.updateStatusIfCurrent(77L, "RUNNING", null, "SCHEDULED")).thenReturn(1);

        AdminMailTemplate template = new AdminMailTemplate();
        template.setId(3L);
        template.setTemplateName("Scheduled");
        template.setMailSubject("{{jobTitle}}");
        template.setMailBody("Count {{applicationCount}}");
        template.setActiveYn("Y");
        when(adminMailTemplateMapper.findById(3L)).thenReturn(template);

        AdminJobMeta jobMeta = new AdminJobMeta();
        jobMeta.setDocumentSrl(9L);
        jobMeta.setClientName("Client A");
        jobMeta.setClientEmail("client@example.com");
        when(jobService.ensureJobMeta(9L)).thenReturn(jobMeta);

        AdminMailSendTarget first = new AdminMailSendTarget();
        first.setSendJobId(77L);
        first.setApplicationId(101L);
        AdminMailSendTarget duplicate = new AdminMailSendTarget();
        duplicate.setSendJobId(77L);
        duplicate.setApplicationId(101L);
        AdminMailSendTarget second = new AdminMailSendTarget();
        second.setSendJobId(77L);
        second.setApplicationId(102L);
        when(adminMailSendTargetMapper.findBySendJobId(77L)).thenReturn(List.of(first, duplicate, second));

        when(exportService.prepareTxt(eq(9L), eq(List.of(101L, 102L))))
                .thenReturn(new ExportPayload("job-9.txt", "text/plain", new byte[]{1, 2}, 2));
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));

        boolean executed = mailingService.executeScheduledSend(77L);

        assertThat(executed).isTrue();
        verify(mailDispatchGateway).dispatch(any());
        verify(adminMailSendTargetMapper).updateResultBySendJobIdAndApplicationIds(eq(77L), eq(List.of(101L, 102L)), eq("SENT"), eq(null), any());
        verify(adminMailingApplicationMapper).updateDeliveryStatus(eq(101L), eq("SENT"), eq(77L), any());
        verify(adminMailingApplicationMapper).updateDeliveryStatus(eq(102L), eq("SENT"), eq(77L), any());
    }

    @Test
    void executeScheduledSendStillCompletesWhenActionLogWriteFails() throws Exception {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setEncryptionKey("test-encryption-key");
        properties.setCaptchaEnabled(false);
        PublicFormProtectionService protectionService = new PublicFormProtectionService(properties);
        mailingService = new MailingService(
                adminMailTemplateMapper,
                adminMailSendJobMapper,
                adminMailSendTargetMapper,
                adminMailingApplicationMapper,
                adminJobMetaMapper,
                adminExportQueryMapper,
                exportService,
                jobService,
                clientService,
                protectionService,
                mailDispatchGateway,
                adminActionLogService
        );

        AdminMailSendJob sendJob = new AdminMailSendJob();
        sendJob.setId(78L);
        sendJob.setDocumentSrl(9L);
        sendJob.setTemplateId(3L);
        sendJob.setAttachmentType("TXT");
        sendJob.setSendStatus("SCHEDULED");
        when(adminMailSendJobMapper.findById(78L)).thenReturn(sendJob);
        when(adminMailSendJobMapper.updateStatusIfCurrent(78L, "RUNNING", null, "SCHEDULED")).thenReturn(1);

        AdminMailTemplate template = new AdminMailTemplate();
        template.setId(3L);
        template.setTemplateName("Scheduled");
        template.setMailSubject("{{jobTitle}}");
        template.setMailBody("Count {{applicationCount}}");
        template.setActiveYn("Y");
        when(adminMailTemplateMapper.findById(3L)).thenReturn(template);

        AdminJobMeta jobMeta = new AdminJobMeta();
        jobMeta.setDocumentSrl(9L);
        jobMeta.setClientName("Client A");
        jobMeta.setClientEmail("client@example.com");
        when(jobService.ensureJobMeta(9L)).thenReturn(jobMeta);

        AdminMailSendTarget target = new AdminMailSendTarget();
        target.setSendJobId(78L);
        target.setApplicationId(101L);
        when(adminMailSendTargetMapper.findBySendJobId(78L)).thenReturn(List.of(target));

        when(exportService.prepareTxt(eq(9L), eq(List.of(101L))))
                .thenReturn(new ExportPayload("job-9.txt", "text/plain", new byte[]{1, 2}, 1));
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));
        doThrow(new RuntimeException("action log insert failed"))
                .when(adminActionLogService)
                .log(eq(null), eq("MAIL_SEND_SCHEDULED_EXECUTE"), eq("MAIL_SEND_JOB"), eq("78"), any(), eq(null));

        boolean executed = mailingService.executeScheduledSend(78L);

        assertThat(executed).isTrue();
        verify(mailDispatchGateway).dispatch(any());
        ArgumentCaptor<AdminMailSendJob> updateCaptor = ArgumentCaptor.forClass(AdminMailSendJob.class);
        verify(adminMailSendJobMapper).updateStatus(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getSendStatus()).isEqualTo("SENT");
        assertThat(updateCaptor.getValue().getSentAt()).isNotNull();
        verify(adminMailSendTargetMapper).updateResultBySendJobIdAndApplicationIds(eq(78L), eq(List.of(101L)), eq("SENT"), eq(null), any());
    }

    @Test
    void executeScheduledSendMarksJobFailedWhenPreparationThrows() throws Exception {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setEncryptionKey("test-encryption-key");
        properties.setCaptchaEnabled(false);
        PublicFormProtectionService protectionService = new PublicFormProtectionService(properties);
        mailingService = new MailingService(
                adminMailTemplateMapper,
                adminMailSendJobMapper,
                adminMailSendTargetMapper,
                adminMailingApplicationMapper,
                adminJobMetaMapper,
                adminExportQueryMapper,
                exportService,
                jobService,
                clientService,
                protectionService,
                mailDispatchGateway,
                adminActionLogService
        );

        AdminMailSendJob sendJob = new AdminMailSendJob();
        sendJob.setId(79L);
        sendJob.setDocumentSrl(9L);
        sendJob.setTemplateId(3L);
        sendJob.setAttachmentType("TXT");
        sendJob.setSendStatus("SCHEDULED");
        when(adminMailSendJobMapper.findById(79L)).thenReturn(sendJob);
        when(adminMailSendJobMapper.updateStatusIfCurrent(79L, "RUNNING", null, "SCHEDULED")).thenReturn(1);

        AdminMailTemplate template = new AdminMailTemplate();
        template.setId(3L);
        template.setTemplateName("Scheduled");
        template.setMailSubject("{{jobTitle}}");
        template.setMailBody("Count {{applicationCount}}");
        template.setActiveYn("Y");
        when(adminMailTemplateMapper.findById(3L)).thenReturn(template);

        AdminJobMeta jobMeta = new AdminJobMeta();
        jobMeta.setDocumentSrl(9L);
        jobMeta.setClientName("Client A");
        jobMeta.setClientEmail("client@example.com");
        when(jobService.ensureJobMeta(9L)).thenReturn(jobMeta);

        AdminMailSendTarget target = new AdminMailSendTarget();
        target.setSendJobId(79L);
        target.setApplicationId(101L);
        when(adminMailSendTargetMapper.findBySendJobId(79L)).thenReturn(List.of(target));
        when(exportService.prepareTxt(eq(9L), eq(List.of(101L))))
                .thenThrow(new IllegalStateException("template attachment failed"));

        boolean executed = mailingService.executeScheduledSend(79L);

        assertThat(executed).isFalse();
        verify(adminMailSendJobMapper).updateStatus(any(AdminMailSendJob.class));
        verify(adminMailSendTargetMapper).updateResultBySendJobIdAndApplicationIds(eq(79L), eq(List.of(101L)), eq("FAILED"), eq("template attachment failed"), eq(null));
        verify(adminMailingApplicationMapper).updateDeliveryStatus(eq(101L), eq("FAILED"), eq(79L), eq(null));
        verify(mailDispatchGateway, never()).dispatch(any());
    }

    @Test
    void executeScheduledSendSkipsWhenAnotherWorkerAlreadyClaimedJob() throws Exception {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setEncryptionKey("test-encryption-key");
        properties.setCaptchaEnabled(false);
        PublicFormProtectionService protectionService = new PublicFormProtectionService(properties);
        mailingService = new MailingService(
                adminMailTemplateMapper,
                adminMailSendJobMapper,
                adminMailSendTargetMapper,
                adminMailingApplicationMapper,
                adminJobMetaMapper,
                adminExportQueryMapper,
                exportService,
                jobService,
                clientService,
                protectionService,
                mailDispatchGateway,
                adminActionLogService
        );

        AdminMailSendJob sendJob = new AdminMailSendJob();
        sendJob.setId(92L);
        sendJob.setDocumentSrl(9L);
        sendJob.setTemplateId(3L);
        sendJob.setAttachmentType("TXT");
        sendJob.setSendStatus("SCHEDULED");
        when(adminMailSendJobMapper.findById(92L)).thenReturn(sendJob);
        when(adminMailSendJobMapper.updateStatusIfCurrent(92L, "RUNNING", null, "SCHEDULED")).thenReturn(0);
        AdminMailSendTarget target = new AdminMailSendTarget();
        target.setSendJobId(92L);
        target.setApplicationId(101L);
        when(adminMailSendTargetMapper.findBySendJobId(92L)).thenReturn(List.of(target));

        boolean executed = mailingService.executeScheduledSend(92L);

        assertThat(executed).isFalse();
        verify(mailDispatchGateway, never()).dispatch(any());
        verify(adminMailSendJobMapper, never()).updateStatus(any(AdminMailSendJob.class));
        verify(adminMailSendTargetMapper, never()).updateResultBySendJobId(any(), any(), any(), any());
    }

    @Test
    void executeDailyScheduledSendUsesApplicationsAfterLastSuccessfulDailySendAndQueuesNextRun() throws Exception {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setEncryptionKey("test-encryption-key");
        properties.setCaptchaEnabled(false);
        PublicFormProtectionService protectionService = new PublicFormProtectionService(properties);
        mailingService = new MailingService(
                adminMailTemplateMapper,
                adminMailSendJobMapper,
                adminMailSendTargetMapper,
                adminMailingApplicationMapper,
                adminJobMetaMapper,
                adminExportQueryMapper,
                exportService,
                jobService,
                clientService,
                protectionService,
                mailDispatchGateway,
                adminActionLogService
        );

        LocalDateTime lastDailySentAt = LocalDateTime.of(2026, 4, 29, 18, 0);
        AdminMailSendJob sendJob = new AdminMailSendJob();
        sendJob.setId(94L);
        sendJob.setDocumentSrl(9L);
        sendJob.setAttachmentType("XLSX");
        sendJob.setMailSubjectSnapshot("Daily subject");
        sendJob.setMailBodySnapshot("Daily body");
        sendJob.setSendStatus("SCHEDULED");
        sendJob.setScheduledAt(LocalDateTime.of(2026, 4, 30, 18, 0));
        sendJob.setTriggerType("SCHEDULED_DAILY");
        sendJob.setRepeatYn("Y");
        sendJob.setRepeatUnit("DAILY");
        sendJob.setCreatedBy(1L);
        when(adminMailSendJobMapper.findById(94L)).thenReturn(sendJob);
        when(adminMailSendJobMapper.updateStatusIfCurrent(94L, "RUNNING", null, "SCHEDULED")).thenReturn(1);
        when(adminMailSendJobMapper.findLastSuccessfulDailyScheduledSentAt(9L)).thenReturn(lastDailySentAt);
        when(adminMailSendJobMapper.findByDuplicatePreventKey(any())).thenReturn(null);

        AdminJobMeta jobMeta = new AdminJobMeta();
        jobMeta.setDocumentSrl(9L);
        jobMeta.setClientName("Client A");
        jobMeta.setClientEmail("client@example.com");
        when(jobService.ensureJobMeta(9L)).thenReturn(jobMeta);
        when(adminExportQueryMapper.findApplicationsByDocumentSrl(9L)).thenReturn(List.of(
                exportApplication(101L, "Y", "N", lastDailySentAt.minusMinutes(1)),
                exportApplication(102L, "Y", "N", lastDailySentAt.plusMinutes(1)),
                exportApplication(103L, "Y", "Y", lastDailySentAt.plusMinutes(2))
        ));
        when(exportService.prepareXlsx(eq(9L), eq(List.of(102L))))
                .thenReturn(new ExportPayload("job-9.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2}, 1));
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));

        boolean executed = mailingService.executeScheduledSend(94L);

        assertThat(executed).isTrue();
        verify(mailDispatchGateway).dispatch(any());
        verify(exportService).prepareXlsx(9L, List.of(102L));
        verify(adminMailSendTargetMapper).insert(argThat(target ->
                target.getSendJobId().equals(94L)
                        && target.getApplicationId().equals(102L)
                        && "SENT".equals(target.getSendResult())));
        verify(adminMailingApplicationMapper, never()).updateDeliveryStatus(eq(101L), any(), any(), any());
        verify(adminMailingApplicationMapper).updateDeliveryStatus(eq(102L), eq("SENT"), eq(94L), any());

        ArgumentCaptor<AdminMailSendJob> insertCaptor = ArgumentCaptor.forClass(AdminMailSendJob.class);
        verify(adminMailSendJobMapper, times(1)).insert(insertCaptor.capture());
        AdminMailSendJob nextJob = insertCaptor.getValue();
        assertThat(nextJob.getTriggerType()).isEqualTo("SCHEDULED_DAILY");
        assertThat(nextJob.getSendStatus()).isEqualTo("SCHEDULED");
        assertThat(nextJob.getScheduledAt()).isEqualTo(LocalDateTime.of(2026, 5, 1, 18, 0));
        assertThat(nextJob.getRepeatYn()).isEqualTo("Y");
        assertThat(nextJob.getRepeatUnit()).isEqualTo("DAILY");
    }

    @Test
    void cancelSendJobCancelsScheduledJobAndResetsApplications() {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setEncryptionKey("test-encryption-key");
        properties.setCaptchaEnabled(false);
        PublicFormProtectionService protectionService = new PublicFormProtectionService(properties);
        mailingService = new MailingService(
                adminMailTemplateMapper,
                adminMailSendJobMapper,
                adminMailSendTargetMapper,
                adminMailingApplicationMapper,
                adminJobMetaMapper,
                adminExportQueryMapper,
                exportService,
                jobService,
                clientService,
                protectionService,
                mailDispatchGateway,
                adminActionLogService
        );

        AdminMailSendJob sendJob = new AdminMailSendJob();
        sendJob.setId(90L);
        sendJob.setDocumentSrl(9L);
        sendJob.setSendStatus("SCHEDULED");
        when(adminMailSendJobMapper.findById(90L)).thenReturn(sendJob);
        when(adminMailSendJobMapper.updateStatusIfCurrent(90L, "CANCELLED", null, "SCHEDULED")).thenReturn(1);

        AdminMailSendTarget first = new AdminMailSendTarget();
        first.setApplicationId(101L);
        AdminMailSendTarget second = new AdminMailSendTarget();
        second.setApplicationId(102L);
        when(adminMailSendTargetMapper.findBySendJobId(90L)).thenReturn(List.of(first, second));

        mailingService.cancelSendJob(
                90L,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        verify(adminMailSendJobMapper).updateStatusIfCurrent(90L, "CANCELLED", null, "SCHEDULED");
        verify(adminMailSendTargetMapper).updateResultBySendJobId(eq(90L), eq("CANCELLED"), any(), eq(null));
        verify(adminMailingApplicationMapper).updateDeliveryStatus(101L, "READY", null, null);
        verify(adminMailingApplicationMapper).updateDeliveryStatus(102L, "READY", null, null);
    }

    @Test
    void cancelSendJobRejectsNonScheduledJob() {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setEncryptionKey("test-encryption-key");
        properties.setCaptchaEnabled(false);
        PublicFormProtectionService protectionService = new PublicFormProtectionService(properties);
        mailingService = new MailingService(
                adminMailTemplateMapper,
                adminMailSendJobMapper,
                adminMailSendTargetMapper,
                adminMailingApplicationMapper,
                adminJobMetaMapper,
                adminExportQueryMapper,
                exportService,
                jobService,
                clientService,
                protectionService,
                mailDispatchGateway,
                adminActionLogService
        );

        AdminMailSendJob sendJob = new AdminMailSendJob();
        sendJob.setId(91L);
        sendJob.setDocumentSrl(9L);
        sendJob.setSendStatus("SENT");
        when(adminMailSendJobMapper.findById(91L)).thenReturn(sendJob);

        assertThatThrownBy(() -> mailingService.cancelSendJob(
                91L,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        )).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void previewSummarizesRecipientsEligibleApplicationsAndBlacklistExclusions() {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setEncryptionKey("test-encryption-key");
        properties.setCaptchaEnabled(false);
        PublicFormProtectionService protectionService = new PublicFormProtectionService(properties);
        mailingService = new MailingService(
                adminMailTemplateMapper,
                adminMailSendJobMapper,
                adminMailSendTargetMapper,
                adminMailingApplicationMapper,
                adminJobMetaMapper,
                adminExportQueryMapper,
                exportService,
                jobService,
                clientService,
                protectionService,
                mailDispatchGateway,
                adminActionLogService
        );

        AdminJobMeta jobMeta = new AdminJobMeta();
        jobMeta.setDocumentSrl(9L);
        jobMeta.setClientName("Client A");
        jobMeta.setClientEmail("client@example.com");
        jobMeta.setClientEmails("client@example.com invalid-email second@example.com");
        when(jobService.ensureJobMeta(9L)).thenReturn(jobMeta);
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));
        when(adminExportQueryMapper.findApplicationsByDocumentSrl(9L)).thenReturn(List.of(
                exportApplication(101L, "Y", "N"),
                exportApplication(102L, "Y", "Y"),
                exportApplication(103L, "N", "N")
        ));

        MailingPreview preview = mailingService.getPreview(9L);

        assertThat(preview.recipientCount()).isEqualTo(2);
        assertThat(preview.excludedRecipientCount()).isEqualTo(2);
        assertThat(preview.eligibleApplicationCount()).isEqualTo(1);
        assertThat(preview.blacklistExcludedCount()).isEqualTo(1);
        assertThat(preview.recipients()).containsExactly("client@example.com", "second@example.com");
    }

    @Test
    void previewPrefersClientEntityRecipientsWhenClientIsLinked() {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setEncryptionKey("test-encryption-key");
        properties.setCaptchaEnabled(false);
        PublicFormProtectionService protectionService = new PublicFormProtectionService(properties);
        mailingService = new MailingService(
                adminMailTemplateMapper,
                adminMailSendJobMapper,
                adminMailSendTargetMapper,
                adminMailingApplicationMapper,
                adminJobMetaMapper,
                adminExportQueryMapper,
                exportService,
                jobService,
                clientService,
                protectionService,
                mailDispatchGateway,
                adminActionLogService
        );

        AdminJobMeta jobMeta = new AdminJobMeta();
        jobMeta.setDocumentSrl(9L);
        jobMeta.setClientId(5L);
        jobMeta.setClientName("Legacy Name");
        jobMeta.setClientEmail("legacy@example.com");
        when(jobService.ensureJobMeta(9L)).thenReturn(jobMeta);
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));
        when(clientService.getActiveRecipientEmails(5L)).thenReturn(List.of("owner@example.com", "team@example.com"));
        when(clientService.getClientSummary(5L)).thenReturn(new ClientSummary(5L, "Real Client", "Ops", "Owner", null, "owner@example.com", "reply@example.com", List.of("owner@example.com", "team@example.com"), true));
        when(adminExportQueryMapper.findApplicationsByDocumentSrl(9L)).thenReturn(List.of(
                exportApplication(101L, "Y", "N")
        ));

        MailingPreview preview = mailingService.getPreview(9L);

        assertThat(preview.recipients()).containsExactly("owner@example.com", "team@example.com");
        assertThat(preview.jobTitle()).isEqualTo("Survey Job");
    }

    @Test
    void manualSendUsesEnsuredJobMetaWhenLegacyJobHasNoStoredMetadata() throws Exception {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setEncryptionKey("test-encryption-key");
        properties.setCaptchaEnabled(false);
        PublicFormProtectionService protectionService = new PublicFormProtectionService(properties);
        mailingService = new MailingService(
                adminMailTemplateMapper,
                adminMailSendJobMapper,
                adminMailSendTargetMapper,
                adminMailingApplicationMapper,
                adminJobMetaMapper,
                adminExportQueryMapper,
                exportService,
                jobService,
                clientService,
                protectionService,
                mailDispatchGateway,
                adminActionLogService
        );

        AdminMailTemplate template = new AdminMailTemplate();
        template.setId(3L);
        template.setTemplateName("Client Send");
        template.setMailSubject("{{jobTitle}}");
        template.setMailBody("Count {{applicationCount}}");
        template.setActiveYn("Y");
        when(adminMailTemplateMapper.findById(3L)).thenReturn(template);

        AdminJobMeta ensuredMeta = new AdminJobMeta();
        ensuredMeta.setDocumentSrl(9L);
        ensuredMeta.setClientName("Client A");
        ensuredMeta.setClientEmail("client@example.com");
        when(jobService.ensureJobMeta(9L)).thenReturn(ensuredMeta);

        when(adminExportQueryMapper.findApplicationsByDocumentSrl(9L)).thenReturn(List.of(
                exportApplication(101L, "Y", "N")
        ));
        when(exportService.prepareXlsx(eq(9L), eq(List.of(101L))))
                .thenReturn(new ExportPayload("job-9.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2}, 1));
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));
        doAnswer(invocation -> {
            AdminMailSendJob sendJob = invocation.getArgument(0);
            sendJob.setId(66L);
            return null;
        }).when(adminMailSendJobMapper).insert(any(AdminMailSendJob.class));
        AdminMailSendJob persistedJob = new AdminMailSendJob();
        persistedJob.setId(66L);
        persistedJob.setSendStatus("SENT");
        when(adminMailSendJobMapper.findById(66L)).thenReturn(persistedJob);

        MailSendManualForm form = new MailSendManualForm();
        form.setDocumentSrl(9L);
        form.setTemplateId(3L);
        form.setAttachmentType("XLSX");

        Long sendJobId = mailingService.sendManual(
                form,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        assertThat(sendJobId).isEqualTo(66L);
        verify(jobService, org.mockito.Mockito.atLeastOnce()).ensureJobMeta(9L);
        verify(mailDispatchGateway).dispatch(any());
    }

    private AdminMailSendJob historyJob(Long id, Long documentSrl, String sendStatus, Integer targetSnapshotCount) {
        AdminMailSendJob sendJob = new AdminMailSendJob();
        sendJob.setId(id);
        sendJob.setDocumentSrl(documentSrl);
        sendJob.setSendStatus(sendStatus);
        sendJob.setTargetSnapshotCount(targetSnapshotCount);
        return sendJob;
    }

    private com.researchi.admin.export.domain.ExportApplicationSource exportApplication(Long id, String provideYn, String blacklisted) {
        com.researchi.admin.export.domain.ExportApplicationSource source = new com.researchi.admin.export.domain.ExportApplicationSource();
        source.setId(id);
        source.setProvideYn(provideYn);
        source.setIsBlacklisted(blacklisted);
        return source;
    }

    private com.researchi.admin.export.domain.ExportApplicationSource exportApplication(Long id, String provideYn, String blacklisted, LocalDateTime appliedAt) {
        com.researchi.admin.export.domain.ExportApplicationSource source = exportApplication(id, provideYn, blacklisted);
        source.setAppliedAt(appliedAt);
        return source;
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
