package com.researchi.admin.legacy.matching.domain;

public class LegacySmsSendLimitExceededException extends RuntimeException {

    private final int requestedCount;
    private final int dailySentCount;
    private final int dailyLimit;
    private final int monthlySentCount;
    private final int monthlyLimit;

    public LegacySmsSendLimitExceededException(
            String message,
            int requestedCount,
            int dailySentCount,
            int dailyLimit,
            int monthlySentCount,
            int monthlyLimit
    ) {
        super(message);
        this.requestedCount = requestedCount;
        this.dailySentCount = dailySentCount;
        this.dailyLimit = dailyLimit;
        this.monthlySentCount = monthlySentCount;
        this.monthlyLimit = monthlyLimit;
    }

    public int getRequestedCount() {
        return requestedCount;
    }

    public int getDailySentCount() {
        return dailySentCount;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public int getMonthlySentCount() {
        return monthlySentCount;
    }

    public int getMonthlyLimit() {
        return monthlyLimit;
    }
}
