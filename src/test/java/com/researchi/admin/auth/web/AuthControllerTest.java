package com.researchi.admin.auth.web;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.auth.service.AdminAuthService;
import com.researchi.admin.auth.service.AdminPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AdminAuthService adminAuthService;

    @Mock
    private AdminActionLogService adminActionLogService;

    @InjectMocks
    private AuthController authController;

    @Test
    void changePasswordReturnsFormWhenConfirmationDoesNotMatch() {
        PasswordChangeForm form = new PasswordChangeForm();
        form.setCurrentPassword("current-pass");
        form.setNewPassword("NewPassword1");
        form.setConfirmPassword("DifferentPassword1");

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "passwordChangeForm");
        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = authController.changePassword(
                adminPrincipal(),
                form,
                bindingResult,
                new MockHttpServletRequest(),
                new MockHttpSession(),
                model
        );

        assertThat(viewName).isEqualTo("auth/password-change");
        assertThat(bindingResult.hasFieldErrors("confirmPassword")).isTrue();
    }

    @Test
    void changePasswordLogsAndInvalidatesSessionOnSuccess() {
        PasswordChangeForm form = new PasswordChangeForm();
        form.setCurrentPassword("current-pass");
        form.setNewPassword("NewPassword1");
        form.setConfirmPassword("NewPassword1");

        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(form, "passwordChangeForm");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        doNothing().when(adminAuthService).changePassword(1L, "current-pass", "NewPassword1");

        String viewName = authController.changePassword(
                adminPrincipal(),
                form,
                bindingResult,
                request,
                session,
                new ExtendedModelMap()
        );

        assertThat(viewName).isEqualTo("redirect:/login?passwordChanged");
        assertThatThrownBy(session::getCreationTime).isInstanceOf(IllegalStateException.class);
        verify(adminActionLogService).log(eq(1L), eq("PASSWORD_CHANGE"), eq("ADMIN_USER"), eq("admin"), eq("관리자 비밀번호 변경"), any(HttpServletRequest.class));
    }

    @Test
    void loginConsumesOneTimeErrorStateFromSession() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(AdminAuthenticationFailureHandler.LOGIN_ERROR_SESSION_KEY, "bad-credentials");
        request.setSession(session);
        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = authController.login(model, request, null);

        assertThat(viewName).isEqualTo("auth/login");
        assertThat(model.get("loginErrorCode")).isEqualTo("bad-credentials");
        assertThat(session.getAttribute(AdminAuthenticationFailureHandler.LOGIN_ERROR_SESSION_KEY)).isNull();
    }

    private AdminPrincipal adminPrincipal() {
        return new AdminPrincipal(1L, "admin", "hash", "관리자", "Y", LocalDateTime.now().minusMinutes(1));
    }
}
