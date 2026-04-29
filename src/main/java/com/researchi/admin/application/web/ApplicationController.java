package com.researchi.admin.application.web;

import com.researchi.admin.application.domain.ApplicationDetail;
import com.researchi.admin.application.service.ApplicationService;
import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.blacklist.service.BlacklistService;
import com.researchi.admin.common.web.PaginationSupport;
import com.researchi.admin.job.domain.JobDetail;
import com.researchi.admin.job.service.JobService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping
public class ApplicationController {

    private final ApplicationService applicationService;
    private final BlacklistService blacklistService;
    private final JobService jobService;

    public ApplicationController(
            ApplicationService applicationService,
            BlacklistService blacklistService,
            JobService jobService
    ) {
        this.applicationService = applicationService;
        this.blacklistService = blacklistService;
        this.jobService = jobService;
    }

    @GetMapping("/applications")
    public String applications(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", required = false) Integer page,
            Model model,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        request.getSession(true);
        populateListModel(model, null, null, keyword, page, request, csrfToken);
        return "applications/list";
    }

    @GetMapping("/jobs/{documentSrl}/applications")
    public String applicationsByJob(
            @PathVariable Long documentSrl,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "page", required = false) Integer page,
            Model model,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        request.getSession(true);
        jobService.requireApplicationBoard(documentSrl);
        populateListModel(model, documentSrl, jobService.getJob(documentSrl), keyword, page, request, csrfToken);
        return "applications/list";
    }

    @GetMapping("/applications/{id}")
    public String applicationDetail(
            @PathVariable Long id,
            Model model,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        request.getSession(true);
        ApplicationDetail detail = applicationService.getApplicationDetail(id);
        model.addAttribute("pageTitle", "지원서 상세");
        model.addAttribute("pageDescription", "단일 지원서와 동적 응답을 상세하게 확인합니다.");
        model.addAttribute("detail", detail);
        model.addAttribute("statusOptions", applicationService.getAllowedStatuses());
        model.addAttribute("returnTo", currentPathWithQuery(request));
        model.addAttribute("_csrf", csrfToken);
        return "applications/detail";
    }

    @PostMapping("/applications/{id}/status")
    public String updateStatus(
            @PathVariable Long id,
            @RequestParam("applicationStatus") String applicationStatus,
            @RequestParam(name = "returnTo", required = false) String returnTo,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        applicationService.updateStatus(id, applicationStatus, principal, request);
        return "redirect:" + sanitizeReturnTo(returnTo, id, "statusUpdated");
    }

    @PostMapping("/applications/{id}/blacklist")
    public String registerBlacklist(
            @PathVariable Long id,
            @RequestParam(name = "returnTo", required = false) String returnTo,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        String flag = "blacklisted";
        try {
            Long blacklistId = blacklistService.registerApplication(id, principal, request);
            if (blacklistId == null) {
                flag = "alreadyBlacklisted";
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            flag = "blacklistFailed";
        }
        return "redirect:" + sanitizeReturnTo(returnTo, id, flag);
    }

    private void populateListModel(
            Model model,
            Long documentSrl,
            JobDetail jobDetail,
            String keyword,
            Integer page,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        model.addAttribute("pageTitle", documentSrl == null ? "전체 지원서" : "공고별 지원서");
        model.addAttribute("pageDescription", "공고별 목록, 빠른 검색, 상태 변경, 응답 상세 확인을 지원하는 지원서 관리 화면입니다.");
        int totalCount = applicationService.countApplications(documentSrl, keyword);
        PaginationSupport.PageWindow pageWindow = PaginationSupport.applyMetadata(
                model,
                request,
                totalCount,
                page,
                PaginationSupport.DEFAULT_PAGE_SIZE
        );
        model.addAttribute(
                "applications",
                applicationService.getApplicationPage(documentSrl, keyword, pageWindow.pageSize(), pageWindow.offset())
        );
        model.addAttribute("jobFilters", applicationService.getJobFilters());
        model.addAttribute("selectedDocumentSrl", documentSrl);
        model.addAttribute("selectedJobDetail", jobDetail);
        model.addAttribute("statusOptions", applicationService.getAllowedStatuses());
        model.addAttribute("keyword", keyword);
        model.addAttribute("returnTo", currentPathWithQuery(request));
        model.addAttribute("_csrf", csrfToken);
    }

    private String currentPathWithQuery(HttpServletRequest request) {
        String queryString = request.getQueryString();
        return queryString == null || queryString.isBlank()
                ? request.getRequestURI()
                : request.getRequestURI() + "?" + queryString;
    }

    private String sanitizeReturnTo(String returnTo, Long applicationId, String flag) {
        if (returnTo == null || returnTo.isBlank() || !returnTo.startsWith("/") || returnTo.contains("://")) {
            return "/applications/" + applicationId + "?" + flag;
        }
        return returnTo.contains("?")
                ? returnTo + "&" + flag
                : returnTo + "?" + flag;
    }
}
