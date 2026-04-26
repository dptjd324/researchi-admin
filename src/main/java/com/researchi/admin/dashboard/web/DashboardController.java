package com.researchi.admin.dashboard.web;

import com.researchi.admin.auth.config.SecurityProperties;
import com.researchi.admin.auth.service.AdminPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardController {

    private final SecurityProperties securityProperties;

    public DashboardController(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @GetMapping("/dashboard")
    public String dashboard(
            @AuthenticationPrincipal AdminPrincipal principal,
            Model model,
            HttpServletRequest request,
            CsrfToken csrfToken
    ) {
        request.getSession(true);
        model.addAttribute("pageTitle", "관리자 대시보드");
        model.addAttribute("pageDescription", "보안 설정을 확인하고 공고, 지원서, 기간 검색, 로그 화면으로 이동합니다.");
        model.addAttribute("adminName", principal.getUserName());
        model.addAttribute("loginId", principal.getUsername());
        model.addAttribute("sessionTimeout", securityProperties.getSessionTimeout());
        model.addAttribute("loginFailLimit", securityProperties.getLoginFailLimit());
        model.addAttribute("lockMinutes", securityProperties.getLockMinutes());
        model.addAttribute("_csrf", csrfToken);
        return "dashboard/index";
    }
}
