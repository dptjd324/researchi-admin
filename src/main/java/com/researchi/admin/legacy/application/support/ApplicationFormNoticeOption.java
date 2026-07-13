package com.researchi.admin.legacy.application.support;

public record ApplicationFormNoticeOption(String value, String label) {

    public static ApplicationFormNoticeOption fromAdminText(String text) {
        String normalized = text == null ? "" : text.trim();
        return new ApplicationFormNoticeOption(normalized, normalized);
    }

    public String displayValue() {
        return label == null || label.isBlank() ? value : label;
    }
}
