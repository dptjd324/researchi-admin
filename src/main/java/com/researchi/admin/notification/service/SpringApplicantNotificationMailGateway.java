package com.researchi.admin.notification.service;

import com.researchi.admin.mailing.config.MailProperties;
import com.researchi.admin.mailing.service.SmtpConfigurationValidator;
import com.researchi.admin.notification.domain.NotificationEmailRequest;
import jakarta.mail.internet.InternetAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
public class SpringApplicantNotificationMailGateway implements ApplicantNotificationMailGateway {

    private static final Logger log = LoggerFactory.getLogger(SpringApplicantNotificationMailGateway.class);

    private final JavaMailSender javaMailSender;
    private final MailProperties mailProperties;
    private final SmtpConfigurationValidator smtpConfigurationValidator;

    public SpringApplicantNotificationMailGateway(
            JavaMailSender javaMailSender,
            MailProperties mailProperties,
            SmtpConfigurationValidator smtpConfigurationValidator
    ) {
        this.javaMailSender = javaMailSender;
        this.mailProperties = mailProperties;
        this.smtpConfigurationValidator = smtpConfigurationValidator;
    }

    @Override
    public void dispatch(NotificationEmailRequest request) throws Exception {
        InternetAddress address = new InternetAddress(request.recipient(), true);
        address.validate();
        if (mailProperties.isSimulateSend()) {
            return;
        }
        smtpConfigurationValidator.validateForRealDelivery();
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.getFromAddress());
        message.setTo(request.recipient());
        message.setSubject(request.subject());
        message.setText(request.body());
        try {
            javaMailSender.send(message);
        } catch (Exception ex) {
            log.error("SMTP keyword notification dispatch failed for recipient {}.", request.recipient(), ex);
            throw ex;
        }
    }
}
