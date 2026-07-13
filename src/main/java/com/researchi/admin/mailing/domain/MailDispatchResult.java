package com.researchi.admin.mailing.domain;

import java.time.LocalDateTime;

public record MailDispatchResult(
        String provider,
        String providerRequestId,
        String providerStatusCode,
        String providerStatusLabel,
        String providerRawResponse,
        LocalDateTime providerRequestedAt
) {

    public static MailDispatchResult simulated() {
        return new MailDispatchResult("simulated", null, "SIMULATED", "Simulated send", null, LocalDateTime.now());
    }

    public static MailDispatchResult smtp() {
        return new MailDispatchResult("smtp", null, "ACCEPTED", "SMTP accepted", null, LocalDateTime.now());
    }
}
