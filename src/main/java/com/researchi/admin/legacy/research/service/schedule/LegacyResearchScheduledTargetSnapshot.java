package com.researchi.admin.legacy.research.service.schedule;

import java.util.List;

public record LegacyResearchScheduledTargetSnapshot(
        List<Long> applicationIds,
        List<Long> blacklistExcludedApplicationIds,
        List<Long> alreadyProvidedApplicationIds
) {
    public LegacyResearchScheduledTargetSnapshot(List<Long> applicationIds, List<Long> blacklistExcludedApplicationIds) {
        this(applicationIds, blacklistExcludedApplicationIds, List.of());
    }
}
