package com.researchi.admin.legacy.matching.domain;

public record LegacySmsSendResult(
        int targetCount,
        int sentCount,
        int skippedDuplicateCount,
        int failedCount
) {
}
