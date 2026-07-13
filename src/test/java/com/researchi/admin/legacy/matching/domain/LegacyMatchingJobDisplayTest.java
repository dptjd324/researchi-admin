package com.researchi.admin.legacy.matching.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyMatchingJobDisplayTest {

    @Test
    void matchingJobDisplaysStoredConditionAsReadableText() {
        LegacyMatchingSearchCondition condition = new LegacyMatchingSearchCondition(
                "\uB0A8\uC790",
                "1975-1985",
                null,
                null,
                null,
                "\uAE08\uC735"
        );
        LegacyMatchingJob job = new LegacyMatchingJob();
        job.setIncludeKeywordText(condition.storageKey());

        assertThat(job.getDisplayIncludeKeywordText())
                .isEqualTo("\uC131\uBCC4: \uB0A8\uC790, \uC0DD\uB144\uC6D4\uC77C: 1975-1985, \uCD94\uAC00\uAE30\uC7AC\uC0AC\uD56D: \uAE08\uC735");
    }

    @Test
    void matchingIndexJobDisplaysStoredConditionAsReadableText() {
        LegacyMatchingSearchCondition condition = new LegacyMatchingSearchCondition(
                "\uB0A8\uC790",
                "1975-1985",
                null,
                null,
                null,
                "\uAE08\uC735"
        );
        LegacyMatchingIndexJob job = new LegacyMatchingIndexJob();
        job.setIncludeKeywordText(condition.storageKey());

        assertThat(job.getDisplayIncludeKeywordText())
                .isEqualTo("\uC131\uBCC4: \uB0A8\uC790, \uC0DD\uB144\uC6D4\uC77C: 1975-1985, \uCD94\uAC00\uAE30\uC7AC\uC0AC\uD56D: \uAE08\uC735");
    }
}
