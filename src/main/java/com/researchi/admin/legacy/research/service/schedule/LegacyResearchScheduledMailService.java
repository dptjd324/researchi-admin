package com.researchi.admin.legacy.research.service.schedule;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.export.domain.ExportPayload;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.legacy.research.service.LegacyResearchExportService;
import com.researchi.admin.legacy.research.service.ResearchMasterService;
import com.researchi.admin.legacy.research.service.mail.LegacyResearchMailContent;
import com.researchi.admin.legacy.research.service.mail.LegacyResearchMailSnapshot;
import com.researchi.admin.legacy.research.service.mail.LegacyResearchMailSupportService;
import com.researchi.admin.legacy.research.service.recipient.LegacyResearchRecipientSelection;
import com.researchi.admin.legacy.research.service.recipient.LegacyResearchRecipientService;
import com.researchi.admin.mailing.domain.AdminMailSendJob;
import com.researchi.admin.mailing.domain.MailAttachmentType;
import com.researchi.admin.mailing.domain.MailDispatchResult;
import com.researchi.admin.mailing.mapper.AdminMailSendJobMapper;
import com.researchi.admin.mailing.mapper.AdminMailSendTargetMapper;
import com.researchi.admin.mailing.service.MailDispatchGateway;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class LegacyResearchScheduledMailService {

    private final ResearchMasterService researchMasterService;
    private final LegacyResearchExportService legacyResearchExportService;
    private final AdminMailSendJobMapper adminMailSendJobMapper;
    private final AdminMailSendTargetMapper adminMailSendTargetMapper;
    private final MailDispatchGateway mailDispatchGateway;
    private final LegacyResearchRecipientService recipientService;
    private final LegacyResearchMailSupportService mailSupport;

    public LegacyResearchScheduledMailService(
            ResearchMasterService researchMasterService,
            LegacyResearchExportService legacyResearchExportService,
            AdminMailSendJobMapper adminMailSendJobMapper,
            AdminMailSendTargetMapper adminMailSendTargetMapper,
            MailDispatchGateway mailDispatchGateway,
            LegacyResearchRecipientService recipientService,
            LegacyResearchMailSupportService mailSupport
    ) {
        this.researchMasterService = researchMasterService;
        this.legacyResearchExportService = legacyResearchExportService;
        this.adminMailSendJobMapper = adminMailSendJobMapper;
        this.adminMailSendTargetMapper = adminMailSendTargetMapper;
        this.mailDispatchGateway = mailDispatchGateway;
        this.recipientService = recipientService;
        this.mailSupport = mailSupport;
    }

    @Transactional("adminTransactionManager")
    public void cancelScheduledJob(Long sendJobId, AdminPrincipal principal, HttpServletRequest request) {
        AdminMailSendJob sendJob = adminMailSendJobMapper.findById(sendJobId);
        if (sendJob == null) {
            throw new IllegalArgumentException("메일 발송 작업을 찾을 수 없습니다.");
        }
        if (!isLegacyMailJob(sendJob)) {
            throw new IllegalArgumentException("구 DB 좌담회/설문 메일 작업이 아닙니다.");
        }
        if (!"SCHEDULED".equals(sendJob.getSendStatus())) {
            throw new IllegalStateException("예약 상태인 메일만 취소할 수 있습니다.");
        }

        int updated = adminMailSendJobMapper.updateStatusIfCurrent(sendJobId, "CANCELLED", null, "SCHEDULED");
        if (updated < 1) {
            throw new IllegalStateException("이미 처리 중인 메일 발송 작업입니다.");
        }

        adminMailSendTargetMapper.updateResultBySendJobId(
                sendJobId,
                "CANCELLED",
                "관리자가 예약 발송을 취소했습니다.",
                null
        );
        mailSupport.safeLog(
                principal == null ? null : principal.getId(),
                "MAIL_SEND_LEGACY_CANCEL",
                "MAIL_SEND_JOB",
                String.valueOf(sendJobId),
                "예약 메일 작업 #" + sendJobId + " 취소",
                request
        );
    }

    @Transactional("adminTransactionManager")
    public Long schedule(
            Long researchNo,
            Long templateId,
            String directMailSubject,
            String directMailBody,
            MailAttachmentType attachmentType,
            LocalDateTime scheduledAt,
            LocalTime dailyScheduledTime,
            boolean dailyRepeat,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        researchMasterService.assertNotHidden(researchNo);
        LocalDateTime resolvedScheduledAt = dailyRepeat
                ? mailSupport.resolveDailyScheduledAt(dailyScheduledTime)
                : scheduledAt;
        if (!dailyRepeat) {
            mailSupport.validateScheduledAt(resolvedScheduledAt);
        }
        ResearchMaster researchMaster = researchMasterService.getResearchMaster(researchNo);
        LegacyResearchMailSnapshot snapshot = dailyRepeat
                ? recipientService.loadThresholdSnapshot(researchNo)
                : recipientService.loadSnapshot(researchNo);
        LegacyResearchRecipientSelection recipients = recipientService.parseRecipients(researchMaster);
        LegacyResearchMailContent mailContent = mailSupport.resolveMailContent(templateId, directMailSubject, directMailBody);
        String duplicateKey = (dailyRepeat ? "LEGACY_SCHEDULED_DAILY" : "LEGACY_SCHEDULED")
                + ":" + researchNo + ":" + resolvedScheduledAt.withNano(0);
        mailSupport.assertNoDuplicate(duplicateKey);

        AdminMailSendJob sendJob = mailSupport.baseJob(
                researchNo,
                mailContent.templateId(),
                mailContent.subject(),
                mailContent.body(),
                attachmentType,
                "SCHEDULED",
                dailyRepeat ? "LEGACY_SCHEDULED_DAILY" : "LEGACY_SCHEDULED",
                recipients,
                snapshot,
                null,
                duplicateKey,
                principal == null ? null : principal.getId()
        );
        boolean failedOnRegister = !dailyRepeat && snapshot.applicationIds().isEmpty();
        sendJob.setSendStatus(failedOnRegister ? "FAILED" : "SCHEDULED");
        sendJob.setScheduledAt(resolvedScheduledAt);
        if (failedOnRegister) {
            sendJob.setSentAt(LocalDateTime.now());
        }
        sendJob.setRepeatYn(dailyRepeat ? "Y" : "N");
        sendJob.setRepeatUnit(dailyRepeat ? "DAILY" : null);
        adminMailSendJobMapper.insert(sendJob);
        if (!dailyRepeat) {
            mailSupport.insertTargets(sendJob.getId(), snapshot.applicationIds(), recipients, "PENDING", null, null);
        }
        String logDetail = "FAILED".equals(sendJob.getSendStatus())
                ? "예약 메일 작업 #" + sendJob.getId() + " 실패: " + LegacyResearchMailSupportService.NO_UNPROVIDED_DATA_REASON
                : "예약 메일 작업 #" + sendJob.getId() + " 등록";
        mailSupport.safeLog(principal == null ? null : principal.getId(), "MAIL_SEND_LEGACY_SCHEDULE", "RESEARCH", String.valueOf(researchNo), logDetail, request);
        return sendJob.getId();
    }

    public LocalDateTime minimumScheduledAt() {
        return mailSupport.minimumScheduledAt();
    }

    public boolean executeScheduledSend(Long sendJobId) {
        AdminMailSendJob sendJob = adminMailSendJobMapper.findById(sendJobId);
        if (sendJob == null || !"SCHEDULED".equals(sendJob.getSendStatus()) || !isLegacyScheduled(sendJob)) {
            return false;
        }
        if (researchMasterService.isHidden(sendJob.getResearchNo())) {
            sendJob.setSendStatus("CANCELLED");
            sendJob.setSentAt(LocalDateTime.now());
            adminMailSendJobMapper.updateStatus(sendJob);
            mailSupport.safeLog(null, "MAIL_SEND_LEGACY_SCHEDULED_BLOCKED", "MAIL_SEND_JOB", String.valueOf(sendJob.getId()), "숨김 처리된 공고의 예약 메일 발송을 차단했습니다.", null);
            return false;
        }

        boolean dailyRepeat = isDailyRepeat(sendJob);
        LegacyResearchMailSnapshot dailySnapshot = dailyRepeat
                ? recipientService.loadThresholdSnapshot(sendJob.getResearchNo())
                : null;
        LegacyResearchScheduledTargetSnapshot targetSnapshot = dailyRepeat
                ? new LegacyResearchScheduledTargetSnapshot(dailySnapshot.applicationIds(), List.of(), List.of())
                : recipientService.loadScheduledTargetSnapshot(sendJob);
        List<Long> applicationIds = targetSnapshot.applicationIds();

        if (applicationIds.isEmpty()) {
            LocalDateTime failedAt = LocalDateTime.now();
            mailSupport.markScheduledBlacklistExcludedTargets(sendJob.getId(), targetSnapshot.blacklistExcludedApplicationIds(), failedAt);
            mailSupport.updateScheduledTargets(
                    sendJob.getId(),
                    targetSnapshot.alreadyProvidedApplicationIds(),
                    "FAILED",
                    LegacyResearchMailSupportService.NO_UNPROVIDED_DATA_REASON,
                    failedAt
            );
            sendJob.setSendStatus(dailyRepeat ? "NO_TARGETS" : "FAILED");
            sendJob.setSentAt(failedAt);
            adminMailSendJobMapper.updateStatus(sendJob);
            mailSupport.safeLog(null, "MAIL_SEND_LEGACY_SCHEDULED_EXECUTE", "MAIL_SEND_JOB", String.valueOf(sendJob.getId()), "예약 메일 작업 #" + sendJob.getId() + " 실패: " + LegacyResearchMailSupportService.NO_UNPROVIDED_DATA_REASON, null);
            if (dailyRepeat) {
                scheduleNextDailySend(sendJob);
            }
            return false;
        }

        int claimed = adminMailSendJobMapper.updateStatusIfCurrent(sendJob.getId(), "RUNNING", null, "SCHEDULED");
        if (claimed < 1) {
            return false;
        }
        sendJob.setSendStatus("RUNNING");
        sendJob.setSentAt(null);

        List<Long> claimedApplicationIds = mailSupport.claimApplications(sendJob.getResearchNo(), applicationIds, sendJob.getId());
        java.util.Set<Long> claimedApplicationIdSet = new java.util.LinkedHashSet<>(claimedApplicationIds);
        List<Long> skippedClaimedApplicationIds = applicationIds.stream()
                .filter(applicationId -> !claimedApplicationIdSet.contains(applicationId))
                .toList();
        applicationIds = claimedApplicationIds;
        String sendStatus;
        String targetResult;
        String failReason = null;
        LocalDateTime sentAt = null;
        LegacyResearchRecipientSelection recipients = null;
        MailDispatchResult dispatchResult = null;
        if (claimedApplicationIds.isEmpty()) {
            sendStatus = "FAILED";
            targetResult = "FAILED";
            failReason = LegacyResearchMailSupportService.CLAIMED_BY_OTHER_SEND_REASON;
            sentAt = LocalDateTime.now();
        } else try {
            ResearchMaster researchMaster = researchMasterService.getResearchMaster(sendJob.getResearchNo());
            recipients = recipientService.parseRecipients(researchMaster);
            if (recipients.recipients().isEmpty()) {
                throw new IllegalStateException("수신 이메일을 찾을 수 없습니다. 수신 이메일을 등록해주세요.");
            }
            LegacyResearchMailContent mailContent = mailSupport.resolveStoredMailContent(sendJob);
            MailAttachmentType attachmentType = MailAttachmentType.fromValue(sendJob.getAttachmentType() == null ? mailSupport.defaultAttachmentType() : sendJob.getAttachmentType());
            ExportPayload attachment = attachmentType == MailAttachmentType.XLSX
                    ? legacyResearchExportService.prepareXlsx(sendJob.getResearchNo(), claimedApplicationIds)
                    : legacyResearchExportService.prepareTxt(sendJob.getResearchNo(), claimedApplicationIds);
            dispatchResult = mailDispatchGateway.dispatch(mailSupport.buildDispatchRequest(researchMaster, recipients.recipients(), mailContent, attachment, attachmentType, claimedApplicationIds.size(), claimedApplicationIds));
            sendStatus = "SENT";
            targetResult = "SENT";
            sentAt = LocalDateTime.now();
        } catch (Exception ex) {
            sendStatus = "FAILED";
            targetResult = "FAILED";
            failReason = mailSupport.trimFailureReason(ex.getMessage());
            sentAt = LocalDateTime.now();
        }

        sendJob.setSendStatus(sendStatus);
        sendJob.setSentAt(sentAt);
        adminMailSendJobMapper.updateStatus(sendJob);
        mailSupport.recordProviderResult(sendJob, dispatchResult);
        if (dailyRepeat) {
            try {
                mailSupport.insertTargets(sendJob.getId(), applicationIds, recipients == null ? new LegacyResearchRecipientSelection(List.of(), 0, "거래처") : recipients, targetResult, failReason, sentAt);
                if ("SENT".equals(sendStatus)) {
                    scheduleNextDailySend(sendJob);
                }
            } catch (RuntimeException ex) {
                mailSupport.safeLog(null, "MAIL_SEND_LEGACY_SCHEDULED_POST_PROCESS_FAILED", "MAIL_SEND_JOB", String.valueOf(sendJob.getId()), "예약 메일 상태 변경 후 후처리 실패: " + mailSupport.trimFailureReason(ex.getMessage()), null);
            }
        } else {
            try {
                mailSupport.markScheduledBlacklistExcludedTargets(sendJob.getId(), targetSnapshot.blacklistExcludedApplicationIds(), sentAt);
                mailSupport.updateScheduledTargets(
                        sendJob.getId(),
                        targetSnapshot.alreadyProvidedApplicationIds(),
                        "FAILED",
                        LegacyResearchMailSupportService.NO_UNPROVIDED_DATA_REASON,
                        sentAt
                );
                mailSupport.updateScheduledTargets(
                        sendJob.getId(),
                        skippedClaimedApplicationIds,
                        "FAILED",
                        LegacyResearchMailSupportService.CLAIMED_BY_OTHER_SEND_REASON,
                        sentAt
                );
                mailSupport.updateScheduledTargets(sendJob.getId(), claimedApplicationIds, targetResult, failReason, sentAt);
            } catch (RuntimeException ex) {
                mailSupport.safeLog(null, "MAIL_SEND_LEGACY_SCHEDULED_POST_PROCESS_FAILED", "MAIL_SEND_JOB", String.valueOf(sendJob.getId()), "예약 메일 상태 변경 후 후처리 실패: " + mailSupport.trimFailureReason(ex.getMessage()), null);
            }
        }
        if ("SENT".equals(sendStatus)) {
            mailSupport.markApplicationsProvided(
                    sendJob.getResearchNo(),
                    claimedApplicationIds,
                    sendJob.getCreatedBy(),
                    "예약 메일 작업 #" + sendJob.getId()
            );
        }
        mailSupport.safeLog(null, "MAIL_SEND_LEGACY_SCHEDULED_EXECUTE", "MAIL_SEND_JOB", String.valueOf(sendJob.getId()), "예약 메일 작업 #" + sendJob.getId() + " 처리 결과: " + mailSupport.displaySendStatus(sendStatus), null);
        if (!"SENT".equals(sendStatus)) {
            mailSupport.releaseApplicationClaims(sendJob.getId());
        }
        return "SENT".equals(sendStatus);
    }

    private void scheduleNextDailySend(AdminMailSendJob completedJob) {
        if (researchMasterService.isHidden(completedJob.getResearchNo())) {
            return;
        }
        LocalDateTime base = completedJob.getScheduledAt() == null ? LocalDateTime.now() : completedJob.getScheduledAt();
        LocalDateTime nextScheduledAt = base.plusDays(1);
        String duplicateKey = "LEGACY_SCHEDULED_DAILY:" + completedJob.getResearchNo() + ":" + nextScheduledAt.withNano(0);
        if (mailSupport.isActiveDuplicate(adminMailSendJobMapper.findByDuplicatePreventKey(duplicateKey))) {
            return;
        }
        LegacyResearchRecipientSelection recipients = recipientService.parseRecipients(researchMasterService.getResearchMaster(completedJob.getResearchNo()));
        AdminMailSendJob nextJob = mailSupport.baseJob(
                completedJob.getResearchNo(),
                completedJob.getTemplateId(),
                completedJob.getMailSubjectSnapshot(),
                completedJob.getMailBodySnapshot(),
                MailAttachmentType.fromValue(completedJob.getAttachmentType() == null ? mailSupport.defaultAttachmentType() : completedJob.getAttachmentType()),
                "SCHEDULED",
                "LEGACY_SCHEDULED_DAILY",
                recipients,
                recipientService.loadThresholdSnapshot(completedJob.getResearchNo()),
                null,
                duplicateKey,
                completedJob.getCreatedBy()
        );
        nextJob.setSendStatus("SCHEDULED");
        nextJob.setScheduledAt(nextScheduledAt);
        nextJob.setRepeatYn("Y");
        nextJob.setRepeatUnit("DAILY");
        adminMailSendJobMapper.insert(nextJob);
    }

    private boolean isLegacyMailJob(AdminMailSendJob job) {
        return job != null && job.getTriggerType() != null && job.getTriggerType().startsWith("LEGACY_");
    }

    private boolean isLegacyScheduled(AdminMailSendJob job) {
        return job != null && job.getTriggerType() != null && job.getTriggerType().startsWith("LEGACY_SCHEDULED");
    }

    private boolean isDailyRepeat(AdminMailSendJob sendJob) {
        return "Y".equals(sendJob.getRepeatYn()) && "DAILY".equals(sendJob.getRepeatUnit());
    }
}
