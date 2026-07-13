package com.researchi.admin.legacy.application.domain;

import java.time.LocalDateTime;

public class LegacyApplicationConsent {

    private Long id;
    private Long researchNo;
    private Long researchAppSeq;
    private String requiredPrivacyYn;
    private String futureRecruitmentYn;
    private String smsYn;
    private String emailYn;
    private String consentVersion;
    private String noticeSnapshot;
    private LocalDateTime consentedAt;
    private LocalDateTime futureConsentExpiresAt;
    private LocalDateTime withdrawnAt;
    private LocalDateTime createdAt;

    public boolean activeFutureRecruitmentAt(LocalDateTime now) {
        return "Y".equalsIgnoreCase(futureRecruitmentYn)
                && withdrawnAt == null
                && futureConsentExpiresAt != null
                && now != null
                && futureConsentExpiresAt.isAfter(now)
                && ("Y".equalsIgnoreCase(smsYn) || "Y".equalsIgnoreCase(emailYn));
    }

    public boolean allowsSmsAt(LocalDateTime now) {
        return activeFutureRecruitmentAt(now) && "Y".equalsIgnoreCase(smsYn);
    }

    public boolean allowsEmailAt(LocalDateTime now) {
        return activeFutureRecruitmentAt(now) && "Y".equalsIgnoreCase(emailYn);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getResearchNo() { return researchNo; }
    public void setResearchNo(Long researchNo) { this.researchNo = researchNo; }
    public Long getResearchAppSeq() { return researchAppSeq; }
    public void setResearchAppSeq(Long researchAppSeq) { this.researchAppSeq = researchAppSeq; }
    public String getRequiredPrivacyYn() { return requiredPrivacyYn; }
    public void setRequiredPrivacyYn(String requiredPrivacyYn) { this.requiredPrivacyYn = requiredPrivacyYn; }
    public String getFutureRecruitmentYn() { return futureRecruitmentYn; }
    public void setFutureRecruitmentYn(String futureRecruitmentYn) { this.futureRecruitmentYn = futureRecruitmentYn; }
    public String getSmsYn() { return smsYn; }
    public void setSmsYn(String smsYn) { this.smsYn = smsYn; }
    public String getEmailYn() { return emailYn; }
    public void setEmailYn(String emailYn) { this.emailYn = emailYn; }
    public String getConsentVersion() { return consentVersion; }
    public void setConsentVersion(String consentVersion) { this.consentVersion = consentVersion; }
    public String getNoticeSnapshot() { return noticeSnapshot; }
    public void setNoticeSnapshot(String noticeSnapshot) { this.noticeSnapshot = noticeSnapshot; }
    public LocalDateTime getConsentedAt() { return consentedAt; }
    public void setConsentedAt(LocalDateTime consentedAt) { this.consentedAt = consentedAt; }
    public LocalDateTime getFutureConsentExpiresAt() { return futureConsentExpiresAt; }
    public void setFutureConsentExpiresAt(LocalDateTime futureConsentExpiresAt) { this.futureConsentExpiresAt = futureConsentExpiresAt; }
    public LocalDateTime getWithdrawnAt() { return withdrawnAt; }
    public void setWithdrawnAt(LocalDateTime withdrawnAt) { this.withdrawnAt = withdrawnAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
