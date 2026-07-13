package com.researchi.admin.legacy.research.domain;

import java.time.LocalDateTime;

public class ResearchVisibility {

    private Long researchNo;
    private String hiddenYn;
    private Long hiddenBy;
    private LocalDateTime hiddenAt;
    private Long restoredBy;
    private LocalDateTime restoredAt;

    public Long getResearchNo() {
        return researchNo;
    }

    public void setResearchNo(Long researchNo) {
        this.researchNo = researchNo;
    }

    public String getHiddenYn() {
        return hiddenYn;
    }

    public void setHiddenYn(String hiddenYn) {
        this.hiddenYn = hiddenYn;
    }

    public Long getHiddenBy() {
        return hiddenBy;
    }

    public void setHiddenBy(Long hiddenBy) {
        this.hiddenBy = hiddenBy;
    }

    public LocalDateTime getHiddenAt() {
        return hiddenAt;
    }

    public void setHiddenAt(LocalDateTime hiddenAt) {
        this.hiddenAt = hiddenAt;
    }

    public Long getRestoredBy() {
        return restoredBy;
    }

    public void setRestoredBy(Long restoredBy) {
        this.restoredBy = restoredBy;
    }

    public LocalDateTime getRestoredAt() {
        return restoredAt;
    }

    public void setRestoredAt(LocalDateTime restoredAt) {
        this.restoredAt = restoredAt;
    }
}
