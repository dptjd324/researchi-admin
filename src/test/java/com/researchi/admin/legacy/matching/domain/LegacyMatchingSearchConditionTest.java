package com.researchi.admin.legacy.matching.domain;

import com.researchi.admin.legacy.research.domain.ResearchApplication;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyMatchingSearchConditionTest {

    @Test
    void birthYearRangeExpandsToInclusiveYears() {
        LegacyMatchingSearchCondition condition = new LegacyMatchingSearchCondition(
                null,
                "1975-1977",
                null,
                null,
                null,
                null
        );

        assertThat(condition.getAppBirthYears()).containsExactly("1975", "1976", "1977");
        assertThat(condition.matchedFilters(application("19760101"))).hasSize(1);
        assertThat(condition.matchedFilters(application("19740101"))).isEmpty();
    }

    @Test
    void birthYearRangeMatchesLegacyTwoDigitBirthDates() {
        LegacyMatchingSearchCondition condition = new LegacyMatchingSearchCondition(
                null,
                "1975-1977",
                null,
                null,
                null,
                null
        );

        assertThat(condition.getAppBirthYearSuffixes()).containsExactly("75", "76", "77");
        assertThat(condition.matchedFilters(application("760101"))).hasSize(1);
        assertThat(condition.matchedFilters(application("740101"))).isEmpty();
    }

    @Test
    void birthYearRangeCanBeCombinedWithCommaSeparatedYears() {
        LegacyMatchingSearchCondition condition = new LegacyMatchingSearchCondition(
                null,
                "1975-1976, 1980",
                null,
                null,
                null,
                null
        );

        assertThat(condition.getAppBirthYears()).containsExactly("1975", "1976", "1980");
    }

    private ResearchApplication application(String birth) {
        ResearchApplication application = new ResearchApplication();
        application.setAppBirth(birth);
        return application;
    }
}
