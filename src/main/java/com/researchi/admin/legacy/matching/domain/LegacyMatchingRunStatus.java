package com.researchi.admin.legacy.matching.domain;

public record LegacyMatchingRunStatus(
        Long jobId,
        Long researchNo,
        int cycleNo,
        String status,
        String failReason,
        String conditionStorageKey
) {
}
