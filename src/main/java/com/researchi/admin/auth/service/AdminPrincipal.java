package com.researchi.admin.auth.service;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;

public class AdminPrincipal implements UserDetails {

    private final Long id;
    private final String loginId;
    private final String passwordHash;
    private final String userName;
    private final String activeYn;
    private final LocalDateTime lockedUntil;

    public AdminPrincipal(
            Long id,
            String loginId,
            String passwordHash,
            String userName,
            String activeYn,
            LocalDateTime lockedUntil
    ) {
        this.id = id;
        this.loginId = loginId;
        this.passwordHash = passwordHash;
        this.userName = userName;
        this.activeYn = activeYn;
        this.lockedUntil = lockedUntil;
    }

    public Long getId() {
        return id;
    }

    public String getUserName() {
        return userName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return AuthorityUtils.createAuthorityList("ROLE_ADMIN");
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return loginId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return lockedUntil == null || lockedUntil.isBefore(LocalDateTime.now());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "Y".equalsIgnoreCase(activeYn);
    }
}
