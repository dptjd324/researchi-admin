package com.researchi.admin.legacy.application.web;

import com.researchi.admin.legacy.application.support.ApplicationFormNoticeItem;
import com.researchi.admin.legacy.application.service.LegacyPublicApplicationService;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.publicform.domain.PublicFormUnavailableException;
import com.researchi.admin.publicform.domain.PublicFormValidationException;
import com.researchi.admin.publicform.web.PublicApplicationForm;
import com.researchi.admin.web.support.TextLinkRenderer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Controller
public class LegacyPublicApplicationController {

    private static final Logger log = LoggerFactory.getLogger(LegacyPublicApplicationController.class);
    private static final Pattern LINK_LINE_PATTERN = Pattern.compile("^\\s*.+?\\s+<https?://[^\\s<>]+>\\s*$", Pattern.CASE_INSENSITIVE);
    private static final Pattern MARKDOWN_LINK_PATTERN = Pattern.compile("\\[[^\\]]*]\\(https?://[^\\s)]+\\)", Pattern.CASE_INSENSITIVE);
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);
    private static final Pattern FORMAT_ALIGN_PATTERN = Pattern.compile("\\[align=(?:left|center|right)](.*?)\\[/align]", Pattern.CASE_INSENSITIVE);
    private static final Pattern FORMAT_SIZE_PATTERN = Pattern.compile("\\[size=(?:4|8|12|14|16|18|20|24|28|32)\\](.*?)\\[/size\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern FORMAT_COLOR_PATTERN = Pattern.compile("\\[color=(?:black|blue|red|green|yellow)\\](.*?)\\[/color\\]", Pattern.CASE_INSENSITIVE);
    private static final Pattern FORMAT_BOLD_PATTERN = Pattern.compile("\\[bold](.*?)\\[/bold\\]", Pattern.CASE_INSENSITIVE);

    private final LegacyPublicApplicationService legacyPublicApplicationService;
    private final TextLinkRenderer textLinkRenderer;

    public LegacyPublicApplicationController(
            LegacyPublicApplicationService legacyPublicApplicationService,
            TextLinkRenderer textLinkRenderer
    ) {
        this.legacyPublicApplicationService = legacyPublicApplicationService;
        this.textLinkRenderer = textLinkRenderer;
    }

    @GetMapping("/research/{researchNo}/apply")
    public String applyForm(
            @PathVariable Long researchNo,
            Model model,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        try {
            ResearchMaster research = legacyPublicApplicationService.getOpenResearch(researchNo);
            PublicApplicationForm form = new PublicApplicationForm();
            populateModel(model, research, form, csrfToken, request);
            return "publicform/legacy-apply";
        } catch (PublicFormUnavailableException ex) {
            return populateResultModel(model, researchNo, "신청 불가", ex.getMessage());
        }
    }

    @PostMapping("/research/{researchNo}/apply")
    public String submit(
            @PathVariable Long researchNo,
            @Valid @ModelAttribute("applicationForm") PublicApplicationForm form,
            BindingResult bindingResult,
            Model model,
            HttpServletRequest request
    ) {
        try {
            ResearchMaster research = legacyPublicApplicationService.getOpenResearch(researchNo);
            if (bindingResult.hasErrors()) {
                populateModel(model, research, form, resolveCsrfToken(request), request);
                return "publicform/legacy-apply";
            }
            legacyPublicApplicationService.submit(researchNo, form, request);
            return "redirect:/research/" + researchNo + "/apply/complete";
        } catch (PublicFormValidationException ex) {
            ex.getFieldErrors().forEach((field, message) -> bindingResult.rejectValue(field, "invalid", message));
            if (ex.getGlobalError() != null) {
                bindingResult.reject("invalid", ex.getGlobalError());
            }
            ResearchMaster research = legacyPublicApplicationService.getOpenResearch(researchNo);
            populateModel(model, research, form, resolveCsrfToken(request), request);
            return "publicform/legacy-apply";
        } catch (PublicFormUnavailableException ex) {
            return populateResultModel(model, researchNo, "신청 불가", ex.getMessage());
        } catch (RuntimeException ex) {
            log.error(
                    "Unexpected legacy public application failure. researchNo={}, applicantName={}, remoteAddr={}",
                    researchNo,
                    maskName(form.getApplicantName()),
                    request.getRemoteAddr(),
                    ex
            );
            return populateResultModel(
                    model,
                    researchNo,
                    "신청 오류",
                    "신청서 제출 중 문제가 발생했습니다. 입력 내용을 확인한 뒤 다시 시도해 주세요."
            );
        }
    }

    @GetMapping("/research/{researchNo}/apply/complete")
    public String complete(@PathVariable Long researchNo, Model model) {
        return populateResultModel(model, researchNo, "신청 완료", "신청서가 정상적으로 제출되었습니다.");
    }

    private void populateModel(
            Model model,
            ResearchMaster research,
            PublicApplicationForm form,
            CsrfToken csrfToken,
            HttpServletRequest request
    ) {
        List<ApplicationFormNoticeItem> additionalItems = legacyPublicApplicationService.additionalItems(research.getResearchNo());
        ensureExtraAnswersSize(form, additionalItems);
        model.addAttribute("pageTitle", "신청서");
        model.addAttribute("research", research);
        model.addAttribute("publicResearchContents", publicResearchContents(research.getResearchContents()));
        model.addAttribute("applicationForm", form);
        model.addAttribute("applicationFormNoticeDetails", additionalItems);
        model.addAttribute("applicationFormNoticeGroups", additionalGroups(additionalItems));
        model.addAttribute("captchaEnabled", legacyPublicApplicationService.isCaptchaEnabled());
        model.addAttribute("captchaQuestion", legacyPublicApplicationService.ensureCaptchaQuestion(request.getSession(true)));
        if (csrfToken != null) {
            model.addAttribute("_csrf", csrfToken);
        }
    }

    private void ensureExtraAnswersSize(PublicApplicationForm form, List<ApplicationFormNoticeItem> additionalItems) {
        if (additionalItems.isEmpty()) {
            return;
        }
        List<String> answers = form.getExtraAnswers();
        while (answers.size() < additionalItems.size()) {
            answers.add("");
        }
        if (answers.size() > additionalItems.size()) {
            form.setExtraAnswers(new ArrayList<>(answers.subList(0, additionalItems.size())));
        }
    }

    private List<String> additionalGroups(List<ApplicationFormNoticeItem> additionalItems) {
        return additionalItems.stream()
                .map(ApplicationFormNoticeItem::groupLabel)
                .filter(group -> group != null && !group.isBlank())
                .distinct()
                .toList();
    }

    private String publicResearchContents(String contents) {
        if (contents == null || contents.isBlank()) {
            return "";
        }
        String[] lines = contents.replace("\r\n", "\n").replace("\r", "\n").split("\n", -1);
        List<String> visibleLines = new ArrayList<>();
        for (String line : lines) {
            String publicLine = stripPublicFormatting(line);
            if (!shouldHidePublicResearchLine(publicLine)) {
                visibleLines.add(line);
            }
        }
        return textLinkRenderer.render(String.join("\n", trimTrailingBlankLines(visibleLines)));
    }

    private String stripPublicFormatting(String value) {
        String stripped = value == null ? "" : value;
        stripped = FORMAT_ALIGN_PATTERN.matcher(stripped).replaceAll("$1");
        stripped = FORMAT_SIZE_PATTERN.matcher(stripped).replaceAll("$1");
        stripped = FORMAT_COLOR_PATTERN.matcher(stripped).replaceAll("$1");
        stripped = FORMAT_BOLD_PATTERN.matcher(stripped).replaceAll("$1");
        return stripped
                .replaceAll("\\[align=(?:left|center|right)]", "")
                .replace("[/align]", "")
                .replaceAll("\\[size=(?:4|8|12|14|16|18|20|24|28|32)\\]", "")
                .replace("[/size]", "")
                .replaceAll("\\[color=(?:black|blue|red|green|yellow)\\]", "")
                .replace("[/color]", "")
                .replaceAll("(?i)\\[bold]", "")
                .replace("[/bold]", "");
    }

    private boolean shouldHidePublicResearchLine(String line) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        if ("신청하기".equals(trimmed) || "신청 버튼".equals(trimmed) || "신청버튼".equals(trimmed)) {
            return true;
        }
        return LINK_LINE_PATTERN.matcher(trimmed).matches()
                || MARKDOWN_LINK_PATTERN.matcher(trimmed).find()
                || (trimmed.contains("신청") && URL_PATTERN.matcher(trimmed).find());
    }

    private List<String> trimTrailingBlankLines(List<String> lines) {
        int end = lines.size();
        while (end > 0 && lines.get(end - 1).trim().isEmpty()) {
            end--;
        }
        return lines.subList(0, end);
    }

    private String populateResultModel(Model model, Long researchNo, String title, String message) {
        model.addAttribute("pageTitle", title);
        model.addAttribute("resultTitle", title);
        model.addAttribute("resultMessage", message);
        model.addAttribute("useHistoryBack", false);
        model.addAttribute("showApplyReturn", false);
        return "publicform/result";
    }

    private String maskName(String value) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        if (trimmed.length() == 1) {
            return "*";
        }
        return trimmed.charAt(0) + "*".repeat(trimmed.length() - 1);
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
