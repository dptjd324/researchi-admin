package com.researchi.admin.legacy.research.service.mail;

import java.util.List;

public record LegacyResearchMailSnapshot(List<Long> applicationIds, int blacklistExcludedCount) {
}
