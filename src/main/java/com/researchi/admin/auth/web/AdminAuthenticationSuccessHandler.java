package com.researchi.admin.auth.web;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.auth.service.AdminAuthService;
import com.researchi.admin.auth.service.AdminPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AdminAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final AdminAuthService adminAuthService;
    private final AdminActionLogService adminActionLogService;

    public AdminAuthenticationSuccessHandler(
            AdminAuthService adminAuthService,
            AdminActionLogService adminActionLogService
    ) {
        this.adminAuthService = adminAuthService;
        this.adminActionLogService = adminActionLogService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        AdminPrincipal principal = (AdminPrincipal) authentication.getPrincipal();
        adminAuthService.recordLoginSuccess(principal.getId());
        adminActionLogService.log(
                principal.getId(),
                "LOGIN_SUCCESS",
                "ADMIN_USER",
                principal.getUsername(),
                "관리자 로그인 성공",
                request
        );
        response.sendRedirect("/jobs");
    }
}
