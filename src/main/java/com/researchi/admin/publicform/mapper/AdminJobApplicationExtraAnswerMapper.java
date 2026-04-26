package com.researchi.admin.publicform.mapper;

import com.researchi.admin.publicform.domain.AdminJobApplicationExtraAnswer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminJobApplicationExtraAnswerMapper {

    void insert(AdminJobApplicationExtraAnswer answer);

    List<AdminJobApplicationExtraAnswer> findByApplicationId(@Param("applicationId") Long applicationId);
}
