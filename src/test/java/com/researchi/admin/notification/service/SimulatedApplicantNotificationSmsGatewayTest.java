package com.researchi.admin.notification.service;

import com.researchi.admin.notification.config.SmsProperties;
import com.researchi.admin.notification.domain.NotificationSmsRequest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimulatedApplicantNotificationSmsGatewayTest {

    @Test
    void dispatchFailsFastWhenRealSmsConfigurationIsMissing() {
        SmsProperties smsProperties = new SmsProperties();
        smsProperties.setSimulateSend(false);

        SimulatedApplicantNotificationSmsGateway gateway = new SimulatedApplicantNotificationSmsGateway(smsProperties);

        assertThatThrownBy(() -> gateway.dispatch(new NotificationSmsRequest("01012345678", "테스트 문자")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("SMS 발송 설정이 완료되지 않았습니다");
    }
}
