package com.researchi.admin.legacy.matching.domain;

import java.time.LocalDateTime;

public class LegacyMatchingJob {

    private Long id;
    private Long researchNo;
    private String includeKeywordText;
    private String excludeKeywordText;
    private String activeKeywordText;
    private String status;
    private Integer candidatePoolCount;
    private Integer matchedCount;
    private Integer blacklistedExcludedCount;
    private String failReason;
    private LocalDateTime requestedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getResearchNo() {
        return researchNo;
    }

    public void setResearchNo(Long researchNo) {
        this.researchNo = researchNo;
    }

    public String getIncludeKeywordText() {
        return includeKeywordText;
    }

    public String getDisplayIncludeKeywordText() {
        return LegacyMatchingKeywordDisplay.displayIncludeKeywordText(includeKeywordText);
    }

    public void setIncludeKeywordText(String includeKeywordText) {
        this.includeKeywordText = includeKeywordText;
    }

    public String getExcludeKeywordText() {
        return excludeKeywordText;
    }

    public void setExcludeKeywordText(String excludeKeywordText) {
        this.excludeKeywordText = excludeKeywordText;
    }

    public String getActiveKeywordText() {
        return activeKeywordText;
    }

    public void setActiveKeywordText(String activeKeywordText) {
        this.activeKeywordText = activeKeywordText;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCandidatePoolCount() {
        return candidatePoolCount;
    }

    public void setCandidatePoolCount(Integer candidatePoolCount) {
        this.candidatePoolCount = candidatePoolCount;
    }

    public Integer getMatchedCount() {
        return matchedCount;
    }

    public void setMatchedCount(Integer matchedCount) {
        this.matchedCount = matchedCount;
    }

    public Integer getBlacklistedExcludedCount() {
        return blacklistedExcludedCount;
    }

    public void setBlacklistedExcludedCount(Integer blacklistedExcludedCount) {
        this.blacklistedExcludedCount = blacklistedExcludedCount;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }
}
