package com.researchi.admin.matching.web;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.matching.domain.MatchingOverview;
import com.researchi.admin.matching.service.MatchingService;
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
@RequestMapping("/matching/jobs")
public class MatchingController {

    private final MatchingService matchingService;
    private final JobService jobService;

    public MatchingController(
            MatchingService matchingService,
            JobService jobService
    ) {
        this.matchingService = matchingService;
        this.jobService = jobService;
    }

    @GetMapping("/{documentSrl}")
    public String detail(
            @PathVariable("documentSrl") Long documentSrl,
            @RequestParam(name = "matchJobId", required = false) Long matchJobId,
            Model model,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        request.getSession(true);
        MatchingOverview overview = matchingService.getOverview(documentSrl, matchJobId);
        model.addAttribute("pageTitle", "키워드 매칭");
        model.addAttribute("pageDescription", "공고·지원자 키워드 추출, 매칭 실행, 알림 발송 대상을 검토하고 발송합니다.");
        model.addAttribute("jobDetail", jobService.getJob(documentSrl));
        model.addAttribute("documentSrl", documentSrl);
        model.addAttribute("selectedMatchJobId", matchJobId);
        model.addAttribute("overview", overview);
        model.addAttribute("_csrf", csrfToken);
        return "matching/detail";
    }

    @PostMapping("/{documentSrl}/run")
    public String run(
            @PathVariable("documentSrl") Long documentSrl,
            @AuthenticationPrincipal AdminPrincipal principal,
            HttpServletRequest request
    ) {
        Long matchJobId = matchingService.run(documentSrl, principal, request);
        return "redirect:/matching/jobs/" + documentSrl + "?matchJobId=" + matchJobId + "&matched";
    }

}
