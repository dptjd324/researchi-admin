package com.researchi.admin.mailing.domain;

import java.util.stream.Collectors;
import java.util.List;

public record MailingPreview(
        Long documentSrl,
        String jobTitle,
        List<String> recipients,
        int recipientCount,
        int excludedRecipientCount,
        int eligibleApplicationCount,
        int blacklistExcludedCount
) {
    public String recipientsSummary() {
        return recipients == null || recipients.isEmpty()
                ? ""
                : recipients.stream().collect(Collectors.joining(", "));
    }
}
