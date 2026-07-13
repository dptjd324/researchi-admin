package com.researchi.admin.mailing.service;

import com.researchi.admin.mailing.config.MailProperties;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class NaverOutboundMailerGatewayTest {

    @Test
    void htmlBodyStartsWithNoReplyNotice() throws Exception {
        NaverOutboundMailerGateway gateway = new NaverOutboundMailerGateway(new MailProperties(), new NaverCloudApiSigner());

        String html = htmlBody(gateway, "안녕하세요.\n본문입니다.");

        assertThat(html).startsWith("본 메일은 발신전용입니다.<br>문의사항은 spirit2@naver.com 로 연락해 주세요.<br><br>");
        assertThat(html).contains("안녕하세요.<br>본문입니다.");
    }

    private String htmlBody(NaverOutboundMailerGateway gateway, String body) throws Exception {
        Method method = NaverOutboundMailerGateway.class.getDeclaredMethod("htmlBody", String.class);
        method.setAccessible(true);
        return (String) method.invoke(gateway, body);
    }
}
