package com.researchi.admin.client.domain;

import java.util.List;

public record ClientImpactSummary(
        Long clientId,
        int linkedJobCount,
        List<ClientImpactJob> linkedJobs
) {
    public boolean hasImpact() {
        return linkedJobCount > 0;
    }
}
