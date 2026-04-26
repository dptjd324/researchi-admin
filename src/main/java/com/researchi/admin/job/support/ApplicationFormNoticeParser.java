package com.researchi.admin.job.support;

import java.util.List;
import java.util.regex.Pattern;

public final class ApplicationFormNoticeParser {

    private static final Pattern SPLIT_PATTERN = Pattern.compile("[\\r\\n]+|\\s*/\\s*");

    private ApplicationFormNoticeParser() {
    }

    public static List<String> parseItems(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return List.of();
        }
        return SPLIT_PATTERN.splitAsStream(rawValue)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
