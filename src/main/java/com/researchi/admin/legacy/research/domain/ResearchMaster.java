package com.researchi.admin.legacy.research.domain;

public class ResearchMaster {

    private Long researchNo;
    private String researchTitle;
    private String researchContents;
    private String addComment;
    private Integer appCnt;
    private Integer appNewCnt;
    private String companyName;
    private String serverName;
    private String contactNo;
    private String brokerageAmt;
    private String calculateYn;
    private String remark;
    private String registDt;
    private String modifyDt;
    private String closeDate;

    public Long getResearchNo() {
        return researchNo;
    }

    public void setResearchNo(Long researchNo) {
        this.researchNo = researchNo;
    }

    public String getResearchTitle() {
        return researchTitle;
    }

    public void setResearchTitle(String researchTitle) {
        this.researchTitle = researchTitle;
    }

    public String getResearchContents() {
        return researchContents;
    }

    public void setResearchContents(String researchContents) {
        this.researchContents = researchContents;
    }

    public String getAddComment() {
        return addComment;
    }

    public void setAddComment(String addComment) {
        this.addComment = addComment;
    }

    public Integer getAppCnt() {
        return appCnt;
    }

    public void setAppCnt(Integer appCnt) {
        this.appCnt = appCnt;
    }

    public Integer getAppNewCnt() {
        return appNewCnt;
    }

    public void setAppNewCnt(Integer appNewCnt) {
        this.appNewCnt = appNewCnt;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getContactNo() {
        return contactNo;
    }

    public void setContactNo(String contactNo) {
        this.contactNo = contactNo;
    }

    public String getBrokerageAmt() {
        return brokerageAmt;
    }

    public void setBrokerageAmt(String brokerageAmt) {
        this.brokerageAmt = brokerageAmt;
    }

    public String getCalculateYn() {
        return calculateYn;
    }

    public void setCalculateYn(String calculateYn) {
        this.calculateYn = calculateYn;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getRegistDt() {
        return registDt;
    }

    public String getRegistDateLabel() {
        return formatDateOnly(registDt);
    }

    public String getRegistDateTimeMinuteLabel() {
        return formatDateTimeMinute(registDt);
    }

    public void setRegistDt(String registDt) {
        this.registDt = registDt;
    }

    public String getModifyDt() {
        return modifyDt;
    }

    public String getModifyDateTimeMinuteLabel() {
        return formatDateTimeMinute(modifyDt);
    }

    public void setModifyDt(String modifyDt) {
        this.modifyDt = modifyDt;
    }

    public String getCloseDate() {
        return closeDate;
    }

    public String getCloseDateLabel() {
        return formatDateOnly(closeDate);
    }

    public void setCloseDate(String closeDate) {
        this.closeDate = closeDate;
    }

    private String formatDateOnly(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String digits = value.trim().replaceAll("\\D", "");
        if (digits.length() >= 8) {
            return digits.substring(0, 4) + "-" + digits.substring(4, 6) + "-" + digits.substring(6, 8);
        }
        return value.trim();
    }

    private String formatDateTimeMinute(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        String digits = value.trim().replaceAll("\\D", "");
        if (digits.length() >= 12) {
            return digits.substring(0, 4) + "-" + digits.substring(4, 6) + "-" + digits.substring(6, 8)
                    + " " + digits.substring(8, 10) + ":" + digits.substring(10, 12);
        }
        return formatDateOnly(value);
    }
}
