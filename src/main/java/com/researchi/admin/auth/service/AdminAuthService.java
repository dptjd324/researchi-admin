package com.researchi.admin.auth.service;

import com.researchi.admin.auth.config.SecurityProperties;
import com.researchi.admin.auth.domain.AdminUser;
import com.researchi.admin.auth.mapper.AdminUserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AdminAuthService {

    private final AdminUserMapper adminUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties securityProperties;

    public AdminAuthService(
            AdminUserMapper adminUserMapper,
            PasswordEncoder passwordEncoder,
            SecurityProperties securityProperties
    ) {
        this.adminUserMapper = adminUserMapper;
        this.passwordEncoder = passwordEncoder;
        this.securityProperties = securityProperties;
    }

    public AdminUser findByLoginId(String loginId) {
        return adminUserMapper.findByLoginId(loginId);
    }

    public AdminUser findById(Long id) {
        return adminUserMapper.findById(id);
    }

    @Transactional("adminTransactionManager")
    public void recordLoginSuccess(Long id) {
        adminUserMapper.updateLoginSuccess(id, LocalDateTime.now());
    }

    @Transactional("adminTransactionManager")
    public int recordLoginFailure(String loginId) {
        AdminUser adminUser = adminUserMapper.findByLoginId(loginId);
        if (adminUser == null) {
            return 0;
        }

        int nextFailCount = (adminUser.getLoginFailCount() == null ? 0 : adminUser.getLoginFailCount()) + 1;
        LocalDateTime lockedUntil = null;
        if (nextFailCount >= securityProperties.getLoginFailLimit()) {
            lockedUntil = LocalDateTime.now().plusMinutes(securityProperties.getLockMinutes());
        }

        adminUserMapper.updateLoginFailure(adminUser.getId(), nextFailCount, lockedUntil);
        return nextFailCount;
    }

    @Transactional("adminTransactionManager")
    public void changePassword(Long adminUserId, String currentPassword, String newPassword) {
        AdminUser adminUser = adminUserMapper.findById(adminUserId);
        if (adminUser == null) {
            throw new IllegalArgumentException("관리자 계정을 찾을 수 없습니다.");
        }
        if (!passwordEncoder.matches(currentPassword, adminUser.getPasswordHash())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        String encodedPassword = passwordEncoder.encode(newPassword);
        adminUserMapper.updatePasswordHash(adminUserId, encodedPassword);
    }
}
