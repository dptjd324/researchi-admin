package com.researchi.admin.mailing.domain;

import java.util.List;
import java.util.stream.Collectors;

public record MailingHistoryItem(
        AdminMailSendJob sendJob,
        List<AdminMailSendTarget> targets
) {

    public List<String> maskedRecipients() {
        return targets.stream()
                .map(AdminMailSendTarget::getTargetEmailMasked)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    public String maskedRecipientsSummary() {
        return maskedRecipients().stream().collect(Collectors.joining(", "));
    }

    public int uniqueApplicationCount() {
        return (int) targets.stream()
                .map(AdminMailSendTarget::getApplicationId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();
    }

    public String applicationSummary() {
        List<Long> applicationIds = targets.stream()
                .map(AdminMailSendTarget::getApplicationId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .limit(5)
                .toList();
        if (applicationIds.isEmpty()) {
            return "-";
        }
        String summary = applicationIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
        long total = targets.stream()
                .map(AdminMailSendTarget::getApplicationId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();
        return total > applicationIds.size() ? summary + " 외 " + (total - applicationIds.size()) + "건" : summary;
    }

    public String failReasonSummary() {
        return targets.stream()
                .map(AdminMailSendTarget::getFailReason)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .collect(Collectors.joining(" / "));
    }

    public boolean cancellable() {
        return sendJob != null && "SCHEDULED".equalsIgnoreCase(sendJob.getSendStatus());
    }
}
