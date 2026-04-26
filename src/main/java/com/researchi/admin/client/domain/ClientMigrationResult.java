package com.researchi.admin.client.domain;

public record ClientMigrationResult(
        int migratedJobCount,
        int createdClientCount,
        int reusedClientCount
) {
}
