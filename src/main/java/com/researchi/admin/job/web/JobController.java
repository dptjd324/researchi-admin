package com.researchi.admin.job.web;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.client.service.ClientService;
import com.researchi.admin.common.web.PaginationSupport;
import com.researchi.admin.job.domain.BoardConfig;
import com.researchi.admin.job.domain.JobDetail;
import com.researchi.admin.job.domain.JobListItem;
import com.researchi.admin.job.domain.JobType;
import com.researchi.admin.job.support.ApplicationFormNoticeParser;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.mailing.service.MailTemplateService;
import com.researchi.admin.notification.config.NotificationProperties;
import com.researchi.admin.matching.service.MatchingService;
import com.researchi.admin.notification.service.NotificationService;
import com.researchi.admin.publicform.domain.PublicFormAvailability;
import com.researchi.admin.publicform.service.PublicFormService;
import jakarta.mail.internet.InternetAddress;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/jobs")
public class JobController {

    private final JobService jobService;
    private final MailTemplateService mailTemplateService;
    private final ClientService clientService;
    private final PublicFormService publicFormService;
    private final NotificationProperties notificationProperties;
    private final MatchingService matchingService;
    private final NotificationService notificationService;

    public JobController(
            JobService jobService,
            MailTemplateService mailTemplateService,
            ClientService clientService,
            PublicFormService publicFormService,
            NotificationProperties notificationProperties,
            MatchingService matchingService,
            NotificationService notificationService
    ) {
        this.jobService = jobService;
        this.mailTemplateService = mailTemplateService;
        this.clientService = clientService;
        this.publicFormService = publicFormService;
        this.notificationProperties = notificationProperties;
        this.matchingService = matchingService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public String jobs(
            Model model,
            @RequestParam(name = "jobType", required = false) String jobType,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", required = false) Integer page,
            @RequestParam(name = "cursor", required = false) Long cursor,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        request.getSession(true);
        model.addAttribute("pageTitle", "\uACF5\uACE0");
        model.addAttribute("pageDescription", "Researchi XE managed boards.");
        String normalizedJobType = isSupportedJobType(jobType) ? jobType : null;
        int totalCount = jobService.countJobs(normalizedJobType, keyword);
        PaginationSupport.PageWindow pageWindow = PaginationSupport.applyMetadata(
                model,
                request,
                totalCount,
                page,
                PaginationSupport.DEFAULT_PAGE_SIZE
        );
        List<JobListItem> jobs = cursor != null && pageWindow.currentPage() > 1
                ? jobService.getJobPageAfter(normalizedJobType, keyword, cursor, pageWindow.pageSize())
                : jobService.getJobPage(normalizedJobType, keyword, pageWindow.pageSize(), pageWindow.offset());
        model.addAttribute("jobs", jobs);
        applyCursorNextPageUrl(model, request, jobs, pageWindow);
        model.addAttribute("selectedJobType", normalizedJobType);
        model.addAttribute("keyword", keyword);
        model.addAttribute("jobTypes", BoardConfig.values());
        model.addAttribute("_csrf", csrfToken);
        return "jobs/list";
    }

    private void applyCursorNextPageUrl(
            Model model,
            HttpServletRequest request,
            List<JobListItem> jobs,
            PaginationSupport.PageWindow pageWindow
    ) {
        if (jobs.isEmpty() || pageWindow.currentPage() >= pageWindow.totalPages()) {
            return;
        }
        Long lastDocumentSrl = jobs.get(jobs.size() - 1).getDocumentSrl();
        if (lastDocumentSrl == null) {
            return;
        }
        model.addAttribute("nextPageUrl", buildPageUrl(request, pageWindow.currentPage() + 1, lastDocumentSrl));
    }

    private String buildPageUrl(HttpServletRequest request, int page, Long cursor) {
        Map<String, String[]> parameterMap = new LinkedHashMap<>(request.getParameterMap());
        parameterMap.put("page", new String[]{String.valueOf(page)});
        parameterMap.put("cursor", new String[]{String.valueOf(cursor)});

        StringBuilder builder = new StringBuilder(request.getRequestURI());
        boolean first = true;
        for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
            if (entry.getValue() == null || entry.getValue().length == 0) {
                continue;
            }
            for (String value : entry.getValue()) {
                if (value == null || value.isBlank()) {
                    continue;
                }
                builder.append(first ? '?' : '&');
                first = false;
                builder.append(encode(entry.getKey())).append('=').append(encode(value));
            }
        }
        return builder.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    @GetMapping("/new")
    public String newJob(Model model, HttpServletRequest request, CsrfToken csrfToken) {
        request.getSession(true);
        JobForm form = new JobForm();
        form.setJobType(JobType.NEW.name());
        form.setRecruitStatus("RECRUITING");
        form.setApplicationEnabled(Boolean.TRUE);
        form.setAutoSendEnabled(Boolean.FALSE);
        form.setAutoSendRepeatYn("N");
        form.setAutoSendAttachmentType("XLSX");
        populateFormModel(model, "\uACF5\uACE0 \uB4F1\uB85D", form, null, csrfToken, request);
        return "jobs/form";
    }

    @GetMapping("/{documentSrl}")
    public String jobDetail(@PathVariable Long documentSrl, Model model, HttpServletRequest request, CsrfToken csrfToken) {
        request.getSession(true);
        JobDetail jobDetail = jobService.getJob(documentSrl);
        model.addAttribute("pageTitle", "\uACF5\uACE0 \uC0C1\uC138");
        model.addAttribute("pageDescription", "\uACF5\uACE0 \uB0B4\uC6A9, \uBAA8\uC9D1 \uC0C1\uD0DC, \uAC70\uB798\uCC98 \uBC0F \uC6B4\uC601 \uBA54\uD0C0 \uC815\uBCF4\uB97C \uD655\uC778\uD569\uB2C8\uB2E4.");
        model.addAttribute("jobDetail", jobDetail);
        model.addAttribute("applicationBoard", jobDetail.isApplicationBoard());
        model.addAttribute(
                "applicationFormNoticeItems",
                jobDetail.getMeta() == null ? List.of() : ApplicationFormNoticeParser.parseItems(jobDetail.getMeta().getApplicationFormNotice())
        );
        model.addAttribute("_csrf", csrfToken);
        populatePublicApplyModel(model, documentSrl, request);
        return "jobs/detail";
    }

    @PostMapping
    public String createJob(
            @AuthenticationPrincipal AdminPrincipal principal,
            @Valid @ModelAttribute("jobForm") JobForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            Model model
    ) {
        validateBusinessRules(form, bindingResult);
        if (bindingResult.hasErrors()) {
            populateFormModel(model, "\uACF5\uACE0 \uB4F1\uB85D", form, null, null, request);
            return "jobs/form";
        }

        Long documentSrl = jobService.createJob(form, principal, request);
        runAutomaticKeywordNotifications(documentSrl, form, principal, request);
        return "redirect:/jobs/" + documentSrl + "/edit?created";
    }

    @GetMapping("/{documentSrl}/edit")
    public String editJob(@PathVariable Long documentSrl, Model model, HttpServletRequest request, CsrfToken csrfToken) {
        request.getSession(true);
        JobDetail jobDetail = jobService.getJob(documentSrl);
        populateFormModel(model, "\uACF5\uACE0 \uC218\uC815", jobService.toForm(jobDetail), documentSrl, csrfToken, request);
        return "jobs/form";
    }

    @PostMapping("/{documentSrl}")
    public String updateJob(
            @PathVariable Long documentSrl,
            @AuthenticationPrincipal AdminPrincipal principal,
            @Valid @ModelAttribute("jobForm") JobForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            Model model
    ) {
        validateBusinessRules(form, bindingResult);
        if (bindingResult.hasErrors()) {
            populateFormModel(model, "\uACF5\uACE0 \uC218\uC815", form, documentSrl, null, request);
            return "jobs/form";
        }

        jobService.updateJob(documentSrl, form, principal, request);
        runAutomaticKeywordNotifications(documentSrl, form, principal, request);
        return "redirect:/jobs/" + documentSrl + "/edit?updated";
    }

    @PostMapping("/{documentSrl}/delete")
    public String deleteJob(
            @PathVariable Long documentSrl,
            @AuthenticationPrincipal AdminPrincipal principal,
            @RequestParam(name = "deleteReason", required = false) String deleteReason,
            HttpServletRequest request
    ) {
        if (deleteReason == null || deleteReason.isBlank()) {
            return "redirect:/jobs/" + documentSrl + "/edit?deleteReasonRequired";
        }
        jobService.deleteContentJob(documentSrl, principal, request, deleteReason);
        return "redirect:/jobs?deleteScheduled";
    }

    @PostMapping("/{documentSrl}/status")
    public String updateStatus(
            @PathVariable Long documentSrl,
            @RequestParam("recruitStatus") String recruitStatus,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        jobService.updateRecruitStatus(documentSrl, recruitStatus, principal, request);
        if ("RECRUITING".equals(recruitStatus)) {
            JobDetail jobDetail = jobService.getJob(documentSrl);
            if (jobDetail.getMeta() != null && "Y".equals(jobDetail.getMeta().getApplicationEnabled())) {
                Long matchJobId = matchingService.run(documentSrl, principal, request);
                notificationService.sendEmailNotifications(documentSrl, matchJobId, principal, request);
                notificationService.sendSmsNotifications(documentSrl, matchJobId, principal, request);
            }
        }
        return "redirect:/jobs?statusUpdated";
    }

    private void runAutomaticKeywordNotifications(
            Long documentSrl,
            JobForm form,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        if (!BoardConfig.fromCode(form.getJobType()).isApplicationEnabled()
                || !Boolean.TRUE.equals(form.getApplicationEnabled())
                || !"RECRUITING".equals(form.getRecruitStatus())) {
            return;
        }
        Long matchJobId = matchingService.run(documentSrl, principal, request);
        notificationService.sendEmailNotifications(documentSrl, matchJobId, principal, request);
        notificationService.sendSmsNotifications(documentSrl, matchJobId, principal, request);
    }

    private void populateFormModel(Model model, String pageTitle, JobForm form, Long documentSrl, CsrfToken csrfToken, HttpServletRequest request) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("pageDescription", "\uACF5\uACE0 \uB0B4\uC6A9, \uACF5\uAC1C \uC9C0\uC6D0 \uC5EC\uBD80, \uC790\uB3D9 \uBC1C\uC1A1 \uC124\uC815\uC744 \uAD00\uB9AC\uD569\uB2C8\uB2E4.");
        model.addAttribute("jobForm", form);
        model.addAttribute("jobTypes", documentSrl == null ? BoardConfig.applicationBoards() : Arrays.asList(BoardConfig.values()));
        model.addAttribute("applicationBoard", isApplicationBoard(form.getJobType()));
        model.addAttribute("mailTemplates", mailTemplateService.getActiveTemplates());
        model.addAttribute("clientOptions", clientService.getAllClientSummaries());
        model.addAttribute("documentSrl", documentSrl);
        model.addAttribute("recruitStatuses", List.of("RECRUITING", "WAITING", "CLOSED"));
        model.addAttribute("applicationFormNoticeItems", ApplicationFormNoticeParser.parseItems(form.getApplicationFormNotice()));
        model.addAttribute("applicationFormNoticeDetails", ApplicationFormNoticeParser.parseDetails(form.getApplicationFormNotice()));
        if (documentSrl != null) {
            populatePublicApplyModel(model, documentSrl, request);
        }
        if (csrfToken != null) {
            model.addAttribute("_csrf", csrfToken);
        }
    }

    private void populatePublicApplyModel(Model model, Long documentSrl, HttpServletRequest request) {
        PublicFormAvailability availability = publicFormService.getAvailability(documentSrl);
        model.addAttribute("publicApplyUrl", buildPublicApplyUrl(documentSrl, request));
        model.addAttribute("publicApplyAvailable", availability.available());
        model.addAttribute("publicApplyMessage", availability.message());
    }

    private String buildPublicApplyUrl(Long documentSrl, HttpServletRequest request) {
        String baseUrl = request != null
                ? ServletUriComponentsBuilder.fromRequestUri(request).replacePath(null).build().toUriString()
                : notificationProperties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8082";
        }
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return normalizedBaseUrl + "/apply/" + documentSrl;
    }

    private void validateBusinessRules(JobForm form, BindingResult bindingResult) {
        validateJobType(form, bindingResult);
        if (form.getAgeMin() != null && form.getAgeMax() != null && form.getAgeMin() > form.getAgeMax()) {
            bindingResult.rejectValue("ageMax", "range", "\uCD5C\uB300 \uB098\uC774\uB294 \uCD5C\uC18C \uB098\uC774\uBCF4\uB2E4 \uC791\uC744 \uC218 \uC5C6\uC2B5\uB2C8\uB2E4.");
        }
        if (Boolean.TRUE.equals(form.getAutoSendEnabled()) && (form.getAutoSendMode() == null || form.getAutoSendMode().isBlank())) {
            bindingResult.rejectValue("autoSendMode", "required", "\uC790\uB3D9 \uBC1C\uC1A1\uC744 \uC0AC\uC6A9\uD558\uB824\uBA74 \uBC1C\uC1A1 \uBC29\uC2DD\uC744 \uC120\uD0DD\uD574\uC8FC\uC138\uC694.");
        }
        if (Boolean.TRUE.equals(form.getAutoSendEnabled())
                && "THRESHOLD".equals(form.getAutoSendMode())
                && (form.getAutoSendThreshold() == null || form.getAutoSendThreshold() < 1)) {
            bindingResult.rejectValue("autoSendThreshold", "required", "\uC784\uACC4\uCE58 \uC790\uB3D9 \uBC1C\uC1A1\uC5D0\uB294 \uC720\uD6A8\uD55C \uC784\uACC4\uCE58\uAC00 \uD544\uC218\uC785\uB2C8\uB2E4.");
        }
        if (Boolean.TRUE.equals(form.getAutoSendEnabled())
                && "THRESHOLD".equals(form.getAutoSendMode())
                && form.getAutoSendTemplateId() == null) {
            bindingResult.rejectValue("autoSendTemplateId", "required", "\uC784\uACC4\uCE58 \uC790\uB3D9 \uBC1C\uC1A1\uC5D0\uB294 \uBA54\uC77C \uD15C\uD50C\uB9BF\uC774 \uD544\uC218\uC785\uB2C8\uB2E4.");
        }
        if (form.getClientId() == null) {
            validateDirectClientEmails(form, bindingResult);
        }
    }

    private void validateJobType(JobForm form, BindingResult bindingResult) {
        if (form.getJobType() == null || form.getJobType().isBlank()) {
            return;
        }
        BoardConfig boardConfig;
        try {
            boardConfig = BoardConfig.fromCode(form.getJobType());
        } catch (IllegalArgumentException ex) {
            bindingResult.rejectValue("jobType", "unsupported", "지원하지 않는 공고 유형입니다.");
            return;
        }
        try {
            if (!jobService.hasBoardModule(form.getJobType())) {
                bindingResult.rejectValue(
                        "jobType",
                        "missingBoard",
                        "선택한 공고 유형의 XE 게시판(mid=" + boardConfig.getMid() + ")을 현재 연결된 XE DB에서 찾을 수 없습니다. XE DB 연결과 xe_modules 게시판 설정을 확인해 주세요."
                );
            }
        } catch (RuntimeException ex) {
            bindingResult.rejectValue(
                    "jobType",
                    "boardCheckFailed",
                    "XE DB 연결 또는 게시판 조회에 실패했습니다. XE_DB_URL과 xe_modules 접근 상태를 확인해 주세요."
            );
        }
    }

    private void validateDirectClientEmails(JobForm form, BindingResult bindingResult) {
        String clientEmail = trimToNull(form.getClientEmail());
        if (clientEmail != null && !isValidEmail(clientEmail)) {
            bindingResult.rejectValue("clientEmail", "email", "올바른 대표 이메일 주소를 입력해 주세요.");
        }
        if (form.getClientEmails() == null || form.getClientEmails().isBlank()) {
            return;
        }
        List<String> invalidEmails = Arrays.stream(form.getClientEmails().split("[,;\\s]+"))
                .map(this::trimToNull)
                .filter(value -> value != null && !isValidEmail(value))
                .distinct()
                .toList();
        if (!invalidEmails.isEmpty()) {
            bindingResult.rejectValue(
                    "clientEmails",
                    "email",
                    "추가 이메일에 올바르지 않은 주소가 있습니다: " + String.join(", ", invalidEmails)
            );
        }
    }

    private boolean isValidEmail(String value) {
        try {
            InternetAddress address = new InternetAddress(value, true);
            address.validate();
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isSupportedJobType(String jobType) {
        if (jobType == null || jobType.isBlank()) {
            return true;
        }
        return Arrays.stream(BoardConfig.values()).anyMatch(value -> value.name().equals(jobType));
    }

    private boolean isApplicationBoard(String jobType) {
        try {
            return BoardConfig.fromCode(jobType).isApplicationEnabled();
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
