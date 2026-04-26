package com.researchi.admin.search.domain;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class PeriodSearchForm {

    private String scope = "APPLICATION";
    private String keyword;
    private Long documentSrl;
    private String status;
    private String datePreset = "TODAY";
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate specificDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getDocumentSrl() {
        return documentSrl;
    }

    public void setDocumentSrl(Long documentSrl) {
        this.documentSrl = documentSrl;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDatePreset() {
        return datePreset;
    }

    public void setDatePreset(String datePreset) {
        this.datePreset = datePreset;
    }

    public LocalDate getSpecificDate() {
        return specificDate;
    }

    public void setSpecificDate(LocalDate specificDate) {
        this.specificDate = specificDate;
    }

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(LocalDate dateFrom) {
        this.dateFrom = dateFrom;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    public void setDateTo(LocalDate dateTo) {
        this.dateTo = dateTo;
    }
}
