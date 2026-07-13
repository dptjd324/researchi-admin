package com.researchi.admin.legacy.research.service.mail;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.export.domain.ExportPayload;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.legacy.research.service.LegacyResearchExportService;
import com.researchi.admin.legacy.research.service.ResearchMasterService;
import com.researchi.admin.legacy.research.service.recipient.LegacyResearchRecipientSelection;
import com.researchi.admin.legacy.research.service.recipient.LegacyResearchRecipientService;
import com.researchi.admin.mailing.domain.AdminMailSendJob;
import com.researchi.admin.mailing.domain.MailAttachmentType;
import com.researchi.admin.mailing.domain.MailDispatchResult;
import com.researchi.admin.mailing.mapper.AdminMailSendJobMapper;
import com.researchi.admin.mailing.service.MailDispatchGateway;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LegacyResearchManualMailService {

    private final ResearchMasterService researchMasterService;
    private final LegacyResearchExportService legacyResearchExportService;
    private final AdminMailSendJobMapper adminMailSendJobMapper;
    private final MailDispatchGateway mailDispatchGateway;
    private final LegacyResearchRecipientService recipientService;
    private final LegacyResearchMailSupportService mailSupport;

    public LegacyResearchManualMailService(
            ResearchMasterService researchMasterService,
            LegacyResearchExportService legacyResearchExportService,
            AdminMailSendJobMapper adminMailSendJobMapper,
            MailDispatchGateway mailDispatchGateway,
            LegacyResearchRecipientService recipientService,
            LegacyResearchMailSupportService mailSupport
    ) {
        this.researchMasterService = researchMasterService;
        this.legacyResearchExportService = legacyResearchExportService;
        this.adminMailSendJobMapper = adminMailSendJobMapper;
        this.mailDispatchGateway = mailDispatchGateway;
        this.recipientService = recipientService;
        this.mailSupport = mailSupport;
    }

    @Transactional("adminTransactionManager")
    public Long sendManual(
            Long researchNo,
            Long templateId,
            String directMailSubject,
            String directMailBody,
            MailAttachmentType attachmentType,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        ResearchMaster researchMaster = researchMasterService.getResearchMaster(researchNo);
        LegacyResearchMailSnapshot snapshot = recipientService.loadSnapshot(researchNo);
        LegacyResearchRecipientSelection recipients = recipientService.parseRecipients(researchMaster);
        LegacyResearchMailContent mailContent = mailSupport.resolveMailContent(templateId, directMailSubject, directMailBody);
        String duplicateKey = "LEGACY_MANUAL:" + researchNo + ":" + System.currentTimeMillis();
        AdminMailSendJob sendJob = mailSupport.baseJob(
                researchNo,
                mailContent.templateId(),
                mailContent.subject(),
                mailContent.body(),
                attachmentType,
                "MANUAL",
                "LEGACY_MANUAL",
                recipients,
                snapshot,
                null,
                duplicateKey,
                principal == null ? null : principal.getId()
        );
        sendJob.setSendStatus(snapshot.applicationIds().isEmpty() ? "FAILED" : "RUNNING");
        adminMailSendJobMapper.insert(sendJob);

        if (snapshot.applicationIds().isEmpty()) {
            mailSupport.safeLog(
                    principal == null ? null : principal.getId(),
                    "MAIL_SEND_LEGACY_MANUAL",
                    "RESEARCH",
                    String.valueOf(researchNo),
                    "수동 메일 작업 #" + sendJob.getId() + " 실패: " + LegacyResearchMailSupportService.NO_UNPROVIDED_DATA_REASON,
                    request
            );
            return mailSupport.requireImmediateSendSuccess(sendJob.getId());
        }

        List<Long> claimedApplicationIds = mailSupport.claimApplications(researchNo, snapshot.applicationIds(), sendJob.getId());
        String sendStatus;
        String targetResult;
        String failReason = null;
        LocalDateTime sentAt = null;
        MailDispatchResult dispatchResult = null;
        if (claimedApplicationIds.isEmpty()) {
            sendStatus = "FAILED";
            targetResult = "FAILED";
            failReason = LegacyResearchMailSupportService.CLAIMED_BY_OTHER_SEND_REASON;
        } else if (recipients.recipients().isEmpty()) {
            sendStatus = "FAILED";
            targetResult = "FAILED";
            failReason = "수신 이메일을 찾을 수 없습니다. 수신 이메일을 등록해주세요.";
        } else {
            ExportPayload attachment = attachmentType == MailAttachmentType.XLSX
                    ? legacyResearchExportService.prepareXlsx(researchNo, claimedApplicationIds)
                    : legacyResearchExportService.prepareTxt(researchNo, claimedApplicationIds);
            try {
                dispatchResult = mailDispatchGateway.dispatch(mailSupport.buildDispatchRequest(researchMaster, recipients.recipients(), mailContent, attachment, attachmentType, claimedApplicationIds.size(), claimedApplicationIds));
                sendStatus = "SENT";
                targetResult = "SENT";
                sentAt = LocalDateTime.now();
            } catch (Exception ex) {
                sendStatus = "FAILED";
                targetResult = "FAILED";
                failReason = mailSupport.trimFailureReason(ex.getMessage());
            }
        }

        sendJob.setSendStatus(sendStatus);
        sendJob.setSentAt(sentAt);
        adminMailSendJobMapper.updateStatus(sendJob);
        mailSupport.recordProviderResult(sendJob, dispatchResult);
        mailSupport.insertTargets(sendJob.getId(), claimedApplicationIds.isEmpty() ? snapshot.applicationIds() : claimedApplicationIds, recipients, targetResult, failReason, sentAt);
        if ("SENT".equals(sendStatus)) {
            mailSupport.markApplicationsProvided(
                    researchNo,
                    claimedApplicationIds,
                    principal == null ? null : principal.getId(),
                    "수동 메일 작업 #" + sendJob.getId()
            );
        }
        mailSupport.safeLog(principal == null ? null : principal.getId(), "MAIL_SEND_LEGACY_MANUAL", "RESEARCH", String.valueOf(researchNo), "수동 메일 작업 #" + sendJob.getId() + " 처리 결과: " + mailSupport.displaySendStatus(sendStatus), request);
        if (!"SENT".equals(sendStatus)) {
            mailSupport.releaseApplicationClaims(sendJob.getId());
        }
        return mailSupport.requireImmediateSendSuccess(sendJob.getId());
    }
}
