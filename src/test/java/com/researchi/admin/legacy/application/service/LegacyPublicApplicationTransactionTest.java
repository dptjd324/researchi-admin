package com.researchi.admin.legacy.application.service;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.annotation.AnnotatedElementUtils;

import static org.assertj.core.api.Assertions.assertThat;

class LegacyPublicApplicationTransactionTest {

    @Test
    void submitUsesOldAdminTransactionSoConsentFailureRollsBackLegacyWrites() throws Exception {
        Transactional transactional = AnnotatedElementUtils.findMergedAnnotation(
                LegacyPublicApplicationService.class.getMethod(
                        "submit",
                        Long.class,
                        com.researchi.admin.publicform.web.PublicApplicationForm.class,
                        jakarta.servlet.http.HttpServletRequest.class
                ),
                Transactional.class
        );

        assertThat(transactional).isNotNull();
        assertThat(transactional.transactionManager()).isEqualTo("oldAdminTransactionManager");
    }
}
