package com.researchi.admin.keyword.mapper;

import com.researchi.admin.keyword.domain.AdminJobKeyword;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminJobKeywordMapper {

    void deleteByDocumentSrl(@Param("documentSrl") Long documentSrl);

    void insert(AdminJobKeyword keyword);

    List<AdminJobKeyword> findByDocumentSrl(@Param("documentSrl") Long documentSrl);
}
