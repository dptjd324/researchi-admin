package com.researchi.admin.legacy.research.web;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.legacy.research.service.LegacyResearchMailService;
import com.researchi.admin.legacy.research.service.ResearchMasterService;
import com.researchi.admin.mailing.domain.MailAttachmentType;
import com.researchi.admin.mailing.service.MailTemplateService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/research/{researchNo}/mail")
public class LegacyResearchMailController {

    private static final int MAX_MAIL_SUBJECT_LENGTH = 255;
    private static final String MISSING_RECIPIENT_EMAIL_MESSAGE = "수신자 이메일을 먼저 등록하세요.";
    private static final String INTRODUCER_MAIL_LABEL = " - 소개자 하진혁(010-2875-3457)";

    private final ResearchMasterService researchMasterService;
    private final LegacyResearchMailService legacyResearchMailService;
    private final MailTemplateService mailTemplateService;

    public LegacyResearchMailController(
            ResearchMasterService researchMasterService,
            LegacyResearchMailService legacyResearchMailService,
            MailTemplateService mailTemplateService
    ) {
        this.researchMasterService = researchMasterService;
        this.legacyResearchMailService = legacyResearchMailService;
        this.mailTemplateService = mailTemplateService;
    }

    @GetMapping
    public String form(@PathVariable Long researchNo, Model model, HttpServletRequest request, CsrfToken csrfToken) {
        request.getSession(true);
        if (researchMasterService.isHidden(researchNo)) {
            return hiddenActionRedirect(researchNo);
        }
        populateModel(researchNo, model, csrfToken, null, null, null, "XLSX", null);
        return "research/mail";
    }

    @PostMapping
    public String send(
            @PathVariable Long researchNo,
            @RequestParam(name = "templateId", required = false) Long templateId,
            @RequestParam(name = "directMailSubject", required = false) String directMailSubject,
            @RequestParam(name = "directMailBody", required = false) String directMailBody,
            @RequestParam(name = "attachmentType", defaultValue = "XLSX") String attachmentType,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request,
            Model model,
            CsrfToken csrfToken
    ) {
        request.getSession(true);
        if (researchMasterService.isHidden(researchNo)) {
            return hiddenActionRedirect(researchNo);
        }
        if (!legacyResearchMailService.hasRecipientEmails(researchNo)) {
            populateModel(researchNo, model, csrfToken, templateId, directMailSubject, directMailBody, attachmentType, MISSING_RECIPIENT_EMAIL_MESSAGE);
            return "research/mail";
        }
        try {
            Long sendJobId = legacyResearchMailService.sendManual(
                    researchNo,
                    templateId,
                    directMailSubject,
                    directMailBody,
                    MailAttachmentType.fromValue(attachmentType),
                    principal,
                    request
            );
            int providedCount = legacyResearchMailService.countProvisionCompletedApplications(sendJobId);
            return "redirect:/research/" + researchNo + "/mail?sent&provided=" + providedCount;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            populateModel(researchNo, model, csrfToken, templateId, directMailSubject, directMailBody, attachmentType, displayErrorMessage(ex.getMessage()));
            return "research/mail";
        }
    }

    @PostMapping("/schedule")
    public String schedule(
            @PathVariable Long researchNo,
            @RequestParam(name = "templateId", required = false) Long templateId,
            @RequestParam(name = "directMailSubject", required = false) String directMailSubject,
            @RequestParam(name = "directMailBody", required = false) String directMailBody,
            @RequestParam(name = "attachmentType", defaultValue = "XLSX") String attachmentType,
            @RequestParam(name = "scheduledAt", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime scheduledAt,
            @RequestParam(name = "dailyScheduledTime", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
            LocalTime dailyScheduledTime,
            @RequestParam(name = "dailyRepeat", defaultValue = "false") boolean dailyRepeat,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request,
            Model model,
            CsrfToken csrfToken
    ) {
        request.getSession(true);
        if (researchMasterService.isHidden(researchNo)) {
            return hiddenActionRedirect(researchNo);
        }
        if (!legacyResearchMailService.hasRecipientEmails(researchNo)) {
            populateModel(researchNo, model, csrfToken, templateId, directMailSubject, directMailBody, attachmentType, MISSING_RECIPIENT_EMAIL_MESSAGE);
            model.addAttribute("scheduledAt", scheduledAt);
            model.addAttribute("dailyScheduledTime", dailyScheduledTime);
            model.addAttribute("dailyRepeat", dailyRepeat);
            return "research/mail";
        }
        try {
            legacyResearchMailService.schedule(
                    researchNo,
                    templateId,
                    directMailSubject,
                    directMailBody,
                    MailAttachmentType.fromValue(attachmentType),
                    scheduledAt,
                    dailyScheduledTime,
                    dailyRepeat,
                    principal,
                    request
            );
            return "redirect:/research/" + researchNo + "/mail?scheduled";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            populateModel(researchNo, model, csrfToken, templateId, directMailSubject, directMailBody, attachmentType, ex.getMessage());
            model.addAttribute("scheduledAt", scheduledAt);
            model.addAttribute("dailyScheduledTime", dailyScheduledTime);
            model.addAttribute("dailyRepeat", dailyRepeat);
            return "research/mail";
        }
    }

    @PostMapping("/threshold-settings")
    public String thresholdSettings(
            @PathVariable Long researchNo,
            @RequestParam(name = "thresholdCount", required = false) Integer thresholdCount,
            @RequestParam(name = "templateId", required = false) Long templateId,
            @RequestParam(name = "directMailSubject", required = false) String directMailSubject,
            @RequestParam(name = "directMailBody", required = false) String directMailBody,
            @RequestParam(name = "attachmentType", defaultValue = "XLSX") String attachmentType,
            @RequestParam(name = "enabled", defaultValue = "false") boolean enabled,
            Model model,
            CsrfToken csrfToken
    ) {
        if (researchMasterService.isHidden(researchNo)) {
            return hiddenActionRedirect(researchNo);
        }
        if (!legacyResearchMailService.hasRecipientEmails(researchNo)) {
            populateModel(researchNo, model, csrfToken, templateId, directMailSubject, directMailBody, attachmentType, MISSING_RECIPIENT_EMAIL_MESSAGE);
            model.addAttribute("thresholdCount", thresholdCount);
            model.addAttribute("thresholdEnabled", true);
            return "research/mail";
        }
        try {
            legacyResearchMailService.saveMailRule(
                    researchNo,
                    thresholdCount,
                    templateId,
                    directMailSubject,
                    directMailBody,
                    MailAttachmentType.fromValue(attachmentType),
                    enabled
            );
            return "redirect:/research/" + researchNo + "/mail?thresholdSaved";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            populateModel(researchNo, model, csrfToken, templateId, directMailSubject, directMailBody, attachmentType, ex.getMessage());
            model.addAttribute("thresholdCount", thresholdCount);
            model.addAttribute("thresholdEnabled", enabled);
            return "research/mail";
        }
    }

    @PostMapping("/scheduled/{sendJobId}/cancel")
    public String cancelScheduled(
            @PathVariable Long researchNo,
            @PathVariable Long sendJobId,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request,
            Model model,
            CsrfToken csrfToken
    ) {
        if (researchMasterService.isHidden(researchNo)) {
            return hiddenActionRedirect(researchNo);
        }
        try {
            legacyResearchMailService.cancelScheduledJob(sendJobId, principal, request);
            return "redirect:/research/" + researchNo + "/mail?scheduledCancelled";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            populateModel(researchNo, model, csrfToken, null, null, null, "XLSX", ex.getMessage());
            return "research/mail";
        }
    }

    @PostMapping("/threshold-trigger")
    public String thresholdTrigger(
            @PathVariable Long researchNo,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request,
            Model model,
            CsrfToken csrfToken
    ) {
        if (researchMasterService.isHidden(researchNo)) {
            return hiddenActionRedirect(researchNo);
        }
        if (!legacyResearchMailService.hasRecipientEmails(researchNo)) {
            populateModel(researchNo, model, csrfToken, null, null, null, "XLSX", MISSING_RECIPIENT_EMAIL_MESSAGE);
            return "research/mail";
        }
        try {
            Long sendJobId = legacyResearchMailService.triggerThreshold(researchNo, principal, request);
            int providedCount = legacyResearchMailService.countProvisionCompletedApplications(sendJobId);
            return "redirect:/research/" + researchNo + "/mail?thresholdSent&provided=" + providedCount;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            populateModel(researchNo, model, csrfToken, null, null, null, "XLSX", ex.getMessage());
            return "research/mail";
        }
    }

    @PostMapping("/threshold-cancel")
    public String cancelThreshold(
            @PathVariable Long researchNo,
            Model model,
            CsrfToken csrfToken
    ) {
        if (researchMasterService.isHidden(researchNo)) {
            return hiddenActionRedirect(researchNo);
        }
        try {
            legacyResearchMailService.cancelMailRule(researchNo);
            return "redirect:/research/" + researchNo + "/mail?thresholdCancelled";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            populateModel(researchNo, model, csrfToken, null, null, null, "XLSX", ex.getMessage());
            return "research/mail";
        }
    }

    @PostMapping("/threshold-rules")
    public String addThresholdRule(
            @PathVariable Long researchNo,
            @RequestParam(name = "thresholdCount", required = false) Integer thresholdCount,
            @RequestParam(name = "templateId", required = false) Long templateId,
            @RequestParam(name = "directMailSubject", required = false) String directMailSubject,
            @RequestParam(name = "directMailBody", required = false) String directMailBody,
            @RequestParam(name = "attachmentType", defaultValue = "XLSX") String attachmentType,
            @RequestParam(name = "enabled", defaultValue = "false") boolean enabled,
            Model model,
            CsrfToken csrfToken
    ) {
        if (researchMasterService.isHidden(researchNo)) {
            return hiddenActionRedirect(researchNo);
        }
        if (!legacyResearchMailService.hasRecipientEmails(researchNo)) {
            populateModel(researchNo, model, csrfToken, templateId, directMailSubject, directMailBody, attachmentType, MISSING_RECIPIENT_EMAIL_MESSAGE);
            model.addAttribute("thresholdCount", thresholdCount);
            model.addAttribute("thresholdEnabled", true);
            return "research/mail";
        }
        try {
            legacyResearchMailService.addMailRuleItem(
                    researchNo,
                    thresholdCount,
                    templateId,
                    directMailSubject,
                    directMailBody,
                    MailAttachmentType.fromValue(attachmentType),
                    enabled
            );
            return "redirect:/research/" + researchNo + "/mail?thresholdRuleAdded";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            populateModel(researchNo, model, csrfToken, templateId, directMailSubject, directMailBody, attachmentType, ex.getMessage());
            model.addAttribute("thresholdCount", thresholdCount);
            model.addAttribute("thresholdEnabled", enabled);
            return "research/mail";
        }
    }

    @PostMapping("/threshold-rules/{ruleId}/trigger")
    public String triggerThresholdRule(
            @PathVariable Long researchNo,
            @PathVariable Long ruleId,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request,
            Model model,
            CsrfToken csrfToken
    ) {
        if (researchMasterService.isHidden(researchNo)) {
            return hiddenActionRedirect(researchNo);
        }
        if (!legacyResearchMailService.hasRecipientEmails(researchNo)) {
            populateModel(researchNo, model, csrfToken, null, null, null, "XLSX", MISSING_RECIPIENT_EMAIL_MESSAGE);
            return "research/mail";
        }
        try {
            Long sendJobId = legacyResearchMailService.triggerThresholdRule(researchNo, ruleId, principal, request);
            int providedCount = legacyResearchMailService.countProvisionCompletedApplications(sendJobId);
            return "redirect:/research/" + researchNo + "/mail?thresholdSent&provided=" + providedCount;
        } catch (IllegalArgumentException | IllegalStateException ex) {
            populateModel(researchNo, model, csrfToken, null, null, null, "XLSX", ex.getMessage());
            return "research/mail";
        }
    }

    @PostMapping("/threshold-rules/{ruleId}/delete")
    public String deleteThresholdRule(
            @PathVariable Long researchNo,
            @PathVariable Long ruleId,
            Model model,
            CsrfToken csrfToken
    ) {
        if (researchMasterService.isHidden(researchNo)) {
            return hiddenActionRedirect(researchNo);
        }
        try {
            legacyResearchMailService.deleteMailRuleItem(researchNo, ruleId);
            return "redirect:/research/" + researchNo + "/mail?thresholdRuleDeleted";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            populateModel(researchNo, model, csrfToken, null, null, null, "XLSX", ex.getMessage());
            return "research/mail";
        }
    }

    private void populateModel(
            Long researchNo,
            Model model,
            CsrfToken csrfToken,
            Long templateId,
            String directMailSubject,
            String directMailBody,
            String attachmentType,
            String errorMessage
    ) {
        ResearchMaster researchMaster = researchMasterService.getResearchMaster(researchNo);
        model.addAttribute("pageTitle", "좌담회/설문 메일 발송");
        model.addAttribute("pageDescription", "지원자 메일을 수동 또는 예약으로 발송합니다.");
        model.addAttribute("research", researchMaster);
        model.addAttribute("preview", legacyResearchMailService.getPreview(researchNo));
        model.addAttribute("historyItems", legacyResearchMailService.getHistory(researchNo));
        var scheduledJobs = legacyResearchMailService.getScheduledJobs(researchNo);
        model.addAttribute("scheduledJobs", scheduledJobs);
        model.addAttribute("dailyScheduledTargetCounts", dailyScheduledTargetCounts(scheduledJobs));
        var legacyMailRule = legacyResearchMailService.getMailRule(researchNo);
        model.addAttribute("legacyMailRule", legacyMailRule);
        model.addAttribute("legacyMailRuleItems", legacyResearchMailService.getMailRuleItems(researchNo));
        model.addAttribute("templateOptions", mailTemplateService.getActiveTemplates());
        model.addAttribute("attachmentTypes", List.of(MailAttachmentType.values()));
        model.addAttribute("templateId", templateId);
        model.addAttribute("defaultMailSubject", defaultMailSubject(researchMaster));
        model.addAttribute("directMailSubject", directMailSubject == null ? defaultMailSubject(researchMaster) : directMailSubject);
        model.addAttribute("directMailBody", directMailBody);
        model.addAttribute("attachmentType", attachmentType == null ? "XLSX" : attachmentType);
        model.addAttribute("minScheduledAt", legacyResearchMailService.minimumScheduledAt());
        model.addAttribute("scheduledAt", LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.MINUTES));
        model.addAttribute("dailyScheduledTime", LocalTime.now().plusHours(1).truncatedTo(ChronoUnit.MINUTES));
        model.addAttribute("dailyRepeat", Boolean.FALSE);
        model.addAttribute("thresholdCount", legacyMailRule.getThresholdCount());
        model.addAttribute("thresholdEnabled", legacyMailRule.isEnabled());
        model.addAttribute("errorMessage", errorMessage);
        model.addAttribute("_csrf", csrfToken);
    }

    private String displayErrorMessage(String message) {
        if (message != null && (message.contains("Recipient email was not found") || message.contains("수신 이메일을 찾을 수 없습니다"))) {
            return "수신 이메일을 찾을 수 없습니다. 수신 이메일을 등록해주세요.";
        }
        if (message != null && (message.contains("PROVIDE_YN=N") || message.contains("발송 대상 신청자 정보가 없습니다"))) {
            return "발송 대상자가 없습니다.";
        }
        return message;
    }

    private Map<Long, Integer> dailyScheduledTargetCounts(List<com.researchi.admin.mailing.domain.AdminMailSendJob> scheduledJobs) {
        return scheduledJobs.stream()
                .filter(job -> "Y".equals(job.getRepeatYn()) && "DAILY".equals(job.getRepeatUnit()))
                .collect(Collectors.toMap(
                        com.researchi.admin.mailing.domain.AdminMailSendJob::getId,
                        job -> legacyResearchMailService.countCurrentDailyScheduledTargets(job.getResearchNo())
                ));
    }

    private String defaultMailSubject(ResearchMaster researchMaster) {
        String subject = researchMaster.getResearchTitle() + INTRODUCER_MAIL_LABEL;
        return subject.length() <= MAX_MAIL_SUBJECT_LENGTH ? subject : subject.substring(0, MAX_MAIL_SUBJECT_LENGTH);
    }

    private String hiddenActionRedirect(Long researchNo) {
        return "redirect:/research/" + researchNo + "?hiddenActionBlocked";
    }
}
