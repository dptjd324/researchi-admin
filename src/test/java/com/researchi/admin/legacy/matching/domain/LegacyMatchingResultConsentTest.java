package com.researchi.admin.legacy.matching.domain;

import com.researchi.admin.legacy.research.domain.ResearchApplication;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyMatchingResultConsentTest {

    @Test
    void consentStatusIsIndependentFromSentStatus() {
        LegacyMatchingResult result = new LegacyMatchingResult(
                1,
                new ResearchApplication(),
                2,
                List.of("조건"),
                List.of()
        ).withConsentStatus(true, false).withNotificationStatus(false, true);

        assertThat(result.smsAllowed()).isTrue();
        assertThat(result.emailAllowed()).isFalse();
        assertThat(result.smsSent()).isFalse();
        assertThat(result.emailSent()).isTrue();
    }
}
