package com.researchi.admin.keyword.mapper;

import com.researchi.admin.keyword.domain.AdminApplicationKeyword;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminApplicationKeywordMapper {

    void deleteByApplicationId(@Param("applicationId") Long applicationId);

    void insert(AdminApplicationKeyword keyword);

    List<AdminApplicationKeyword> findByApplicationIds(@Param("applicationIds") List<Long> applicationIds);
}
