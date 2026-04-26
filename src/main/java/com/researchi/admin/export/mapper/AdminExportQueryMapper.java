package com.researchi.admin.export.mapper;

import com.researchi.admin.export.domain.ExportAnswerSource;
import com.researchi.admin.export.domain.ExportApplicationSource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminExportQueryMapper {

    List<ExportApplicationSource> findApplicationsByDocumentSrl(@Param("documentSrl") Long documentSrl);

    List<ExportAnswerSource> findAnswersByDocumentSrl(@Param("documentSrl") Long documentSrl);
}
