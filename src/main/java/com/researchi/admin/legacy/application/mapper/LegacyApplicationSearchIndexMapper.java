package com.researchi.admin.legacy.application.mapper;

import com.researchi.admin.legacy.research.domain.ResearchApplication;
import com.researchi.admin.legacy.research.web.ResearchApplicationSearchForm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LegacyApplicationSearchIndexMapper {

    List<ResearchApplication> findPage(
            @Param("researchNo") Long researchNo,
            @Param("search") ResearchApplicationSearchForm search,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    int count(
            @Param("researchNo") Long researchNo,
            @Param("search") ResearchApplicationSearchForm search
    );

    ResearchApplication findByResearchNoAndSeq(
            @Param("researchNo") Long researchNo,
            @Param("researchAppSeq") Long researchAppSeq
    );

    List<ResearchApplication> findByResearchNo(@Param("researchNo") Long researchNo);

    boolean isReady();

    int upsert(@Param("application") ResearchApplication application);
}
