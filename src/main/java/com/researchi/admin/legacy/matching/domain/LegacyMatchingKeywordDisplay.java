package com.researchi.admin.legacy.matching.domain;

import java.util.List;

final class LegacyMatchingKeywordDisplay {

    private LegacyMatchingKeywordDisplay() {
    }

    static String displayIncludeKeywordText(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        try {
            LegacyMatchingSearchCondition condition = LegacyMatchingSearchCondition.fromStorageKey(trimmed);
            List<String> filters = condition.displayFilters();
            if (!filters.isEmpty()) {
                return String.join(", ", filters);
            }
        } catch (RuntimeException ignored) {
            return trimmed;
        }
        return trimmed;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
