package com.researchi.admin.legacy.application.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyApplicationConsentTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 7, 13, 12, 0);

    @Test
    void activeFutureRecruitmentRequiresConsentBeforeExpiryWithoutWithdrawal() {
        LegacyApplicationConsent consent = activeConsent();

        assertThat(consent.activeFutureRecruitmentAt(NOW)).isTrue();

        consent.setFutureRecruitmentYn("N");
        assertThat(consent.activeFutureRecruitmentAt(NOW)).isFalse();

        consent.setFutureRecruitmentYn("Y");
        consent.setFutureConsentExpiresAt(NOW);
        assertThat(consent.activeFutureRecruitmentAt(NOW)).isFalse();

        consent.setFutureConsentExpiresAt(NOW.plusDays(1));
        consent.setWithdrawnAt(NOW.minusMinutes(1));
        assertThat(consent.activeFutureRecruitmentAt(NOW)).isFalse();

        consent.setWithdrawnAt(null);
        consent.setSmsYn("N");
        consent.setEmailYn("N");
        assertThat(consent.activeFutureRecruitmentAt(NOW)).isFalse();
    }

    @Test
    void channelConsentRequiresActiveFutureRecruitmentAndMatchingChannelFlag() {
        LegacyApplicationConsent consent = activeConsent();

        assertThat(consent.allowsSmsAt(NOW)).isTrue();
        assertThat(consent.allowsEmailAt(NOW)).isTrue();

        consent.setSmsYn("N");
        assertThat(consent.allowsSmsAt(NOW)).isFalse();
        assertThat(consent.allowsEmailAt(NOW)).isTrue();

        consent.setSmsYn("Y");
        consent.setEmailYn("N");
        assertThat(consent.allowsSmsAt(NOW)).isTrue();
        assertThat(consent.allowsEmailAt(NOW)).isFalse();

        consent.setWithdrawnAt(NOW);
        assertThat(consent.allowsSmsAt(NOW)).isFalse();
        assertThat(consent.allowsEmailAt(NOW)).isFalse();
    }

    private LegacyApplicationConsent activeConsent() {
        LegacyApplicationConsent consent = new LegacyApplicationConsent();
        consent.setFutureRecruitmentYn("Y");
        consent.setSmsYn("Y");
        consent.setEmailYn("Y");
        consent.setFutureConsentExpiresAt(NOW.plusYears(1));
        return consent;
    }
}
