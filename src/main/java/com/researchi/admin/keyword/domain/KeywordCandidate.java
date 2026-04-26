package com.researchi.admin.keyword.domain;

public record KeywordCandidate(
        String keyword,
        String normalized,
        String sourceType
) {
}
