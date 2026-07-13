package com.researchi.admin.legacy.research.service.threshold;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.export.domain.ExportPayload;
import com.researchi.admin.legacy.mail.domain.LegacyMailRule;
import com.researchi.admin.legacy.mail.mapper.LegacyMailRuleMapper;
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
import com.researchi.admin.mailing.service.MailDispatchGateway;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class LegacyResearchThresholdMailService {

    private final ResearchMasterService researchMasterService;
    private final LegacyResearchExportService legacyResearchExportService;
    private final LegacyMailRuleMapper legacyMailRuleMapper;
    private final AdminMailSendJobMapper adminMailSendJobMapper;
    private final MailDispatchGateway mailDispatchGateway;
    private final LegacyResearchRecipientService recipientService;
    private final LegacyResearchMailSupportService mailSupport;

    public LegacyResearchThresholdMailService(
            ResearchMasterService researchMasterService,
            LegacyResearchExportService legacyResearchExportService,
            LegacyMailRuleMapper legacyMailRuleMapper,
            AdminMailSendJobMapper adminMailSendJobMapper,
            MailDispatchGateway mailDispatchGateway,
            LegacyResearchRecipientService recipientService,
            LegacyResearchMailSupportService mailSupport
    ) {
        this.researchMasterService = researchMasterService;
        this.legacyResearchExportService = legacyResearchExportService;
        this.legacyMailRuleMapper = legacyMailRuleMapper;
        this.adminMailSendJobMapper = adminMailSendJobMapper;
        this.mailDispatchGateway = mailDispatchGateway;
        this.recipientService = recipientService;
        this.mailSupport = mailSupport;
    }

    @Transactional(value = "adminTransactionManager", propagation = Propagation.NOT_SUPPORTED)
    public Long triggerThreshold(Long researchNo, AdminPrincipal principal, HttpServletRequest request) {
        researchMasterService.assertNotHidden(researchNo);
        LegacyMailRule rule = legacyMailRuleMapper.findByResearchNo(researchNo);
        if (rule == null || rule.getThresholdCount() == null || rule.getThresholdCount() < 1) {
            throw new IllegalStateException("임계치 발송 설정이 없습니다.");
        }
        LegacyResearchMailSnapshot snapshot = recipientService.loadThresholdSnapshot(researchNo);
        if (snapshot.applicationIds().isEmpty()) {
            Long sendJobId = sendThresholdNow(rule, snapshot, principal, request, false);
            return mailSupport.requireImmediateSendSuccess(sendJobId);
        }
        Long sendJobId = sendThresholdNow(rule, snapshot, principal, request, false);
        return mailSupport.requireImmediateSendSuccess(sendJobId);
    }

    @Transactional(value = "adminTransactionManager", propagation = Propagation.NOT_SUPPORTED)
    public Long triggerThresholdRule(Long researchNo, Long ruleId, AdminPrincipal principal, HttpServletRequest request) {
        researchMasterService.assertNotHidden(researchNo);
        LegacyMailRule rule = legacyMailRuleMapper.findRuleItemById(ruleId);
        if (rule == null || !Objects.equals(rule.getResearchNo(), researchNo)) {
            throw new IllegalStateException("임계치 발송 설정이 없습니다.");
        }
        if (rule.getThresholdCount() == null || rule.getThresholdCount() < 1) {
            throw new IllegalStateException("임계치 발송 설정이 없습니다.");
        }
        LegacyResearchMailSnapshot snapshot = recipientService.loadThresholdSnapshot(researchNo);
        if (snapshot.applicationIds().isEmpty()) {
            Long sendJobId = sendThresholdNow(rule, snapshot, principal, request, true);
            return mailSupport.requireImmediateSendSuccess(sendJobId);
        }
        Long sendJobId = sendThresholdNow(rule, snapshot, principal, request, true);
        return mailSupport.requireImmediateSendSuccess(sendJobId);
    }

    @Transactional(value = "adminTransactionManager", propagation = Propagation.NOT_SUPPORTED)
    public boolean triggerThresholdAutomatically(Long researchNo) {
        if (researchMasterService.isHidden(researchNo)) {
            return false;
        }
        LegacyMailRule rule = legacyMailRuleMapper.findByResearchNo(researchNo);
        if (rule == null || !rule.isEnabled() || rule.getThresholdCount() == null || rule.getThresholdCount() < 1) {
            return false;
        }
        LegacyResearchMailSnapshot snapshot = recipientService.loadThresholdSnapshot(researchNo);
        if (snapshot.applicationIds().size() < rule.getThresholdCount()) {
            return false;
        }
        Long sendJobId = sendThresholdNow(
                rule,
                snapshot,
                new AdminPrincipal(null, "scheduler", "", "Scheduler", "Y", null),
                null,
                false
        );
        AdminMailSendJob sendJob = adminMailSendJobMapper.findById(sendJobId);
        return sendJob != null && "SENT".equals(sendJob.getSendStatus());
    }

    @Transactional(value = "adminTransactionManager", propagation = Propagation.NOT_SUPPORTED)
    public boolean triggerThresholdRuleAutomatically(Long ruleId) {
        LegacyMailRule rule = legacyMailRuleMapper.findRuleItemById(ruleId);
        if (rule == null || !rule.isEnabled() || rule.getThresholdCount() == null || rule.getThresholdCount() < 1) {
            return false;
        }
        if (researchMasterService.isHidden(rule.getResearchNo())) {
            return false;
        }
        LegacyResearchMailSnapshot snapshot = recipientService.loadThresholdSnapshot(rule.getResearchNo());
        if (snapshot.applicationIds().size() < rule.getThresholdCount()) {
            return false;
        }
        Long sendJobId = sendThresholdNow(
                rule,
                snapshot,
                new AdminPrincipal(null, "scheduler", "", "Scheduler", "Y", null),
                null,
                true
        );
        AdminMailSendJob sendJob = adminMailSendJobMapper.findById(sendJobId);
        return sendJob != null && "SENT".equals(sendJob.getSendStatus());
    }

    public List<Long> getEnabledThresholdResearchNos() {
        return legacyMailRuleMapper.findEnabled().stream()
                .map(LegacyMailRule::getResearchNo)
                .distinct()
                .toList();
    }

    public List<Long> getEnabledThresholdRuleIds() {
        return legacyMailRuleMapper.findEnabledRuleItems().stream()
                .map(LegacyMailRule::getId)
                .toList();
    }

    private Long sendThresholdNow(
            LegacyMailRule rule,
            LegacyResearchMailSnapshot snapshot,
            AdminPrincipal principal,
            HttpServletRequest request,
            boolean ruleItem
    ) {
        ResearchMaster researchMaster = researchMasterService.getResearchMaster(rule.getResearchNo());
        LegacyResearchRecipientSelection recipients = recipientService.parseRecipients(researchMaster);
        LegacyResearchMailContent mailContent = mailSupport.resolveMailContent(rule.getTemplateId(), rule.getDirectMailSubject(), rule.getDirectMailBody());
        MailAttachmentType attachmentType = MailAttachmentType.fromValue(rule.getAttachmentType() == null ? mailSupport.defaultAttachmentType() : rule.getAttachmentType());
        String applicationKey = Integer.toHexString(snapshot.applicationIds().hashCode());
        String duplicateKey = (ruleItem ? "LEGACY_THRESHOLD_RULE:" + rule.getId() : "LEGACY_THRESHOLD:" + rule.getResearchNo())
                + ":" + rule.getThresholdCount() + ":" + applicationKey;
        mailSupport.assertNoDuplicate(duplicateKey);

        AdminMailSendJob sendJob = mailSupport.baseJob(
                rule.getResearchNo(),
                mailContent.templateId(),
                mailContent.subject(),
                mailContent.body(),
                attachmentType,
                "AUTO",
                "LEGACY_THRESHOLD",
                recipients,
                snapshot,
                rule.getThresholdCount(),
                duplicateKey,
                principal == null ? null : principal.getId()
        );
        sendJob.setSendStatus(snapshot.applicationIds().isEmpty() ? "FAILED" : "RUNNING");
        adminMailSendJobMapper.insert(sendJob);

        List<Long> claimedApplicationIds = snapshot.applicationIds().isEmpty()
                ? List.of()
                : mailSupport.claimApplications(rule.getResearchNo(), snapshot.applicationIds(), sendJob.getId());
        String sendStatus;
        String targetResult;
        String failReason = null;
        LocalDateTime sentAt = null;
        MailDispatchResult dispatchResult = null;
        if (snapshot.applicationIds().isEmpty()) {
            sendStatus = "FAILED";
            targetResult = "FAILED";
            failReason = LegacyResearchMailSupportService.NO_UNPROVIDED_DATA_REASON;
        } else if (claimedApplicationIds.isEmpty()) {
            sendStatus = "FAILED";
            targetResult = "FAILED";
            failReason = LegacyResearchMailSupportService.CLAIMED_BY_OTHER_SEND_REASON;
        } else if (recipients.recipients().isEmpty()) {
            sendStatus = "FAILED";
            targetResult = "FAILED";
            failReason = "수신 이메일을 찾을 수 없습니다. 수신 이메일을 등록해주세요.";
        } else {
            ExportPayload attachment = attachmentType == MailAttachmentType.XLSX
                    ? legacyResearchExportService.prepareXlsx(rule.getResearchNo(), claimedApplicationIds)
                    : legacyResearchExportService.prepareTxt(rule.getResearchNo(), claimedApplicationIds);
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
            if (ruleItem) {
                legacyMailRuleMapper.touchRuleItemLastTriggeredAt(rule.getId(), sentAt);
            } else {
                legacyMailRuleMapper.touchLastTriggeredAt(rule.getResearchNo(), sentAt);
            }
            mailSupport.markApplicationsProvided(
                    rule.getResearchNo(),
                    claimedApplicationIds,
                    principal == null ? null : principal.getId(),
                    "임계치 메일 작업 #" + sendJob.getId()
            );
        }
        mailSupport.safeLog(principal == null ? null : principal.getId(), "MAIL_SEND_LEGACY_THRESHOLD", "RESEARCH", String.valueOf(rule.getResearchNo()), "임계치 메일 작업 #" + sendJob.getId() + " 처리 결과: " + mailSupport.displaySendStatus(sendStatus), request);
        if (!"SENT".equals(sendStatus)) {
            mailSupport.releaseApplicationClaims(sendJob.getId());
        }
        return sendJob.getId();
    }
}
