package com.researchi.admin.legacy.research.web;

import com.researchi.admin.legacy.research.domain.ResearchMaster;
import jakarta.validation.constraints.NotBlank;

public class ResearchMasterForm {

    private Long researchNo;

    @NotBlank(message = "좌담회/설문 제목을 입력해 주세요.")
    private String researchTitle;

    private String researchContents;
    private String addComment;
    private String companyName;
    private String serverName;
    private String contactNo;
    private String brokerageAmt;
    private String calculateYn;
    private String remark;

    @NotBlank(message = "마감일자를 입력해 주세요.")
    private String closeDate;
    private Long clientId;

    public static ResearchMasterForm from(ResearchMaster researchMaster) {
        ResearchMasterForm form = new ResearchMasterForm();
        if (researchMaster == null) {
            form.setCalculateYn("N");
            return form;
        }
        form.setResearchNo(researchMaster.getResearchNo());
        form.setResearchTitle(researchMaster.getResearchTitle());
        form.setResearchContents(researchMaster.getResearchContents());
        form.setAddComment(researchMaster.getAddComment());
        form.setCompanyName(researchMaster.getCompanyName());
        form.setServerName(researchMaster.getServerName());
        form.setContactNo(researchMaster.getContactNo());
        form.setBrokerageAmt(researchMaster.getBrokerageAmt());
        form.setCalculateYn(researchMaster.getCalculateYn());
        form.setRemark(researchMaster.getRemark());
        form.setCloseDate(researchMaster.getCloseDate());
        return form;
    }

    public ResearchMaster toResearchMaster(Long researchNo) {
        ResearchMaster researchMaster = new ResearchMaster();
        researchMaster.setResearchNo(researchNo);
        researchMaster.setResearchTitle(researchTitle);
        researchMaster.setResearchContents(researchContents);
        researchMaster.setAddComment(addComment);
        researchMaster.setCompanyName(companyName);
        researchMaster.setServerName(serverName);
        researchMaster.setContactNo(contactNo);
        researchMaster.setBrokerageAmt(brokerageAmt);
        researchMaster.setCalculateYn(calculateYn);
        researchMaster.setRemark(remark);
        researchMaster.setCloseDate(closeDate);
        return researchMaster;
    }

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

    public String getCloseDate() {
        return closeDate;
    }

    public void setCloseDate(String closeDate) {
        this.closeDate = closeDate;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }
}
