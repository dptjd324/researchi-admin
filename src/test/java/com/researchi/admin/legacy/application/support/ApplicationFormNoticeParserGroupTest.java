package com.researchi.admin.legacy.application.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationFormNoticeParserGroupTest {

    @Test
    void parseDetailsReadsNumberedGroups() {
        List<ApplicationFormNoticeItem> items = ApplicationFormNoticeParser.parseDetails("""
                [대출 서비스 이용자]
                1. 기혼/미혼 여부
                2. 사용 중인 금융 앱

                [대출 서비스 비이용자]
                1. 미이용 사유
                """);

        assertThat(items)
                .extracting(ApplicationFormNoticeItem::label)
                .containsExactly("기혼/미혼 여부", "사용 중인 금융 앱", "미이용 사유");
        assertThat(items)
                .extracting(ApplicationFormNoticeItem::groupLabel)
                .containsExactly("대출 서비스 이용자", "대출 서비스 이용자", "대출 서비스 비이용자");
    }
}
