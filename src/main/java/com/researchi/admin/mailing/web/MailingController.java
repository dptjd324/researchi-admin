package com.researchi.admin.mailing.web;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.mailing.domain.MailAttachmentType;
import com.researchi.admin.mailing.domain.MailingPreview;
import com.researchi.admin.mailing.service.MailTemplateService;
import com.researchi.admin.mailing.service.MailingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/mail/send")
public class MailingController {

    private final MailingService mailingService;
    private final MailTemplateService mailTemplateService;
    private final JobService jobService;

    public MailingController(
            MailingService mailingService,
            MailTemplateService mailTemplateService,
            JobService jobService
    ) {
        this.mailingService = mailingService;
        this.mailTemplateService = mailTemplateService;
        this.jobService = jobService;
    }

    @GetMapping("/history")
    public String history(
            @RequestParam(name = "documentSrl", required = false) Long documentSrl,
            Model model,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        request.getSession(true);
        populateModel(model, documentSrl, defaultManualForm(documentSrl), defaultScheduleForm(documentSrl), defaultThresholdForm(documentSrl), csrfToken);
        return "mail/history";
    }

    @PostMapping("/manual")
    public String manual(
            @Valid @ModelAttribute("manualForm") MailSendManualForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            populateModel(model, form.getDocumentSrl(), form, defaultScheduleForm(form.getDocumentSrl()), defaultThresholdForm(form.getDocumentSrl()), resolveCsrfToken(request));
            return "mail/history";
        }
        try {
            mailingService.sendManual(form, principal, request);
            return redirectToHistory(form.getDocumentSrl(), "manualSent");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            bindingResult.reject("manualError", ex.getMessage());
            populateModel(model, form.getDocumentSrl(), form, defaultScheduleForm(form.getDocumentSrl()), defaultThresholdForm(form.getDocumentSrl()), resolveCsrfToken(request));
            return "mail/history";
        }
    }

    @PostMapping("/schedule")
    public String schedule(
            @Valid @ModelAttribute("scheduleForm") MailScheduleForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            populateModel(model, form.getDocumentSrl(), defaultManualForm(form.getDocumentSrl()), form, defaultThresholdForm(form.getDocumentSrl()), resolveCsrfToken(request));
            return "mail/history";
        }
        try {
            mailingService.schedule(form, principal, request);
            return redirectToHistory(form.getDocumentSrl(), "scheduled");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            bindingResult.reject("scheduleError", ex.getMessage());
            populateModel(model, form.getDocumentSrl(), defaultManualForm(form.getDocumentSrl()), form, defaultThresholdForm(form.getDocumentSrl()), resolveCsrfToken(request));
            return "mail/history";
        }
    }

    @PostMapping("/threshold-trigger")
    public String thresholdTrigger(
            @Valid @ModelAttribute("thresholdForm") MailThresholdTriggerForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            populateModel(model, form.getDocumentSrl(), defaultManualForm(form.getDocumentSrl()), defaultScheduleForm(form.getDocumentSrl()), form, resolveCsrfToken(request));
            return "mail/history";
        }
        try {
            mailingService.triggerThreshold(form, principal, request);
            return redirectToHistory(form.getDocumentSrl(), "thresholdSent");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            bindingResult.reject("thresholdError", ex.getMessage());
            populateModel(model, form.getDocumentSrl(), defaultManualForm(form.getDocumentSrl()), defaultScheduleForm(form.getDocumentSrl()), form, resolveCsrfToken(request));
            return "mail/history";
        }
    }

    @PostMapping("/cancel")
    public String cancel(
            @RequestParam("sendJobId") Long sendJobId,
            @RequestParam(name = "documentSrl", required = false) Long documentSrl,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request,
            Model model
    ) {
        try {
            mailingService.cancelSendJob(sendJobId, principal, request);
            return redirectToHistory(documentSrl, "cancelled");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            model.addAttribute("cancelError", ex.getMessage());
            populateModel(model, documentSrl, defaultManualForm(documentSrl), defaultScheduleForm(documentSrl), defaultThresholdForm(documentSrl), resolveCsrfToken(request));
            return "mail/history";
        }
    }

    private void populateModel(
            Model model,
            Long documentSrl,
            MailSendManualForm manualForm,
            MailScheduleForm scheduleForm,
            MailThresholdTriggerForm thresholdForm,
            CsrfToken csrfToken
    ) {
        Map<Long, MailingPreview> previewCache = new LinkedHashMap<>();
        model.addAttribute("pageTitle", "메일 발송 이력");
        model.addAttribute("pageDescription", "수동 발송, 예약 발송, 임계치 발송 실행과 수신자 스냅샷, 발송 이력을 관리합니다.");
        model.addAttribute("selectedDocumentSrl", documentSrl);
        model.addAttribute("jobOptions", jobService.getJobs());
        model.addAttribute("templateOptions", mailTemplateService.getActiveTemplates());
        model.addAttribute("attachmentTypes", List.of(MailAttachmentType.values()));
        model.addAttribute("historyItems", mailingService.getHistory(documentSrl));
        model.addAttribute("manualForm", manualForm);
        model.addAttribute("scheduleForm", scheduleForm);
        model.addAttribute("thresholdForm", thresholdForm);
        model.addAttribute("manualPreview", resolvePreview(previewCache, manualForm.getDocumentSrl()));
        model.addAttribute("schedulePreview", resolvePreview(previewCache, scheduleForm.getDocumentSrl()));
        model.addAttribute("thresholdPreview", resolvePreview(previewCache, thresholdForm.getDocumentSrl()));
        model.addAttribute("_csrf", csrfToken);
    }

    private MailingPreview resolvePreview(Map<Long, MailingPreview> previewCache, Long documentSrl) {
        if (documentSrl == null) {
            return null;
        }
        return previewCache.computeIfAbsent(documentSrl, mailingService::getPreview);
    }

    private MailSendManualForm defaultManualForm(Long documentSrl) {
        MailSendManualForm form = new MailSendManualForm();
        form.setDocumentSrl(documentSrl);
        return form;
    }

    private MailScheduleForm defaultScheduleForm(Long documentSrl) {
        MailScheduleForm form = new MailScheduleForm();
        form.setDocumentSrl(documentSrl);
        form.setScheduledAt(LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.MINUTES));
        return form;
    }

    private MailThresholdTriggerForm defaultThresholdForm(Long documentSrl) {
        MailThresholdTriggerForm form = new MailThresholdTriggerForm();
        form.setDocumentSrl(documentSrl);
        return form;
    }

    private String redirectToHistory(Long documentSrl, String flag) {
        return documentSrl == null
                ? "redirect:/mail/send/history?" + flag
                : "redirect:/mail/send/history?documentSrl=" + documentSrl + "&" + flag;
    }

    private CsrfToken resolveCsrfToken(HttpServletRequest request) {
        Object token = request.getAttribute(CsrfToken.class.getName());
        if (token instanceof CsrfToken csrfToken) {
            return csrfToken;
        }
        Object fallback = request.getAttribute("_csrf");
        return fallback instanceof CsrfToken csrfToken ? csrfToken : null;
    }
}
