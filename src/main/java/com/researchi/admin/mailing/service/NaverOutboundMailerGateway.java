package com.researchi.admin.mailing.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.researchi.admin.mailing.config.MailProperties;
import com.researchi.admin.mailing.domain.MailDispatchRequest;
import com.researchi.admin.mailing.domain.MailDispatchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class NaverOutboundMailerGateway {

    private static final Logger log = LoggerFactory.getLogger(NaverOutboundMailerGateway.class);
    private static final String NO_REPLY_NOTICE = """
            본 메일은 발신전용입니다.
            문의사항은 spirit2@naver.com 로 연락해 주세요.
            """;

    private final MailProperties mailProperties;
    private final NaverCloudApiSigner signer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public NaverOutboundMailerGateway(
            MailProperties mailProperties,
            NaverCloudApiSigner signer
    ) {
        this.mailProperties = mailProperties;
        this.signer = signer;
    }

    public MailDispatchResult dispatch(MailDispatchRequest request) throws Exception {
        if (mailProperties.isSimulateSend()) {
            return MailDispatchResult.simulated();
        }
        validateConfiguration();
        validateLimits(request);

        List<String> attachFileIds = uploadAttachment(request);
        URI uri = endpoint("/mails");
        String payload = objectMapper.writeValueAsString(mailPayload(request, attachFileIds));
        HttpResponse<String> response = httpClient.send(
                signedBuilder("POST", uri)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.error("Naver Outbound Mailer dispatch failed. status={}, body={}", response.statusCode(), response.body());
            throw new IllegalStateException("Naver Outbound Mailer dispatch failed: HTTP " + response.statusCode());
        }
        JsonNode json = objectMapper.readTree(response.body());
        return new MailDispatchResult(
                "naver-outbound-mailer",
                text(json, "requestId"),
                String.valueOf(response.statusCode()),
                "REQUESTED",
                response.body(),
                LocalDateTime.now()
        );
    }

    private Map<String, Object> mailPayload(MailDispatchRequest request, List<String> attachFileIds) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("senderAddress", mailProperties.getFromAddress());
        payload.put("senderName", mailProperties.getFromName());
        payload.put("title", request.subject());
        payload.put("body", htmlBody(request.body()));
        payload.put("individual", true);
        payload.put("confirmAndSend", false);
        payload.put("advertising", false);
        payload.put("recipients", request.recipients().stream()
                .map(recipient -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("address", recipient);
                    item.put("name", null);
                    item.put("type", "R");
                    return item;
                })
                .toList());
        if (!attachFileIds.isEmpty()) {
            payload.put("attachFileIds", attachFileIds);
        }
        return payload;
    }

    private String htmlBody(String body) {
        String fullBody = NO_REPLY_NOTICE + "\n" + (body == null ? "" : body);
        return fullBody
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replace("\n", "<br>");
    }

    private List<String> uploadAttachment(MailDispatchRequest request) throws Exception {
        if (request.attachmentFileName() == null
                || request.attachmentFileName().isBlank()
                || request.attachmentContent() == null
                || request.attachmentContent().length == 0) {
            return List.of();
        }
        URI uri = endpoint("/files");
        String boundary = "researchi-" + UUID.randomUUID();
        byte[] body = multipartBody(boundary, request.attachmentFileName(), request.attachmentContentType(), request.attachmentContent());
        HttpResponse<String> response = httpClient.send(
                signedBuilder("POST", uri)
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.error("Naver Outbound Mailer attachment upload failed. status={}, body={}", response.statusCode(), response.body());
            throw new IllegalStateException("Naver Outbound Mailer attachment upload failed: HTTP " + response.statusCode());
        }
        JsonNode files = objectMapper.readTree(response.body()).path("files");
        List<String> fileIds = new ArrayList<>();
        if (files.isArray()) {
            for (JsonNode file : files) {
                String fileId = text(file, "fileId");
                if (fileId != null && !fileId.isBlank()) {
                    fileIds.add(fileId);
                }
            }
        }
        if (fileIds.isEmpty()) {
            throw new IllegalStateException("Naver Outbound Mailer attachment upload returned no fileId.");
        }
        return fileIds;
    }

    private byte[] multipartBody(String boundary, String fileName, String contentType, byte[] content) {
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"fileList\"; filename=\"" + fileName.replace("\"", "") + "\"\r\n"
                + "Content-Type: " + (contentType == null || contentType.isBlank() ? "application/octet-stream" : contentType) + "\r\n\r\n";
        String footer = "\r\n--" + boundary + "--\r\n";
        byte[] headerBytes = header.getBytes(StandardCharsets.UTF_8);
        byte[] footerBytes = footer.getBytes(StandardCharsets.UTF_8);
        byte[] body = new byte[headerBytes.length + content.length + footerBytes.length];
        System.arraycopy(headerBytes, 0, body, 0, headerBytes.length);
        System.arraycopy(content, 0, body, headerBytes.length, content.length);
        System.arraycopy(footerBytes, 0, body, headerBytes.length + content.length, footerBytes.length);
        return body;
    }

    private HttpRequest.Builder signedBuilder(String method, URI uri) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        MailProperties.Naver naver = mailProperties.getNaver();
        return HttpRequest.newBuilder(uri)
                .header("x-ncp-apigw-timestamp", timestamp)
                .header("x-ncp-iam-access-key", naver.getAccessKey())
                .header("x-ncp-apigw-signature-v2", signer.sign(method, requestUri(uri), timestamp, naver.getAccessKey(), naver.getSecretKey()));
    }

    private URI endpoint(String path) {
        String baseUrl = mailProperties.getNaver().getBaseUrl();
        return URI.create(baseUrl.replaceAll("/+$", "") + path);
    }

    private String requestUri(URI uri) {
        return uri.getRawQuery() == null ? uri.getRawPath() : uri.getRawPath() + "?" + uri.getRawQuery();
    }

    private void validateConfiguration() {
        MailProperties.Naver naver = mailProperties.getNaver();
        if (isBlank(mailProperties.getFromAddress()) || isBlank(naver.getAccessKey()) || isBlank(naver.getSecretKey())) {
            throw new IllegalStateException("Naver Outbound Mailer configuration is incomplete.");
        }
    }

    private void validateLimits(MailDispatchRequest request) {
        long bodyBytes = request.body() == null ? 0 : request.body().getBytes(StandardCharsets.UTF_8).length;
        if (bodyBytes > mailProperties.getNaver().getMaxBodySizeBytes()) {
            throw new IllegalStateException("Naver Outbound Mailer body limit exceeded.");
        }
        long attachmentBytes = request.attachmentContent() == null ? 0 : request.attachmentContent().length;
        if (attachmentBytes > mailProperties.getNaver().getMaxFileSizeBytes()
                || attachmentBytes > mailProperties.getNaver().getMaxTotalAttachmentSizeBytes()) {
            throw new IllegalStateException("Naver Outbound Mailer attachment limit exceeded.");
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
