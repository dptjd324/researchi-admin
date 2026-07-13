package com.researchi.admin.legacy.matching.web;

import com.researchi.admin.legacy.matching.domain.LegacyMatchingOverview;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingRunTicket;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingSearchCondition;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StringUtils;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.List;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyMatchingTemplateTest {

    @Test
    void matchingRunWindowConfirmsSelectedAndSingleNotifications() throws Exception {
        String template = new ClassPathResource("templates/research/matching-run-window.html")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(StringUtils.countOccurrencesOf(template, "data-send-confirm-form")).isEqualTo(5);
        assertThat(StringUtils.countOccurrencesOf(template, "data-send-scope=\"selected\"")).isEqualTo(2);
        assertThat(StringUtils.countOccurrencesOf(template, "data-send-scope=\"single\"")).isEqualTo(2);
        assertThat(template)
                .contains("data-send-confirm-form")
                .contains("data-send-channel=\"SMS\"")
                .contains("data-send-channel=\"이메일\"")
                .contains("data-send-scope=\"selected\"")
                .contains("data-send-scope=\"single\"")
                .contains("SMS를 보내시겠습니까?")
                .contains("이메일을 보내시겠습니까?")
                .contains("명에게 SMS를 발송하시겠습니까?")
                .contains("명에게 이메일을 발송하시겠습니까?")
                .contains("if (!confirm(message)) {\n                        event.preventDefault();")
                .contains("if (scope === 'selected') {\n                        appendSelectedInputs(form, selected);");

        int confirmIndex = template.indexOf("if (!confirm(message))");
        int appendIndex = template.indexOf("appendSelectedInputs(form, selected);", confirmIndex);
        assertThat(confirmIndex).isGreaterThanOrEqualTo(0);
        assertThat(appendIndex).isGreaterThan(confirmIndex);

        String downloadSection = template.substring(
                template.indexOf("<section class=\"download-panel\""),
                template.indexOf("</section>", template.indexOf("<section class=\"download-panel\""))
        );
        assertThat(downloadSection).doesNotContain("data-send-confirm-form");
        assertThat(template).contains("th:disabled=\"${row.smsSent()}\"")
                .contains("th:disabled=\"${row.emailSent()}\"")
                .contains("th:if=\"${row.smsAllowed()}\"")
                .contains("th:if=\"${row.emailAllowed()}\"")
                .contains("data-sms-allowed")
                .contains("data-email-allowed")
                .contains("data-consent-channel=\"sms\"")
                .contains("data-consent-channel=\"email\"")
                .contains("function selectedValuesForChannel(channel)")
                .contains("checkbox.dataset[channel + 'Allowed'] === 'true'")
                .contains("selectedValuesForChannel(form.dataset.consentChannel)")
                .contains("button.textContent = label + ' ' + channelSelected.length + '명'")
                .contains("button.disabled = channelSelected.length === 0");
    }

    @Test
    void matchingRunWindowBlocksEveryFormSubmissionInPreviewMode() throws Exception {
        String template = new ClassPathResource("templates/research/matching-run-window.html")
                .getContentAsString(StandardCharsets.UTF_8);

        assertThat(template)
                .contains("data-preview-mode")
                .contains("document.addEventListener('submit'")
                .contains("previewRoot.dataset.previewMode !== 'true'")
                .contains("event.preventDefault()")
                .contains("event.stopImmediatePropagation()")
                .contains("미리보기에서는 실제 발송이나 다운로드가 실행되지 않습니다.");
    }

    @Test
    void matchingProgressTemplateParses() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        ResearchMaster research = new ResearchMaster();
        research.setResearchNo(46408L);
        research.setResearchTitle("테스트 좌담회");
        LegacyMatchingSearchForm searchForm = new LegacyMatchingSearchForm();
        searchForm.setAppSex("남자");
        searchForm.setAppBirth("1988-1995");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        WebContext context = new WebContext(
                JakartaServletWebApplication.buildApplication(request.getServletContext()).buildExchange(request, response),
                request.getLocale()
        );
        context.setVariable("pageTitle", "매칭 진행 중");
        context.setVariable("research", research);
        context.setVariable("searchForm", searchForm);
        context.setVariable("activeKeywordText", String.join(", ", searchForm.toCondition().displayFilters()));
        context.setVariable("runTicket", new LegacyMatchingRunTicket(77L, 16, "PENDING", false));
        context.setVariable("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token"));

        String rendered = templateEngine.process("research/matching-progress-window", context);

        assertThat(rendered)
                .contains("response.status === 404")
                .contains("매칭 작업 정보를 찾을 수 없습니다");
    }

    @Test
    void matchingTemplateParsesWithFieldSearchForm() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        ResearchMaster research = new ResearchMaster();
        research.setResearchNo(46408L);
        research.setResearchTitle("테스트 좌담회");

        LegacyMatchingSearchCondition condition = new LegacyMatchingSearchCondition(
                "남자",
                "1990",
                "직장인",
                "삼성",
                "서울",
                "비타민"
        );
        LegacyMatchingOverview overview = new LegacyMatchingOverview(
                research,
                condition.storageKey(),
                "",
                condition,
                5000,
                condition.displayFilters(),
                List.of(),
                0,
                0,
                0,
                0,
                1,
                500,
                null,
                "NO_RESULT",
                null,
                null
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        WebContext context = new WebContext(
                JakartaServletWebApplication.buildApplication(request.getServletContext()).buildExchange(request, response),
                request.getLocale()
        );
        context.setVariable("pageTitle", "매칭");
        context.setVariable("pageDescription", "매칭");
        context.setVariable("research", research);
        context.setVariable("overview", overview);
        context.setVariable("searchForm", new LegacyMatchingSearchForm());
        context.setVariable("conditionChecked", false);
        context.setVariable("_csrf", new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token"));
        context.setVariable("hideNavigation", true);

        String rendered = templateEngine.process("research/matching", context);

        assertThat(rendered).doesNotContain("popup.document.write");
    }
}
