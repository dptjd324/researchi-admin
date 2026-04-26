package com.researchi.admin.keyword.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.keyword")
public class KeywordProperties {

    private int minLength = 2;
    private int maxLength = 40;
    private int maxKeywordsPerSource = 40;

    public int getMinLength() {
        return minLength;
    }

    public void setMinLength(int minLength) {
        this.minLength = minLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public int getMaxKeywordsPerSource() {
        return maxKeywordsPerSource;
    }

    public void setMaxKeywordsPerSource(int maxKeywordsPerSource) {
        this.maxKeywordsPerSource = maxKeywordsPerSource;
    }
}
