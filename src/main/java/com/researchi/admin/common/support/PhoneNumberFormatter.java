package com.researchi.admin.common.support;

public final class PhoneNumberFormatter {

    private PhoneNumberFormatter() {
    }

    public static String formatForDisplay(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }

        String digits = trimmed.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return null;
        }
        if (digits.length() == 11 && digits.startsWith("010")) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7);
        }
        if (digits.length() == 11) {
            return digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7);
        }
        if (digits.length() == 10) {
            if (digits.startsWith("02")) {
                return digits.substring(0, 2) + "-" + digits.substring(2, 6) + "-" + digits.substring(6);
            }
            return digits.substring(0, 3) + "-" + digits.substring(3, 6) + "-" + digits.substring(6);
        }
        if (digits.length() == 9 && digits.startsWith("02")) {
            return digits.substring(0, 2) + "-" + digits.substring(2, 5) + "-" + digits.substring(5);
        }
        if (digits.length() == 8) {
            return digits.substring(0, 4) + "-" + digits.substring(4);
        }
        return trimmed;
    }

    public static String digitsOnly(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String digits = trimmed.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
