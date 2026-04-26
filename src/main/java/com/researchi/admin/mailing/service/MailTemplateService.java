package com.researchi.admin.mailing.service;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.mailing.domain.AdminMailTemplate;
import com.researchi.admin.mailing.mapper.AdminMailTemplateMapper;
import com.researchi.admin.mailing.web.MailTemplateForm;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MailTemplateService {

    private final AdminMailTemplateMapper adminMailTemplateMapper;
    private final AdminActionLogService adminActionLogService;

    public MailTemplateService(
            AdminMailTemplateMapper adminMailTemplateMapper,
            AdminActionLogService adminActionLogService
    ) {
        this.adminMailTemplateMapper = adminMailTemplateMapper;
        this.adminActionLogService = adminActionLogService;
    }

    public List<AdminMailTemplate> getTemplates() {
        return adminMailTemplateMapper.findAll();
    }

    public List<AdminMailTemplate> getActiveTemplates() {
        return adminMailTemplateMapper.findActive();
    }

    public AdminMailTemplate getTemplate(Long id) {
        return id == null ? null : adminMailTemplateMapper.findById(id);
    }

    @Transactional("adminTransactionManager")
    public Long save(MailTemplateForm form, AdminPrincipal principal, HttpServletRequest request) {
        AdminMailTemplate template = new AdminMailTemplate();
        template.setId(form.getId());
        template.setTemplateName(form.getTemplateName().trim());
        template.setMailSubject(form.getMailSubject().trim());
        template.setMailBody(form.getMailBody().trim());
        template.setActiveYn(Boolean.TRUE.equals(form.getActive()) ? "Y" : "N");

        if (template.getId() == null) {
            adminMailTemplateMapper.insert(template);
            adminActionLogService.log(principal.getId(), "MAIL_TEMPLATE_CREATE", "MAIL_TEMPLATE", String.valueOf(template.getId()), "메일 템플릿 생성: " + template.getTemplateName(), request);
        } else {
            if (adminMailTemplateMapper.findById(template.getId()) == null) {
                throw new IllegalArgumentException("메일 템플릿을 찾을 수 없습니다.");
            }
            adminMailTemplateMapper.update(template);
            adminActionLogService.log(principal.getId(), "MAIL_TEMPLATE_UPDATE", "MAIL_TEMPLATE", String.valueOf(template.getId()), "메일 템플릿 수정: " + template.getTemplateName(), request);
        }
        return template.getId();
    }
}
