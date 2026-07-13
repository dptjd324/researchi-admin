package com.researchi.admin.legacy.research.domain;

import com.researchi.admin.common.support.PhoneNumberFormatter;

public class ResearchApplication {

    private Long researchNo;
    private Long researchAppSeq;
    private String appName;
    private String appSex;
    private String appBirth;
    private String appAge;
    private String appJob;
    private String appCompany;
    private String appHphone;
    private String appTele;
    private String appEmail;
    private String appAddr;
    private String addComment;
    private String attendResearch;
    private String provideYn;
    private String registDt;
    private String modifyDt;
    private String blacklistYn;
    private String researchTitle;

    public Long getResearchNo() {
        return researchNo;
    }

    public void setResearchNo(Long researchNo) {
        this.researchNo = researchNo;
    }

    public Long getResearchAppSeq() {
        return researchAppSeq;
    }

    public void setResearchAppSeq(Long researchAppSeq) {
        this.researchAppSeq = researchAppSeq;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppSex() {
        return appSex;
    }

    public String getAppSexLabel() {
        if ("1".equals(appSex)) {
            return "남자";
        }
        if ("2".equals(appSex)) {
            return "여자";
        }
        return appSex;
    }

    public void setAppSex(String appSex) {
        this.appSex = appSex;
    }

    public String getAppBirth() {
        return appBirth;
    }

    public void setAppBirth(String appBirth) {
        this.appBirth = appBirth;
    }

    public String getAppAge() {
        return appAge;
    }

    public void setAppAge(String appAge) {
        this.appAge = appAge;
    }

    public String getAppJob() {
        return appJob;
    }

    public void setAppJob(String appJob) {
        this.appJob = appJob;
    }

    public String getAppCompany() {
        return appCompany;
    }

    public void setAppCompany(String appCompany) {
        this.appCompany = appCompany;
    }

    public String getAppHphone() {
        return appHphone;
    }

    public String getAppHphoneLabel() {
        return formatPhone(appHphone);
    }

    public void setAppHphone(String appHphone) {
        this.appHphone = appHphone;
    }

    public String getAppTele() {
        return appTele;
    }

    public String getAppTeleLabel() {
        return formatPhone(appTele);
    }

    public void setAppTele(String appTele) {
        this.appTele = appTele;
    }

    public String getAppEmail() {
        return appEmail;
    }

    public void setAppEmail(String appEmail) {
        this.appEmail = appEmail;
    }

    public String getAppAddr() {
        return appAddr;
    }

    public void setAppAddr(String appAddr) {
        this.appAddr = appAddr;
    }

    public String getAddComment() {
        return addComment;
    }

    public void setAddComment(String addComment) {
        this.addComment = addComment;
    }

    public String getAttendResearch() {
        return attendResearch;
    }

    public void setAttendResearch(String attendResearch) {
        this.attendResearch = attendResearch;
    }

    public String getProvideYn() {
        return provideYn;
    }

    public void setProvideYn(String provideYn) {
        this.provideYn = provideYn;
    }

    public String getRegistDt() {
        return registDt;
    }

    public void setRegistDt(String registDt) {
        this.registDt = registDt;
    }

    public String getModifyDt() {
        return modifyDt;
    }

    public void setModifyDt(String modifyDt) {
        this.modifyDt = modifyDt;
    }

    public String getBlacklistYn() {
        return blacklistYn;
    }

    public void setBlacklistYn(String blacklistYn) {
        this.blacklistYn = blacklistYn;
    }

    public boolean isBlacklisted() {
        return "Y".equalsIgnoreCase(blacklistYn);
    }

    public String getResearchTitle() {
        return researchTitle;
    }

    public void setResearchTitle(String researchTitle) {
        this.researchTitle = researchTitle;
    }

    public String getProvidePreviewLine() {
        return String.join("/",
                display(appName),
                display(getAppSexLabel()),
                display(appBirth),
                display(appAge),
                display(appJob),
                display(appCompany),
                display(getAppHphoneLabel()),
                display(getAppTeleLabel()),
                display(appEmail),
                display(appAddr),
                display(addComment)
        );
    }

    private String formatPhone(String value) {
        String formatted = PhoneNumberFormatter.formatForDisplay(value);
        return formatted == null ? value : formatted;
    }

    private String display(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim();
    }
}
