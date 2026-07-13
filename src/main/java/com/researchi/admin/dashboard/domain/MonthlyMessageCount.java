package com.researchi.admin.dashboard.domain;

public class MonthlyMessageCount {

    private String monthKey;
    private int sentCount;

    public String getMonthKey() {
        return monthKey;
    }

    public void setMonthKey(String monthKey) {
        this.monthKey = monthKey;
    }

    public int getSentCount() {
        return sentCount;
    }

    public void setSentCount(int sentCount) {
        this.sentCount = sentCount;
    }
}
