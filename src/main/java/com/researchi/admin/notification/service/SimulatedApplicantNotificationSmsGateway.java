package com.researchi.admin.notification.service;

import com.researchi.admin.notification.config.SmsProperties;
import com.researchi.admin.notification.domain.NotificationSmsRequest;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Component
public class SimulatedApplicantNotificationSmsGateway implements ApplicantNotificationSmsGateway {

    private final SmsProperties smsProperties;
    private final HttpClient httpClient;

    public SimulatedApplicantNotificationSmsGateway(SmsProperties smsProperties) {
        this.smsProperties = smsProperties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public void dispatch(NotificationSmsRequest request) throws Exception {
        if (!smsProperties.isSimulateSend()) {
            dispatchNaverSens(request);
        }
    }

    private void dispatchNaverSens(NotificationSmsRequest request) throws Exception {
        if (!"naver-sens".equalsIgnoreCase(trimToEmpty(smsProperties.getProvider()))) {
            throw new UnsupportedOperationException("지원하지 않는 SMS 발송 제공자입니다.");
        }
        validateNaverSensConfiguration();

        String uri = "/sms/v2/services/" + smsProperties.getServiceId().trim() + "/messages";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String payload = buildPayload(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(trimTrailingSlash(smsProperties.getBaseUrl()) + uri))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json; charset=utf-8")
                .header("x-ncp-apigw-timestamp", timestamp)
                .header("x-ncp-iam-access-key", smsProperties.getAccessKey().trim())
                .header("x-ncp-apigw-signature-v2", signature(timestamp, uri))
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("SMS 발송 요청이 실패했습니다. 상태 코드: " + response.statusCode());
        }
    }

    private void validateNaverSensConfiguration() {
        if (isBlank(smsProperties.getAccessKey())
                || isBlank(smsProperties.getSecretKey())
                || isBlank(smsProperties.getServiceId())
                || isBlank(smsProperties.getFromNumber())) {
            throw new IllegalStateException("SMS 발송 설정이 완료되지 않았습니다. APP_SMS_SIMULATE_SEND=false 사용 시 APP_SMS_ACCESS_KEY, APP_SMS_SECRET_KEY, APP_SMS_SERVICE_ID, APP_SMS_FROM_NUMBER 값을 설정해 주세요.");
        }
    }

    private String buildPayload(NotificationSmsRequest request) {
        String message = escapeJson(request.message());
        return "{"
                + "\"type\":\"" + escapeJson(blankToDefault(smsProperties.getMessageType(), "LMS")) + "\","
                + "\"from\":\"" + escapeJson(normalizePhone(smsProperties.getFromNumber())) + "\","
                + "\"content\":\"" + message + "\","
                + "\"messages\":[{"
                + "\"to\":\"" + escapeJson(normalizePhone(request.recipient())) + "\","
                + "\"content\":\"" + message + "\""
                + "}]"
                + "}";
    }

    private String signature(String timestamp, String uri) throws Exception {
        String message = "POST " + uri + "\n" + timestamp + "\n" + smsProperties.getAccessKey().trim();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(smsProperties.getSecretKey().trim().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
    }

    private String normalizePhone(String value) {
        return trimToEmpty(value).replaceAll("[^0-9]", "");
    }

    private String trimTrailingSlash(String value) {
        String trimmed = blankToDefault(value, "https://sens.apigw.ntruss.com").trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private String blankToDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (current < 0x20) {
                        builder.append(String.format("\\u%04x", (int) current));
                    } else {
                        builder.append(current);
                    }
                }
            }
        }
        return builder.toString();
    }
}
