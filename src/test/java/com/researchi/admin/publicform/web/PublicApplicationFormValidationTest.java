package com.researchi.admin.publicform.web;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PublicApplicationFormValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void requiresCommonFieldsAndPrivacyConsent() {
        PublicApplicationForm form = new PublicApplicationForm();

        Set<ConstraintViolation<PublicApplicationForm>> violations = validator.validate(form);

        assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
                .contains(
                        "applicantName",
                        "genderCode",
                        "birthDate",
                        "ageText",
                        "jobText",
                        "organizationText",
                        "mobilePhone",
                        "address",
                        "provideYnAccepted"
                );
    }

    @Test
    void optionalRecruitmentAndChannelConsentsDoNotCreateValidationErrors() {
        PublicApplicationForm form = new PublicApplicationForm();
        form.setFutureRecruitmentYn(false);
        form.setNotifySmsYn(false);
        form.setNotifyEmailYn(false);

        Set<ConstraintViolation<PublicApplicationForm>> violations = validator.validate(form);

        assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
                .doesNotContain("futureRecruitmentYn", "notifySmsYn", "notifyEmailYn");
    }

    @Test
    void futureRecruitmentRequiresAtLeastOneChannelAndRejectsOrphanChannelConsent() {
        PublicApplicationForm form = new PublicApplicationForm();
        form.setFutureRecruitmentYn(true);

        assertThat(validator.validate(form)).extracting(violation -> violation.getPropertyPath().toString())
                .contains("futureRecruitmentChannelAccepted");

        form.setNotifySmsYn(true);
        assertThat(validator.validate(form)).extracting(violation -> violation.getPropertyPath().toString())
                .doesNotContain("futureRecruitmentChannelAccepted");

        form.setNotifySmsYn(false);
        form.setNotifyEmailYn(true);
        assertThat(validator.validate(form)).extracting(violation -> violation.getPropertyPath().toString())
                .doesNotContain("futureRecruitmentChannelAccepted");

        form.setNotifySmsYn(true);
        assertThat(validator.validate(form)).extracting(violation -> violation.getPropertyPath().toString())
                .doesNotContain("futureRecruitmentChannelAccepted");

        form.setFutureRecruitmentYn(false);
        assertThat(validator.validate(form)).extracting(violation -> violation.getPropertyPath().toString())
                .contains("futureRecruitmentChannelAccepted");
    }
}
