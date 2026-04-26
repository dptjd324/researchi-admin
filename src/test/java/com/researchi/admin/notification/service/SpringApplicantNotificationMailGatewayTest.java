package com.researchi.admin.notification.service;

import com.researchi.admin.mailing.config.MailProperties;
import com.researchi.admin.mailing.service.SmtpConfigurationValidator;
import com.researchi.admin.notification.domain.NotificationEmailRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpringApplicantNotificationMailGatewayTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Test
    void dispatchSkipsWhenSimulationIsEnabled() throws Exception {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setSimulateSend(true);

        SpringApplicantNotificationMailGateway gateway = new SpringApplicantNotificationMailGateway(
                javaMailSender,
                mailProperties,
                new SmtpConfigurationValidator(mailProperties, "localhost", 1025, "", "")
        );

        gateway.dispatch(new NotificationEmailRequest("recipient@example.com", "subject", "body"));

        verify(javaMailSender, never()).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    }

    @Test
    void dispatchFailsFastWhenConfigurationIsIncomplete() {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setSimulateSend(false);
        mailProperties.setFromAddress("sender@example.com");

        SpringApplicantNotificationMailGateway gateway = new SpringApplicantNotificationMailGateway(
                javaMailSender,
                mailProperties,
                new SmtpConfigurationValidator(mailProperties, "email-smtp.ap-northeast-2.amazonaws.com", 587, "smtp-user", "")
        );

        assertThatThrownBy(() -> gateway.dispatch(new NotificationEmailRequest("recipient@example.com", "subject", "body")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SMTP 설정이 완료되지 않았습니다");
    }
}
