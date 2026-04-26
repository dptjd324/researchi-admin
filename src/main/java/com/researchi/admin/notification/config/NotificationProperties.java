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
