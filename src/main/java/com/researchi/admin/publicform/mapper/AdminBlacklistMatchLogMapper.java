package com.researchi.admin.publicform.mapper;

import com.researchi.admin.publicform.domain.AdminBlacklistMatchLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminBlacklistMatchLogMapper {

    void insert(AdminBlacklistMatchLog matchLog);
}
