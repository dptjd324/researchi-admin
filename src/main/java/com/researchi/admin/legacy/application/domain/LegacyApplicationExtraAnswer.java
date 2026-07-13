package com.researchi.admin.legacy.application.domain;

public class LegacyApplicationExtraAnswer {

    private Long id;
    private Long researchNo;
    private Long researchAppSeq;
    private Integer answerOrder;
    private String questionGroup;
    private String questionLabel;
    private String answerText;
    private String rawAnswerText;

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

    public Long getResearchAppSeq() {
        return researchAppSeq;
    }

    public void setResearchAppSeq(Long researchAppSeq) {
        this.researchAppSeq = researchAppSeq;
    }

    public Integer getAnswerOrder() {
        return answerOrder;
    }

    public void setAnswerOrder(Integer answerOrder) {
        this.answerOrder = answerOrder;
    }

    public String getQuestionGroup() {
        return questionGroup;
    }

    public void setQuestionGroup(String questionGroup) {
        this.questionGroup = questionGroup;
    }

    public String getQuestionLabel() {
        return questionLabel;
    }

    public void setQuestionLabel(String questionLabel) {
        this.questionLabel = questionLabel;
    }

    public String getAnswerText() {
        return answerText;
    }

    public void setAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public String getRawAnswerText() {
        return rawAnswerText;
    }

    public void setRawAnswerText(String rawAnswerText) {
        this.rawAnswerText = rawAnswerText;
    }
}
