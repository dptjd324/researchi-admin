package com.researchi.admin.blacklist.service;

import com.researchi.admin.publicform.domain.AdminBlacklist;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class BlacklistModePolicy {

    public static final String PERMANENT_BLOCK = "PERMANENT_BLOCK";
    public static final String TEMPORARY_BLOCK = "TEMPORARY_BLOCK";
    public static final String MANUAL_REVIEW = "MANUAL_REVIEW";

    private static final List<String> ALLOWED_MODES = List.of(
            PERMANENT_BLOCK,
            TEMPORARY_BLOCK,
            MANUAL_REVIEW
    );

    private BlacklistModePolicy() {
    }

    public static List<String> allowedModes() {
        return ALLOWED_MODES;
    }

    public static String normalize(String mode) {
        if (mode == null) {
            return null;
        }
        String normalized = mode.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "BLOCK", "PERMANENT", PERMANENT_BLOCK -> PERMANENT_BLOCK;
            case "TEMP", "TEMPORARY", TEMPORARY_BLOCK -> TEMPORARY_BLOCK;
            case "HOLD", "MANUAL", "MANUAL_HOLD", MANUAL_REVIEW -> MANUAL_REVIEW;
            default -> normalized;
        };
    }

    public static AdminBlacklist effectiveMatch(List<AdminBlacklist> matches) {
        return matches.stream()
                .max(Comparator.comparingInt(match -> priority(normalize(match.getBlackMode()))))
                .orElse(null);
    }

    public static String actionTaken(String mode) {
        return switch (normalize(mode)) {
            case MANUAL_REVIEW -> "MANUAL_REVIEW";
            case TEMPORARY_BLOCK -> "TEMPORARY_BLOCKED";
            default -> "PERMANENT_BLOCKED";
        };
    }

    public static String applicationStatus(String mode) {
        return switch (normalize(mode)) {
            case MANUAL_REVIEW -> "REVIEWING";
            case TEMPORARY_BLOCK, PERMANENT_BLOCK -> "BLOCKED";
            default -> "BLOCKED";
        };
    }

    private static int priority(String mode) {
        return switch (mode) {
            case PERMANENT_BLOCK -> 3;
            case TEMPORARY_BLOCK -> 2;
            case MANUAL_REVIEW -> 1;
            default -> 0;
        };
    }
}
