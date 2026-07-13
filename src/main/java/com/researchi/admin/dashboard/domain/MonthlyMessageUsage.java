package com.researchi.admin.dashboard.domain;

public record MonthlyMessageUsage(
        String monthKey,
        String monthLabel,
        int emailCount,
        int smsCount,
        int smsBillableCount,
        String emailCostLabel,
        String smsCostLabel,
        String totalCostLabel
) {
    public int totalCount() {
        return emailCount + smsCount;
    }
}
