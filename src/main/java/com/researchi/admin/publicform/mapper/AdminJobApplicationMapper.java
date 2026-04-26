package com.researchi.admin.publicform.mapper;

import com.researchi.admin.publicform.domain.AdminJobApplication;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminJobApplicationMapper {

    void insert(AdminJobApplication application);
}
