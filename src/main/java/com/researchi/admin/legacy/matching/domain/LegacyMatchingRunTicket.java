package com.researchi.admin.legacy.matching.domain;

public record LegacyMatchingRunTicket(
        Long jobId,
        int cycleNo,
        String status,
        boolean reused
) {
}
