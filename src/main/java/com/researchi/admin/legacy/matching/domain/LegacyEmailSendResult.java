package com.researchi.admin.legacy.matching.domain;

public record LegacyEmailSendResult(
        int targetCount,
        int sentCount,
        int skippedDuplicateCount,
        int failedCount
) {
}
