package com.researchi.admin.publicform.service;

import com.researchi.admin.publicform.config.PublicFormProperties;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class PublicFormProtectionServiceTest {

    @Test
    void rateLimitAllowsKeyAgainAfterWindowAndCleansExpiredSlots() {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setRateLimitCount(1);
        properties.setRateLimitWindowSeconds(10);
        MutableClock clock = new MutableClock(Instant.parse("2026-04-28T00:00:00Z"));
        PublicFormProtectionService service = new PublicFormProtectionService(properties, clock, new SecureRandom());

        assertThat(service.tryAcquireRateLimitSlot("apply:127.0.0.1")).isTrue();
        assertThat(service.tryAcquireRateLimitSlot("apply:127.0.0.1")).isFalse();

        clock.current = Instant.parse("2026-04-28T00:00:11Z");

        assertThat(service.tryAcquireRateLimitSlot("apply:127.0.0.1")).isTrue();
    }

    private static class MutableClock extends Clock {
        private Instant current;

        private MutableClock(Instant current) {
            this.current = current;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
