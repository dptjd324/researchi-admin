package com.researchi.admin.mailing.web;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.mailing.domain.AdminMailTemplate;
import com.researchi.admin.mailing.service.MailTemplateService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailTemplateControllerTest {

    @Mock
    private MailTemplateService mailTemplateService;

    @InjectMocks
    private MailTemplateController mailTemplateController;

    @Test
    void templatesPopulatesModel() {
        AdminMailTemplate template = new AdminMailTemplate();
        template.setId(7L);
        template.setTemplateName("Default");
        template.setActiveYn("Y");
        when(mailTemplateService.getTemplate(7L)).thenReturn(template);
        when(mailTemplateService.getTemplates()).thenReturn(List.of(template));

        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = mailTemplateController.templates(7L, model, new MockHttpServletRequest("GET", "/mail/templates"), null);

        assertThat(viewName).isEqualTo("mail/templates");
        assertThat(model.get("templates")).isEqualTo(List.of(template));
    }

    @Test
    void saveRedirectsAfterSuccessfulSave() {
        MailTemplateForm form = new MailTemplateForm();
        form.setTemplateName("Default");
        form.setMailSubject("Subject");
        form.setMailBody("Body");
        form.setActive(Boolean.TRUE);
        when(mailTemplateService.save(any(), any(), any())).thenReturn(7L);

        String viewName = mailTemplateController.save(
                form,
                new BeanPropertyBindingResult(form, "templateForm"),
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest(),
                new ExtendedModelMap()
        );

        assertThat(viewName).isEqualTo("redirect:/mail/templates?templateId=7&saved");
        verify(mailTemplateService).save(any(), any(), any());
    }
}
