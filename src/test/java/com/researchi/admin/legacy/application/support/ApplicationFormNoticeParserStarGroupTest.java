package com.researchi.admin.legacy.application.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationFormNoticeParserStarGroupTest {

    @Test
    void parseDetailsReadsStarGroupHeadings() {
        List<ApplicationFormNoticeItem> items = ApplicationFormNoticeParser.parseDetails("""
                * Loan users
                1. Marital status
                2. Phone model
                """);

        assertThat(items)
                .extracting(ApplicationFormNoticeItem::label)
                .containsExactly("Marital status", "Phone model");
        assertThat(items)
                .extracting(ApplicationFormNoticeItem::groupLabel)
                .containsExactly("Loan users", "Loan users");
    }
}
