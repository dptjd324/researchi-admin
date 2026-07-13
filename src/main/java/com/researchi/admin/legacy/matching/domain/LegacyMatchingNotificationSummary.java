package com.researchi.admin.legacy.matching.domain;

import java.time.LocalDateTime;

public class LegacyMatchingNotificationSummary {

    private String channelType;
    private Integer sentCount;
    private Integer skippedDuplicateCount;
    private Integer failedCount;
    private LocalDateTime latestCreatedAt;

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public Integer getSentCount() {
        return sentCount;
    }

    public void setSentCount(Integer sentCount) {
        this.sentCount = sentCount;
    }

    public Integer getSkippedDuplicateCount() {
        return skippedDuplicateCount;
    }

    public void setSkippedDuplicateCount(Integer skippedDuplicateCount) {
        this.skippedDuplicateCount = skippedDuplicateCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public LocalDateTime getLatestCreatedAt() {
        return latestCreatedAt;
    }

    public void setLatestCreatedAt(LocalDateTime latestCreatedAt) {
        this.latestCreatedAt = latestCreatedAt;
    }

    public String channelLabel() {
        if ("LEGACY_EMAIL".equalsIgnoreCase(channelType)) {
            return "메일";
        }
        if ("LEGACY_SMS".equalsIgnoreCase(channelType)) {
            return "SMS";
        }
        return channelType == null || channelType.isBlank() ? "-" : channelType;
    }
}
