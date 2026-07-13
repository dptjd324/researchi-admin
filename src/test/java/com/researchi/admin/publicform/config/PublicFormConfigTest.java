package com.researchi.admin.publicform.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublicFormConfigTest {

    private final PublicFormConfig config = new PublicFormConfig();

    @Test
    void rejectsDefaultSecretsWhenNoProfileIsActive() {
        PublicFormProperties properties = new PublicFormProperties();

        assertThatThrownBy(() -> config.publicFormProtectionService(properties, new MockEnvironment()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ENCRYPTION_KEY and PHONE_HASH_KEY");
    }

    @Test
    void allowsDefaultSecretsForExplicitLocalProfile() {
        PublicFormProperties properties = new PublicFormProperties();
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");

        assertThatCode(() -> config.publicFormProtectionService(properties, environment))
                .doesNotThrowAnyException();
    }
}
