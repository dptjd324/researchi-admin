package com.researchi.admin.mailing.web;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.mailing.domain.MailingPreview;
import com.researchi.admin.mailing.service.MailTemplateService;
import com.researchi.admin.mailing.service.MailingService;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailingControllerTest {

    @Mock
    private MailingService mailingService;
    @Mock
    private MailTemplateService mailTemplateService;
    @Mock
    private JobService jobService;

    @InjectMocks
    private MailingController mailingController;

    @Test
    void historyPopulatesModel() {
        when(jobService.getJobs()).thenReturn(List.of());
        when(mailTemplateService.getActiveTemplates()).thenReturn(List.of());
        when(mailingService.getHistory(9L)).thenReturn(List.of());
        when(mailingService.getPreview(9L)).thenReturn(new MailingPreview(9L, "Survey Job", List.of("client@example.com"), 1, 0, 3, 1));

        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = mailingController.history(9L, model, new MockHttpServletRequest("GET", "/mail/send/history"), null);

        assertThat(viewName).isEqualTo("mail/history");
        assertThat(model.get("selectedDocumentSrl")).isEqualTo(9L);
        assertThat(model.get("historyItems")).isEqualTo(List.of());
        assertThat(model.get("manualPreview")).isNotNull();
        assertThat(model.get("schedulePreview")).isNotNull();
        assertThat(model.get("thresholdPreview")).isNotNull();
    }

    @Test
    void manualRedirectsAfterSuccessfulSend() {
        MailSendManualForm form = new MailSendManualForm();
        form.setDocumentSrl(9L);
        form.setTemplateId(3L);
        form.setAttachmentType("XLSX");
        when(mailingService.sendManual(any(), any(), any())).thenReturn(55L);

        String viewName = mailingController.manual(
                form,
                new BeanPropertyBindingResult(form, "manualForm"),
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest(),
                new ExtendedModelMap()
        );

        assertThat(viewName).isEqualTo("redirect:/mail/send/history?documentSrl=9&manualSent");
        verify(mailingService).sendManual(any(), any(), any());
    }

    @Test
    void manualRendersHistoryWhenSendFails() {
        MailSendManualForm form = new MailSendManualForm();
        form.setDocumentSrl(9L);
        form.setTemplateId(3L);
        form.setAttachmentType("XLSX");
        when(mailingService.sendManual(any(), any(), any())).thenThrow(new IllegalStateException("발송 대상 지원서가 없어 메일을 보내지 않았습니다."));
        when(jobService.getJobs()).thenReturn(List.of());
        when(mailTemplateService.getActiveTemplates()).thenReturn(List.of());
        when(mailingService.getHistory(9L)).thenReturn(List.of());
        when(mailingService.getPreview(9L)).thenReturn(new MailingPreview(9L, "Survey Job", List.of("client@example.com"), 1, 0, 0, 0));

        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = mailingController.manual(
                form,
                new BeanPropertyBindingResult(form, "manualForm"),
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest(),
                model
        );

        assertThat(viewName).isEqualTo("mail/history");
        assertThat(model.get("manualPreview")).isNotNull();
    }

    @Test
    void cancelRedirectsAfterSuccessfulCancellation() {
        String viewName = mailingController.cancel(
                77L,
                9L,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest(),
                new ExtendedModelMap()
        );

        assertThat(viewName).isEqualTo("redirect:/mail/send/history?documentSrl=9&cancelled");
        verify(mailingService).cancelSendJob(any(), any(), any());
    }

    @Test
    void cancelRendersHistoryWhenCancellationFails() {
        doThrow(new IllegalStateException("예약 상태인 메일만 취소할 수 있습니다."))
                .when(mailingService)
                .cancelSendJob(any(), any(), any());
        when(jobService.getJobs()).thenReturn(List.of());
        when(mailTemplateService.getActiveTemplates()).thenReturn(List.of());
        when(mailingService.getHistory(9L)).thenReturn(List.of());
        when(mailingService.getPreview(9L)).thenReturn(new MailingPreview(9L, "Survey Job", List.of("client@example.com"), 1, 0, 1, 0));

        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = mailingController.cancel(
                77L,
                9L,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest(),
                model
        );

        assertThat(viewName).isEqualTo("mail/history");
        assertThat(model.get("cancelError")).isEqualTo("예약 상태인 메일만 취소할 수 있습니다.");
    }
}
