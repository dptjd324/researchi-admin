package com.researchi.admin.legacy.matching.mapper;

import com.researchi.admin.legacy.matching.domain.LegacyMatchingIndexJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LegacyMatchingIndexJobMapper {

    void insertJob(LegacyMatchingIndexJob job);

    LegacyMatchingIndexJob findById(@Param("id") Long id);

    Integer findNextCycleNo(@Param("researchNo") Long researchNo);

    List<LegacyMatchingIndexJob> findRecentByResearchNo(
            @Param("researchNo") Long researchNo,
            @Param("limit") int limit
    );

    LegacyMatchingIndexJob findRunningForCriteria(
            @Param("researchNo") Long researchNo,
            @Param("includeKeywordText") String includeKeywordText,
            @Param("excludeKeywordText") String excludeKeywordText
    );

    int markStarted(@Param("id") Long id);

    void markCompleted(LegacyMatchingIndexJob job);

    void markFailed(
            @Param("id") Long id,
            @Param("failReason") String failReason
    );

    int markPendingFailed(
            @Param("id") Long id,
            @Param("failReason") String failReason
    );

    int markInterruptedRunsFailed(
            @Param("failReason") String failReason,
            @Param("requestedBefore") java.time.LocalDateTime requestedBefore
    );

    int deleteByResearchNos(@Param("researchNos") List<Long> researchNos);
}
