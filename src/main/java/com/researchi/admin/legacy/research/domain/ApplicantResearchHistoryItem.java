package com.researchi.admin.legacy.research.domain;

import com.researchi.admin.common.support.PhoneNumberFormatter;

public class ApplicantResearchHistoryItem {

    private Long researchNo;
    private Long researchAppSeq;
    private String researchTitle;
    private String appName;
    private String appBirth;
    private String appHphone;
    private String appJob;
    private String appCompany;
    private String provideYn;
    private String registDt;

    public Long getResearchNo() { return researchNo; }
    public void setResearchNo(Long researchNo) { this.researchNo = researchNo; }
    public Long getResearchAppSeq() { return researchAppSeq; }
    public void setResearchAppSeq(Long researchAppSeq) { this.researchAppSeq = researchAppSeq; }
    public String getResearchTitle() { return researchTitle; }
    public void setResearchTitle(String researchTitle) { this.researchTitle = researchTitle; }
    public String getAppName() { return appName; }
    public void setAppName(String appName) { this.appName = appName; }
    public String getAppBirth() { return appBirth; }
    public void setAppBirth(String appBirth) { this.appBirth = appBirth; }
    public String getAppHphone() { return appHphone; }
    public void setAppHphone(String appHphone) { this.appHphone = appHphone; }
    public String getAppJob() { return appJob; }
    public void setAppJob(String appJob) { this.appJob = appJob; }
    public String getAppCompany() { return appCompany; }
    public void setAppCompany(String appCompany) { this.appCompany = appCompany; }
    public String getProvideYn() { return provideYn; }
    public void setProvideYn(String provideYn) { this.provideYn = provideYn; }
    public String getRegistDt() { return registDt; }
    public void setRegistDt(String registDt) { this.registDt = registDt; }

    public String getAppHphoneLabel() {
        String formatted = PhoneNumberFormatter.formatForDisplay(appHphone);
        return formatted == null ? appHphone : formatted;
    }
}
