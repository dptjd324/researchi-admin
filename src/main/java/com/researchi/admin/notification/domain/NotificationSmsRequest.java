package com.researchi.admin.notification.domain;

public record NotificationSmsRequest(
        String recipient,
        String message
) {
}
