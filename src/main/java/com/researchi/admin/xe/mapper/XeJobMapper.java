package com.researchi.admin.xe.mapper;

import com.researchi.admin.xe.domain.XeJobDocument;
import com.researchi.admin.xe.domain.XeModule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface XeJobMapper {

    List<XeModule> findJobModules(@Param("mids") List<String> mids);

    XeModule findModuleByMid(@Param("mid") String mid);

    List<XeJobDocument> findJobDocumentsByIds(
            @Param("documentSrls") List<Long> documentSrls,
            @Param("mids") List<String> mids
    );

    List<Long> findJobDocumentSrlsByTitle(
            @Param("normalizedKeyword") String normalizedKeyword,
            @Param("keywordTokens") List<String> keywordTokens,
            @Param("mids") List<String> mids
    );

    List<XeJobDocument> findJobDocumentsPage(
            @Param("mid") String mid,
            @Param("normalizedKeyword") String normalizedKeyword,
            @Param("keywordTokens") List<String> keywordTokens,
            @Param("mids") List<String> mids,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    List<XeJobDocument> findJobDocumentsAfter(
            @Param("mid") String mid,
            @Param("normalizedKeyword") String normalizedKeyword,
            @Param("keywordTokens") List<String> keywordTokens,
            @Param("mids") List<String> mids,
            @Param("afterDocumentSrl") Long afterDocumentSrl,
            @Param("limit") int limit
    );

    int countJobDocuments(
            @Param("mid") String mid,
            @Param("normalizedKeyword") String normalizedKeyword,
            @Param("keywordTokens") List<String> keywordTokens,
            @Param("mids") List<String> mids
    );

    XeJobDocument findJobDocumentById(@Param("documentSrl") Long documentSrl, @Param("mids") List<String> mids);

    Long findNextDocumentSrl();

    Long findNextListOrder();

    void insertJobDocument(XeJobDocument jobDocument);

    int updateJobDocument(@Param("jobDocument") XeJobDocument jobDocument, @Param("mids") List<String> mids);

    int updateJobDocumentStatus(
            @Param("documentSrl") Long documentSrl,
            @Param("status") String status,
            @Param("lastUpdate") String lastUpdate,
            @Param("mids") List<String> mids
    );

    int deleteJobDocument(@Param("documentSrl") Long documentSrl, @Param("mids") List<String> mids);
}
