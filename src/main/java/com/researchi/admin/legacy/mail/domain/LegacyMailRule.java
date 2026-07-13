package com.researchi.admin.legacy.mail.domain;

import java.time.LocalDateTime;

public class LegacyMailRule {

    private Long id;
    private Long researchNo;
    private Integer thresholdCount;
    private Long templateId;
    private String directMailSubject;
    private String directMailBody;
    private String attachmentType;
    private String enabledYn;
    private LocalDateTime lastTriggeredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getResearchNo() { return researchNo; }
    public void setResearchNo(Long researchNo) { this.researchNo = researchNo; }
    public Integer getThresholdCount() { return thresholdCount; }
    public void setThresholdCount(Integer thresholdCount) { this.thresholdCount = thresholdCount; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getDirectMailSubject() { return directMailSubject; }
    public void setDirectMailSubject(String directMailSubject) { this.directMailSubject = directMailSubject; }
    public String getDirectMailBody() { return directMailBody; }
    public void setDirectMailBody(String directMailBody) { this.directMailBody = directMailBody; }
    public String getAttachmentType() { return attachmentType; }
    public void setAttachmentType(String attachmentType) { this.attachmentType = attachmentType; }
    public String getEnabledYn() { return enabledYn; }
    public void setEnabledYn(String enabledYn) { this.enabledYn = enabledYn; }
    public LocalDateTime getLastTriggeredAt() { return lastTriggeredAt; }
    public void setLastTriggeredAt(LocalDateTime lastTriggeredAt) { this.lastTriggeredAt = lastTriggeredAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public boolean isEnabled() {
        return "Y".equalsIgnoreCase(enabledYn);
    }
}
