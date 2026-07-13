package com.researchi.admin.legacy.application.service;

import com.researchi.admin.legacy.application.domain.LegacyApplicationExtraAnswer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyApplicationExtraAnswerFormatterTest {

    @Test
    void formatAddsFallbackGroupForUngroupedAnswers() {
        LegacyApplicationExtraAnswer answer = answer(null, "Marital status", "Married");

        assertThat(LegacyApplicationExtraAnswerFormatter.format(List.of(answer)))
                .contains("Marital status: Married");
    }

    @Test
    void formatUsesCurrentResearchGroupForOldUngroupedRows() {
        LegacyApplicationExtraAnswer answer = answer(null, "Marital status", "Married");

        assertThat(LegacyApplicationExtraAnswerFormatter.format(List.of(answer), "Loan users"))
                .startsWith("[Loan users]")
                .contains("Marital status: Married");
    }

    @Test
    void formatTreatsStarQuestionAsGroupHeader() {
        LegacyApplicationExtraAnswer heading = answer(null, "* Loan users", "ignored");
        LegacyApplicationExtraAnswer answer = answer(null, "Phone model", "iPhone");

        assertThat(LegacyApplicationExtraAnswerFormatter.format(List.of(heading, answer)))
                .contains("[Loan users]")
                .contains("Phone model: iPhone")
                .doesNotContain("* Loan users: ignored");
    }

    @Test
    void formatKeepsSelectedGroupMarkerEvenWhenAnswerRowsArePartial() {
        LegacyApplicationExtraAnswer marker = answer("Loan users", LegacyApplicationExtraAnswerFormatter.GROUP_MARKER_LABEL, "Loan users");
        LegacyApplicationExtraAnswer answer = answer("Loan users", "Phone model", "iPhone");

        assertThat(LegacyApplicationExtraAnswerFormatter.format(List.of(marker, answer)))
                .startsWith("[Loan users]")
                .contains("Phone model: iPhone")
                .doesNotContain(LegacyApplicationExtraAnswerFormatter.GROUP_MARKER_LABEL);
    }

    private LegacyApplicationExtraAnswer answer(String group, String question, String value) {
        LegacyApplicationExtraAnswer answer = new LegacyApplicationExtraAnswer();
        answer.setQuestionGroup(group);
        answer.setQuestionLabel(question);
        answer.setAnswerText(value);
        return answer;
    }
}
