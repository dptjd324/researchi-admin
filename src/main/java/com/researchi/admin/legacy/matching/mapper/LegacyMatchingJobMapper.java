package com.researchi.admin.legacy.matching.mapper;

import com.researchi.admin.legacy.matching.domain.LegacyMatchingJob;
import com.researchi.admin.legacy.matching.domain.LegacyStoredMatchingResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LegacyMatchingJobMapper {

    void insertJob(LegacyMatchingJob job);

    LegacyMatchingJob findById(@Param("id") Long id);

    void markStarted(@Param("id") Long id);

    void markCompleted(LegacyMatchingJob job);

    void markFailed(@Param("id") Long id, @Param("failReason") String failReason);

    LegacyMatchingJob findLatestForCriteria(
            @Param("researchNo") Long researchNo,
            @Param("includeKeywordText") String includeKeywordText,
            @Param("excludeKeywordText") String excludeKeywordText
    );

    List<LegacyMatchingJob> findRecentByResearchNo(
            @Param("researchNo") Long researchNo,
            @Param("limit") int limit
    );

    void insertResults(@Param("results") List<LegacyStoredMatchingResult> results);

    List<LegacyStoredMatchingResult> findResultsByJobId(@Param("matchingJobId") Long matchingJobId, @Param("limit") int limit);

    int deleteResultsByResearchNos(@Param("researchNos") List<Long> researchNos);

    int deleteJobsByResearchNos(@Param("researchNos") List<Long> researchNos);
}
