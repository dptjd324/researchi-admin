package com.researchi.admin.mailing.service;

import com.researchi.admin.mailing.config.MailProperties;
import com.researchi.admin.mailing.domain.MailDispatchRequest;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpringMailDispatchGatewayTest {

    @Mock
    private JavaMailSender javaMailSender;

    @Test
    void dispatchSkipsWhenSimulationIsEnabled() throws Exception {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setSimulateSend(true);

        SpringMailDispatchGateway gateway = new SpringMailDispatchGateway(
                javaMailSender,
                mailProperties,
                new SmtpConfigurationValidator(mailProperties, "localhost", 1025, "", "")
        );

        gateway.dispatch(dispatchRequest());

        verify(javaMailSender, never()).send(org.mockito.ArgumentMatchers.any(MimeMessage.class));
    }

    @Test
    void dispatchFailsFastWhenUsingDefaultPlaceholderMailConfiguration() {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setSimulateSend(false);
        mailProperties.setFromAddress("no-reply@researchi.local");

        SpringMailDispatchGateway gateway = new SpringMailDispatchGateway(
                javaMailSender,
                mailProperties,
                new SmtpConfigurationValidator(mailProperties, "localhost", 1025, "", "")
        );

        assertThatThrownBy(() -> gateway.dispatch(dispatchRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("실제 메일 발송용 SMTP 설정이 되어 있지 않습니다");
    }

    @Test
    void dispatchSendsWhenRealMailConfigurationExists() throws Exception {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setSimulateSend(false);
        mailProperties.setFromAddress("sender@example.com");

        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        SpringMailDispatchGateway gateway = new SpringMailDispatchGateway(
                javaMailSender,
                mailProperties,
                new SmtpConfigurationValidator(mailProperties, "smtp.example.com", 587, "smtp-user", "smtp-password")
        );

        gateway.dispatch(dispatchRequest());

        verify(javaMailSender).send(mimeMessage);
    }

    @Test
    void dispatchFailsFastWhenSmtpPasswordIsMissing() {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setSimulateSend(false);
        mailProperties.setFromAddress("sender@example.com");

        SpringMailDispatchGateway gateway = new SpringMailDispatchGateway(
                javaMailSender,
                mailProperties,
                new SmtpConfigurationValidator(mailProperties, "email-smtp.ap-northeast-2.amazonaws.com", 587, "smtp-user", "")
        );

        assertThatThrownBy(() -> gateway.dispatch(dispatchRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SMTP 설정이 완료되지 않았습니다");
    }

    @Test
    void dispatchFailsFastWhenSesExampleFromAddressIsStillConfigured() {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setSimulateSend(false);
        mailProperties.setFromAddress("no-reply@your-verified-domain.example");

        SpringMailDispatchGateway gateway = new SpringMailDispatchGateway(
                javaMailSender,
                mailProperties,
                new SmtpConfigurationValidator(mailProperties, "email-smtp.ap-northeast-2.amazonaws.com", 587, "smtp-user", "smtp-password")
        );

        assertThatThrownBy(() -> gateway.dispatch(dispatchRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SES 예시 주소로 남아 있습니다");
    }

    private MailDispatchRequest dispatchRequest() {
        return new MailDispatchRequest(
                List.of("recipient@example.com"),
                "reply@example.com",
                "subject",
                "body",
                "applications.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3}
        );
    }
}
