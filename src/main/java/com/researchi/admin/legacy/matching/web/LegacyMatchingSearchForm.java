package com.researchi.admin.legacy.matching.web;

import com.researchi.admin.legacy.matching.domain.LegacyMatchingSearchCondition;

public class LegacyMatchingSearchForm {

    private String appSex;
    private String appBirth;
    private String appJob;
    private String appCompany;
    private String appAddr;
    private String addComment;

    public boolean hasAnyParameter() {
        return appSex != null
                || appBirth != null
                || appJob != null
                || appCompany != null
                || appAddr != null
                || addComment != null;
    }

    public boolean hasInput() {
        return toCondition().hasInput();
    }

    public LegacyMatchingSearchCondition toCondition() {
        return new LegacyMatchingSearchCondition(appSex, appBirth, appJob, appCompany, appAddr, addComment);
    }

    public String getAppSex() {
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
}
