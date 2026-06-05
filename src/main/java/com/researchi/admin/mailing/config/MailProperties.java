package com.researchi.admin.mailing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.mail")
public class MailProperties {

    private String provider = "smtp";
    private boolean simulateSend = true;
    private String fromAddress = "no-reply@researchi.local";
    private String fromName = "Researchi";
    private Naver naver = new Naver();

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isSimulateSend() {
        return simulateSend;
    }

    public void setSimulateSend(boolean simulateSend) {
        this.simulateSend = simulateSend;
    }

    public String getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(String fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public Naver getNaver() {
        return naver;
    }

    public void setNaver(Naver naver) {
        this.naver = naver;
    }

    public static class Naver {
        private String baseUrl = "https://mail.apigw.ntruss.com/api/v1";
        private String accessKey = "";
        private String secretKey = "";
        private long maxFileSizeBytes = 10L * 1024L * 1024L;
        private long maxTotalAttachmentSizeBytes = 20L * 1024L * 1024L;
        private long maxBodySizeBytes = 500L * 1024L;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
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

        public long getMaxFileSizeBytes() {
            return maxFileSizeBytes;
        }

        public void setMaxFileSizeBytes(long maxFileSizeBytes) {
            this.maxFileSizeBytes = maxFileSizeBytes;
        }

        public long getMaxTotalAttachmentSizeBytes() {
            return maxTotalAttachmentSizeBytes;
        }

        public void setMaxTotalAttachmentSizeBytes(long maxTotalAttachmentSizeBytes) {
            this.maxTotalAttachmentSizeBytes = maxTotalAttachmentSizeBytes;
        }

        public long getMaxBodySizeBytes() {
            return maxBodySizeBytes;
        }

        public void setMaxBodySizeBytes(long maxBodySizeBytes) {
            this.maxBodySizeBytes = maxBodySizeBytes;
        }
    }
}
