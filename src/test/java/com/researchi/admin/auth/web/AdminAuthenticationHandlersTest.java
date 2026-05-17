package com.researchi.admin.auth.web;

import com.researchi.admin.auth.domain.AdminUser;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthenticationHandlersTest {

    @Mock
    private AdminAuthService adminAuthService;

    @Mock
    private AdminActionLogService adminActionLogService;

    @InjectMocks
    private AdminAuthenticationFailureHandler failureHandler;

    @InjectMocks
    private AdminAuthenticationSuccessHandler successHandler;

    @Test
    void failureHandlerRedirectsToLockedWhenUserIsAlreadyLocked() throws Exception {
        AdminUser adminUser = new AdminUser();
        adminUser.setId(1L);
        adminUser.setLoginId("admin");
        adminUser.setLoginFailCount(5);
        adminUser.setLockedUntil(LocalDateTime.now().plusMinutes(10));

        when(adminAuthService.findByLoginId("admin")).thenReturn(adminUser);
        doNothing().when(adminActionLogService).log(any(), any(), any(), any(), any(), any());

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addParameter("username", "admin");
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(request, response, new BadCredentialsException("bad credentials"));

        assertThat(response.getRedirectedUrl()).isEqualTo("/login");
        assertThat(request.getSession(false)).isNotNull();
        assertThat(request.getSession(false).getAttribute(AdminAuthenticationFailureHandler.LOGIN_ERROR_SESSION_KEY)).isEqualTo("locked");
        verify(adminAuthService, never()).recordLoginFailure("admin");
        verify(adminActionLogService).log(eq(1L), eq("LOGIN_FAILURE"), eq("ADMIN_USER"), eq("admin"), eq("로그인 잠금 상태"), any(HttpServletRequest.class));
    }

    @Test
    void successHandlerUpdatesLoginStateAndRedirects() throws Exception {
        doNothing().when(adminAuthService).recordLoginSuccess(1L);
        doNothing().when(adminActionLogService).log(any(), any(), any(), any(), any(), any());

        AdminPrincipal principal = new AdminPrincipal(1L, "admin", "hash", "관리자", "Y", LocalDateTime.now().minusMinutes(1));
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                principal,
                principal.getPassword(),
                principal.getAuthorities()
        );

        MockHttpServletResponse response = new MockHttpServletResponse();
        successHandler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        assertThat(response.getRedirectedUrl()).isEqualTo("/research");
        verify(adminAuthService).recordLoginSuccess(1L);
        verify(adminActionLogService).log(eq(1L), eq("LOGIN_SUCCESS"), eq("ADMIN_USER"), eq("admin"), eq("관리자 로그인 성공"), any(HttpServletRequest.class));
    }
}
