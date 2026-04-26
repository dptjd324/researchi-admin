package com.researchi.admin.matching.domain;

public class MatchingTargetView extends AdminKeywordMatchTarget {

    private Long documentSrl;
    private String applicantName;
    private String jobTitle;
    private String applicationStatus;
    private String emailAddressMasked;
    private String mobilePhoneMasked;

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

    public String getMobilePhoneMasked() {
        return mobilePhoneMasked;
    }

    public void setMobilePhoneMasked(String mobilePhoneMasked) {
        this.mobilePhoneMasked = mobilePhoneMasked;
    }
}
