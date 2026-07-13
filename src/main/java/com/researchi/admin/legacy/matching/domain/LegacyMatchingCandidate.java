package com.researchi.admin.legacy.matching.domain;

public class LegacyMatchingCandidate {

    private Long researchNo;
    private Long researchAppSeq;
    private Integer matchScore;

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

    public Integer getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Integer matchScore) {
        this.matchScore = matchScore;
    }
}
