package com.researchi.admin.publicform.domain;

import com.researchi.admin.form.domain.FormFieldDetail;
import com.researchi.admin.job.domain.JobDetail;

import java.util.List;

public record PublicFormPage(
        JobDetail jobDetail,
        List<FormFieldDetail> fields,
        String captchaQuestion,
        boolean captchaEnabled
) {
}
