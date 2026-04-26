package com.researchi.admin.auth.service;

import com.researchi.admin.auth.domain.AdminUser;
import com.researchi.admin.auth.mapper.AdminUserMapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminUserMapper adminUserMapper;

    public AdminUserDetailsService(AdminUserMapper adminUserMapper) {
        this.adminUserMapper = adminUserMapper;
    }

    @Override
    public AdminPrincipal loadUserByUsername(String username) throws UsernameNotFoundException {
        AdminUser adminUser = adminUserMapper.findByLoginId(username);
        if (adminUser == null) {
            throw new UsernameNotFoundException("관리자 계정을 찾을 수 없습니다.");
        }

        return new AdminPrincipal(
                adminUser.getId(),
                adminUser.getLoginId(),
                adminUser.getPasswordHash(),
                adminUser.getUserName(),
                adminUser.getActiveYn(),
                adminUser.getLockedUntil()
        );
    }
}
