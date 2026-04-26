package com.researchi.admin.form.web;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.form.domain.FormFieldType;
import com.researchi.admin.form.service.FormFieldService;
import com.researchi.admin.job.domain.JobDetail;
import com.researchi.admin.job.service.JobService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/jobs/{documentSrl}/fields")
public class FormFieldController {

    private final JobService jobService;
    private final FormFieldService formFieldService;

    public FormFieldController(JobService jobService, FormFieldService formFieldService) {
        this.jobService = jobService;
        this.formFieldService = formFieldService;
    }

    @GetMapping
    public String fields(
            @PathVariable Long documentSrl,
            @RequestParam(name = "editFieldId", required = false) Long editFieldId,
            Model model,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        request.getSession(true);
        JobDetail jobDetail = jobService.getJob(documentSrl);
        populateModel(model, documentSrl, jobDetail, formFieldService.getForm(documentSrl, editFieldId), csrfToken);
        return "form/fields";
    }

    @PostMapping
    public String saveField(
            @PathVariable Long documentSrl,
            @AuthenticationPrincipal AdminPrincipal principal,
            @Valid @ModelAttribute("fieldForm") FormFieldForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            Model model
    ) {
        JobDetail jobDetail = jobService.getJob(documentSrl);
        if (bindingResult.hasErrors()) {
            populateModel(model, documentSrl, jobDetail, form, resolveCsrfToken(request));
            return "form/fields";
        }

        try {
            Long fieldId = formFieldService.saveField(documentSrl, form, principal, request);
            return "redirect:/jobs/" + documentSrl + "/fields?saved&editFieldId=" + fieldId;
        } catch (IllegalArgumentException ex) {
            bindingResult.reject("fieldError", ex.getMessage());
            populateModel(model, documentSrl, jobDetail, form, resolveCsrfToken(request));
            return "form/fields";
        }
    }

    @PostMapping("/{fieldId}")
    public String updateField(
            @PathVariable Long documentSrl,
            @PathVariable Long fieldId,
            @AuthenticationPrincipal AdminPrincipal principal,
            @Valid @ModelAttribute("fieldForm") FormFieldForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            Model model
    ) {
        form.setId(fieldId);
        return saveField(documentSrl, principal, form, bindingResult, request, model);
    }

    @PostMapping("/{fieldId}/delete")
    public String deleteField(
            @PathVariable Long documentSrl,
            @PathVariable Long fieldId,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        formFieldService.deleteField(documentSrl, fieldId, principal, request);
        return "redirect:/jobs/" + documentSrl + "/fields?deleted";
    }

    private void populateModel(
            Model model,
            Long documentSrl,
            JobDetail jobDetail,
            FormFieldForm form,
            CsrfToken csrfToken
    ) {
        model.addAttribute("pageTitle", "동적 폼 필드");
        model.addAttribute("pageDescription", "선택한 공고의 동적 지원서 필드를 관리합니다.");
        model.addAttribute("documentSrl", documentSrl);
        model.addAttribute("jobDetail", jobDetail);
        model.addAttribute("fieldForm", form);
        model.addAttribute("fieldTypes", FormFieldType.values());
        model.addAttribute("fieldItems", formFieldService.getFields(documentSrl));
        if (csrfToken != null) {
            model.addAttribute("_csrf", csrfToken);
        }
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
