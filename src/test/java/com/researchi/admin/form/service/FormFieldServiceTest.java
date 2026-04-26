package com.researchi.admin.form.service;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.form.domain.AdminFormField;
import com.researchi.admin.form.mapper.AdminFormFieldMapper;
import com.researchi.admin.form.web.FormFieldForm;
import com.researchi.admin.job.domain.JobDetail;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.xe.domain.XeJobDocument;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FormFieldServiceTest {

    @Mock
    private JobService jobService;

    @Mock
    private AdminFormFieldMapper adminFormFieldMapper;

    @Mock
    private AdminActionLogService adminActionLogService;

    @Test
    void saveFieldCreatesOptionBackedFieldAndLogsAction() {
        FormFieldService formFieldService = new FormFieldService(
                jobService,
                adminFormFieldMapper,
                adminActionLogService
        );
        when(jobService.getJob(7L)).thenReturn(jobDetail(7L));
        when(adminFormFieldMapper.findByDocumentSrlAndFieldKey(7L, "availability")).thenReturn(null);
        doAnswer(invocation -> {
            AdminFormField field = invocation.getArgument(0);
            field.setId(41L);
            return null;
        }).when(adminFormFieldMapper).insert(any(AdminFormField.class));
        doNothing().when(adminActionLogService).log(any(), any(), any(), any(), any(), any());

        FormFieldForm form = new FormFieldForm();
        form.setFieldKey("availability");
        form.setFieldLabel("Availability");
        form.setFieldType("SELECT");
        form.setFieldOrder(1);
        form.setRequired(true);
        form.setOptionsText("weekday|Weekday\nweekend|Weekend");
        form.setActive(true);

        Long fieldId = formFieldService.saveField(7L, form, principal(), request());

        assertThat(fieldId).isEqualTo(41L);
        ArgumentCaptor<AdminFormField> captor = ArgumentCaptor.forClass(AdminFormField.class);
        verify(adminFormFieldMapper).insert(captor.capture());
        assertThat(captor.getValue().getOptionsJson()).contains("weekday");
        assertThat(captor.getValue().getRequiredYn()).isEqualTo("Y");
        verify(adminActionLogService).log(eq(1L), eq("FORM_FIELD_CREATE"), eq("FORM_FIELD"), eq("41"), eq("Created field for job 7"), any(HttpServletRequest.class));
    }

    @Test
    void saveFieldRejectsDuplicateFieldKey() {
        FormFieldService formFieldService = new FormFieldService(
                jobService,
                adminFormFieldMapper,
                adminActionLogService
        );
        when(jobService.getJob(7L)).thenReturn(jobDetail(7L));
        AdminFormField existing = new AdminFormField();
        existing.setId(12L);
        existing.setDocumentSrl(7L);
        existing.setFieldKey("availability");
        when(adminFormFieldMapper.findByDocumentSrlAndFieldKey(7L, "availability")).thenReturn(existing);

        FormFieldForm form = new FormFieldForm();
        form.setFieldKey("availability");
        form.setFieldLabel("Availability");
        form.setFieldType("TEXT");
        form.setFieldOrder(1);
        form.setActive(true);

        assertThatThrownBy(() -> formFieldService.saveField(7L, form, principal(), request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이 공고에 이미 같은 필드 키가 있습니다.");
    }

    @Test
    void getFormMapsStoredOptionsBackToTextareaText() {
        FormFieldService formFieldService = new FormFieldService(
                jobService,
                adminFormFieldMapper,
                adminActionLogService
        );
        when(jobService.getJob(7L)).thenReturn(jobDetail(7L));
        when(adminFormFieldMapper.findById(12L)).thenReturn(existingField());

        FormFieldForm form = formFieldService.getForm(7L, 12L);

        assertThat(form.getFieldKey()).isEqualTo("availability");
        assertThat(form.getOptionsText()).contains("weekday|Weekday");
        assertThat(form.getRequired()).isTrue();
        assertThat(form.getActive()).isTrue();
    }

    @Test
    void deleteFieldRequiresFieldToBelongToJob() {
        FormFieldService formFieldService = new FormFieldService(
                jobService,
                adminFormFieldMapper,
                adminActionLogService
        );
        AdminFormField field = existingField();
        field.setDocumentSrl(8L);
        when(adminFormFieldMapper.findById(12L)).thenReturn(field);

        assertThatThrownBy(() -> formFieldService.deleteField(7L, 12L, principal(), request()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이 공고의 필드를 찾을 수 없습니다.");
    }

    private JobDetail jobDetail(Long documentSrl) {
        XeJobDocument document = new XeJobDocument();
        document.setDocumentSrl(documentSrl);
        document.setTitle("Test Job");
        document.setMid("newjob");
        document.setStatus("PUBLIC");
        return new JobDetail(document, null);
    }

    private AdminFormField existingField() {
        AdminFormField field = new AdminFormField();
        field.setId(12L);
        field.setDocumentSrl(7L);
        field.setFieldKey("availability");
        field.setFieldLabel("Availability");
        field.setFieldType("SELECT");
        field.setFieldOrder(2);
        field.setRequiredYn("Y");
        field.setOptionsJson("[{\"value\":\"weekday\",\"label\":\"Weekday\"}]");
        field.setActiveYn("Y");
        return field;
    }

    private AdminPrincipal principal() {
        return new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1));
    }

    private HttpServletRequest request() {
        return new org.springframework.mock.web.MockHttpServletRequest();
    }
}
