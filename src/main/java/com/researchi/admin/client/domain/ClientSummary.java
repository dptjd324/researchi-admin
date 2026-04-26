package com.researchi.admin.client.domain;

import java.util.List;

public record ClientSummary(
        Long id,
        String clientName,
        String departmentName,
        String primaryContactName,
        String primaryEmail,
        String replyToEmail,
        List<String> activeEmails,
        boolean active
) {
    public String emailsSummary() {
        return activeEmails == null || activeEmails.isEmpty() ? "" : String.join(", ", activeEmails);
    }
}
