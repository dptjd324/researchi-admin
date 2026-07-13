package com.researchi.admin.legacy.blacklist.web;

import com.researchi.admin.legacy.research.domain.ResearchApplication;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyBlacklistFormTest {

    @Test
    void fromApplicantPrefillsActiveBlacklistFields() {
        ResearchApplication application = new ResearchApplication();
        application.setResearchNo(46433L);
        application.setResearchAppSeq(101L);
        application.setAppName("김d");
        application.setAppBirth("000112");
        application.setAppHphone("010-4156-3768");
        application.setAppTele("02-0000-0000");

        LegacyBlacklistForm form = LegacyBlacklistForm.fromApplicant(application);

        assertThat(form.getBlackYn()).isEqualTo("Y");
        assertThat(form.getBlackUserName()).isEqualTo("김d");
        assertThat(form.getBlackUserBirth()).isEqualTo("000112");
        assertThat(form.getBlackUserContact()).isEqualTo("010-4156-3768");
        assertThat(form.getBlackUserComment()).isEqualTo("신청자 조회에서 블랙 등록 (공고번호: 46433, 신청자번호: 101)");
    }

    @Test
    void fromApplicantUsesTelephoneWhenMobileIsBlank() {
        ResearchApplication application = new ResearchApplication();
        application.setResearchNo(46433L);
        application.setResearchAppSeq(102L);
        application.setAppName("테스터");
        application.setAppBirth("010530");
        application.setAppHphone(" ");
        application.setAppTele("02-1234-5678");

        LegacyBlacklistForm form = LegacyBlacklistForm.fromApplicant(application);

        assertThat(form.getBlackUserContact()).isEqualTo("02-1234-5678");
    }
}
