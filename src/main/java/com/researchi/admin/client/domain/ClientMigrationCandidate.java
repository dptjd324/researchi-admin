package com.researchi.admin.client.domain;

public record ClientMigrationCandidate(
        Long documentSrl,
        String jobTitle,
        String legacyClientName,
        String legacyPrimaryEmail,
        String legacyAdditionalEmails
) {
}
