package com.researchi.admin.matching.domain;

public class MatchingTargetView extends AdminKeywordMatchTarget {

    private Long documentSrl;
    private String applicantName;
    private String jobTitle;
    private String applicationStatus;
    private String emailAddressEnc;
    private String emailAddressMasked;
    private String emailAddressDisplay;
    private String mobilePhoneEnc;
    private String mobilePhoneMasked;
    private String mobilePhoneDisplay;

    public Long getDocumentSrl() {
        return documentSrl;
    }

    public void setDocumentSrl(Long documentSrl) {
        this.documentSrl = documentSrl;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public void setJobTitle(String jobTitle) {
        this.jobTitle = jobTitle;
    }

    public String getApplicationStatus() {
        return applicationStatus;
    }

    public void setApplicationStatus(String applicationStatus) {
        this.applicationStatus = applicationStatus;
    }

    public String getEmailAddressMasked() {
        return emailAddressMasked;
    }

    public void setEmailAddressMasked(String emailAddressMasked) {
        this.emailAddressMasked = emailAddressMasked;
    }

    public String getEmailAddressEnc() {
        return emailAddressEnc;
    }

    public void setEmailAddressEnc(String emailAddressEnc) {
        this.emailAddressEnc = emailAddressEnc;
    }

    public String getEmailAddressDisplay() {
        return emailAddressDisplay;
    }

    public void setEmailAddressDisplay(String emailAddressDisplay) {
        this.emailAddressDisplay = emailAddressDisplay;
    }

    public String getMobilePhoneMasked() {
        return mobilePhoneMasked;
    }

    public void setMobilePhoneMasked(String mobilePhoneMasked) {
        this.mobilePhoneMasked = mobilePhoneMasked;
    }

    public String getMobilePhoneEnc() {
        return mobilePhoneEnc;
    }

    public void setMobilePhoneEnc(String mobilePhoneEnc) {
        this.mobilePhoneEnc = mobilePhoneEnc;
    }

    public String getMobilePhoneDisplay() {
        return mobilePhoneDisplay;
    }

    public void setMobilePhoneDisplay(String mobilePhoneDisplay) {
        this.mobilePhoneDisplay = mobilePhoneDisplay;
    }
}
