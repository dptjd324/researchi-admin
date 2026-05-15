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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(JobController.class);

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
        model.addAttribute("pageTitle", "공고");
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
        populateFormModel(model, "공고 등록", form, null, csrfToken, request);
        return "jobs/form";
    }

    @GetMapping("/{documentSrl}")
    public String jobDetail(@PathVariable Long documentSrl, Model model, HttpServletRequest request, CsrfToken csrfToken) {
        request.getSession(true);
        JobDetail jobDetail = jobService.getJob(documentSrl);
        model.addAttribute("pageTitle", "공고 상세");
        model.addAttribute("pageDescription", "공고 내용, 모집 상태, 거래처 및 운영 메타 정보를 확인합니다.");
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
            populateFormModel(model, "공고 등록", form, null, null, request);
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
        populateFormModel(model, "공고 수정", jobService.toForm(jobDetail), documentSrl, csrfToken, request);
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
            populateFormModel(model, "공고 수정", form, documentSrl, null, request);
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
            runRecruitingNotificationsIfEligible(documentSrl, principal, request);
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
        runAutomaticKeywordNotifications(documentSrl, principal, request);
    }

    private void runRecruitingNotificationsIfEligible(
            Long documentSrl,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        try {
            JobDetail jobDetail = jobService.getJob(documentSrl);
            if (jobDetail.getMeta() != null && "Y".equals(jobDetail.getMeta().getApplicationEnabled())) {
                runAutomaticKeywordNotifications(documentSrl, principal, request);
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to run automatic notifications after recruiting status update. documentSrl={}", documentSrl, ex);
        }
    }

    private void runAutomaticKeywordNotifications(
            Long documentSrl,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        try {
            Long matchJobId = matchingService.run(documentSrl, principal, request);
            notificationService.sendEmailNotifications(documentSrl, matchJobId, principal, request);
            notificationService.sendSmsNotifications(documentSrl, matchJobId, principal, request);
        } catch (RuntimeException ex) {
            log.warn("Failed to run automatic keyword notifications after job save. documentSrl={}", documentSrl, ex);
        }
    }

    private void populateFormModel(Model model, String pageTitle, JobForm form, Long documentSrl, CsrfToken csrfToken, HttpServletRequest request) {
        model.addAttribute("pageTitle", pageTitle);
        model.addAttribute("pageDescription", "공고 내용, 공개 지원 여부, 자동 발송 설정을 관리합니다.");
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
            bindingResult.rejectValue("ageMax", "range", "최대 나이는 최소 나이보다 작을 수 없습니다.");
        }
        if (Boolean.TRUE.equals(form.getAutoSendEnabled()) && (form.getAutoSendMode() == null || form.getAutoSendMode().isBlank())) {
            bindingResult.rejectValue("autoSendMode", "required", "자동 발송을 사용하려면 발송 방식을 선택해주세요.");
        }
        if (Boolean.TRUE.equals(form.getAutoSendEnabled())
                && "THRESHOLD".equals(form.getAutoSendMode())
                && (form.getAutoSendThreshold() == null || form.getAutoSendThreshold() < 1)) {
            bindingResult.rejectValue("autoSendThreshold", "required", "임계치 자동 발송에는 유효한 임계치가 필수입니다.");
        }
        if (Boolean.TRUE.equals(form.getAutoSendEnabled())
                && "THRESHOLD".equals(form.getAutoSendMode())
                && form.getAutoSendTemplateId() == null) {
            bindingResult.rejectValue("autoSendTemplateId", "required", "임계치 자동 발송에는 메일 템플릿이 필수입니다.");
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
