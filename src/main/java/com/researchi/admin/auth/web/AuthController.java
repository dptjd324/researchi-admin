package com.researchi.admin.auth.web;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.auth.service.AdminAuthService;
import com.researchi.admin.auth.service.AdminPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final AdminAuthService adminAuthService;
    private final AdminActionLogService adminActionLogService;

    public AuthController(
            AdminAuthService adminAuthService,
            AdminActionLogService adminActionLogService
    ) {
        this.adminAuthService = adminAuthService;
        this.adminActionLogService = adminActionLogService;
    }

    @GetMapping("/login")
    public String login(Model model, HttpServletRequest request, CsrfToken csrfToken) {
        HttpSession session = request.getSession(true);
        model.addAttribute("pageTitle", "관리자 로그인");
        model.addAttribute("pageDescription", "Researchi 관리자 계정으로 로그인해 운영 화면에 접근합니다.");
        model.addAttribute("hideNavigation", true);
        model.addAttribute("hidePageHeader", true);
        model.addAttribute("hideStatusDock", true);
        model.addAttribute("contentContainerClass", "auth-page-container");
        model.addAttribute("loginForm", new LoginForm());
        model.addAttribute("loginErrorCode", session.getAttribute(AdminAuthenticationFailureHandler.LOGIN_ERROR_SESSION_KEY));
        session.removeAttribute(AdminAuthenticationFailureHandler.LOGIN_ERROR_SESSION_KEY);
        model.addAttribute("_csrf", csrfToken);
        return "auth/login";
    }

    @GetMapping("/account/password")
    public String passwordChange(Model model, HttpServletRequest request, CsrfToken csrfToken) {
        request.getSession(true);
        model.addAttribute("pageTitle", "비밀번호 변경");
        model.addAttribute("pageDescription", "관리자 비밀번호를 변경합니다.");
        model.addAttribute("passwordChangeForm", new PasswordChangeForm());
        model.addAttribute("_csrf", csrfToken);
        return "auth/password-change";
    }

    @PostMapping("/account/password")
    public String changePassword(
            @AuthenticationPrincipal AdminPrincipal principal,
            @Valid @ModelAttribute("passwordChangeForm") PasswordChangeForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            HttpSession session,
            Model model
    ) {
        model.addAttribute("pageTitle", "비밀번호 변경");
        model.addAttribute("pageDescription", "관리자 비밀번호를 변경합니다.");

        if (!form.getNewPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "mismatch", "새 비밀번호 확인이 일치하지 않습니다.");
        }

        if (bindingResult.hasErrors()) {
            return "auth/password-change";
        }

        try {
            adminAuthService.changePassword(principal.getId(), form.getCurrentPassword(), form.getNewPassword());
        } catch (IllegalArgumentException exception) {
            bindingResult.rejectValue("currentPassword", "invalid", exception.getMessage());
            return "auth/password-change";
        }

        adminActionLogService.log(
                principal.getId(),
                "PASSWORD_CHANGE",
                "ADMIN_USER",
                principal.getUsername(),
                "관리자 비밀번호 변경",
                request
        );

        session.invalidate();
        return "redirect:/login?passwordChanged";
    }
}
