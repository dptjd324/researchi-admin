package com.researchi.admin.legacy.research.service.recipient;

import java.util.List;

public record LegacyResearchRecipientSelection(List<String> recipients, int excludedCount, String targetName) {
}
