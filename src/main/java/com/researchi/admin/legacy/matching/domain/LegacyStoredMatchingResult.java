package com.researchi.admin.legacy.matching.domain;

public class LegacyStoredMatchingResult {

    private Long matchingJobId;
    private Long researchNo;
    private Long researchAppSeq;
    private Integer rowNo;
    private Integer matchScore;
    private String matchedKeywordText;

    public Long getMatchingJobId() {
        return matchingJobId;
    }

    public void setMatchingJobId(Long matchingJobId) {
        this.matchingJobId = matchingJobId;
    }

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

    public Integer getRowNo() {
        return rowNo;
    }

    public void setRowNo(Integer rowNo) {
        this.rowNo = rowNo;
    }

    public Integer getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Integer matchScore) {
        this.matchScore = matchScore;
    }

    public String getMatchedKeywordText() {
        return matchedKeywordText;
    }

    public void setMatchedKeywordText(String matchedKeywordText) {
        this.matchedKeywordText = matchedKeywordText;
    }
}
