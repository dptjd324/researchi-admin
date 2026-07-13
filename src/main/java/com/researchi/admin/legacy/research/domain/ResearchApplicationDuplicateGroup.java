package com.researchi.admin.legacy.research.domain;

public class ResearchApplicationDuplicateGroup {

    private Long researchNo;
    private Long researchAppSeq;
    private Integer duplicateCount;
    private String appNames;
    private String appHphones;
    private String appBirths;
    private String provideYns;
    private String firstRegistDt;
    private String lastModifyDt;

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

    public Integer getDuplicateCount() {
        return duplicateCount;
    }

    public void setDuplicateCount(Integer duplicateCount) {
        this.duplicateCount = duplicateCount;
    }

    public String getAppNames() {
        return appNames;
    }

    public void setAppNames(String appNames) {
        this.appNames = appNames;
    }

    public String getAppHphones() {
        return appHphones;
    }

    public void setAppHphones(String appHphones) {
        this.appHphones = appHphones;
    }

    public String getAppBirths() {
        return appBirths;
    }

    public void setAppBirths(String appBirths) {
        this.appBirths = appBirths;
    }

    public String getProvideYns() {
        return provideYns;
    }

    public void setProvideYns(String provideYns) {
        this.provideYns = provideYns;
    }

    public String getFirstRegistDt() {
        return firstRegistDt;
    }

    public void setFirstRegistDt(String firstRegistDt) {
        this.firstRegistDt = firstRegistDt;
    }

    public String getLastModifyDt() {
        return lastModifyDt;
    }

    public void setLastModifyDt(String lastModifyDt) {
        this.lastModifyDt = lastModifyDt;
    }
}
