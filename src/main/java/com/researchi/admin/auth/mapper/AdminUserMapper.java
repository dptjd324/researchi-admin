package com.researchi.admin.auth.mapper;

import com.researchi.admin.auth.domain.AdminUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface AdminUserMapper {

    AdminUser findByLoginId(@Param("loginId") String loginId);

    AdminUser findById(@Param("id") Long id);

    void updateLoginSuccess(@Param("id") Long id, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    void updateLoginFailure(
            @Param("id") Long id,
            @Param("loginFailCount") int loginFailCount,
            @Param("lockedUntil") LocalDateTime lockedUntil
    );

    void updatePasswordHash(@Param("id") Long id, @Param("passwordHash") String passwordHash);
}
