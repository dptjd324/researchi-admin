package com.researchi.admin.mailing.service;

import com.researchi.admin.mailing.config.MailProperties;
import com.researchi.admin.mailing.domain.MailDispatchRequest;
import com.researchi.admin.mailing.domain.MailDispatchResult;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class SpringMailDispatchGateway {

    private static final Logger log = LoggerFactory.getLogger(SpringMailDispatchGateway.class);

    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;
    private final SmtpConfigurationValidator smtpConfigurationValidator;

    public SpringMailDispatchGateway(
            JavaMailSender javaMailSender,
            MailProperties mailProperties,
            SmtpConfigurationValidator smtpConfigurationValidator
    ) {
        this.javaMailSender = javaMailSender;
        this.mailProperties = mailProperties;
        this.smtpConfigurationValidator = smtpConfigurationValidator;
    }

    public MailDispatchResult dispatch(MailDispatchRequest request) throws Exception {
        if (mailProperties.isSimulateSend()) {
            return MailDispatchResult.simulated();
        }
        smtpConfigurationValidator.validateForRealDelivery();

        var message = javaMailSender.createMimeMessage();
        var helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        helper.setFrom(mailProperties.getFromAddress());
        helper.setTo(request.recipients().toArray(String[]::new));
        if (request.replyTo() != null && !request.replyTo().isBlank()) {
            helper.setReplyTo(request.replyTo());
        }
        helper.setSubject(request.subject());
        helper.setText(request.body(), false);
        if (request.attachmentFileName() != null
                && !request.attachmentFileName().isBlank()
                && request.attachmentContent() != null
                && request.attachmentContent().length > 0) {
            helper.addAttachment(
                    request.attachmentFileName(),
                    new ByteArrayResource(request.attachmentContent()),
                    request.attachmentContentType()
            );
        }
        try {
            javaMailSender.send(message);
            return MailDispatchResult.smtp();
        } catch (Exception ex) {
            log.error("SMTP dispatch failed for {} recipient(s).", request.recipients().size(), ex);
            throw ex;
        }
    }
}
