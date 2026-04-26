package com.researchi.admin.form.domain;

public enum FormFieldType {
    TEXT(false),
    TEXTAREA(false),
    RADIO(true),
    CHECKBOX(true),
    SELECT(true),
    NUMBER(false),
    DATE(false);

    private final boolean optionsRequired;

    FormFieldType(boolean optionsRequired) {
        this.optionsRequired = optionsRequired;
    }

    public boolean isOptionsRequired() {
        return optionsRequired;
    }
}
