package com.researchi.admin.auth.mapper;

import com.researchi.admin.auth.domain.AdminActionLog;
import com.researchi.admin.log.domain.ActionLogItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminActionLogMapper {

    void insert(AdminActionLog actionLog);

    List<ActionLogItem> findAll();

    List<ActionLogItem> findPage(@Param("limit") int limit, @Param("offset") int offset);

    long countAll();

    LocalDateTime findLatestCreatedAt();
}
