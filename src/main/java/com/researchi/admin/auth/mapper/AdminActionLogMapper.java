package com.researchi.admin.auth.mapper;

import com.researchi.admin.auth.domain.AdminActionLog;
import com.researchi.admin.log.domain.ActionLogItem;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AdminActionLogMapper {

    void insert(AdminActionLog actionLog);

    List<ActionLogItem> findAll();
}
