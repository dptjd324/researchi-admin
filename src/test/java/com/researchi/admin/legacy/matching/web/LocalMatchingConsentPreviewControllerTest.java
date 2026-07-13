package com.researchi.admin.legacy.matching.web;

import com.researchi.admin.legacy.matching.domain.LegacyMatchingOverview;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.security.web.csrf.DefaultCsrfToken;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class LocalMatchingConsentPreviewControllerTest {

    @Test
    void controllerIsLocalOnlyAndExposesPreviewRoute() throws Exception {
        Profile profile = LocalMatchingConsentPreviewController.class.getAnnotation(Profile.class);
        Method method = LocalMatchingConsentPreviewController.class
                .getMethod("preview", org.springframework.ui.Model.class, org.springframework.security.web.csrf.CsrfToken.class);
        GetMapping getMapping = method.getAnnotation(GetMapping.class);

        assertThat(profile).isNotNull();
        assertThat(profile.value()).containsExactly("local");
        assertThat(getMapping.value()).containsExactly("/dev/matching-consent-preview");
    }

    @Test
    void previewUsesSyntheticNegativeIdsAndAllChannelCombinations() {
        LocalMatchingConsentPreviewController controller = new LocalMatchingConsentPreviewController();
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.preview(
                model,
                new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "preview-token")
        );

        assertThat(view).isEqualTo("research/matching-run-window");
        assertThat(model.get("previewMode")).isEqualTo(true);
        LegacyMatchingOverview overview = (LegacyMatchingOverview) model.get("overview");
        assertThat(overview.research().getResearchNo()).isNegative();
        assertThat(overview.results())
                .extracting(result -> result.application().getResearchAppSeq())
                .allMatch(id -> id < 0);
        assertThat(overview.results()).extracting("smsAllowed", "emailAllowed")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(true, false),
                        org.assertj.core.groups.Tuple.tuple(false, true),
                        org.assertj.core.groups.Tuple.tuple(true, true)
                );
    }
}
