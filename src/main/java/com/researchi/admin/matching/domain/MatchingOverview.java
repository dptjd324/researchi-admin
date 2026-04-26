package com.researchi.admin.matching.domain;

import com.researchi.admin.notification.domain.AdminNotificationLog;

import java.util.List;

public record MatchingOverview(
        List<String> jobKeywords,
        List<AdminKeywordMatchJob> matchJobs,
        List<MatchingTargetView> targets,
        List<AdminNotificationLog> notificationLogs
) {
}
