package com.researchi.admin.legacy.application.service;

import com.researchi.admin.legacy.application.mapper.LegacyApplicationExtraAnswerMapper;
import com.researchi.admin.legacy.application.mapper.LegacyApplicationSearchIndexMapper;
import com.researchi.admin.legacy.matching.service.LegacyMatchingService;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.legacy.research.mapper.ResearchApplicationMapper;
import com.researchi.admin.legacy.research.service.ResearchMasterService;
import com.researchi.admin.publicform.domain.PublicFormUnavailableException;
import com.researchi.admin.publicform.domain.PublicFormValidationException;
import com.researchi.admin.publicform.service.PublicFormProtectionService;
import com.researchi.admin.publicform.web.PublicApplicationForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class LegacyPublicApplicationServiceTest {

    @Mock
    private ResearchMasterService researchMasterService;
    @Mock
    private ResearchApplicationMapper researchApplicationMapper;
    @Mock
    private LegacyApplicationExtraAnswerMapper legacyApplicationExtraAnswerMapper;
    @Mock
    private LegacyApplicationSearchIndexMapper legacyApplicationSearchIndexMapper;
    @Mock
    private PublicFormProtectionService protectionService;
    @Mock
    private LegacyMatchingService legacyMatchingService;
    @Mock
    private LegacyApplicationConsentService legacyApplicationConsentService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpSession session;

    @Test
    void submitRejectsDuplicateByResearchNameAndPhone() {
        LegacyPublicApplicationService service = service();
        PublicApplicationForm form = validForm();

        when(researchMasterService.getResearchMaster(46408L)).thenReturn(researchMaster());
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getSession()).thenReturn(session);
        when(protectionService.tryAcquireRateLimitSlot("legacy:46408:127.0.0.1")).thenReturn(true);
        when(protectionService.validateCaptcha(session, form.getCaptchaAnswer())).thenReturn(true);
        when(protectionService.normalizePhone("010-1234-5678")).thenReturn("01012345678");
        when(protectionService.formatPhoneForDisplay("010-1234-5678")).thenReturn("010-1234-5678");
        when(researchApplicationMapper.findDuplicateSeqByNameAndPhone(46408L, "김테스트", "010-1234-5678"))
                .thenReturn(4120001L);

        assertThatThrownBy(() -> service.submit(46408L, form, request))
                .isInstanceOf(PublicFormValidationException.class)
                .hasMessageContaining("이미 같은 이름");

        verify(researchApplicationMapper, never()).insert(org.mockito.ArgumentMatchers.any());
        verify(researchApplicationMapper, never()).incrementCounts(46408L);
    }

    @Test
    void submitRecordsConsentBeforeIndexingApplication() {
        LegacyPublicApplicationService service = service();
        PublicApplicationForm form = validForm();
        form.setFutureRecruitmentYn(true);
        form.setNotifySmsYn(true);
        form.setNotifyEmailYn(false);

        when(researchMasterService.getResearchMaster(46408L)).thenReturn(researchMaster());
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getSession()).thenReturn(session);
        when(protectionService.tryAcquireRateLimitSlot("legacy:46408:127.0.0.1")).thenReturn(true);
        when(protectionService.validateCaptcha(session, form.getCaptchaAnswer())).thenReturn(true);
        when(protectionService.normalizePhone("010-1234-5678")).thenReturn("01012345678");
        when(protectionService.formatPhoneForDisplay("010-1234-5678")).thenReturn("010-1234-5678");
        when(researchApplicationMapper.findDuplicateSeqByNameAndPhone(46408L, "김테스트", "010-1234-5678"))
                .thenReturn(null);
        when(researchApplicationMapper.findNextResearchAppSeq()).thenReturn(4120002L);

        assertThat(service.submit(46408L, form, request)).isEqualTo(4120002L);

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(
                researchApplicationMapper,
                legacyApplicationConsentService,
                legacyApplicationSearchIndexMapper
        );
        inOrder.verify(researchApplicationMapper).insert(org.mockito.ArgumentMatchers.any());
        inOrder.verify(legacyApplicationConsentService).recordSubmissionConsent(46408L, 4120002L, form);
    }

    @Test
    void submitRejectsFutureRecruitmentWithoutAnyNotificationChannel() {
        LegacyPublicApplicationService service = service();
        PublicApplicationForm form = validForm();
        form.setFutureRecruitmentYn(true);
        form.setNotifySmsYn(false);
        form.setNotifyEmailYn(false);

        when(researchMasterService.getResearchMaster(46408L)).thenReturn(researchMaster());
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getSession()).thenReturn(session);
        when(protectionService.tryAcquireRateLimitSlot("legacy:46408:127.0.0.1")).thenReturn(true);
        when(protectionService.validateCaptcha(session, form.getCaptchaAnswer())).thenReturn(true);
        when(protectionService.normalizePhone("010-1234-5678")).thenReturn("01012345678");

        assertThatThrownBy(() -> service.submit(46408L, form, request))
                .isInstanceOf(PublicFormValidationException.class)
                .satisfies(ex -> assertThat(((PublicFormValidationException) ex).getFieldErrors())
                        .containsKey("futureRecruitmentChannelAccepted"));

        verify(researchApplicationMapper, never()).insert(any());
        verify(legacyApplicationConsentService, never()).recordSubmissionConsent(any(), any(), any());
    }

    @Test
    void getOpenResearchRejectsFromDayAfterCloseDate() {
        LegacyPublicApplicationService service = service();
        ResearchMaster researchMaster = researchMaster();
        researchMaster.setCloseDate(LocalDate.now().minusDays(1).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE));

        when(researchMasterService.getResearchMaster(46408L)).thenReturn(researchMaster);

        assertThatThrownBy(() -> service.getOpenResearch(46408L))
                .isInstanceOf(PublicFormUnavailableException.class)
                .hasMessageContaining("마감");
    }

    @Test
    void getOpenResearchAllowsOnCloseDate() {
        LegacyPublicApplicationService service = service();
        ResearchMaster researchMaster = researchMaster();
        researchMaster.setCloseDate(LocalDate.now().format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE));

        when(researchMasterService.getResearchMaster(46408L)).thenReturn(researchMaster);

        org.assertj.core.api.Assertions.assertThat(service.getOpenResearch(46408L)).isSameAs(researchMaster);
    }

    @Test
    void submitRejectsInvalidApplicantName() {
        assertInvalidApplicantName("A");
        assertInvalidApplicantName("Kim1");
        assertInvalidApplicantName("Kim!");
        assertInvalidApplicantName("김d");
    }

    @Test
    void submitRejectsValuesThatExceedLegacyColumnLimits() {
        LegacyPublicApplicationService service = service();
        PublicApplicationForm form = validForm();
        form.setJobText("직업명이 스무자를 확실하게 넘어서 저장 실패하는 입력");
        form.setAddress("서울시 강남구 ".repeat(20));
        form.setPriorResearchText("최근 참여 이력 ".repeat(30));

        when(researchMasterService.getResearchMaster(46408L)).thenReturn(researchMaster());
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getSession()).thenReturn(session);
        when(protectionService.tryAcquireRateLimitSlot("legacy:46408:127.0.0.1")).thenReturn(true);
        when(protectionService.validateCaptcha(session, form.getCaptchaAnswer())).thenReturn(true);
        when(protectionService.normalizePhone("010-1234-5678")).thenReturn("01012345678");
        when(protectionService.formatPhoneForDisplay("010-1234-5678")).thenReturn("010-1234-5678");

        assertThatThrownBy(() -> service.submit(46408L, form, request))
                .isInstanceOf(PublicFormValidationException.class)
                .satisfies(ex -> assertThat(((PublicFormValidationException) ex).getFieldErrors())
                        .containsKeys("jobText", "address", "priorResearchText"));

        verify(researchApplicationMapper, never()).findDuplicateSeqByNameAndPhone(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
        verify(researchApplicationMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void submitRejectsOversizedAdditionalAnswersBeforeInsert() {
        LegacyPublicApplicationService service = service();
        PublicApplicationForm form = validForm();
        form.setExtraAnswers(java.util.List.of("긴 답변".repeat(4000)));
        ResearchMaster researchMaster = researchMaster();
        researchMaster.setAddComment("추가기재사항");

        when(researchMasterService.getResearchMaster(46408L)).thenReturn(researchMaster);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getSession()).thenReturn(session);
        when(protectionService.tryAcquireRateLimitSlot("legacy:46408:127.0.0.1")).thenReturn(true);
        when(protectionService.validateCaptcha(session, form.getCaptchaAnswer())).thenReturn(true);
        when(protectionService.normalizePhone("010-1234-5678")).thenReturn("01012345678");
        when(protectionService.formatPhoneForDisplay("010-1234-5678")).thenReturn("010-1234-5678");

        assertThatThrownBy(() -> service.submit(46408L, form, request))
                .isInstanceOf(PublicFormValidationException.class)
                .satisfies(ex -> assertThat(((PublicFormValidationException) ex).getFieldErrors())
                        .containsKey("extraAnswers[0]"));

        verify(researchApplicationMapper, never()).insert(org.mockito.ArgumentMatchers.any());
    }

    private LegacyPublicApplicationService service() {
        return new LegacyPublicApplicationService(
                researchMasterService,
                researchApplicationMapper,
                legacyApplicationExtraAnswerMapper,
                legacyApplicationSearchIndexMapper,
                protectionService,
                legacyMatchingService,
                legacyApplicationConsentService
        );
    }

    private ResearchMaster researchMaster() {
        ResearchMaster researchMaster = new ResearchMaster();
        researchMaster.setResearchNo(46408L);
        researchMaster.setResearchTitle("테스트 좌담회");
        return researchMaster;
    }

    private PublicApplicationForm validForm() {
        PublicApplicationForm form = new PublicApplicationForm();
        form.setApplicantName("김테스트");
        form.setGenderCode("1");
        form.setBirthDate(LocalDate.of(1990, 1, 1));
        form.setAgeText("36");
        form.setJobText("회사원");
        form.setOrganizationText("테스트회사");
        form.setMobilePhone("010-1234-5678");
        form.setAddress("서울시 강남구");
        form.setEmailAddress("applicant@example.com");
        form.setProvideYn(true);
        form.setCaptchaAnswer("1234");
        return form;
    }

    private void assertInvalidApplicantName(String applicantName) {
        LegacyPublicApplicationService service = service();
        PublicApplicationForm form = validForm();
        form.setApplicantName(applicantName);

        when(researchMasterService.getResearchMaster(46408L)).thenReturn(researchMaster());
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getSession()).thenReturn(session);
        when(protectionService.tryAcquireRateLimitSlot("legacy:46408:127.0.0.1")).thenReturn(true);
        when(protectionService.validateCaptcha(session, form.getCaptchaAnswer())).thenReturn(true);
        when(protectionService.normalizePhone("010-1234-5678")).thenReturn("01012345678");

        assertThatThrownBy(() -> service.submit(46408L, form, request))
                .isInstanceOf(PublicFormValidationException.class)
                .satisfies(ex -> assertThat(((PublicFormValidationException) ex).getFieldErrors())
                        .containsKey("applicantName"));
    }
}
