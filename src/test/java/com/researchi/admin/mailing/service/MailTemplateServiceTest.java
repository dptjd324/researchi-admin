package com.researchi.admin.mailing.service;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.mailing.domain.AdminMailTemplate;
import com.researchi.admin.mailing.mapper.AdminMailTemplateMapper;
import com.researchi.admin.mailing.web.MailTemplateForm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailTemplateServiceTest {

    @Mock
    private AdminMailTemplateMapper adminMailTemplateMapper;
    @Mock
    private AdminActionLogService adminActionLogService;

    @InjectMocks
    private MailTemplateService mailTemplateService;

    @Test
    void saveCreatesMailTemplate() {
        doAnswer(invocation -> {
            AdminMailTemplate template = invocation.getArgument(0);
            template.setId(9L);
            return null;
        }).when(adminMailTemplateMapper).insert(any(AdminMailTemplate.class));

        MailTemplateForm form = new MailTemplateForm();
        form.setTemplateName(" Default ");
        form.setMailSubject(" Subject ");
        form.setMailBody(" Body ");
        form.setActive(Boolean.TRUE);

        Long savedId = mailTemplateService.save(
                form,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        assertThat(savedId).isEqualTo(9L);
        ArgumentCaptor<AdminMailTemplate> captor = ArgumentCaptor.forClass(AdminMailTemplate.class);
        verify(adminMailTemplateMapper).insert(captor.capture());
        assertThat(captor.getValue().getTemplateName()).isEqualTo("Default");
        assertThat(captor.getValue().getMailSubject()).isEqualTo("Subject");
        assertThat(captor.getValue().getMailBody()).isEqualTo("Body");
        assertThat(captor.getValue().getActiveYn()).isEqualTo("Y");
    }

    @Test
    void saveUpdatesMailTemplate() {
        AdminMailTemplate existing = new AdminMailTemplate();
        existing.setId(9L);
        when(adminMailTemplateMapper.findById(9L)).thenReturn(existing);

        MailTemplateForm form = new MailTemplateForm();
        form.setId(9L);
        form.setTemplateName("Edited");
        form.setMailSubject("Edited Subject");
        form.setMailBody("Edited Body");
        form.setActive(Boolean.FALSE);

        Long savedId = mailTemplateService.save(
                form,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        assertThat(savedId).isEqualTo(9L);
        verify(adminMailTemplateMapper).update(any(AdminMailTemplate.class));
    }
}
