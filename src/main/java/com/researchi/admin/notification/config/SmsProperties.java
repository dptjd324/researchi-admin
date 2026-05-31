package com.researchi.admin.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.sms")
public class SmsProperties {

    private boolean simulateSend = true;
    private String senderId = "researchi";
    private String provider = "naver-sens";
    private String accessKey;
    private String secretKey;
    private String serviceId;
    private String fromNumber;
    private String baseUrl = "https://sens.apigw.ntruss.com";
    private String messageType = "LMS";
    private int dailySendLimit = 500;
    private int monthlySendLimit = 10000;

    public boolean isSimulateSend() {
        return simulateSend;
    }

    public void setSimulateSend(boolean simulateSend) {
        this.simulateSend = simulateSend;
    }

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getAccessKey() {
        return accessKey;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getFromNumber() {
        return fromNumber;
    }

    public void setFromNumber(String fromNumber) {
        this.fromNumber = fromNumber;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public int getDailySendLimit() {
        return dailySendLimit;
    }

    public void setDailySendLimit(int dailySendLimit) {
        this.dailySendLimit = dailySendLimit;
    }

    public int getMonthlySendLimit() {
        return monthlySendLimit;
    }

    public void setMonthlySendLimit(int monthlySendLimit) {
        this.monthlySendLimit = monthlySendLimit;
    }
}
