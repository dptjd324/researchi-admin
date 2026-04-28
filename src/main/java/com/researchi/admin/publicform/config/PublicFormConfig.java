package com.researchi.admin.publicform.config;

import com.researchi.admin.publicform.service.PublicFormProtectionService;
import org.springframework.core.env.Environment;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class PublicFormConfig {

    @Bean
    public PublicFormProtectionService publicFormProtectionService(PublicFormProperties properties, Environment environment) {
        validateEncryptionKey(properties, environment);
        return new PublicFormProtectionService(properties);
    }

    private void validateEncryptionKey(PublicFormProperties properties, Environment environment) {
        String encryptionKey = properties.getEncryptionKey();
        String phoneHashKey = properties.getPhoneHashKey();
        if (!PublicFormProperties.DEFAULT_ENCRYPTION_KEY.equals(encryptionKey)
                && !PublicFormProperties.DEFAULT_PHONE_HASH_KEY.equals(phoneHashKey)) {
            return;
        }
        if (isAwsRuntime(environment)) {
            throw new IllegalStateException("ENCRYPTION_KEY and PHONE_HASH_KEY must be set to production secrets before running on AWS.");
        }
        Set<String> activeProfiles = Set.of(environment.getActiveProfiles());
        boolean localProfile = activeProfiles.isEmpty()
                || activeProfiles.stream().anyMatch(profile -> Set.of("local", "dev", "test").contains(profile));
        if (localProfile) {
            return;
        }
        throw new IllegalStateException("ENCRYPTION_KEY and PHONE_HASH_KEY must be set to production secrets before running outside local/dev/test profiles.");
    }

    private boolean isAwsRuntime(Environment environment) {
        return hasText(environment.getProperty("AWS_EXECUTION_ENV"))
                || hasText(environment.getProperty("ECS_CONTAINER_METADATA_URI"))
                || hasText(environment.getProperty("ECS_CONTAINER_METADATA_URI_V4"))
                || hasText(environment.getProperty("EB_ENVIRONMENT_NAME"));
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
