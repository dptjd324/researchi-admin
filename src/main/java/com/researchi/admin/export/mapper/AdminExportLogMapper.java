package com.researchi.admin.export.mapper;

import com.researchi.admin.export.domain.AdminExportLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminExportLogMapper {

    void insert(AdminExportLog exportLog);
}
