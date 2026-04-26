package com.researchi.admin.job.domain;

public enum JobType {
    NEW("newjob", "신규일감"),
    ADDITIONAL("additional", "추가일감");

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
        throw new IllegalArgumentException("지원하지 않는 공고 타입입니다.");
    }
}
