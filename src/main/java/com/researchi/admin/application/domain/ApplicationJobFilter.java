package com.researchi.admin.application.domain;

public record ApplicationJobFilter(
        Long documentSrl,
        String jobTitle,
        long applicationCount
) {
}
