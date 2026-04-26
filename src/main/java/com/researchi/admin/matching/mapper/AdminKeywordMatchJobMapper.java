package com.researchi.admin.matching.mapper;

import com.researchi.admin.matching.domain.AdminKeywordMatchJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminKeywordMatchJobMapper {

    void insert(AdminKeywordMatchJob matchJob);

    void update(AdminKeywordMatchJob matchJob);

    List<AdminKeywordMatchJob> findByDocumentSrl(@Param("documentSrl") Long documentSrl);

    AdminKeywordMatchJob findById(@Param("id") Long id);
}
