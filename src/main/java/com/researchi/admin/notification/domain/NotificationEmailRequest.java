package com.researchi.admin.notification.domain;

public record NotificationEmailRequest(
        String recipient,
        String subject,
        String body
) {
}
