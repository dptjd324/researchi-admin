package com.researchi.admin.common.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneNumberFormatterTest {

    @Test
    void formatForDisplayAddsMobileHyphens() {
        assertThat(PhoneNumberFormatter.formatForDisplay("01031442009")).isEqualTo("010-3144-2009");
        assertThat(PhoneNumberFormatter.formatForDisplay("010-3144-2009")).isEqualTo("010-3144-2009");
    }

    @Test
    void formatForDisplayKeepsSupportedLandlineShapes() {
        assertThat(PhoneNumberFormatter.formatForDisplay("0212345678")).isEqualTo("02-1234-5678");
        assertThat(PhoneNumberFormatter.formatForDisplay("021234567")).isEqualTo("02-123-4567");
    }
}
