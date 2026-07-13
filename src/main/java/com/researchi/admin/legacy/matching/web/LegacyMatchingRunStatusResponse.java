package com.researchi.admin.legacy.matching.web;

public record LegacyMatchingRunStatusResponse(
        Long jobId,
        int cycleNo,
        String status,
        String failReason,
        String resultUrl
) {
}
