package com.researchi.admin.web.support;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class UiLabelHelperTest {

    private final UiLabelHelper uiLabels = new UiLabelHelper();

    @Test
    void formatsLocalDateTimeWithKoreanUnits() {
        assertThat(uiLabels.dateTime(LocalDateTime.of(2026, 4, 16, 12, 5)))
                .isEqualTo("2026년 04월 16일 12시 05분");
    }

    @Test
    void formatsCompactDateTimeStringWithKoreanUnits() {
        assertThat(uiLabels.xeDateTime("20260415183227"))
                .isEqualTo("2026년 04월 15일 18시 32분");
    }

    @Test
    void formatsDateAndTimeWithKoreanUnits() {
        assertThat(uiLabels.date(LocalDate.of(2026, 4, 28))).isEqualTo("2026년 04월 28일");
        assertThat(uiLabels.time(LocalTime.of(18, 30))).isEqualTo("18시 30분");
    }

    @Test
    void keepsUnexpectedCompactDateTimeValueVisible() {
        assertThat(uiLabels.xeDateTime("20260415")).isEqualTo("20260415");
        assertThat(uiLabels.xeDateTime(null)).isEqualTo("-");
    }

    @Test
    void marksAnnouncementCreateUpdateDeleteActionTypes() {
        assertThat(uiLabels.actionTypeToneClass("JOB_CREATE")).isEqualTo("action-type-badge--job-change");
        assertThat(uiLabels.actionTypeToneClass("JOB_UPDATE")).isEqualTo("action-type-badge--job-change");
        assertThat(uiLabels.actionTypeToneClass("JOB_DELETE")).isEqualTo("action-type-badge--job-change");
        assertThat(uiLabels.actionTypeToneClass("LOGIN_SUCCESS")).isEmpty();
    }

    @Test
    void convertsHtmlContentToReadableText() {
        String html = "<p class=\"se-text-paragraph\"><span>첫 소식&nbsp;콘텐츠</span></p>"
                + "<p><span>&lt;지원&gt; 안녕</span></p>"
                + "<script>alert('x')</script>";

        assertThat(uiLabels.htmlToText(html)).isEqualTo("첫 소식 콘텐츠\n<지원> 안녕");
    }

    @Test
    void translatesActionLogValuesToKorean() {
        assertThat(uiLabels.actionType("APPLICATION_EXPORT")).isEqualTo("신청자 자료 내보내기");
        assertThat(uiLabels.targetType("RESEARCH")).isEqualTo("좌담회/설문");
        assertThat(uiLabels.logDetail("Exported LEGACY_RESEARCH_PROVIDE_TXT applications (0 rows)"))
                .isEqualTo("정보 제공 대상 텍스트 내보내기: 신청자 0건");
    }
}
