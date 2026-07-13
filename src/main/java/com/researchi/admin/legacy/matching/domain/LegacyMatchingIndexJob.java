package com.researchi.admin.legacy.matching.domain;

import java.time.LocalDateTime;

public class LegacyMatchingIndexJob {

    private Long id;
    private Long researchNo;
    private Integer cycleNo;
    private String includeKeywordText;
    private String excludeKeywordText;
    private Integer appliedYears;
    private Integer indexLimit;
    private Integer batchSize;
    private String requireContactYn;
    private String excludeBlacklistYn;
    private String resetBeforeRunYn;
    private String status;
    private Integer indexedApplicationCount;
    private Integer insertedKeywordCount;
    private Integer skippedAlreadyIndexedCount;
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

    public Integer getCycleNo() {
        return cycleNo;
    }

    public void setCycleNo(Integer cycleNo) {
        this.cycleNo = cycleNo;
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

    public Integer getAppliedYears() {
        return appliedYears;
    }

    public void setAppliedYears(Integer appliedYears) {
        this.appliedYears = appliedYears;
    }

    public Integer getIndexLimit() {
        return indexLimit;
    }

    public void setIndexLimit(Integer indexLimit) {
        this.indexLimit = indexLimit;
    }

    public Integer getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public String getRequireContactYn() {
        return requireContactYn;
    }

    public void setRequireContactYn(String requireContactYn) {
        this.requireContactYn = requireContactYn;
    }

    public String getExcludeBlacklistYn() {
        return excludeBlacklistYn;
    }

    public void setExcludeBlacklistYn(String excludeBlacklistYn) {
        this.excludeBlacklistYn = excludeBlacklistYn;
    }

    public String getResetBeforeRunYn() {
        return resetBeforeRunYn;
    }

    public void setResetBeforeRunYn(String resetBeforeRunYn) {
        this.resetBeforeRunYn = resetBeforeRunYn;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getIndexedApplicationCount() {
        return indexedApplicationCount;
    }

    public void setIndexedApplicationCount(Integer indexedApplicationCount) {
        this.indexedApplicationCount = indexedApplicationCount;
    }

    public Integer getInsertedKeywordCount() {
        return insertedKeywordCount;
    }

    public void setInsertedKeywordCount(Integer insertedKeywordCount) {
        this.insertedKeywordCount = insertedKeywordCount;
    }

    public Integer getSkippedAlreadyIndexedCount() {
        return skippedAlreadyIndexedCount;
    }

    public void setSkippedAlreadyIndexedCount(Integer skippedAlreadyIndexedCount) {
        this.skippedAlreadyIndexedCount = skippedAlreadyIndexedCount;
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
