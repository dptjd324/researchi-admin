package com.researchi.admin.mailing.service;

import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class NaverCloudApiSigner {

    public String sign(String method, String requestUri, String timestamp, String accessKey, String secretKey) {
        try {
            String message = method + " " + requestUri + "\n" + timestamp + "\n" + accessKey;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getEncoder().encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign Naver Cloud API request.", ex);
        }
    }
}
