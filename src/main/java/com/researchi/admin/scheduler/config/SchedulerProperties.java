package com.researchi.admin.scheduler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.scheduler")
public class SchedulerProperties {

    private boolean enabled;
    private String scheduledSendCron = "0 * * * * *";
    private String thresholdCron = "30 * * * * *";
    private String cleanupCron = "0 0 3 * * *";
    private String blacklistExpiryCron = "0 15 * * * *";
    private String keywordMatchCron = "0 30 * * * *";
    private int retentionMonths = 6;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getScheduledSendCron() {
        return scheduledSendCron;
    }

    public void setScheduledSendCron(String scheduledSendCron) {
        this.scheduledSendCron = scheduledSendCron;
    }

    public String getThresholdCron() {
        return thresholdCron;
    }

    public void setThresholdCron(String thresholdCron) {
        this.thresholdCron = thresholdCron;
    }

    public String getCleanupCron() {
        return cleanupCron;
    }

    public void setCleanupCron(String cleanupCron) {
        this.cleanupCron = cleanupCron;
    }

    public String getBlacklistExpiryCron() {
        return blacklistExpiryCron;
    }

    public void setBlacklistExpiryCron(String blacklistExpiryCron) {
        this.blacklistExpiryCron = blacklistExpiryCron;
    }

    public String getKeywordMatchCron() {
        return keywordMatchCron;
    }

    public void setKeywordMatchCron(String keywordMatchCron) {
        this.keywordMatchCron = keywordMatchCron;
    }

    public int getRetentionMonths() {
        return retentionMonths;
    }

    public void setRetentionMonths(int retentionMonths) {
        this.retentionMonths = retentionMonths;
    }
}
