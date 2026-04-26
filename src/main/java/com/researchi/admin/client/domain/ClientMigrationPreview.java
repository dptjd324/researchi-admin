package com.researchi.admin.client.domain;

import java.util.List;

public record ClientMigrationPreview(
        int candidateCount,
        List<ClientMigrationCandidate> candidates
) {
    public boolean hasCandidates() {
        return candidateCount > 0;
    }
}
