package com.researchi.admin.publicform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.public-form")
public class PublicFormProperties {

    public static final String DEFAULT_ENCRYPTION_KEY = "dev-only-phase5-key-change-me";
    public static final String DEFAULT_PHONE_HASH_KEY = "dev-only-phase5-phone-hash-key-change-me";

    private String encryptionKey = DEFAULT_ENCRYPTION_KEY;
    private String phoneHashKey = DEFAULT_PHONE_HASH_KEY;
    private boolean captchaEnabled = true;
    private int rateLimitCount = 5;
    private int rateLimitWindowSeconds = 300;

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public String getPhoneHashKey() {
        return phoneHashKey;
    }

    public void setPhoneHashKey(String phoneHashKey) {
        this.phoneHashKey = phoneHashKey;
    }

    public boolean isCaptchaEnabled() {
        return captchaEnabled;
    }

    public void setCaptchaEnabled(boolean captchaEnabled) {
        this.captchaEnabled = captchaEnabled;
    }

    public int getRateLimitCount() {
        return rateLimitCount;
    }

    public void setRateLimitCount(int rateLimitCount) {
        this.rateLimitCount = rateLimitCount;
    }

    public int getRateLimitWindowSeconds() {
        return rateLimitWindowSeconds;
    }

    public void setRateLimitWindowSeconds(int rateLimitWindowSeconds) {
        this.rateLimitWindowSeconds = rateLimitWindowSeconds;
    }
}
