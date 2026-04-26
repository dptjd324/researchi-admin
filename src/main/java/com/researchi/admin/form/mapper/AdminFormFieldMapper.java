package com.researchi.admin.form.mapper;

import com.researchi.admin.form.domain.AdminFormField;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminFormFieldMapper {

    List<AdminFormField> findByDocumentSrl(@Param("documentSrl") Long documentSrl);

    AdminFormField findById(@Param("id") Long id);

    AdminFormField findByDocumentSrlAndFieldKey(@Param("documentSrl") Long documentSrl, @Param("fieldKey") String fieldKey);

    void insert(AdminFormField adminFormField);

    void update(AdminFormField adminFormField);

    void deleteById(@Param("id") Long id);
}
