package com.researchi.admin.publicform.web;

import com.researchi.admin.job.support.ApplicationFormNoticeParser;
import com.researchi.admin.publicform.domain.PublicFormPage;
import com.researchi.admin.publicform.domain.PublicFormSubmissionResult;
import com.researchi.admin.publicform.domain.PublicFormSubmissionStatus;
import com.researchi.admin.publicform.domain.PublicFormUnavailableException;
import com.researchi.admin.publicform.domain.PublicFormValidationException;
import com.researchi.admin.publicform.service.PublicFormService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PublicFormController {

    private static final DateTimeFormatter XE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final DateTimeFormatter DISPLAY_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 HH시 mm분");

    private final PublicFormService publicFormService;

    public PublicFormController(PublicFormService publicFormService) {
        this.publicFormService = publicFormService;
    }

    @GetMapping("/apply/{documentSrl}")
    public String applyForm(
            @PathVariable Long documentSrl,
            Model model,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        try {
            PublicFormPage page = publicFormService.getPage(documentSrl, request.getSession(true));
            populateApplyModel(model, documentSrl, page, new PublicApplicationForm(), Map.of(), Map.of(), csrfToken);
            return "publicform/apply";
        } catch (PublicFormUnavailableException ex) {
            return populateResultModel(model, documentSrl, "지원 불가", ex.getMessage(), "publicform/result");
        }
    }

    @PostMapping("/apply/{documentSrl}")
    public String submitApplication(
            @PathVariable Long documentSrl,
            @Valid @ModelAttribute("applicationForm") PublicApplicationForm form,
            BindingResult bindingResult,
            Model model,
            HttpServletRequest request
    ) {
        try {
            PublicFormPage page = publicFormService.getPage(documentSrl, request.getSession(true));
            Map<Long, List<String>> dynamicValues = publicFormService.extractDynamicValues(documentSrl, request);
            if (bindingResult.hasErrors()) {
                populateApplyModel(model, documentSrl, page, form, dynamicValues, Map.of(), resolveCsrfToken(request));
                return "publicform/apply";
            }

            PublicFormSubmissionResult result = publicFormService.submit(documentSrl, form, dynamicValues, request);
            if (result.status() == PublicFormSubmissionStatus.COMPLETE) {
                return "redirect:/apply/" + documentSrl + "/complete";
            }
            if (result.status() == PublicFormSubmissionStatus.DUPLICATE) {
                return "redirect:/apply/" + documentSrl + "/duplicate";
            }
            return "redirect:/apply/" + documentSrl + "/blocked";
        } catch (PublicFormValidationException ex) {
            ex.getFieldErrors().forEach((field, message) -> bindingResult.rejectValue(field, "invalid", message));
            if (ex.getGlobalError() != null) {
                bindingResult.reject("invalid", ex.getGlobalError());
            }
            PublicFormPage page = publicFormService.getPage(documentSrl, request.getSession(true));
            populateApplyModel(
                    model,
                    documentSrl,
                    page,
                    form,
                    publicFormService.extractDynamicValues(documentSrl, request),
                    ex.getDynamicFieldErrors(),
                    resolveCsrfToken(request)
            );
            return "publicform/apply";
        } catch (PublicFormUnavailableException ex) {
            return populateResultModel(model, documentSrl, "지원 불가", ex.getMessage(), "publicform/result");
        }
    }

    @GetMapping("/apply/{documentSrl}/complete")
    public String complete(@PathVariable Long documentSrl, Model model) {
        return populateResultModel(
                model,
                documentSrl,
                "지원 완료",
                "지원서가 정상적으로 제출되었습니다.",
                true,
                "publicform/result"
        );
    }

    @GetMapping("/apply/{documentSrl}/duplicate")
    public String duplicate(@PathVariable Long documentSrl, Model model) {
        return populateResultModel(
                model,
                documentSrl,
                "중복 지원 안내",
                "동일한 휴대전화 번호로 이미 접수된 지원서가 있습니다.",
                "publicform/result"
        );
    }

    @GetMapping("/apply/{documentSrl}/blocked")
    public String blocked(@PathVariable Long documentSrl, Model model) {
        return populateResultModel(
                model,
                documentSrl,
                "지원 제한",
                "현재 지원서를 접수할 수 없습니다.",
                "publicform/result"
        );
    }

    private void populateApplyModel(
            Model model,
            Long documentSrl,
            PublicFormPage page,
            PublicApplicationForm form,
            Map<Long, List<String>> dynamicValues,
            Map<Long, String> dynamicErrors,
            CsrfToken csrfToken
    ) {
        String applicationFormNotice = page.jobDetail().getMeta() == null ? null : page.jobDetail().getMeta().getApplicationFormNotice();
        List<String> applicationFormNoticeItems = ApplicationFormNoticeParser.parseItems(applicationFormNotice);
        ensureExtraAnswersSize(form, applicationFormNoticeItems);

        model.addAttribute("pageTitle", "리서치아일랜드 지원서");
        model.addAttribute("documentSrl", documentSrl);
        model.addAttribute("jobDetail", page.jobDetail());
        model.addAttribute("applicationForm", form);
        model.addAttribute("dynamicFields", page.fields());
        model.addAttribute("dynamicValues", new LinkedHashMap<>(dynamicValues));
        model.addAttribute("dynamicErrors", new LinkedHashMap<>(dynamicErrors));
        model.addAttribute("publicRewardText", page.jobDetail().getMeta() != null ? page.jobDetail().getMeta().getRewardText() : null);
        model.addAttribute("publicStartText", buildStartText(page));
        model.addAttribute("publicDeadlineText", buildDeadlineText(page));
        model.addAttribute("publicScheduleText", buildScheduleText(page));
        model.addAttribute("applicationFormNoticeItems", applicationFormNoticeItems);
        model.addAttribute("applicationFormNoticeDetails", ApplicationFormNoticeParser.parseDetails(applicationFormNotice));
        model.addAttribute("captchaEnabled", page.captchaEnabled());
        model.addAttribute("captchaQuestion", page.captchaQuestion());
        if (csrfToken != null) {
            model.addAttribute("_csrf", csrfToken);
        }
    }

    private void ensureExtraAnswersSize(PublicApplicationForm form, List<String> applicationFormNoticeItems) {
        if (applicationFormNoticeItems.isEmpty()) {
            return;
        }
        List<String> answers = form.getExtraAnswers();
        while (answers.size() < applicationFormNoticeItems.size()) {
            answers.add("");
        }
        if (answers.size() > applicationFormNoticeItems.size()) {
            form.setExtraAnswers(new ArrayList<>(answers.subList(0, applicationFormNoticeItems.size())));
        }
    }

    private String populateResultModel(Model model, Long documentSrl, String title, String message, String viewName) {
        return populateResultModel(model, documentSrl, title, message, false, viewName);
    }

    private String populateResultModel(
            Model model,
            Long documentSrl,
            String title,
            String message,
            boolean useHistoryBack,
            String viewName
    ) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("documentSrl", documentSrl);
        model.addAttribute("resultTitle", title);
        model.addAttribute("resultMessage", message);
        model.addAttribute("useHistoryBack", useHistoryBack);
        return viewName;
    }

    private CsrfToken resolveCsrfToken(HttpServletRequest request) {
        Object token = request.getAttribute(CsrfToken.class.getName());
        if (token instanceof CsrfToken csrfToken) {
            return csrfToken;
        }
        Object fallback = request.getAttribute("_csrf");
        return fallback instanceof CsrfToken csrfToken ? csrfToken : null;
    }

    private String buildScheduleText(PublicFormPage page) {
        String start = buildStartText(page);
        String deadline = buildDeadlineText(page);
        if (start == null && deadline == null) {
            return null;
        }
        if (start == null) {
            return "마감 " + deadline;
        }
        if (deadline == null) {
            return "시작 " + start + " · 마감일 미설정";
        }
        return "시작 " + start + " · 마감 " + deadline;
    }

    private String buildStartText(PublicFormPage page) {
        if (page == null || page.jobDetail() == null) {
            return null;
        }
        return formatXeDateTime(page.jobDetail().getDocument().getRegdate());
    }

    private String buildDeadlineText(PublicFormPage page) {
        if (page == null || page.jobDetail() == null || page.jobDetail().getMeta() == null) {
            return null;
        }
        if (page.jobDetail().getMeta() == null || page.jobDetail().getMeta().getCloseDate() == null) {
            return null;
        }
        LocalDate closeDate = page.jobDetail().getMeta().getCloseDate();
        LocalDateTime closeDateTime = LocalDateTime.of(closeDate, LocalTime.of(23, 59));
        return closeDateTime.format(DISPLAY_TIMESTAMP);
    }

    private String formatXeDateTime(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(rawValue, XE_TIMESTAMP).format(DISPLAY_TIMESTAMP);
        } catch (DateTimeParseException ex) {
            return rawValue;
        }
    }
}
