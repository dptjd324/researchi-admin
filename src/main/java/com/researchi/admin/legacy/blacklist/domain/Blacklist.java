package com.researchi.admin.legacy.blacklist.domain;

public class Blacklist {

    private Long blacklistNo;
    private String blackUserBirth;
    private String blackUserName;
    private String blackUserContact;
    private String blackUserComment;
    private String blackYn;
    private String registDt;
    private String modifyDt;

    public Long getBlacklistNo() {
        return blacklistNo;
    }

    public void setBlacklistNo(Long blacklistNo) {
        this.blacklistNo = blacklistNo;
    }

    public String getBlackUserBirth() {
        return blackUserBirth;
    }

    public void setBlackUserBirth(String blackUserBirth) {
        this.blackUserBirth = blackUserBirth;
    }

    public String getBlackUserName() {
        return blackUserName;
    }

    public void setBlackUserName(String blackUserName) {
        this.blackUserName = blackUserName;
    }

    public String getBlackUserContact() {
        return blackUserContact;
    }

    public void setBlackUserContact(String blackUserContact) {
        this.blackUserContact = blackUserContact;
    }

    public String getBlackUserComment() {
        return blackUserComment;
    }

    public void setBlackUserComment(String blackUserComment) {
        this.blackUserComment = blackUserComment;
    }

    public String getBlackYn() {
        return blackYn;
    }

    public void setBlackYn(String blackYn) {
        this.blackYn = blackYn;
    }

    public String getRegistDt() {
        return registDt;
    }

    public void setRegistDt(String registDt) {
        this.registDt = registDt;
    }

    public String getModifyDt() {
        return modifyDt;
    }

    public void setModifyDt(String modifyDt) {
        this.modifyDt = modifyDt;
    }
}
