package com.researchi.admin.legacy.matching.mapper;

import com.researchi.admin.legacy.matching.domain.LegacyApplicationKeyword;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingCandidate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LegacyApplicationKeywordMapper {

    int countIndexedApplications();

    List<LegacyMatchingCandidate> findIndexedApplications(@Param("limit") int limit);

    int deleteByApplication(
            @Param("researchNo") Long researchNo,
            @Param("researchAppSeq") Long researchAppSeq
    );

    int deleteAll();

    int insertBatch(@Param("keywords") List<LegacyApplicationKeyword> keywords);

    List<LegacyMatchingCandidate> findCandidates(
            @Param("includeKeywords") List<String> includeKeywords,
            @Param("excludeKeywords") List<String> excludeKeywords,
            @Param("currentResearchNo") Long currentResearchNo,
            @Param("limit") int limit
    );
}
