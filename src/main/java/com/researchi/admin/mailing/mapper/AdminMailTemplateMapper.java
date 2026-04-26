package com.researchi.admin.mailing.mapper;

import com.researchi.admin.mailing.domain.AdminMailTemplate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminMailTemplateMapper {

    List<AdminMailTemplate> findAll();

    List<AdminMailTemplate> findActive();

    AdminMailTemplate findById(@Param("id") Long id);

    AdminMailTemplate findActiveByName(@Param("templateName") String templateName);

    void insert(AdminMailTemplate template);

    void update(AdminMailTemplate template);
}
