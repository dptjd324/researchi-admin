package com.researchi.admin.auth.web;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.auth.service.AdminPrincipal;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AdminLogoutSuccessHandler implements LogoutSuccessHandler {

    private final AdminActionLogService adminActionLogService;

    public AdminLogoutSuccessHandler(AdminActionLogService adminActionLogService) {
        this.adminActionLogService = adminActionLogService;
    }

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        if (authentication != null && authentication.getPrincipal() instanceof AdminPrincipal principal) {
            adminActionLogService.log(
                    principal.getId(),
                    "LOGOUT",
                    "ADMIN_USER",
                    principal.getUsername(),
                    "관리자 로그아웃",
                    request
            );
        }
        response.sendRedirect("/login?logout");
    }
}
