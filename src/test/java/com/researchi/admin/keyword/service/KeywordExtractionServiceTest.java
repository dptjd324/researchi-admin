package com.researchi.admin.keyword.service;

import com.researchi.admin.application.domain.ApplicationExtraAnswerItem;
import com.researchi.admin.application.domain.ApplicationRecord;
import com.researchi.admin.job.domain.AdminJobMeta;
import com.researchi.admin.job.support.ApplicationFormNoticeOption;
import com.researchi.admin.job.support.ApplicationFormNoticeParser;
import com.researchi.admin.keyword.config.KeywordProperties;
import com.researchi.admin.keyword.domain.KeywordCandidate;
import com.researchi.admin.xe.domain.XeJobDocument;
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

    @Test
    void jobKeywordsComeFromTitleContentRegionAndExtraQuestionTitlesOnly() {
        KeywordExtractionService service = new KeywordExtractionService(
                new KeywordProperties(),
                null,
                null,
                null,
                null,
                null
        );
        XeJobDocument jobDocument = new XeJobDocument();
        jobDocument.setTitle("coffee survey");
        jobDocument.setContent("panel interview");
        AdminJobMeta meta = new AdminJobMeta();
        meta.setRegionText("Seoul");
        meta.setRewardText("reward should not be indexed");
        meta.setApplicationFormNotice(String.join("\n",
                ApplicationFormNoticeParser.serializeItem(
                        "참석 가능 시간",
                        "SELECT",
                        List.of(
                                ApplicationFormNoticeOption.fromAdminText("평일"),
                                ApplicationFormNoticeOption.fromAdminText("주말")
                        )
                ),
                ApplicationFormNoticeParser.serializeItem("구매 횟수", "NUMBER", List.of())
        ));

        List<KeywordCandidate> keywords = service.extractJobKeywords(jobDocument, meta);

        assertThat(keywords)
                .extracting(KeywordCandidate::normalized)
                .contains("coffee", "survey", "panel", "interview", "seoul", "참석", "가능", "시간", "구매", "횟수")
                .doesNotContain("평일", "주말", "reward");
        assertThat(keywords)
                .extracting(KeywordCandidate::sourceType)
                .contains("JOB_TITLE", "JOB_CONTENT", "JOB_REGION", "JOB_EXTRA_TITLE");
    }
}
