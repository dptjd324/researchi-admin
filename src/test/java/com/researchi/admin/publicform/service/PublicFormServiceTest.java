package com.researchi.admin.publicform.service;

import com.researchi.admin.blacklist.service.BlacklistModePolicy;
import com.researchi.admin.form.domain.FormFieldDetail;
import com.researchi.admin.form.domain.FormFieldOption;
import com.researchi.admin.form.service.FormFieldService;
import com.researchi.admin.job.domain.AdminJobMeta;
import com.researchi.admin.job.domain.JobDetail;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.job.support.ApplicationFormNoticeOption;
import com.researchi.admin.job.support.ApplicationFormNoticeParser;
import com.researchi.admin.keyword.service.KeywordExtractionService;
import com.researchi.admin.publicform.config.PublicFormProperties;
import com.researchi.admin.publicform.domain.AdminApplicationDuplicateLog;
import com.researchi.admin.publicform.domain.AdminBlacklist;
import com.researchi.admin.publicform.domain.AdminJobApplication;
import com.researchi.admin.publicform.domain.PublicFormSubmissionResult;
import com.researchi.admin.publicform.domain.PublicFormSubmissionStatus;
import com.researchi.admin.publicform.domain.PublicFormUnavailableException;
import com.researchi.admin.publicform.domain.PublicFormValidationException;
import com.researchi.admin.publicform.mapper.AdminApplicationDuplicateLogMapper;
import com.researchi.admin.publicform.mapper.AdminBlacklistMapper;
import com.researchi.admin.publicform.mapper.AdminBlacklistMatchLogMapper;
import com.researchi.admin.publicform.mapper.AdminFormSubmissionAnswerMapper;
import com.researchi.admin.publicform.mapper.AdminJobApplicationExtraAnswerMapper;
import com.researchi.admin.publicform.mapper.AdminJobApplicationMapper;
import com.researchi.admin.publicform.mapper.AdminPrivacyConsentMapper;
import com.researchi.admin.publicform.web.PublicApplicationForm;
import com.researchi.admin.xe.domain.XeJobDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicFormServiceTest {

    @Mock
    private JobService jobService;
    @Mock
    private FormFieldService formFieldService;
    @Mock
    private AdminJobApplicationMapper adminJobApplicationMapper;
    @Mock
    private AdminFormSubmissionAnswerMapper adminFormSubmissionAnswerMapper;
    @Mock
    private AdminApplicationDuplicateLogMapper adminApplicationDuplicateLogMapper;
    @Mock
    private AdminBlacklistMapper adminBlacklistMapper;
    @Mock
    private AdminBlacklistMatchLogMapper adminBlacklistMatchLogMapper;
    @Mock
    private AdminPrivacyConsentMapper adminPrivacyConsentMapper;
    @Mock
    private AdminJobApplicationExtraAnswerMapper adminJobApplicationExtraAnswerMapper;
    @Mock
    private KeywordExtractionService keywordExtractionService;

    private PublicFormService publicFormService;

    @BeforeEach
    void setUp() {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setCaptchaEnabled(false);
        publicFormService = new PublicFormService(
                jobService,
                formFieldService,
                adminJobApplicationMapper,
                adminFormSubmissionAnswerMapper,
                adminApplicationDuplicateLogMapper,
                adminBlacklistMapper,
                adminBlacklistMatchLogMapper,
                adminPrivacyConsentMapper,
                adminJobApplicationExtraAnswerMapper,
                new PublicFormProtectionService(properties),
                keywordExtractionService
        );
    }

    @Test
    void submitPersistsApplicationAnswersConsentAndExtraAnswers() {
        when(jobService.getJob(11L)).thenReturn(jobDetail(11L));
        when(formFieldService.getFields(11L)).thenReturn(List.of(selectField()));
        when(adminApplicationDuplicateLogMapper.findLatestByDocumentSrlAndMobilePhoneHashes(eq(11L), any())).thenReturn(null);
        when(adminApplicationDuplicateLogMapper.countPrimaryByMobilePhoneHashes(any())).thenReturn(0);
        when(adminBlacklistMapper.findActiveMatches(any(), any(), any())).thenReturn(List.of());
        doAnswer(invocation -> {
            AdminJobApplication application = invocation.getArgument(0);
            application.setId(87L);
            return null;
        }).when(adminJobApplicationMapper).insert(any(AdminJobApplication.class));

        PublicFormSubmissionResult result = publicFormService.submit(11L, baseForm(), Map.of(41L, List.of("weekday")), request());

        assertThat(result.status()).isEqualTo(PublicFormSubmissionStatus.COMPLETE);
        ArgumentCaptor<AdminJobApplication> captor = ArgumentCaptor.forClass(AdminJobApplication.class);
        verify(adminJobApplicationMapper).insert(captor.capture());
        assertThat(captor.getValue().getMobilePhoneMasked()).isEqualTo("01012345678");
        assertThat(captor.getValue().getExtraComment()).contains("결혼여부: 미혼");
        assertThat(captor.getValue().getExtraComment()).contains("자녀유무: 없음");
        verify(adminJobApplicationExtraAnswerMapper, times(2)).insert(any());
        verify(adminFormSubmissionAnswerMapper).insert(any());
        verify(adminPrivacyConsentMapper, times(2)).insert(any());
        verify(adminApplicationDuplicateLogMapper).insert(any());
        verify(keywordExtractionService).syncApplicationKeywords(87L);
    }

    @Test
    void submitEnablesRecommendationWhenNotificationChannelIsSelected() {
        when(jobService.getJob(11L)).thenReturn(jobDetail(11L));
        when(formFieldService.getFields(11L)).thenReturn(List.of(selectField()));
        when(adminApplicationDuplicateLogMapper.findLatestByDocumentSrlAndMobilePhoneHashes(eq(11L), any())).thenReturn(null);
        when(adminApplicationDuplicateLogMapper.countPrimaryByMobilePhoneHashes(any())).thenReturn(0);
        when(adminBlacklistMapper.findActiveMatches(any(), any(), any())).thenReturn(List.of());
        doAnswer(invocation -> {
            AdminJobApplication application = invocation.getArgument(0);
            application.setId(87L);
            return null;
        }).when(adminJobApplicationMapper).insert(any(AdminJobApplication.class));
        PublicApplicationForm form = baseForm();
        form.setNotifyEmailYn(true);
        form.setEmailAddress("applicant@example.com");

        publicFormService.submit(11L, form, Map.of(41L, List.of("weekday")), request());

        ArgumentCaptor<AdminJobApplication> captor = ArgumentCaptor.forClass(AdminJobApplication.class);
        verify(adminJobApplicationMapper).insert(captor.capture());
        assertThat(captor.getValue().getNotifyEmailYn()).isEqualTo("Y");
        assertThat(captor.getValue().getNotifyKeywordYn()).isEqualTo("Y");
    }

    @Test
    void submitUsesRemoteAddressInsteadOfForwardedForHeaderForAuditIp() {
        when(jobService.getJob(11L)).thenReturn(jobDetail(11L));
        when(formFieldService.getFields(11L)).thenReturn(List.of(selectField()));
        when(adminApplicationDuplicateLogMapper.findLatestByDocumentSrlAndMobilePhoneHashes(eq(11L), any())).thenReturn(null);
        when(adminApplicationDuplicateLogMapper.countPrimaryByMobilePhoneHashes(any())).thenReturn(0);
        when(adminBlacklistMapper.findActiveMatches(any(), any(), any())).thenReturn(List.of());
        doAnswer(invocation -> {
            AdminJobApplication application = invocation.getArgument(0);
            application.setId(87L);
            return null;
        }).when(adminJobApplicationMapper).insert(any(AdminJobApplication.class));
        MockHttpServletRequest request = request();
        request.setRemoteAddr("203.0.113.10");
        request.addHeader("X-Forwarded-For", "198.51.100.77");

        publicFormService.submit(11L, baseForm(), Map.of(41L, List.of("weekday")), request);

        ArgumentCaptor<com.researchi.admin.publicform.domain.AdminPrivacyConsent> captor =
                ArgumentCaptor.forClass(com.researchi.admin.publicform.domain.AdminPrivacyConsent.class);
        verify(adminPrivacyConsentMapper, times(2)).insert(captor.capture());
        assertThat(captor.getAllValues()).allMatch(consent -> "203.0.113.10".equals(consent.getIpAddress()));
    }


    @Test
    void submitRequiresEachAdditionalAnswer() {
        when(jobService.getJob(11L)).thenReturn(jobDetail(11L));
        when(formFieldService.getFields(11L)).thenReturn(List.of(selectField()));
        PublicApplicationForm form = baseForm();
        form.setExtraAnswers(List.of("미혼"));

        assertThatThrownBy(() -> publicFormService.submit(11L, form, Map.of(41L, List.of("weekday")), request()))
                .isInstanceOf(PublicFormValidationException.class);
    }

    @Test
    void submitValidatesTypedAdditionalAnswersAndStoresDisplayLabels() {
        when(jobService.getJob(11L)).thenReturn(typedJobDetail(11L));
        when(formFieldService.getFields(11L)).thenReturn(List.of(selectField()));
        when(adminApplicationDuplicateLogMapper.findLatestByDocumentSrlAndMobilePhoneHashes(eq(11L), any())).thenReturn(null);
        when(adminApplicationDuplicateLogMapper.countPrimaryByMobilePhoneHashes(any())).thenReturn(0);
        when(adminBlacklistMapper.findActiveMatches(any(), any(), any())).thenReturn(List.of());
        doAnswer(invocation -> {
            AdminJobApplication application = invocation.getArgument(0);
            application.setId(87L);
            return null;
        }).when(adminJobApplicationMapper).insert(any(AdminJobApplication.class));
        PublicApplicationForm form = baseForm();
        form.setExtraAnswers(List.of("평일", "뷰티,식품", "7", LocalDate.now().toString()));

        publicFormService.submit(11L, form, Map.of(41L, List.of("weekday")), request());

        ArgumentCaptor<AdminJobApplication> captor = ArgumentCaptor.forClass(AdminJobApplication.class);
        verify(adminJobApplicationMapper).insert(captor.capture());
        assertThat(captor.getValue().getExtraComment()).contains("참석 가능 시간: 평일");
        assertThat(captor.getValue().getExtraComment()).contains("관심 분야: 뷰티, 식품");
        assertThat(captor.getValue().getExtraComment()).contains("구매 횟수: 7");
    }

    @Test
    void submitReturnsDuplicateWhenMobileHashAlreadyExistsForJob() {
        when(jobService.getJob(11L)).thenReturn(jobDetail(11L));
        when(formFieldService.getFields(11L)).thenReturn(List.of(selectField()));
        AdminApplicationDuplicateLog duplicateLog = new AdminApplicationDuplicateLog();
        duplicateLog.setMatchedApplicationId(23L);
        when(adminApplicationDuplicateLogMapper.findLatestByDocumentSrlAndMobilePhoneHashes(eq(11L), any())).thenReturn(duplicateLog);

        PublicFormSubmissionResult result = publicFormService.submit(11L, baseForm(), Map.of(41L, List.of("weekday")), request());

        assertThat(result.status()).isEqualTo(PublicFormSubmissionStatus.DUPLICATE);
        verify(adminJobApplicationMapper, never()).insert(any());
        verify(adminApplicationDuplicateLogMapper).insert(any());
    }

    @Test
    void submitMarksApplicationBlockedWhenBlacklistMatches() {
        when(jobService.getJob(11L)).thenReturn(jobDetail(11L));
        when(formFieldService.getFields(11L)).thenReturn(List.of(selectField()));
        when(adminApplicationDuplicateLogMapper.findLatestByDocumentSrlAndMobilePhoneHashes(eq(11L), any())).thenReturn(null);
        when(adminApplicationDuplicateLogMapper.countPrimaryByMobilePhoneHashes(any())).thenReturn(2);
        AdminBlacklist blacklist = new AdminBlacklist();
        blacklist.setId(5L);
        blacklist.setBlackMode("BLOCK");
        blacklist.setMatchType("MOBILE_PHONE_HASH");
        when(adminBlacklistMapper.findActiveMatches(any(), any(), any())).thenReturn(List.of(blacklist));
        doAnswer(invocation -> {
            AdminJobApplication application = invocation.getArgument(0);
            application.setId(88L);
            return null;
        }).when(adminJobApplicationMapper).insert(any(AdminJobApplication.class));

        PublicFormSubmissionResult result = publicFormService.submit(11L, baseForm(), Map.of(41L, List.of("weekday")), request());

        assertThat(result.status()).isEqualTo(PublicFormSubmissionStatus.BLOCKED);
        verify(adminBlacklistMatchLogMapper).insert(any());
    }

    @Test
    void submitRejectsWrongCaptcha() {
        PublicFormProperties properties = new PublicFormProperties();
        properties.setCaptchaEnabled(true);
        PublicFormService captchaProtectedService = new PublicFormService(
                jobService,
                formFieldService,
                adminJobApplicationMapper,
                adminFormSubmissionAnswerMapper,
                adminApplicationDuplicateLogMapper,
                adminBlacklistMapper,
                adminBlacklistMatchLogMapper,
                adminPrivacyConsentMapper,
                adminJobApplicationExtraAnswerMapper,
                new PublicFormProtectionService(properties),
                keywordExtractionService
        );
        when(jobService.getJob(11L)).thenReturn(jobDetail(11L));
        when(formFieldService.getFields(11L)).thenReturn(List.of(selectField()));

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        captchaProtectedService.getPage(11L, session);

        PublicApplicationForm form = baseForm();
        form.setCaptchaAnswer("wrong");

        assertThatThrownBy(() -> captchaProtectedService.submit(11L, form, Map.of(41L, List.of("weekday")), request))
                .isInstanceOf(PublicFormValidationException.class);
    }

    @Test
    void getPageRejectsUnavailableJob() {
        XeJobDocument document = new XeJobDocument();
        document.setDocumentSrl(11L);
        document.setStatus("PUBLIC");
        document.setMid("newjob");

        AdminJobMeta meta = new AdminJobMeta();
        meta.setDocumentSrl(11L);
        meta.setRecruitStatus("CLOSED");
        meta.setApplicationEnabled("Y");

        when(jobService.getJob(11L)).thenReturn(new JobDetail(document, meta));

        assertThatThrownBy(() -> publicFormService.getPage(11L, new MockHttpSession()))
                .isInstanceOf(PublicFormUnavailableException.class)
                .hasMessageContaining("모집중");
    }

    private PublicApplicationForm baseForm() {
        PublicApplicationForm form = new PublicApplicationForm();
        form.setApplicantName("Applicant");
        form.setGenderCode("F");
        form.setBirthDate(LocalDate.of(1996, 4, 8));
        form.setAgeText("29");
        form.setJobText("Research participant");
        form.setOrganizationText("Researchi");
        form.setMobilePhone("010-1234-5678");
        form.setAddress("123 Teheran-ro, Seoul");
        form.setExtraAnswers(List.of("미혼", "없음"));
        form.setProvideYn(true);
        return form;
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.setSession(new MockHttpSession());
        return request;
    }

    private JobDetail jobDetail(Long documentSrl) {
        XeJobDocument document = new XeJobDocument();
        document.setDocumentSrl(documentSrl);
        document.setTitle("Public Job");
        document.setContent("Job content");
        document.setMid("newjob");
        document.setStatus("PUBLIC");

        AdminJobMeta meta = new AdminJobMeta();
        meta.setDocumentSrl(documentSrl);
        meta.setRecruitStatus("RECRUITING");
        meta.setApplicationEnabled("Y");
        meta.setApplicationFormNotice("결혼여부\n자녀유무");
        meta.setCloseDate(LocalDate.now().plusDays(5));
        return new JobDetail(document, meta);
    }

    private JobDetail typedJobDetail(Long documentSrl) {
        JobDetail detail = jobDetail(documentSrl);
        detail.getMeta().setApplicationFormNotice(String.join("\n",
                ApplicationFormNoticeParser.serializeItem(
                        "참석 가능 시간",
                        "SELECT",
                        List.of(
                                ApplicationFormNoticeOption.fromAdminText("평일"),
                                ApplicationFormNoticeOption.fromAdminText("주말")
                        )
                ),
                ApplicationFormNoticeParser.serializeItem(
                        "관심 분야",
                        "CHECKBOX",
                        List.of(
                                ApplicationFormNoticeOption.fromAdminText("뷰티"),
                                ApplicationFormNoticeOption.fromAdminText("식품")
                        )
                ),
                ApplicationFormNoticeParser.serializeItem("구매 횟수", "NUMBER", List.of()),
                ApplicationFormNoticeParser.serializeItem("참석 가능일", "DATE", List.of())
        ));
        return detail;
    }

    private FormFieldDetail selectField() {
        return new FormFieldDetail(
                41L,
                "availability",
                "Availability",
                "SELECT",
                1,
                true,
                null,
                null,
                List.of(new FormFieldOption("weekday", "Weekday")),
                true
        );
    }
}
