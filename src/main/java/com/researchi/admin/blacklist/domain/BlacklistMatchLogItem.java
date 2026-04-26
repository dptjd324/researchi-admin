package com.researchi.admin.blacklist.domain;

import java.time.LocalDateTime;

public class BlacklistMatchLogItem {

    private Long id;
    private Long applicationId;
    private Long blacklistId;
    private String applicantName;
    private Long documentSrl;
    private String applicationStatus;
    private String blackModeApplied;
    private String matchType;
    private String actionTaken;
    private LocalDateTime matchedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getApplicationId() { return applicationId; }
    public void setApplicationId(Long applicationId) { this.applicationId = applicationId; }
    public Long getBlacklistId() { return blacklistId; }
    public void setBlacklistId(Long blacklistId) { this.blacklistId = blacklistId; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public Long getDocumentSrl() { return documentSrl; }
    public void setDocumentSrl(Long documentSrl) { this.documentSrl = documentSrl; }
    public String getApplicationStatus() { return applicationStatus; }
    public void setApplicationStatus(String applicationStatus) { this.applicationStatus = applicationStatus; }
    public String getBlackModeApplied() { return blackModeApplied; }
    public void setBlackModeApplied(String blackModeApplied) { this.blackModeApplied = blackModeApplied; }
    public String getMatchType() { return matchType; }
    public void setMatchType(String matchType) { this.matchType = matchType; }
    public String getActionTaken() { return actionTaken; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }
    public LocalDateTime getMatchedAt() { return matchedAt; }
    public void setMatchedAt(LocalDateTime matchedAt) { this.matchedAt = matchedAt; }
}
