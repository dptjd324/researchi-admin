package com.researchi.admin.form.domain;

import java.util.List;

public record FormFieldDetail(
        Long id,
        String fieldKey,
        String fieldLabel,
        String fieldType,
        Integer fieldOrder,
        boolean required,
        String placeholderText,
        String helpText,
        List<FormFieldOption> options,
        boolean active
) {
}
