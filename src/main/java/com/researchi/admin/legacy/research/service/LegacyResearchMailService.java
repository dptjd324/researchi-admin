package com.researchi.admin.legacy.research.service;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.legacy.mail.domain.LegacyMailRule;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.legacy.research.service.history.LegacyResearchMailHistoryService;
import com.researchi.admin.legacy.research.service.mail.LegacyResearchMailSnapshot;
import com.researchi.admin.legacy.research.service.mail.LegacyResearchManualMailService;
import com.researchi.admin.legacy.research.service.recipient.LegacyResearchRecipientSelection;
import com.researchi.admin.legacy.research.service.recipient.LegacyResearchRecipientService;
import com.researchi.admin.legacy.research.service.rule.LegacyResearchMailRuleService;
import com.researchi.admin.legacy.research.service.schedule.LegacyResearchScheduledMailService;
import com.researchi.admin.legacy.research.service.threshold.LegacyResearchThresholdMailService;
import com.researchi.admin.mailing.domain.AdminMailSendJob;
import com.researchi.admin.mailing.domain.MailAttachmentType;
import com.researchi.admin.mailing.domain.MailingHistoryItem;
import com.researchi.admin.mailing.domain.MailingPreview;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class LegacyResearchMailService {

    private final ResearchMasterService researchMasterService;
    private final LegacyResearchMailHistoryService historyService;
    private final LegacyResearchMailRuleService mailRuleService;
    private final LegacyResearchRecipientService recipientService;
    private final LegacyResearchManualMailService manualMailService;
    private final LegacyResearchScheduledMailService scheduledMailService;
    private final LegacyResearchThresholdMailService thresholdMailService;

    public LegacyResearchMailService(
            ResearchMasterService researchMasterService,
            LegacyResearchMailHistoryService historyService,
            LegacyResearchMailRuleService mailRuleService,
            LegacyResearchRecipientService recipientService,
            LegacyResearchManualMailService manualMailService,
            LegacyResearchScheduledMailService scheduledMailService,
            LegacyResearchThresholdMailService thresholdMailService
    ) {
        this.researchMasterService = researchMasterService;
        this.historyService = historyService;
        this.mailRuleService = mailRuleService;
        this.recipientService = recipientService;
        this.manualMailService = manualMailService;
        this.scheduledMailService = scheduledMailService;
        this.thresholdMailService = thresholdMailService;
    }

    public List<MailingHistoryItem> getHistory(Long researchNo) {
        return historyService.getHistory(researchNo);
    }

    public List<AdminMailSendJob> getScheduledJobs(Long researchNo) {
        return historyService.getScheduledJobs(researchNo);
    }

    public int countProvisionCompletedApplications(Long sendJobId) {
        return historyService.countProvisionCompletedApplications(sendJobId);
    }

    public int countCurrentDailyScheduledTargets(Long researchNo) {
        return recipientService.loadThresholdSnapshot(researchNo).applicationIds().size();
    }

    public boolean hasRecipientEmails(Long researchNo) {
        if (researchNo == null) {
            return false;
        }
        ResearchMaster researchMaster = researchMasterService.getResearchMaster(researchNo);
        return !recipientService.parseRecipients(researchMaster).recipients().isEmpty();
    }

    public MailingPreview getPreview(Long researchNo) {
        if (researchNo == null) {
            return null;
        }
        ResearchMaster researchMaster = researchMasterService.getResearchMaster(researchNo);
        LegacyResearchRecipientSelection recipients = recipientService.parseRecipients(researchMaster);
        LegacyResearchMailSnapshot snapshot = recipientService.loadSnapshot(researchNo);
        return new MailingPreview(
                researchNo,
                researchMaster.getResearchTitle(),
                recipients.recipients(),
                recipients.recipients().size(),
                recipients.excludedCount(),
                snapshot.applicationIds().size(),
                snapshot.blacklistExcludedCount()
        );
    }

    public LegacyMailRule getMailRule(Long researchNo) {
        return mailRuleService.getMailRule(researchNo);
    }

    public List<LegacyMailRule> getMailRuleItems(Long researchNo) {
        return mailRuleService.getMailRuleItems(researchNo);
    }

    public void saveMailRule(
            Long researchNo,
            Integer thresholdCount,
            Long templateId,
            String directMailSubject,
            String directMailBody,
            MailAttachmentType attachmentType,
            boolean enabled
    ) {
        researchMasterService.assertNotHidden(researchNo);
        mailRuleService.saveMailRule(researchNo, thresholdCount, templateId, directMailSubject, directMailBody, attachmentType, enabled);
    }

    public void addMailRuleItem(
            Long researchNo,
            Integer thresholdCount,
            Long templateId,
            String directMailSubject,
            String directMailBody,
            MailAttachmentType attachmentType,
            boolean enabled
    ) {
        researchMasterService.assertNotHidden(researchNo);
        mailRuleService.addMailRuleItem(researchNo, thresholdCount, templateId, directMailSubject, directMailBody, attachmentType, enabled);
    }

    public void deleteMailRuleItem(Long researchNo, Long ruleId) {
        researchMasterService.assertNotHidden(researchNo);
        mailRuleService.deleteMailRuleItem(researchNo, ruleId);
    }

    public void cancelMailRule(Long researchNo) {
        researchMasterService.assertNotHidden(researchNo);
        mailRuleService.cancelMailRule(researchNo);
    }

    public void cancelScheduledJob(Long sendJobId, AdminPrincipal principal, HttpServletRequest request) {
        scheduledMailService.cancelScheduledJob(sendJobId, principal, request);
    }

    public Long sendManual(
            Long researchNo,
            Long templateId,
            String directMailSubject,
            String directMailBody,
            MailAttachmentType attachmentType,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        researchMasterService.assertNotHidden(researchNo);
        return manualMailService.sendManual(researchNo, templateId, directMailSubject, directMailBody, attachmentType, principal, request);
    }

    public Long triggerThreshold(Long researchNo, AdminPrincipal principal, HttpServletRequest request) {
        researchMasterService.assertNotHidden(researchNo);
        return thresholdMailService.triggerThreshold(researchNo, principal, request);
    }

    public Long triggerThresholdRule(Long researchNo, Long ruleId, AdminPrincipal principal, HttpServletRequest request) {
        researchMasterService.assertNotHidden(researchNo);
        return thresholdMailService.triggerThresholdRule(researchNo, ruleId, principal, request);
    }

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
        return scheduledMailService.schedule(researchNo, templateId, directMailSubject, directMailBody, attachmentType, scheduledAt, dailyScheduledTime, dailyRepeat, principal, request);
    }

    public LocalDateTime minimumScheduledAt() {
        return scheduledMailService.minimumScheduledAt();
    }

    public boolean triggerThresholdAutomatically(Long researchNo) {
        if (researchMasterService.isHidden(researchNo)) {
            return false;
        }
        return thresholdMailService.triggerThresholdAutomatically(researchNo);
    }

    public boolean triggerThresholdRuleAutomatically(Long ruleId) {
        return thresholdMailService.triggerThresholdRuleAutomatically(ruleId);
    }

    public List<Long> getEnabledThresholdResearchNos() {
        return thresholdMailService.getEnabledThresholdResearchNos();
    }

    public List<Long> getEnabledThresholdRuleIds() {
        return thresholdMailService.getEnabledThresholdRuleIds();
    }

    public boolean executeScheduledSend(Long sendJobId) {
        return scheduledMailService.executeScheduledSend(sendJobId);
    }
}
