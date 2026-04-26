package com.researchi.admin.form.web;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.form.service.FormFieldService;
import com.researchi.admin.job.domain.JobDetail;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.xe.domain.XeJobDocument;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.validation.BeanPropertyBindingResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FormFieldControllerTest {

    @Mock
    private JobService jobService;

    @Mock
    private FormFieldService formFieldService;

    @InjectMocks
    private FormFieldController formFieldController;

    @Test
    void fieldsPopulatesModel() {
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));
        when(formFieldService.getForm(9L, null)).thenReturn(new FormFieldForm());
        when(formFieldService.getFields(9L)).thenReturn(List.of());

        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = formFieldController.fields(9L, null, model, new MockHttpServletRequest(), null);

        assertThat(viewName).isEqualTo("form/fields");
        assertThat(model.get("documentSrl")).isEqualTo(9L);
        assertThat(model.get("jobDetail")).isNotNull();
    }

    @Test
    void updateFieldRedirectsToSavedField() {
        when(jobService.getJob(9L)).thenReturn(jobDetail(9L));
        when(formFieldService.saveField(eq(9L), any(FormFieldForm.class), any(AdminPrincipal.class), any(HttpServletRequest.class)))
                .thenReturn(22L);

        FormFieldForm form = new FormFieldForm();
        form.setFieldKey("notes");
        form.setFieldLabel("Notes");
        form.setFieldType("TEXTAREA");
        form.setFieldOrder(1);
        form.setActive(true);

        String viewName = formFieldController.updateField(
                9L,
                22L,
                principal(),
                form,
                new BeanPropertyBindingResult(form, "fieldForm"),
                new MockHttpServletRequest(),
                new ExtendedModelMap()
        );

        assertThat(viewName).isEqualTo("redirect:/jobs/9/fields?saved&editFieldId=22");
        verify(formFieldService).saveField(eq(9L), any(FormFieldForm.class), any(AdminPrincipal.class), any(HttpServletRequest.class));
    }

    private JobDetail jobDetail(Long documentSrl) {
        XeJobDocument document = new XeJobDocument();
        document.setDocumentSrl(documentSrl);
        document.setTitle("Test Job");
        document.setMid("newjob");
        document.setStatus("PUBLIC");
        return new JobDetail(document, null);
    }

    private AdminPrincipal principal() {
        return new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1));
    }
}
