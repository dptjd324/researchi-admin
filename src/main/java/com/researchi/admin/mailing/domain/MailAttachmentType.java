package com.researchi.admin.mailing.domain;

import java.util.Locale;

public enum MailAttachmentType {
    XLSX("XLSX"),
    TXT("TXT");

    private final String label;

    MailAttachmentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static MailAttachmentType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("첨부 형식을 선택해 주세요.");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("지원하지 않는 첨부 형식입니다.");
        }
    }
}
