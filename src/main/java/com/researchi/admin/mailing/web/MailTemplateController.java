package com.researchi.admin.mailing.web;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.mailing.domain.AdminMailTemplate;
import com.researchi.admin.mailing.service.MailTemplateService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/mail/templates")
public class MailTemplateController {

    private final MailTemplateService mailTemplateService;

    public MailTemplateController(MailTemplateService mailTemplateService) {
        this.mailTemplateService = mailTemplateService;
    }

    @GetMapping
    public String templates(
            @RequestParam(name = "templateId", required = false) Long templateId,
            Model model,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        request.getSession(true);
        populateModel(model, toForm(mailTemplateService.getTemplate(templateId)), csrfToken);
        return "mail/templates";
    }

    @PostMapping
    public String save(
            @Valid MailTemplateForm form,
            BindingResult bindingResult,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            populateModel(model, form, resolveCsrfToken(request));
            return "mail/templates";
        }

        Long templateId = mailTemplateService.save(form, principal, request);
        return "redirect:/mail/templates?templateId=" + templateId + "&saved";
    }

    private void populateModel(Model model, MailTemplateForm form, CsrfToken csrfToken) {
        model.addAttribute("pageTitle", "메일 템플릿");
        model.addAttribute("pageDescription", "수동 발송, 예약 발송, 임계치 발송에 사용할 메일 템플릿을 관리합니다.");
        model.addAttribute("templateForm", form);
        model.addAttribute("templates", mailTemplateService.getTemplates());
        model.addAttribute("_csrf", csrfToken);
    }

    private MailTemplateForm toForm(AdminMailTemplate template) {
        MailTemplateForm form = new MailTemplateForm();
        if (template == null) {
            return form;
        }
        form.setId(template.getId());
        form.setTemplateName(template.getTemplateName());
        form.setMailSubject(template.getMailSubject());
        form.setMailBody(template.getMailBody());
        form.setActive(template.isActive());
        return form;
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
