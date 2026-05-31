package com.researchi.admin.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.notification")
public class NotificationProperties {

    private String baseUrl = "http://localhost:8082";
    private String recommendationTemplateName = "일감 추천 알림";
    private String recommendationEmailSubject = "[Researchi] 새로운 일감을 추천드립니다";
    private String recommendationEmailBody = """
            안녕하세요 {{applicantName}}님,

            새로 등록된 일감이 신청자님의 관심 키워드와 잘 맞아 추천드립니다.

            추천 일감: {{jobTitle}}
            관련 키워드: {{keywordSummary}}
            신청 링크: {{applyUrl}}

            관심 있으시면 링크에서 자세한 내용을 확인해 주세요.
            """;
    private String recommendationSmsMessage = "[Researchi] 새로운 일감을 추천드립니다. {{jobTitle}} / 신청: {{applyUrl}}";
    private String legacyMatchingSmsMessage = """
            [Researchi] \uc88c\ub2f4\ud68c/\uc124\ubb38 \ucc38\uc5ec \uc548\ub0b4

            {{researchTitle}}

            \uc870\uac74\uc5d0 \ub9de\ub294 \uc88c\ub2f4\ud68c/\uc124\ubb38\uc774 \uc788\uc5b4 \uc548\ub0b4\ub4dc\ub9bd\ub2c8\ub2e4.
            \uc2e0\uccad \ub9c1\ud06c: {{applyUrl}}
            {{keywordLine}}
            """;
    private String legacyMatchingEmailSubject = "[Researchi] {{researchTitle}}";
    private String legacyMatchingEmailBody = """
            \uc548\ub155\ud558\uc138\uc694.

            \uc870\uac74\uc5d0 \ub9de\ub294 \uc88c\ub2f4\ud68c/\uc124\ubb38\uc774 \uc788\uc5b4 \uc548\ub0b4\ub4dc\ub9bd\ub2c8\ub2e4.

            \uc88c\ub2f4\ud68c/\uc124\ubb38: {{researchTitle}}
            \uc2e0\uccad \ub9c1\ud06c: {{applyUrl}}
            {{keywordLine}}

            \ucc38\uc5ec\ub97c \uc6d0\ud558\uc2dc\uba74 \uc704 \ub9c1\ud06c\uc5d0\uc11c \uc790\uc138\ud55c \ub0b4\uc6a9\uc744 \ud655\uc778\ud574 \uc8fc\uc138\uc694.
            """;
    private boolean secondaryEmailEnabled = false;
    private String secondaryEmailSubject = "[Researchi] 추천 일감 다시 안내드립니다";

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getRecommendationTemplateName() {
        return recommendationTemplateName;
    }

    public void setRecommendationTemplateName(String recommendationTemplateName) {
        this.recommendationTemplateName = recommendationTemplateName;
    }

    public String getRecommendationEmailSubject() {
        return recommendationEmailSubject;
    }

    public void setRecommendationEmailSubject(String recommendationEmailSubject) {
        this.recommendationEmailSubject = recommendationEmailSubject;
    }

    public String getRecommendationEmailBody() {
        return recommendationEmailBody;
    }

    public void setRecommendationEmailBody(String recommendationEmailBody) {
        this.recommendationEmailBody = recommendationEmailBody;
    }

    public String getRecommendationSmsMessage() {
        return recommendationSmsMessage;
    }

    public void setRecommendationSmsMessage(String recommendationSmsMessage) {
        this.recommendationSmsMessage = recommendationSmsMessage;
    }

    public String getLegacyMatchingSmsMessage() {
        return legacyMatchingSmsMessage;
    }

    public void setLegacyMatchingSmsMessage(String legacyMatchingSmsMessage) {
        this.legacyMatchingSmsMessage = legacyMatchingSmsMessage;
    }

    public String getLegacyMatchingEmailSubject() {
        return legacyMatchingEmailSubject;
    }

    public void setLegacyMatchingEmailSubject(String legacyMatchingEmailSubject) {
        this.legacyMatchingEmailSubject = legacyMatchingEmailSubject;
    }

    public String getLegacyMatchingEmailBody() {
        return legacyMatchingEmailBody;
    }

    public void setLegacyMatchingEmailBody(String legacyMatchingEmailBody) {
        this.legacyMatchingEmailBody = legacyMatchingEmailBody;
    }

    public boolean isSecondaryEmailEnabled() {
        return secondaryEmailEnabled;
    }

    public void setSecondaryEmailEnabled(boolean secondaryEmailEnabled) {
        this.secondaryEmailEnabled = secondaryEmailEnabled;
    }

    public String getSecondaryEmailSubject() {
        return secondaryEmailSubject;
    }

    public void setSecondaryEmailSubject(String secondaryEmailSubject) {
        this.secondaryEmailSubject = secondaryEmailSubject;
    }
}
