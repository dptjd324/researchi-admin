package com.researchi.admin.publicform.mapper;

import com.researchi.admin.publicform.domain.AdminFormSubmissionAnswer;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminFormSubmissionAnswerMapper {

    void insert(AdminFormSubmissionAnswer answer);
}
