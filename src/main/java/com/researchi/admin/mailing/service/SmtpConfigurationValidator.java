package com.researchi.admin.mailing.service;

import com.researchi.admin.mailing.config.MailProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SmtpConfigurationValidator {

    private static final String SES_EXAMPLE_FROM_ADDRESS = "no-reply@your-verified-domain.example";

    private final MailProperties mailProperties;
    private final String smtpHost;
    private final Integer smtpPort;
    private final String smtpUsername;
    private final String smtpPassword;

    public SmtpConfigurationValidator(
            MailProperties mailProperties,
            @Value("${spring.mail.host:localhost}") String smtpHost,
            @Value("${spring.mail.port:1025}") Integer smtpPort,
            @Value("${spring.mail.username:}") String smtpUsername,
            @Value("${spring.mail.password:}") String smtpPassword
    ) {
        this.mailProperties = mailProperties;
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.smtpUsername = smtpUsername;
        this.smtpPassword = smtpPassword;
    }

    public void validateForRealDelivery() {
        if (mailProperties.isSimulateSend()) {
            return;
        }

        String fromAddress = mailProperties.getFromAddress();
        boolean usingDefaultLocalMailCatcher = "localhost".equalsIgnoreCase(smtpHost)
                && Integer.valueOf(1025).equals(smtpPort)
                && isBlank(smtpUsername)
                && isBlank(smtpPassword)
                && (isBlank(fromAddress) || "no-reply@researchi.local".equalsIgnoreCase(fromAddress));

        if (usingDefaultLocalMailCatcher) {
            throw new IllegalStateException(
                    "실제 메일 발송용 SMTP 설정이 되어 있지 않습니다. SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD, SMTP_AUTH, SMTP_STARTTLS_ENABLE, APP_MAIL_FROM_ADDRESS 값을 설정해 주세요."
            );
        }

        if (SES_EXAMPLE_FROM_ADDRESS.equalsIgnoreCase(fromAddress)) {
            throw new IllegalStateException(
                    "APP_MAIL_FROM_ADDRESS가 SES 예시 주소로 남아 있습니다. 설정된 리전에서 검증된 발신자 주소로 변경해 주세요."
            );
        }

        if (isBlank(smtpHost) || smtpPort == null || isBlank(smtpUsername) || isBlank(smtpPassword) || isBlank(fromAddress)) {
            throw new IllegalStateException(
                    "SMTP 설정이 완료되지 않았습니다. SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD, SMTP_AUTH, SMTP_STARTTLS_ENABLE, APP_MAIL_FROM_ADDRESS 값을 확인해 주세요."
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
