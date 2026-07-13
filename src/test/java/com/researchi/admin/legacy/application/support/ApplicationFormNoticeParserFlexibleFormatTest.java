package com.researchi.admin.legacy.application.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationFormNoticeParserFlexibleFormatTest {

    @Test
    void parseDetailsConvertsQuestionBlocksWithChoicesToRadioItems() {
        List<ApplicationFormNoticeItem> items = ApplicationFormNoticeParser.parseDetails("""
                질문1) Insurance type
                Employee
                Local
                Dependent

                질문2) Single/Married no child/Married with child 중 택 1
                """);

        assertThat(items).hasSize(2);
        assertThat(items.get(0).type()).isEqualTo("RADIO");
        assertThat(items.get(0).options())
                .extracting(ApplicationFormNoticeOption::label)
                .containsExactly("Employee", "Local", "Dependent");
        assertThat(items.get(1).type()).isEqualTo("RADIO");
        assertThat(items.get(1).options())
                .extracting(ApplicationFormNoticeOption::label)
                .containsExactly("Single", "Married no child", "Married with child");
    }

    @Test
    void parseDetailsHandlesCircledAndNestedFreeTextQuestions() {
        List<ApplicationFormNoticeItem> items = ApplicationFormNoticeParser.parseDetails("""
                ①Group
                ⑥Purchase month(ex 2025.12)
                7-1. Child age/gender
                10-1. Hair transplant date
                """);

        assertThat(items)
                .extracting(ApplicationFormNoticeItem::label)
                .containsExactly(
                        "Group",
                        "Purchase month(ex 2025.12)",
                        "Child age/gender",
                        "Hair transplant date"
                );
        assertThat(items)
                .extracting(ApplicationFormNoticeItem::type)
                .containsOnly("TEXT");
    }

    @Test
    void parseDetailsKeepsSingleContinuationAsQuestionText() {
        List<ApplicationFormNoticeItem> items = ApplicationFormNoticeParser.parseDetails("""
                4. Preferred whisky brands
                Also write cognac brands if any.
                """);

        assertThat(items).singleElement().satisfies(item -> {
            assertThat(item.type()).isEqualTo("TEXT");
            assertThat(item.label()).isEqualTo("Preferred whisky brands Also write cognac brands if any.");
        });
    }
}
