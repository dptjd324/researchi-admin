package com.researchi.admin.application.domain;

public class ApplicationAnswerItem {

    private Long fieldId;
    private String fieldKey;
    private String fieldLabel;
    private String fieldType;
    private Integer fieldOrder;
    private String answerText;
    private String answerJson;
    private String displayAnswer;

    public Long getFieldId() { return fieldId; }
    public void setFieldId(Long fieldId) { this.fieldId = fieldId; }
    public String getFieldKey() { return fieldKey; }
    public void setFieldKey(String fieldKey) { this.fieldKey = fieldKey; }
    public String getFieldLabel() { return fieldLabel; }
    public void setFieldLabel(String fieldLabel) { this.fieldLabel = fieldLabel; }
    public String getFieldType() { return fieldType; }
    public void setFieldType(String fieldType) { this.fieldType = fieldType; }
    public Integer getFieldOrder() { return fieldOrder; }
    public void setFieldOrder(Integer fieldOrder) { this.fieldOrder = fieldOrder; }
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
    public String getAnswerJson() { return answerJson; }
    public void setAnswerJson(String answerJson) { this.answerJson = answerJson; }
    public String getDisplayAnswer() { return displayAnswer; }
    public void setDisplayAnswer(String displayAnswer) { this.displayAnswer = displayAnswer; }
}
