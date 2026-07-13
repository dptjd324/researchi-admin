package com.researchi.admin.legacy.blacklist.web;

import com.researchi.admin.legacy.blacklist.domain.Blacklist;
import com.researchi.admin.legacy.research.domain.ResearchApplication;
import jakarta.validation.constraints.NotBlank;

public class LegacyBlacklistForm {

    private Long blacklistNo;
    private String blackUserBirth;
    @NotBlank(message = "Name is required.")
    private String blackUserName;
    private String blackUserContact;
    private String blackUserComment;
    private String blackYn;

    public static LegacyBlacklistForm from(Blacklist blacklist) {
        LegacyBlacklistForm form = new LegacyBlacklistForm();
        if (blacklist == null) {
            form.setBlackYn("Y");
            return form;
        }
        form.setBlacklistNo(blacklist.getBlacklistNo());
        form.setBlackUserBirth(blacklist.getBlackUserBirth());
        form.setBlackUserName(blacklist.getBlackUserName());
        form.setBlackUserContact(blacklist.getBlackUserContact());
        form.setBlackUserComment(blacklist.getBlackUserComment());
        form.setBlackYn(blacklist.getBlackYn());
        return form;
    }

    public static LegacyBlacklistForm fromApplicant(ResearchApplication application) {
        if (application == null) {
            throw new IllegalArgumentException("application is required.");
        }
        LegacyBlacklistForm form = new LegacyBlacklistForm();
        form.setBlackUserBirth(trimToNull(application.getAppBirth()));
        form.setBlackUserName(trimToNull(application.getAppName()));
        form.setBlackUserContact(firstPresent(application.getAppHphone(), application.getAppTele()));
        form.setBlackUserComment("신청자 조회에서 블랙 등록 (공고번호: "
                + application.getResearchNo()
                + ", 신청자번호: "
                + application.getResearchAppSeq()
                + ")");
        form.setBlackYn("Y");
        return form;
    }

    public Blacklist toBlacklist(Long blacklistNo) {
        Blacklist blacklist = new Blacklist();
        blacklist.setBlacklistNo(blacklistNo);
        blacklist.setBlackUserBirth(blackUserBirth);
        blacklist.setBlackUserName(blackUserName);
        blacklist.setBlackUserContact(blackUserContact);
        blacklist.setBlackUserComment(blackUserComment);
        blacklist.setBlackYn(blackYn);
        return blacklist;
    }

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

    private static String firstPresent(String first, String second) {
        String trimmedFirst = trimToNull(first);
        return trimmedFirst != null ? trimmedFirst : trimToNull(second);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
