package com.researchi.admin.legacy.application.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationFormNoticeParserTest {

    @Test
    void parseDetailsKeepsLegacyItemsAsText() {
        List<ApplicationFormNoticeItem> items = ApplicationFormNoticeParser.parseDetails("결혼여부/자녀유무\n알러지 유무");

        assertThat(items)
                .extracting(ApplicationFormNoticeItem::label)
                .containsExactly("결혼여부", "자녀유무", "알러지 유무");
        assertThat(items)
                .extracting(ApplicationFormNoticeItem::type)
                .containsOnly("TEXT");
    }

    @Test
    void parseDetailsReadsTypedOptions() {
        String value = ApplicationFormNoticeParser.serializeItem(
                "참석 가능 시간",
                "SELECT",
                List.of(
                        ApplicationFormNoticeOption.fromAdminText("평일"),
                        ApplicationFormNoticeOption.fromAdminText("주말")
                )
        );

        List<ApplicationFormNoticeItem> items = ApplicationFormNoticeParser.parseDetails(value);

        assertThat(items).singleElement().satisfies(item -> {
            assertThat(item.label()).isEqualTo("참석 가능 시간");
            assertThat(item.type()).isEqualTo("SELECT");
            assertThat(item.options()).containsExactly(
                    ApplicationFormNoticeOption.fromAdminText("평일"),
                    ApplicationFormNoticeOption.fromAdminText("주말")
            );
        });
    }

    @Test
    void parseDetailsKeepsOldValueLabelOptionsAsDisplayText() {
        List<ApplicationFormNoticeItem> items = ApplicationFormNoticeParser.parseDetails("참석 가능 시간|SELECT|weekday=평일;weekend=주말");

        assertThat(items).singleElement().satisfies(item -> assertThat(item.options()).containsExactly(
                ApplicationFormNoticeOption.fromAdminText("평일"),
                ApplicationFormNoticeOption.fromAdminText("주말")
        ));
    }
    @Test
    void parseDetailsReadsCheckboxOptions() {
        List<ApplicationFormNoticeItem> items = ApplicationFormNoticeParser.parseDetails("외모 관리 경험|CHECKBOX|화장품 비교;피부과 비교;기타");

        assertThat(items).singleElement().satisfies(item -> {
            assertThat(item.type()).isEqualTo("CHECKBOX");
            assertThat(item.options()).containsExactly(
                    ApplicationFormNoticeOption.fromAdminText("화장품 비교"),
                    ApplicationFormNoticeOption.fromAdminText("피부과 비교"),
                    ApplicationFormNoticeOption.fromAdminText("기타")
            );
        });
    }
}
