package com.researchi.admin.mailing.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class MailSendManualForm {

    @NotNull(message = "공고를 선택해 주세요.")
    private Long documentSrl;

    @NotNull(message = "템플릿을 선택해 주세요.")
    private Long templateId;

    @NotBlank(message = "첨부 형식을 선택해 주세요.")
    private String attachmentType = "XLSX";

    public Long getDocumentSrl() { return documentSrl; }
    public void setDocumentSrl(Long documentSrl) { this.documentSrl = documentSrl; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getAttachmentType() { return attachmentType; }
    public void setAttachmentType(String attachmentType) { this.attachmentType = attachmentType; }
}
