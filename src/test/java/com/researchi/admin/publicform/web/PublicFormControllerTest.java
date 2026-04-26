package com.researchi.admin.publicform.web;

import com.researchi.admin.form.domain.FormFieldDetail;
import com.researchi.admin.job.domain.AdminJobMeta;
import com.researchi.admin.job.domain.JobDetail;
import com.researchi.admin.publicform.domain.PublicFormPage;
import com.researchi.admin.publicform.domain.PublicFormSubmissionResult;
import com.researchi.admin.publicform.domain.PublicFormSubmissionStatus;
import com.researchi.admin.publicform.domain.PublicFormUnavailableException;
import com.researchi.admin.publicform.domain.PublicFormValidationException;
import com.researchi.admin.publicform.service.PublicFormService;
import com.researchi.admin.xe.domain.XeJobDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicFormControllerTest {

    @Mock
    private PublicFormService publicFormService;

    @InjectMocks
    private PublicFormController publicFormController;

    @Test
    void applyFormPopulatesModel() {
        when(publicFormService.getPage(eq(9L), any())).thenReturn(page(9L));

        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new MockHttpSession());

        String viewName = publicFormController.applyForm(9L, model, request, null);

        assertThat(viewName).isEqualTo("publicform/apply");
        assertThat(model.get("jobDetail")).isNotNull();
        assertThat(model.get("dynamicFields")).isNotNull();
        assertThat(model.get("applicationFormNoticeItems")).isEqualTo(List.of("결혼여부", "자녀유무", "알러지 유무"));
        PublicApplicationForm form = (PublicApplicationForm) model.get("applicationForm");
        assertThat(form.getExtraAnswers()).hasSize(3);
    }

    @Test
    void submitRedirectsToDuplicatePage() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new MockHttpSession());
        request.addParameter("field_41", "weekday");
        when(publicFormService.getPage(eq(9L), any())).thenReturn(page(9L));
        when(publicFormService.extractDynamicValues(eq(9L), any())).thenReturn(Map.of(41L, List.of("weekday")));
        when(publicFormService.submit(eq(9L), any(), any(), any()))
                .thenReturn(new PublicFormSubmissionResult(PublicFormSubmissionStatus.DUPLICATE, 12L));

        PublicApplicationForm form = validForm();

        String viewName = publicFormController.submitApplication(9L, form, new BeanPropertyBindingResult(form, "applicationForm"), new ExtendedModelMap(), request);

        assertThat(viewName).isEqualTo("redirect:/apply/9/duplicate");
    }

    @Test
    void submitReturnsApplyViewWhenValidationFails() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new MockHttpSession());
        request.addParameter("field_41", "");
        when(publicFormService.getPage(eq(9L), any())).thenReturn(page(9L));
        when(publicFormService.extractDynamicValues(eq(9L), any())).thenReturn(Map.of(41L, List.of()));
        when(publicFormService.submit(eq(9L), any(), any(), any()))
                .thenThrow(new PublicFormValidationException(Map.of("extraAnswers[1]", "추가기재사항 답변을 입력해주세요."), Map.of(41L, "필수 항목입니다."), null));

        PublicApplicationForm form = validForm();
        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = publicFormController.submitApplication(9L, form, new BeanPropertyBindingResult(form, "applicationForm"), model, request);

        assertThat(viewName).isEqualTo("publicform/apply");
        assertThat(model.get("dynamicErrors")).isNotNull();
    }

    @Test
    void applyFormReturnsResultWhenUnavailable() {
        when(publicFormService.getPage(eq(9L), any())).thenThrow(new PublicFormUnavailableException("현재 지원서를 접수할 수 없습니다."));

        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setSession(new MockHttpSession());

        String viewName = publicFormController.applyForm(9L, model, request, null);

        assertThat(viewName).isEqualTo("publicform/result");
        assertThat(model.get("resultTitle")).isEqualTo("지원 불가");
    }

    @Test
    void completePageUsesResultTemplate() {
        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = publicFormController.complete(9L, model);

        assertThat(viewName).isEqualTo("publicform/result");
        assertThat(model.get("resultTitle")).isEqualTo("지원 완료");
        assertThat(model.get("resultMessage")).isEqualTo("지원서가 정상적으로 제출되었습니다.");
    }

    private PublicApplicationForm validForm() {
        PublicApplicationForm form = new PublicApplicationForm();
        form.setApplicantName("Applicant");
        form.setGenderCode("F");
        form.setBirthDate(LocalDate.of(1995, 5, 2));
        form.setAgeText("30");
        form.setJobText("Research participant");
        form.setOrganizationText("Researchi");
        form.setMobilePhone("01012345678");
        form.setAddress("123 Teheran-ro");
        form.setExtraAnswers(List.of("기혼", "없음", "없음"));
        form.setProvideYn(true);
        return form;
    }

    private PublicFormPage page(Long documentSrl) {
        XeJobDocument document = new XeJobDocument();
        document.setDocumentSrl(documentSrl);
        document.setTitle("Public Job");
        document.setContent("Job content");
        document.setStatus("PUBLIC");
        document.setMid("newjob");

        AdminJobMeta meta = new AdminJobMeta();
        meta.setDocumentSrl(documentSrl);
        meta.setRecruitStatus("RECRUITING");
        meta.setApplicationEnabled("Y");
        meta.setApplicationFormNotice("결혼여부/자녀유무\n알러지 유무");

        return new PublicFormPage(
                new JobDetail(document, meta),
                List.of(new FormFieldDetail(41L, "availability", "Availability", "SELECT", 1, true, null, null, List.of(), true)),
                "1 + 1 = ?",
                true
        );
    }
}
