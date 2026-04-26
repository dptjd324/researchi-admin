package com.researchi.admin.xe.domain;

public class XeJobDocument {

    private Long documentSrl;
    private Long moduleSrl;
    private String mid;
    private String title;
    private String content;
    private String status;
    private String regdate;
    private String lastUpdate;
    private String ipAddress;
    private Long listOrder;

    public Long getDocumentSrl() {
        return documentSrl;
    }

    public void setDocumentSrl(Long documentSrl) {
        this.documentSrl = documentSrl;
    }

    public Long getModuleSrl() {
        return moduleSrl;
    }

    public void setModuleSrl(Long moduleSrl) {
        this.moduleSrl = moduleSrl;
    }

    public String getMid() {
        return mid;
    }

    public void setMid(String mid) {
        this.mid = mid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRegdate() {
        return regdate;
    }

    public void setRegdate(String regdate) {
        this.regdate = regdate;
    }

    public String getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(String lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Long getListOrder() {
        return listOrder;
    }

    public void setListOrder(Long listOrder) {
        this.listOrder = listOrder;
    }
}
