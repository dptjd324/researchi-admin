package com.researchi.admin.export.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ExportApplicationSource {

    private Long id;
    private Long documentSrl;
    private String applicantName;
    private String genderCode;
    private LocalDate birthDate;
    private String ageText;
    private String jobText;
    private String organizationText;
    private String mobilePhoneEnc;
    private String telPhoneEnc;
    private String regionText;
    private String addressEnc;
    private String extraComment;
    private String priorResearchText;
    private String emailAddressEnc;
    private String notifyEmailYn;
    private String notifySmsYn;
    private String notifyKeywordYn;
    private String applicationStatus;
    private String isNewApplicant;
    private String isBlacklisted;
    private String blackModeApplied;
    private String provideYn;
    private String deliveryStatus;
    private LocalDateTime appliedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDocumentSrl() { return documentSrl; }
    public void setDocumentSrl(Long documentSrl) { this.documentSrl = documentSrl; }
    public String getApplicantName() { return applicantName; }
    public void setApplicantName(String applicantName) { this.applicantName = applicantName; }
    public String getGenderCode() { return genderCode; }
    public void setGenderCode(String genderCode) { this.genderCode = genderCode; }
    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
    public String getAgeText() { return ageText; }
    public void setAgeText(String ageText) { this.ageText = ageText; }
    public String getJobText() { return jobText; }
    public void setJobText(String jobText) { this.jobText = jobText; }
    public String getOrganizationText() { return organizationText; }
    public void setOrganizationText(String organizationText) { this.organizationText = organizationText; }
    public String getMobilePhoneEnc() { return mobilePhoneEnc; }
    public void setMobilePhoneEnc(String mobilePhoneEnc) { this.mobilePhoneEnc = mobilePhoneEnc; }
    public String getTelPhoneEnc() { return telPhoneEnc; }
    public void setTelPhoneEnc(String telPhoneEnc) { this.telPhoneEnc = telPhoneEnc; }
    public String getRegionText() { return regionText; }
    public void setRegionText(String regionText) { this.regionText = regionText; }
    public String getAddressEnc() { return addressEnc; }
    public void setAddressEnc(String addressEnc) { this.addressEnc = addressEnc; }
    public String getExtraComment() { return extraComment; }
    public void setExtraComment(String extraComment) { this.extraComment = extraComment; }
    public String getPriorResearchText() { return priorResearchText; }
    public void setPriorResearchText(String priorResearchText) { this.priorResearchText = priorResearchText; }
    public String getEmailAddressEnc() { return emailAddressEnc; }
    public void setEmailAddressEnc(String emailAddressEnc) { this.emailAddressEnc = emailAddressEnc; }
    public String getNotifyEmailYn() { return notifyEmailYn; }
    public void setNotifyEmailYn(String notifyEmailYn) { this.notifyEmailYn = notifyEmailYn; }
    public String getNotifySmsYn() { return notifySmsYn; }
    public void setNotifySmsYn(String notifySmsYn) { this.notifySmsYn = notifySmsYn; }
    public String getNotifyKeywordYn() { return notifyKeywordYn; }
    public void setNotifyKeywordYn(String notifyKeywordYn) { this.notifyKeywordYn = notifyKeywordYn; }
    public String getApplicationStatus() { return applicationStatus; }
    public void setApplicationStatus(String applicationStatus) { this.applicationStatus = applicationStatus; }
    public String getIsNewApplicant() { return isNewApplicant; }
    public void setIsNewApplicant(String isNewApplicant) { this.isNewApplicant = isNewApplicant; }
    public String getIsBlacklisted() { return isBlacklisted; }
    public void setIsBlacklisted(String isBlacklisted) { this.isBlacklisted = isBlacklisted; }
    public String getBlackModeApplied() { return blackModeApplied; }
    public void setBlackModeApplied(String blackModeApplied) { this.blackModeApplied = blackModeApplied; }
    public String getProvideYn() { return provideYn; }
    public void setProvideYn(String provideYn) { this.provideYn = provideYn; }
    public String getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(String deliveryStatus) { this.deliveryStatus = deliveryStatus; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
}
