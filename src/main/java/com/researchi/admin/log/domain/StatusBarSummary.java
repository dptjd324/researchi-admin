package com.researchi.admin.log.domain;

import java.time.LocalDateTime;

public record StatusBarSummary(
        long actionCount,
        long mailCount,
        long searchCount,
        long notificationCount,
        LocalDateTime latestActionAt,
        LocalDateTime latestMailAt,
        LocalDateTime latestSearchAt,
        LocalDateTime latestNotificationAt
) {
}
