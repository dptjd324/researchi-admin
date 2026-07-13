package com.researchi.admin.legacy.matching.domain;

import com.researchi.admin.legacy.research.domain.ResearchMaster;

import java.time.LocalDateTime;
import java.util.List;

public record LegacyMatchingOverview(
        ResearchMaster research,
        String includeKeywordText,
        String excludeKeywordText,
        LegacyMatchingSearchCondition searchCondition,
        int indexLimit,
        List<String> activeKeywords,
        List<LegacyMatchingResult> results,
        int candidatePoolCount,
        int indexedApplicationCount,
        int blacklistedExcludedCount,
        int latestCycleNo,
        int nextCycleNo,
        int cycleBatchSize,
        Long matchingJobId,
        String matchingStatus,
        String failReason,
        LocalDateTime finishedAt
) {

    public int matchedCount() {
        return results.size();
    }

    public String activeKeywordText() {
        return String.join(", ", activeKeywords);
    }
}
