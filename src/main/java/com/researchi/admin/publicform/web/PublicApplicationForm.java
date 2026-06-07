package com.researchi.admin.publicform.web;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PublicApplicationForm {

    @NotBlank(message = "성명을 입력해주세요.")
    @Size(max = 100, message = "성명은 100자 이하여야 합니다.")
    private String applicantName;

    @NotBlank(message = "성별을 입력해주세요.")
    private String genderCode;

    @NotNull(message = "생년월일을 입력해주세요.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;

    @NotBlank(message = "만나이를 입력해주세요.")
    @Size(max = 20, message = "만나이는 20자 이하여야 합니다.")
    private String ageText;

    @NotBlank(message = "직업을 입력해주세요.")
    @Size(max = 150, message = "직업은 150자 이하여야 합니다.")
    private String jobText;

    @NotBlank(message = "회사/학교명을 입력해주세요.")
    @Size(max = 150, message = "회사/학교명은 150자 이하여야 합니다.")
    private String organizationText;

    @NotBlank(message = "연락처를 입력해주세요.")
    private String mobilePhone;

    private String telPhone;

    @Size(max = 150, message = "지역은 150자 이하여야 합니다.")
    private String regionText;

    @NotBlank(message = "주소를 입력해주세요.")
    @Size(max = 255, message = "주소는 255자 이하여야 합니다.")
    private String address;

    @NotBlank(message = "이메일 주소를 입력해 주세요.")
    @Size(max = 150, message = "이메일 주소는 150자 이하여야 합니다.")
    @Email(message = "올바른 이메일 주소를 입력해 주세요.")
    private String emailAddress;

    @Size(max = 1000, message = "기존 조사 경험은 1000자 이하여야 합니다.")
    private String priorResearchText;

    @Size(max = 4000, message = "추가기재사항 요약은 4000자 이하여야 합니다.")
    private String extraComment;

    private String selectedExtraGroup;
    private List<String> extraAnswers = new ArrayList<>();

    private Boolean notifyEmailYn = Boolean.FALSE;
    private Boolean notifySmsYn = Boolean.FALSE;
    private Boolean notifyKeywordYn = Boolean.FALSE;
    private Boolean provideYn = Boolean.FALSE;
    private String captchaAnswer;
    private String website;

    public String getApplicantName() {
        return applicantName;
    }

    public void setApplicantName(String applicantName) {
        this.applicantName = applicantName;
    }

    public String getGenderCode() {
        return genderCode;
    }

    public void setGenderCode(String genderCode) {
        this.genderCode = genderCode;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public String getJobText() {
        return jobText;
    }

    public void setJobText(String jobText) {
        this.jobText = jobText;
    }

    public String getAgeText() {
        return ageText;
    }

    public void setAgeText(String ageText) {
        this.ageText = ageText;
    }

    public String getOrganizationText() {
        return organizationText;
    }

    public void setOrganizationText(String organizationText) {
        this.organizationText = organizationText;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }

    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public String getTelPhone() {
        return telPhone;
    }

    public void setTelPhone(String telPhone) {
        this.telPhone = telPhone;
    }

    public String getRegionText() {
        return regionText;
    }

    public void setRegionText(String regionText) {
        this.regionText = regionText;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }

    public String getPriorResearchText() {
        return priorResearchText;
    }

    public void setPriorResearchText(String priorResearchText) {
        this.priorResearchText = priorResearchText;
    }

    public String getExtraComment() {
        return extraComment;
    }

    public void setExtraComment(String extraComment) {
        this.extraComment = extraComment;
    }

    public String getSelectedExtraGroup() {
        return selectedExtraGroup;
    }

    public void setSelectedExtraGroup(String selectedExtraGroup) {
        this.selectedExtraGroup = selectedExtraGroup;
    }

    public List<String> getExtraAnswers() {
        return extraAnswers;
    }

    public void setExtraAnswers(List<String> extraAnswers) {
        this.extraAnswers = extraAnswers == null ? new ArrayList<>() : extraAnswers;
    }

    public Boolean getNotifyEmailYn() {
        return notifyEmailYn;
    }

    public void setNotifyEmailYn(Boolean notifyEmailYn) {
        this.notifyEmailYn = notifyEmailYn;
    }

    public Boolean getNotifySmsYn() {
        return notifySmsYn;
    }

    public void setNotifySmsYn(Boolean notifySmsYn) {
        this.notifySmsYn = notifySmsYn;
    }

    public Boolean getNotifyKeywordYn() {
        return notifyKeywordYn;
    }

    public void setNotifyKeywordYn(Boolean notifyKeywordYn) {
        this.notifyKeywordYn = notifyKeywordYn;
    }

    public Boolean getProvideYn() {
        return provideYn;
    }

    public void setProvideYn(Boolean provideYn) {
        this.provideYn = provideYn;
    }

    @AssertTrue(message = "개인정보 수집 및 이용 동의가 필요합니다.")
    public boolean isProvideYnAccepted() {
        return Boolean.TRUE.equals(provideYn);
    }

    public String getCaptchaAnswer() {
        return captchaAnswer;
    }

    public void setCaptchaAnswer(String captchaAnswer) {
        this.captchaAnswer = captchaAnswer;
    }

    public String getWebsite() {
        return website;
    }

    public void setWebsite(String website) {
        this.website = website;
    }
}
