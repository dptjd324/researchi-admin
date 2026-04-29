package com.researchi.admin.job.domain;

public enum JobType {
    NEW("newjob", "\uc2e0\uaddc\uc77c\uac10"),
    ADDITIONAL("additional", "\ucd94\uac00\uc77c\uac10"),
    FAST("fast", "\uae09\uc9c4\ud589\uc2e0\uccad"),
    RECRUIT("recruit", "\uc804\uad6d/\uc9c0\uc5ed\ubaa8\uc9d1");

    private final String mid;
    private final String label;

    JobType(String mid, String label) {
        this.mid = mid;
        this.label = label;
    }

    public String getMid() {
        return mid;
    }

    public String getLabel() {
        return label;
    }

    public static JobType fromMid(String mid) {
        for (JobType value : values()) {
            if (value.mid.equalsIgnoreCase(mid)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unsupported application board mid.");
    }

    public static boolean supportsMid(String mid) {
        for (JobType value : values()) {
            if (value.mid.equalsIgnoreCase(mid)) {
                return true;
            }
        }
        return false;
    }
}
