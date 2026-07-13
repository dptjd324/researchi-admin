package com.researchi.admin.dashboard.service;

import com.researchi.admin.dashboard.domain.DashboardMessageUsage;
import com.researchi.admin.dashboard.domain.MonthlyMessageCount;
import com.researchi.admin.dashboard.domain.MonthlyMessageUsage;
import com.researchi.admin.mailing.mapper.AdminMailSendJobMapper;
import com.researchi.admin.notification.mapper.AdminNotificationLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.text.NumberFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Service
public class DashboardUsageService {

    private static final int MONTH_RANGE = 12;
    private static final int LMS_FREE_COUNT = 10;
    private static final int LMS_UNIT_KRW = 30;
    private static final int EMAIL_FREE_COUNT = 1000;
    private static final double EMAIL_UNIT_KRW = 0.45;
    private static final DateTimeFormatter MONTH_KEY = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final NumberFormat KRW_FORMAT = NumberFormat.getIntegerInstance(Locale.KOREA);

    private final AdminMailSendJobMapper mailSendJobMapper;
    private final AdminNotificationLogMapper notificationLogMapper;

    public DashboardUsageService(
            AdminMailSendJobMapper mailSendJobMapper,
            AdminNotificationLogMapper notificationLogMapper
    ) {
        this.mailSendJobMapper = mailSendJobMapper;
        this.notificationLogMapper = notificationLogMapper;
    }

    public DashboardMessageUsage getMessageUsage() {
        YearMonth currentMonth = YearMonth.now();
        LocalDate startDate = currentMonth.minusMonths(MONTH_RANGE - 1L).atDay(1);
        Map<String, MutableUsage> usageMap = emptyUsageMap(currentMonth);

        applyEmailCounts(usageMap, mailSendJobMapper.countSentRecipientsByMonth(startDate));
        applySmsCounts(usageMap, notificationLogMapper.countSentSmsByMonth(startDate));

        List<MonthlyMessageUsage> monthlyUsages = usageMap.values().stream()
                .map(MutableUsage::toUsage)
                .toList();
        MonthlyMessageUsage currentUsage = monthlyUsages.get(monthlyUsages.size() - 1);
        int maxMonthlyTotal = monthlyUsages.stream()
                .mapToInt(MonthlyMessageUsage::totalCount)
                .max()
                .orElse(0);
        return new DashboardMessageUsage(
                currentUsage.emailCount(),
                currentUsage.smsCount(),
                currentUsage.emailCostLabel(),
                currentUsage.smsCostLabel(),
                currentUsage.totalCostLabel(),
                maxMonthlyTotal,
                monthlyUsages
        );
    }

    private Map<String, MutableUsage> emptyUsageMap(YearMonth currentMonth) {
        Map<String, MutableUsage> usageMap = new LinkedHashMap<>();
        for (int index = MONTH_RANGE - 1; index >= 0; index--) {
            YearMonth month = currentMonth.minusMonths(index);
            String key = month.format(MONTH_KEY);
            usageMap.put(key, new MutableUsage(key, month.getYear() + "." + String.format("%02d", month.getMonthValue())));
        }
        return usageMap;
    }

    private void applyEmailCounts(Map<String, MutableUsage> usageMap, List<MonthlyMessageCount> counts) {
        for (MonthlyMessageCount count : counts) {
            MutableUsage usage = usageMap.get(count.getMonthKey());
            if (usage != null) {
                usage.emailCount = count.getSentCount();
            }
        }
    }

    private void applySmsCounts(Map<String, MutableUsage> usageMap, List<MonthlyMessageCount> counts) {
        for (MonthlyMessageCount count : counts) {
            MutableUsage usage = usageMap.get(count.getMonthKey());
            if (usage != null) {
                usage.smsCount = count.getSentCount();
            }
        }
    }

    private static class MutableUsage {
        private final String monthKey;
        private final String monthLabel;
        private int emailCount;
        private int smsCount;

        private MutableUsage(String monthKey, String monthLabel) {
            this.monthKey = monthKey;
            this.monthLabel = monthLabel;
        }

        private MonthlyMessageUsage toUsage() {
            int smsBillableCount = Math.max(smsCount - LMS_FREE_COUNT, 0);
            int emailBillableCount = Math.max(emailCount - EMAIL_FREE_COUNT, 0);
            int emailCost = (int) Math.ceil(emailBillableCount * EMAIL_UNIT_KRW);
            int smsCost = smsBillableCount * LMS_UNIT_KRW;
            int totalCost = emailCost + smsCost;
            return new MonthlyMessageUsage(
                    monthKey,
                    monthLabel,
                    emailCount,
                    smsCount,
                    smsBillableCount,
                    formatKrw(emailCost),
                    formatKrw(smsCost),
                    formatKrw(totalCost)
            );
        }
    }

    private static String formatKrw(int amount) {
        return KRW_FORMAT.format(amount) + "원";
    }

}
