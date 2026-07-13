package com.researchi.admin.legacy.matching.domain;

public class LegacyApplicationKeyword {

    private Long researchNo;
    private Long researchAppSeq;
    private String applicationRegistDt;
    private String keywordNormalized;
    private String keyword;
    private String sourceType;

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

    public String getApplicationRegistDt() {
        return applicationRegistDt;
    }

    public void setApplicationRegistDt(String applicationRegistDt) {
        this.applicationRegistDt = applicationRegistDt;
    }

    public String getKeywordNormalized() {
        return keywordNormalized;
    }

    public void setKeywordNormalized(String keywordNormalized) {
        this.keywordNormalized = keywordNormalized;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }
}
