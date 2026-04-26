package com.researchi.admin.xe.mapper;

import com.researchi.admin.xe.domain.XeJobDocument;
import com.researchi.admin.xe.domain.XeModule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface XeJobMapper {

    List<XeModule> findJobModules();

    XeModule findModuleByMid(@Param("mid") String mid);

    List<XeJobDocument> findJobDocuments();

    XeJobDocument findJobDocumentById(@Param("documentSrl") Long documentSrl);

    Long findNextDocumentSrl();

    Long findNextListOrder();

    void insertJobDocument(XeJobDocument jobDocument);

    void updateJobDocument(XeJobDocument jobDocument);

    void updateJobDocumentStatus(@Param("documentSrl") Long documentSrl, @Param("status") String status, @Param("lastUpdate") String lastUpdate);
}
