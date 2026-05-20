package com.researchi.admin.mailing.domain;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public record  MailingHistoryItem(
        AdminMailSendJob sendJob,
        List<AdminMailSendTarget> targets,
        List<String> recipientAddresses,
        int cumulativeSentCount
) {

    public MailingHistoryItem(AdminMailSendJob sendJob, List<AdminMailSendTarget> targets) {
        this(sendJob, targets, recipientAddressesFromTargets(targets), 0);
    }

    public MailingHistoryItem(AdminMailSendJob sendJob, List<AdminMailSendTarget> targets, List<String> recipientAddresses) {
        this(sendJob, targets, recipientAddresses, 0);
    }

    public MailingHistoryItem {
        targets = targets == null ? List.of() : List.copyOf(targets);
        recipientAddresses = recipientAddresses == null ? List.of() : List.copyOf(recipientAddresses);
    }

    public static List<String> recipientAddressesFromTargets(List<AdminMailSendTarget> targets) {
        if (targets == null || targets.isEmpty()) {
            return List.of();
        }
        return targets.stream()
                .map(AdminMailSendTarget::getTargetEmailMasked)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
    }

    public String recipientAddressesSummary() {
        return recipientAddresses().stream().collect(Collectors.joining(", "));
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
        String summary = targets.stream()
                .map(AdminMailSendTarget::getFailReason)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .collect(Collectors.joining(" / "));
        if (!summary.isBlank()) {
            return summary;
        }
        if (failed()) {
            return "발송 가능한 지원자(PROVIDE_YN=N)가 없거나 발송 처리 중 오류가 발생했습니다.";
        }
        return "";
    }

    public LocalDateTime activityAt() {
        if (sendJob == null) {
            return null;
        }
        if (sendJob.getSentAt() != null) {
            return sendJob.getSentAt();
        }
        if ("SCHEDULED".equalsIgnoreCase(sendJob.getSendStatus())) {
            return sendJob.getScheduledAt() != null ? sendJob.getScheduledAt() : sendJob.getCreatedAt();
        }
        return sendJob.getCreatedAt() != null ? sendJob.getCreatedAt() : sendJob.getScheduledAt();
    }

    public boolean failed() {
        return sendJob != null && ("FAILED".equalsIgnoreCase(sendJob.getSendStatus())
                || "NO_TARGETS".equalsIgnoreCase(sendJob.getSendStatus()));
    }

    public int provisionCompletedCount() {
        if (sendJob == null || sendJob.getTriggerType() == null || !sendJob.getTriggerType().startsWith("LEGACY_")) {
            return 0;
        }
        if (!"SENT".equalsIgnoreCase(sendJob.getSendStatus())) {
            return 0;
        }
        return (int) targets.stream()
                .filter(target -> "SENT".equalsIgnoreCase(target.getSendResult()))
                .map(AdminMailSendTarget::getApplicationId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();
    }

    public boolean cancellable() {
        return sendJob != null && "SCHEDULED".equalsIgnoreCase(sendJob.getSendStatus());
    }
}
