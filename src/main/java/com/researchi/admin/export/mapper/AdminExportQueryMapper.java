package com.researchi.admin.export.mapper;

import com.researchi.admin.export.domain.ExportAnswerSource;
import com.researchi.admin.export.domain.ExportApplicationSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminExportQueryMapper {

    List<ExportApplicationSource> findApplicationsByDocumentSrl(@Param("documentSrl") Long documentSrl);

    List<ExportApplicationSource> findApplicationsPageByDocumentSrl(
            @Param("documentSrl") Long documentSrl,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    List<ExportAnswerSource> findAnswersByDocumentSrl(@Param("documentSrl") Long documentSrl);

    List<ExportAnswerSource> findAnswersByApplicationIds(@Param("applicationIds") List<Long> applicationIds);
}
