package com.researchi.admin.legacy.research.service;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.export.domain.ExportPayload;
import com.researchi.admin.legacy.blacklist.mapper.LegacyBlacklistMapper;
import com.researchi.admin.legacy.mail.domain.LegacyMailRule;
import com.researchi.admin.legacy.mail.mapper.LegacyMailRuleMapper;
import com.researchi.admin.legacy.research.domain.ResearchApplication;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.legacy.research.mapper.ResearchApplicationMapper;
import com.researchi.admin.legacy.research.service.history.LegacyResearchMailHistoryService;
import com.researchi.admin.legacy.research.service.mail.LegacyResearchMailSupportService;
import com.researchi.admin.legacy.research.service.mail.LegacyResearchManualMailService;
import com.researchi.admin.legacy.research.service.recipient.LegacyResearchRecipientService;
import com.researchi.admin.legacy.research.service.rule.LegacyResearchMailRuleService;
import com.researchi.admin.legacy.research.service.schedule.LegacyResearchScheduledMailService;
import com.researchi.admin.legacy.research.service.threshold.LegacyResearchThresholdMailService;
import com.researchi.admin.mailing.domain.AdminMailSendJob;
import com.researchi.admin.mailing.domain.AdminMailSendTarget;
import com.researchi.admin.mailing.domain.MailAttachmentType;
import com.researchi.admin.mailing.domain.MailingPreview;
import com.researchi.admin.mailing.mapper.AdminMailApplicationClaimMapper;
import com.researchi.admin.mailing.mapper.AdminMailSendJobMapper;
import com.researchi.admin.mailing.mapper.AdminMailSendTargetMapper;
import com.researchi.admin.mailing.mapper.AdminMailTemplateMapper;
import com.researchi.admin.mailing.service.MailDispatchGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyResearchMailServiceTest {

    @Mock
    private ResearchMasterService researchMasterService;
    @Mock
    private ResearchApplicationService researchApplicationService;
    @Mock
    private LegacyResearchExportService legacyResearchExportService;
    @Mock
    private ResearchApplicationMapper researchApplicationMapper;
    @Mock
    private LegacyBlacklistMapper legacyBlacklistMapper;
    @Mock
    private LegacyMailRuleMapper legacyMailRuleMapper;
    @Mock
    private AdminMailTemplateMapper adminMailTemplateMapper;
    @Mock
    private AdminMailSendJobMapper adminMailSendJobMapper;
    @Mock
    private AdminMailSendTargetMapper adminMailSendTargetMapper;
    @Mock
    private AdminMailApplicationClaimMapper adminMailApplicationClaimMapper;
    @Mock
    private MailDispatchGateway mailDispatchGateway;
    @Mock
    private AdminActionLogService adminActionLogService;

    @Test
    void previewCountsOnlyNonBlacklistedLegacyApplicants() {
        LegacyResearchMailService service = service();
        ResearchMaster master = researchMaster();
        when(researchMasterService.getResearchMaster(46408L)).thenReturn(master);

        ResearchApplication included = application(101L, "Kim", "900101", "01012345678", "N");
        ResearchApplication blacklisted = application(102L, "Park", "910101", "01099998888", "N");
        ResearchApplication alreadyProvided = application(103L, "Lee", "920101", "01011112222", "Y");
        when(researchApplicationMapper.findAllByResearchNo(46408L)).thenReturn(List.of(included, blacklisted, alreadyProvided));
        when(legacyBlacklistMapper.countActiveMatch(anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> "Park".equals(invocation.getArgument(0)) ? 1 : 0);

        MailingPreview preview = service.getPreview(46408L);

        assertThat(preview.eligibleApplicationCount()).isEqualTo(1);
        assertThat(preview.blacklistExcludedCount()).isEqualTo(1);
        assertThat(preview.recipients()).containsExactly("client@example.com");
    }

    @Test
    void manualSendMarksProvidedForSentLegacyApplicants() throws Exception {
        LegacyResearchMailService service = service();
        ResearchMaster master = researchMaster();
        ResearchApplication applicant = application(101L, "Kim", "900101", "01012345678", "N");
        AtomicReference<AdminMailSendJob> insertedJob = new AtomicReference<>();

        when(researchMasterService.getResearchMaster(46408L)).thenReturn(master);
        when(researchApplicationMapper.findAllByResearchNo(46408L)).thenReturn(List.of(applicant));
        when(researchApplicationMapper.findUnprovidedByResearchNoAndSeqs(46408L, List.of(101L))).thenReturn(List.of(applicant));
        when(legacyResearchExportService.prepareXlsx(46408L, List.of(101L)))
                .thenReturn(new ExportPayload("research.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1}, 1));
        doAnswer(invocation -> {
            AdminMailSendJob job = invocation.getArgument(0);
            job.setId(77L);
            insertedJob.set(job);
            return null;
        }).when(adminMailSendJobMapper).insert(any(AdminMailSendJob.class));
        when(adminMailSendJobMapper.findById(77L)).thenAnswer(invocation -> insertedJob.get());

        Long sendJobId = service.sendManual(
                46408L,
                null,
                "Subject",
                "Body",
                MailAttachmentType.XLSX,
                null,
                null
        );

        assertThat(sendJobId).isEqualTo(77L);
        assertThat(insertedJob.get().getSendStatus()).isEqualTo("SENT");
        verify(mailDispatchGateway).dispatch(any());
        verify(researchApplicationService).updateProvideYn(46408L, 101L, "Y", null);

        ArgumentCaptor<AdminMailSendJob> statusCaptor = ArgumentCaptor.forClass(AdminMailSendJob.class);
        verify(adminMailSendJobMapper).updateStatus(statusCaptor.capture());
        assertThat(statusCaptor.getValue().getSendStatus()).isEqualTo("SENT");
    }

    @Test
    void manualSendFailsWhenNoUnprovidedApplicantsRemain() {
        LegacyResearchMailService service = service();
        ResearchMaster master = researchMaster();
        ResearchApplication provided = application(101L, "Kim", "900101", "01012345678", "Y");
        AtomicReference<AdminMailSendJob> insertedJob = new AtomicReference<>();

        when(researchMasterService.getResearchMaster(46408L)).thenReturn(master);
        when(researchApplicationMapper.findAllByResearchNo(46408L)).thenReturn(List.of(provided));
        doAnswer(invocation -> {
            AdminMailSendJob job = invocation.getArgument(0);
            job.setId(78L);
            insertedJob.set(job);
            return null;
        }).when(adminMailSendJobMapper).insert(any(AdminMailSendJob.class));
        when(adminMailSendJobMapper.findById(78L)).thenAnswer(invocation -> insertedJob.get());
        when(adminMailSendTargetMapper.findBySendJobId(78L)).thenReturn(List.of());

        assertThatThrownBy(() -> service.sendManual(
                46408L,
                null,
                "Subject",
                "Body",
                MailAttachmentType.XLSX,
                null,
                null
        )).hasMessageContaining("PROVIDE_YN=N");

        assertThat(insertedJob.get().getSendStatus()).isEqualTo("FAILED");
    }

    @Test
    void thresholdSendKeepsThresholdRuleAfterSuccessfulSend() throws Exception {
        LegacyResearchMailService service = service();
        LegacyMailRule rule = new LegacyMailRule();
        rule.setResearchNo(46408L);
        rule.setThresholdCount(1);
        rule.setDirectMailSubject("Subject");
        rule.setDirectMailBody("Body");
        rule.setAttachmentType("XLSX");
        rule.setEnabledYn("N");
        ResearchApplication applicant = application(101L, "Kim", "900101", "01012345678", "N");
        AtomicReference<AdminMailSendJob> insertedJob = new AtomicReference<>();

        when(legacyMailRuleMapper.findByResearchNo(46408L)).thenReturn(rule);
        when(researchMasterService.getResearchMaster(46408L)).thenReturn(researchMaster());
        when(researchApplicationMapper.findAllByResearchNo(46408L)).thenReturn(List.of(applicant));
        when(researchApplicationMapper.findUnprovidedByResearchNoAndSeqs(46408L, List.of(101L))).thenReturn(List.of(applicant));
        when(legacyResearchExportService.prepareXlsx(46408L, List.of(101L)))
                .thenReturn(new ExportPayload("research.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1}, 1));
        doAnswer(invocation -> {
            AdminMailSendJob job = invocation.getArgument(0);
            job.setId(82L);
            insertedJob.set(job);
            return null;
        }).when(adminMailSendJobMapper).insert(any(AdminMailSendJob.class));
        when(adminMailSendJobMapper.findById(82L)).thenAnswer(invocation -> insertedJob.get());

        Long sendJobId = service.triggerThreshold(46408L, null, null);

        assertThat(sendJobId).isEqualTo(82L);
        assertThat(insertedJob.get().getTriggerType()).isEqualTo("LEGACY_THRESHOLD");
        verify(legacyMailRuleMapper).touchLastTriggeredAt(eq(46408L), any(LocalDateTime.class));
        verify(legacyMailRuleMapper, never()).completeByResearchNo(eq(46408L), any(LocalDateTime.class));
        verify(researchApplicationService).updateProvideYn(46408L, 101L, "Y", null);
    }

    @Test
    void thresholdSendKeepsEnabledRuleAfterSuccessfulSend() throws Exception {
        LegacyResearchMailService service = service();
        LegacyMailRule rule = new LegacyMailRule();
        rule.setResearchNo(46408L);
        rule.setThresholdCount(1);
        rule.setDirectMailSubject("Subject");
        rule.setDirectMailBody("Body");
        rule.setAttachmentType("XLSX");
        rule.setEnabledYn("Y");
        ResearchApplication applicant = application(101L, "Kim", "900101", "01012345678", "N");
        AtomicReference<AdminMailSendJob> insertedJob = new AtomicReference<>();

        when(legacyMailRuleMapper.findByResearchNo(46408L)).thenReturn(rule);
        when(researchMasterService.getResearchMaster(46408L)).thenReturn(researchMaster());
        when(researchApplicationMapper.findAllByResearchNo(46408L)).thenReturn(List.of(applicant));
        when(researchApplicationMapper.findUnprovidedByResearchNoAndSeqs(46408L, List.of(101L))).thenReturn(List.of(applicant));
        when(legacyResearchExportService.prepareXlsx(46408L, List.of(101L)))
                .thenReturn(new ExportPayload("research.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1}, 1));
        doAnswer(invocation -> {
            AdminMailSendJob job = invocation.getArgument(0);
            job.setId(83L);
            insertedJob.set(job);
            return null;
        }).when(adminMailSendJobMapper).insert(any(AdminMailSendJob.class));
        when(adminMailSendJobMapper.findById(83L)).thenAnswer(invocation -> insertedJob.get());

        Long sendJobId = service.triggerThreshold(46408L, null, null);

        assertThat(sendJobId).isEqualTo(83L);
        verify(legacyMailRuleMapper).touchLastTriggeredAt(eq(46408L), any(LocalDateTime.class));
        verify(researchApplicationService).updateProvideYn(46408L, 101L, "Y", null);
    }

    @Test
    void scheduledSendMarksJobFailedWhenStoredTargetsAreAlreadyProvided() {
        LegacyResearchMailService service = service();
        AdminMailSendJob sendJob = new AdminMailSendJob();
        sendJob.setId(79L);
        sendJob.setResearchNo(46408L);
        sendJob.setTriggerType("LEGACY_SCHEDULED");
        sendJob.setSendStatus("SCHEDULED");
        sendJob.setAttachmentType("XLSX");
        sendJob.setRepeatYn("N");
        sendJob.setMailSubjectSnapshot("Subject");
        sendJob.setMailBodySnapshot("Body");

        AdminMailSendTarget target = new AdminMailSendTarget();
        target.setSendJobId(79L);
        target.setApplicationId(101L);
        ResearchApplication provided = application(101L, "Kim", "900101", "01012345678", "Y");

        when(adminMailSendJobMapper.findById(79L)).thenReturn(sendJob);
        when(adminMailSendTargetMapper.findBySendJobId(79L)).thenReturn(List.of(target));
        when(researchApplicationMapper.findAllByResearchNo(46408L)).thenReturn(List.of(provided));

        boolean executed = service.executeScheduledSend(79L);

        assertThat(executed).isFalse();
        ArgumentCaptor<AdminMailSendJob> statusCaptor = ArgumentCaptor.forClass(AdminMailSendJob.class);
        verify(adminMailSendJobMapper).updateStatus(statusCaptor.capture());
        assertThat(statusCaptor.getValue().getSendStatus()).isEqualTo("FAILED");
        assertThat(statusCaptor.getValue().getSentAt()).isNotNull();
        verify(adminMailSendTargetMapper).updateResultBySendJobIdAndApplicationIds(
                eq(79L),
                eq(List.of(101L)),
                eq("FAILED"),
                eq(LegacyResearchMailSupportService.NO_UNPROVIDED_DATA_REASON),
                any(LocalDateTime.class)
        );
    }

    @Test
    void scheduledRegistrationFailsWhenNoUnprovidedApplicantsRemain() {
        LegacyResearchMailService service = service();
        ResearchMaster master = researchMaster();
        ResearchApplication provided = application(101L, "Kim", "900101", "01012345678", "Y");
        AtomicReference<AdminMailSendJob> insertedJob = new AtomicReference<>();

        when(researchMasterService.getResearchMaster(46408L)).thenReturn(master);
        when(researchApplicationMapper.findAllByResearchNo(46408L)).thenReturn(List.of(provided));
        doAnswer(invocation -> {
            AdminMailSendJob job = invocation.getArgument(0);
            job.setId(80L);
            insertedJob.set(job);
            return null;
        }).when(adminMailSendJobMapper).insert(any(AdminMailSendJob.class));

        Long sendJobId = service.schedule(
                46408L,
                null,
                "Subject",
                "Body",
                MailAttachmentType.XLSX,
                LocalDateTime.now().plusMinutes(5),
                null,
                false,
                null,
                null
        );

        assertThat(sendJobId).isEqualTo(80L);
        assertThat(insertedJob.get().getSendStatus()).isEqualTo("FAILED");
        assertThat(insertedJob.get().getSentAt()).isNotNull();
    }

    @Test
    void historySortsTerminalJobsByExecutionTimeInsteadOfFutureReservationTime() {
        LegacyResearchMailService service = service();
        AdminMailSendJob failedFutureReservation = historyJob(
                82L,
                "LEGACY_SCHEDULED",
                "FAILED",
                LocalDateTime.of(2026, 5, 1, 10, 0),
                LocalDateTime.of(2026, 5, 29, 5, 33),
                null
        );
        AdminMailSendJob completed = historyJob(
                83L,
                "LEGACY_SCHEDULED",
                "SENT",
                LocalDateTime.of(2026, 5, 1, 10, 0),
                LocalDateTime.of(2026, 5, 16, 16, 3),
                LocalDateTime.of(2026, 5, 16, 16, 5)
        );

        when(adminMailSendJobMapper.findLegacyByResearchNo(46408L)).thenReturn(List.of(failedFutureReservation, completed));
        when(researchMasterService.getResearchMaster(46408L)).thenReturn(researchMaster());
        when(adminMailSendTargetMapper.findBySendJobIds(List.of(83L, 82L))).thenReturn(List.of());

        assertThat(service.getHistory(46408L)).extracting(item -> item.sendJob().getId())
                .containsExactly(83L, 82L);
    }

    @Test
    void scheduledSendKeepsSentStatusWhenPostProcessingFailsAfterDispatch() {
        LegacyResearchMailService service = service();
        AdminMailSendJob sendJob = new AdminMailSendJob();
        sendJob.setId(81L);
        sendJob.setResearchNo(46408L);
        sendJob.setTriggerType("LEGACY_SCHEDULED");
        sendJob.setSendStatus("SCHEDULED");
        sendJob.setAttachmentType("XLSX");
        sendJob.setRepeatYn("N");
        sendJob.setMailSubjectSnapshot("Subject");
        sendJob.setMailBodySnapshot("Body");
        ResearchApplication applicant = application(101L, "Kim", "900101", "01012345678", "N");
        AdminMailSendTarget target = new AdminMailSendTarget();
        target.setSendJobId(81L);
        target.setApplicationId(101L);

        when(adminMailSendJobMapper.findById(81L)).thenReturn(sendJob);
        when(adminMailSendTargetMapper.findBySendJobId(81L)).thenReturn(List.of(target));
        when(researchApplicationMapper.findAllByResearchNo(46408L)).thenReturn(List.of(applicant));
        when(adminMailSendJobMapper.updateStatusIfCurrent(81L, "RUNNING", null, "SCHEDULED")).thenReturn(1);
        when(researchMasterService.getResearchMaster(46408L)).thenReturn(researchMaster());
        when(legacyResearchExportService.prepareXlsx(46408L, List.of(101L)))
                .thenReturn(new ExportPayload("research.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[] {1}, 1));
        doThrow(new IllegalStateException("target update failed"))
                .when(adminMailSendTargetMapper)
                .updateResultBySendJobIdAndApplicationIds(eq(81L), eq(List.of(101L)), eq("SENT"), eq(null), any(LocalDateTime.class));
        when(researchApplicationMapper.findUnprovidedByResearchNoAndSeqs(46408L, List.of(101L))).thenReturn(List.of(applicant));

        boolean executed = service.executeScheduledSend(81L);

        assertThat(executed).isTrue();
        ArgumentCaptor<AdminMailSendJob> statusCaptor = ArgumentCaptor.forClass(AdminMailSendJob.class);
        verify(adminMailSendJobMapper).updateStatus(statusCaptor.capture());
        assertThat(statusCaptor.getValue().getSendStatus()).isEqualTo("SENT");
        verify(researchApplicationService).updateProvideYn(46408L, 101L, "Y", null);
    }

    private LegacyResearchMailService service() {
        LegacyResearchRecipientService recipientService = new LegacyResearchRecipientService(
                researchApplicationMapper,
                legacyBlacklistMapper,
                adminMailSendTargetMapper,
                adminMailApplicationClaimMapper
        );
        lenient().when(adminMailApplicationClaimMapper.insertIgnore(any(), any(), any())).thenReturn(1);
        LegacyResearchMailSupportService mailSupport = new LegacyResearchMailSupportService(
                adminMailTemplateMapper,
                adminMailSendJobMapper,
                adminMailSendTargetMapper,
                adminMailApplicationClaimMapper,
                researchApplicationMapper,
                researchApplicationService,
                adminActionLogService
        );
        LegacyResearchManualMailService manualMailService = new LegacyResearchManualMailService(
                researchMasterService,
                legacyResearchExportService,
                adminMailSendJobMapper,
                mailDispatchGateway,
                recipientService,
                mailSupport
        );
        LegacyResearchScheduledMailService scheduledMailService = new LegacyResearchScheduledMailService(
                researchMasterService,
                legacyResearchExportService,
                adminMailSendJobMapper,
                adminMailSendTargetMapper,
                mailDispatchGateway,
                recipientService,
                mailSupport
        );
        LegacyResearchThresholdMailService thresholdMailService = new LegacyResearchThresholdMailService(
                researchMasterService,
                legacyResearchExportService,
                legacyMailRuleMapper,
                adminMailSendJobMapper,
                mailDispatchGateway,
                recipientService,
                mailSupport
        );
        LegacyResearchMailHistoryService historyService = new LegacyResearchMailHistoryService(
                researchMasterService,
                adminMailSendJobMapper,
                adminMailSendTargetMapper
        );
        LegacyResearchMailRuleService mailRuleService = new LegacyResearchMailRuleService(
                researchMasterService,
                legacyMailRuleMapper,
                mailSupport
        );
        return new LegacyResearchMailService(
                researchMasterService,
                historyService,
                mailRuleService,
                recipientService,
                manualMailService,
                scheduledMailService,
                thresholdMailService
        );
    }

    private ResearchMaster researchMaster() {
        ResearchMaster master = new ResearchMaster();
        master.setResearchNo(46408L);
        master.setResearchTitle("Research title");
        master.setCompanyName("Client");
        master.setRemark("client@example.com");
        return master;
    }

    private ResearchApplication application(Long seq, String name, String birth, String phone, String provideYn) {
        ResearchApplication application = new ResearchApplication();
        application.setResearchNo(46408L);
        application.setResearchAppSeq(seq);
        application.setAppName(name);
        application.setAppBirth(birth);
        application.setAppHphone(phone);
        application.setProvideYn(provideYn);
        return application;
    }

    private AdminMailSendJob historyJob(
            Long id,
            String triggerType,
            String sendStatus,
            LocalDateTime createdAt,
            LocalDateTime scheduledAt,
            LocalDateTime sentAt
    ) {
        AdminMailSendJob job = new AdminMailSendJob();
        job.setId(id);
        job.setResearchNo(46408L);
        job.setTriggerType(triggerType);
        job.setSendStatus(sendStatus);
        job.setCreatedAt(createdAt);
        job.setScheduledAt(scheduledAt);
        job.setSentAt(sentAt);
        job.setTargetSnapshotCount(0);
        return job;
    }
}
