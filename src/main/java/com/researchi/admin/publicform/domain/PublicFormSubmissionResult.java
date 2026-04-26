package com.researchi.admin.publicform.domain;

public record PublicFormSubmissionResult(
        PublicFormSubmissionStatus status,
        Long applicationId
) {
}
