package com.researchi.admin.auth.web;

import com.researchi.admin.auth.domain.AdminUser;
import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.auth.service.AdminAuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class AdminAuthenticationFailureHandler implements AuthenticationFailureHandler {

    static final String LOGIN_ERROR_SESSION_KEY = "LOGIN_ERROR_CODE";

    private final AdminAuthService adminAuthService;
    private final AdminActionLogService adminActionLogService;

    public AdminAuthenticationFailureHandler(
            AdminAuthService adminAuthService,
            AdminActionLogService adminActionLogService
    ) {
        this.adminAuthService = adminAuthService;
        this.adminActionLogService = adminActionLogService;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        String loginId = request.getParameter("username");
        AdminUser adminUser = adminAuthService.findByLoginId(loginId);
        boolean alreadyLocked = adminUser != null
                && adminUser.getLockedUntil() != null
                && adminUser.getLockedUntil().isAfter(LocalDateTime.now());
        int failCount = alreadyLocked ? adminUser.getLoginFailCount() : adminAuthService.recordLoginFailure(loginId);
        Long adminUserId = adminUser != null ? adminUser.getId() : null;

        String detail = alreadyLocked || exception instanceof LockedException
                ? "로그인 잠금 상태"
                : "로그인 실패 횟수 " + failCount;

        adminActionLogService.log(
                adminUserId,
                "LOGIN_FAILURE",
                "ADMIN_USER",
                loginId,
                detail,
                request
        );

        String errorCode = alreadyLocked || exception instanceof LockedException ? "locked" : "bad-credentials";
        request.getSession(true).setAttribute(LOGIN_ERROR_SESSION_KEY, errorCode);
        response.sendRedirect("/login");
    }
}
