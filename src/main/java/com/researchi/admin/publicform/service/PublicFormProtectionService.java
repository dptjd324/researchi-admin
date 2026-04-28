package com.researchi.admin.publicform.service;

import com.researchi.admin.publicform.config.PublicFormProperties;
import jakarta.servlet.http.HttpSession;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PublicFormProtectionService {

    private static final String CAPTCHA_VALUE = "publicFormCaptchaValue";
    private static final String CAPTCHA_QUESTION = "publicFormCaptchaQuestion";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    private final PublicFormProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom;
    private final Map<String, Deque<Instant>> submissionsByKey = new ConcurrentHashMap<>();

    public PublicFormProtectionService(PublicFormProperties properties) {
        this(properties, Clock.systemDefaultZone(), new SecureRandom());
    }

    PublicFormProtectionService(PublicFormProperties properties, Clock clock, SecureRandom secureRandom) {
        this.properties = properties;
        this.clock = clock;
        this.secureRandom = secureRandom;
    }

    public boolean isCaptchaEnabled() {
        return properties.isCaptchaEnabled();
    }

    public String ensureCaptchaQuestion(HttpSession session) {
        if (!isCaptchaEnabled()) {
            return null;
        }
        Object existingQuestion = session.getAttribute(CAPTCHA_QUESTION);
        if (existingQuestion instanceof String question && !question.isBlank()) {
            return question;
        }
        int left = secureRandom.nextInt(8) + 1;
        int right = secureRandom.nextInt(8) + 1;
        session.setAttribute(CAPTCHA_VALUE, String.valueOf(left + right));
        String question = left + " + " + right + " = ?";
        session.setAttribute(CAPTCHA_QUESTION, question);
        return question;
    }

    public boolean validateCaptcha(HttpSession session, String answer) {
        if (!isCaptchaEnabled()) {
            return true;
        }
        Object expected = session.getAttribute(CAPTCHA_VALUE);
        if (!(expected instanceof String expectedValue)) {
            return false;
        }
        boolean matched = expectedValue.equals(answer == null ? "" : answer.trim());
        if (matched) {
            session.removeAttribute(CAPTCHA_VALUE);
            session.removeAttribute(CAPTCHA_QUESTION);
        }
        return matched;
    }

    public boolean tryAcquireRateLimitSlot(String key) {
        if (properties.getRateLimitCount() <= 0 || properties.getRateLimitWindowSeconds() <= 0) {
            return true;
        }
        Instant now = clock.instant();
        Instant threshold = now.minusSeconds(properties.getRateLimitWindowSeconds());
        cleanupExpiredRateLimitSlots(threshold);
        Deque<Instant> attempts = submissionsByKey.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            while (!attempts.isEmpty() && attempts.peekFirst().isBefore(threshold)) {
                attempts.removeFirst();
            }
            if (attempts.size() >= properties.getRateLimitCount()) {
                return false;
            }
            attempts.addLast(now);
            return true;
        }
    }

    private void cleanupExpiredRateLimitSlots(Instant threshold) {
        submissionsByKey.entrySet().removeIf(entry -> {
            Deque<Instant> attempts = entry.getValue();
            synchronized (attempts) {
                while (!attempts.isEmpty() && attempts.peekFirst().isBefore(threshold)) {
                    attempts.removeFirst();
                }
                return attempts.isEmpty();
            }
        });
    }

    public String normalizePhone(String value) {
        if (value == null) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        return digits.isBlank() ? null : digits;
    }

    public String normalizeEmail(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.isEmpty() ? null : normalized;
    }

    public String encrypt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception ex) {
            throw new IllegalStateException("민감 정보를 암호화하지 못했습니다.", ex);
        }
    }

    public String decrypt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(value);
            if (combined.length <= GCM_IV_LENGTH) {
                throw new IllegalStateException("암호화된 데이터가 올바르지 않습니다.");
            }
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec(), new GCMParameterSpec(GCM_TAG_LENGTH, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("민감 정보를 복호화하지 못했습니다.", ex);
        }
    }

    public String sha256(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte current : bytes) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("민감 정보를 해시 처리하지 못했습니다.", ex);
        }
    }

    public String phoneHash(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(properties.getPhoneHashKey().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] bytes = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte current : bytes) {
                builder.append(String.format("%02x", current));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("휴대전화 해시를 처리하지 못했습니다.", ex);
        }
    }

    public String legacyPhoneHash(String value) {
        return sha256(value);
    }

    private SecretKeySpec secretKeySpec() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = digest.digest(properties.getEncryptionKey().getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, "AES");
    }
}
