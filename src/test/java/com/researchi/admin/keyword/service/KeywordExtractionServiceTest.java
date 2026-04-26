package com.researchi.admin.keyword.service;

import com.researchi.admin.application.domain.ApplicationExtraAnswerItem;
import com.researchi.admin.application.domain.ApplicationRecord;
import com.researchi.admin.keyword.config.KeywordProperties;
import com.researchi.admin.keyword.domain.KeywordCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordExtractionServiceTest {

    @Test
    void applicationKeywordsIncludeNinthExtraAnswerText() {
        KeywordExtractionService service = new KeywordExtractionService(
                new KeywordProperties(),
                null,
                null,
                null,
                null,
                null
        );
        ApplicationRecord application = new ApplicationRecord();
        ApplicationExtraAnswerItem extraAnswer = new ApplicationExtraAnswerItem();
        extraAnswer.setAnswerText("coffee survey panel");

        List<KeywordCandidate> keywords = service.extractApplicationKeywords(
                application,
                List.of(),
                List.of(extraAnswer)
        );

        assertThat(keywords)
                .extracting(KeywordCandidate::normalized)
                .contains("coffee", "survey", "panel");
        assertThat(keywords)
                .filteredOn(keyword -> "coffee".equals(keyword.normalized()))
                .singleElement()
                .extracting(KeywordCandidate::sourceType)
                .isEqualTo("APPLICATION_EXTRA_ANSWER");
    }
}
