package com.researchi.admin.auth.service;

import com.researchi.admin.auth.config.SecurityProperties;
import com.researchi.admin.auth.domain.AdminUser;
import com.researchi.admin.auth.mapper.AdminUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock
    private AdminUserMapper adminUserMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminAuthService adminAuthService;

    @BeforeEach
    void setUp() {
        SecurityProperties securityProperties = new SecurityProperties();
        securityProperties.setLoginFailLimit(3);
        securityProperties.setLockMinutes(10);
        adminAuthService = new AdminAuthService(adminUserMapper, passwordEncoder, securityProperties);
    }

    @Test
    void recordLoginFailureLocksUserWhenLimitIsReached() {
        AdminUser user = new AdminUser();
        user.setId(7L);
        user.setLoginId("admin");
        user.setLoginFailCount(2);
        when(adminUserMapper.findByLoginId("admin")).thenReturn(user);

        int failCount = adminAuthService.recordLoginFailure("admin");

        assertThat(failCount).isEqualTo(3);
        ArgumentCaptor<LocalDateTime> lockedUntilCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(adminUserMapper).updateLoginFailure(eq(7L), eq(3), lockedUntilCaptor.capture());
        assertThat(lockedUntilCaptor.getValue()).isAfter(LocalDateTime.now().plusMinutes(9));
    }

    @Test
    void changePasswordEncodesAndUpdatesHash() {
        AdminUser user = new AdminUser();
        user.setId(7L);
        user.setPasswordHash("stored");
        when(adminUserMapper.findById(7L)).thenReturn(user);
        when(passwordEncoder.matches("current-pass", "stored")).thenReturn(true);
        when(passwordEncoder.encode("NewPassword1")).thenReturn("encoded");

        adminAuthService.changePassword(7L, "current-pass", "NewPassword1");

        verify(adminUserMapper).updatePasswordHash(7L, "encoded");
    }

    @Test
    void changePasswordRejectsWrongCurrentPassword() {
        AdminUser user = new AdminUser();
        user.setId(7L);
        user.setPasswordHash("stored");
        when(adminUserMapper.findById(7L)).thenReturn(user);
        when(passwordEncoder.matches("wrong", "stored")).thenReturn(false);

        assertThatThrownBy(() -> adminAuthService.changePassword(7L, "wrong", "NewPassword1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("현재 비밀번호가 일치하지 않습니다.");
    }
}
