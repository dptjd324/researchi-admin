package com.researchi.admin.legacy.matching.domain;

import java.util.List;

public record LegacyMatchingHistory(
        List<LegacyMatchingJob> matchingJobs,
        List<LegacyMatchingIndexJob> indexJobs,
        List<LegacyMatchingNotificationSummary> notificationSummaries
) {
}
