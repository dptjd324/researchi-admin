package com.researchi.admin.publicform.web;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class PublicConsentTemplateTest {

    @Test
    void applicationTemplateContainsIndependentRequiredAndOptionalConsents() throws Exception {
        String template = new ClassPathResource("templates/publicform/legacy-apply.html")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("10. 개인정보 수집·이용 및 리서치 안내 수신 동의")
                .contains("th:field=\"*{provideYn}\"")
                .contains("th:field=\"*{futureRecruitmentYn}\"")
                .contains("th:field=\"*{notifySmsYn}\"")
                .contains("th:field=\"*{notifyEmailYn}\"")
                .contains("data-future-recruitment")
                .contains("data-future-channel")
                .contains("data-future-channel-group")
                .contains("futureRecruitment.checked = true")
                .contains("channel.checked = false")
                .contains("SMS 또는 이메일 중 최소 한 가지 수신 방법을 선택해 주세요.")
                .contains("<strong>4. 동의 거부 권리</strong>")
                .contains("개인정보 수집·이용 동의를 거부할 수 있습니다. 필수 동의 거부 시 리서치 신청이 제한되며, 선택 동의 거부에 따른 불이익은 없습니다.")
                .doesNotContain("필수 개인정보 수집·이용에 동의하지 않으면")
                .contains("해당 리서치 종료 후 2년")
                .contains("동의일로부터 2년 또는 동의 철회일까지")
                .contains("현재 리서치 신청·운영을 위한 개인정보 수집·이용에 동의합니다. (필수)")
                .contains("향후 리서치 모집·추천을 위한 개인정보 수집·이용에 동의합니다. (선택)")
                .contains("향후 리서치 모집 안내 SMS 수신에 동의합니다. (선택)")
                .contains("향후 리서치 모집 안내 이메일 수신에 동의합니다. (선택)");
    }
}
