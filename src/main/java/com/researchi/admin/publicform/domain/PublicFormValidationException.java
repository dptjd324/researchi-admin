package com.researchi.admin.publicform.domain;

import java.util.LinkedHashMap;
import java.util.Map;

public class PublicFormValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;
    private final Map<Long, String> dynamicFieldErrors;
    private final String globalError;

    public PublicFormValidationException(
            Map<String, String> fieldErrors,
            Map<Long, String> dynamicFieldErrors,
            String globalError
    ) {
        super(globalError == null ? "지원서 입력값을 확인해 주세요." : globalError);
        this.fieldErrors = fieldErrors == null ? Map.of() : new LinkedHashMap<>(fieldErrors);
        this.dynamicFieldErrors = dynamicFieldErrors == null ? Map.of() : new LinkedHashMap<>(dynamicFieldErrors);
        this.globalError = globalError;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }

    public Map<Long, String> getDynamicFieldErrors() {
        return dynamicFieldErrors;
    }

    public String getGlobalError() {
        return globalError;
    }
}
