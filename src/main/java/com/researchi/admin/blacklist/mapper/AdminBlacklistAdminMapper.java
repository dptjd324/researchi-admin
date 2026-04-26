package com.researchi.admin.blacklist.mapper;

import com.researchi.admin.blacklist.domain.BlacklistActionLogItem;
import com.researchi.admin.blacklist.domain.BlacklistEntry;
import com.researchi.admin.blacklist.domain.BlacklistMatchLogItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminBlacklistAdminMapper {

    List<BlacklistEntry> findEntries(
            @Param("keyword") String keyword,
            @Param("activeYn") String activeYn,
            @Param("blackMode") String blackMode
    );

    BlacklistEntry findById(@Param("id") Long id);

    void insert(BlacklistEntry entry);

    int update(BlacklistEntry entry);

    int updateActiveStatus(
            @Param("id") Long id,
            @Param("activeYn") String activeYn,
            @Param("updatedAt") LocalDateTime updatedAt
    );

    List<BlacklistEntry> findExpiredActiveEntries(@Param("expiresAt") LocalDateTime expiresAt);

    List<BlacklistMatchLogItem> findRecentMatchLogs(@Param("blacklistId") Long blacklistId);

    List<BlacklistActionLogItem> findRecentActionLogs(@Param("targetId") String targetId);
}
