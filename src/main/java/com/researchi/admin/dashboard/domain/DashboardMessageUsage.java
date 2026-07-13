package com.researchi.admin.dashboard.domain;

import java.util.List;

public record DashboardMessageUsage(
        int currentMonthEmailCount,
        int currentMonthSmsCount,
        String currentMonthEmailCostLabel,
        String currentMonthSmsCostLabel,
        String currentMonthTotalCostLabel,
        int maxMonthlyTotal,
        List<MonthlyMessageUsage> monthlyUsages
) {
}
