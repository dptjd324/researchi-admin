package com.researchi.admin.legacy.application.web;

import com.researchi.admin.legacy.application.service.LegacyPublicApplicationService;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.publicform.web.PublicApplicationForm;
import com.researchi.admin.web.support.TextLinkRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class LegacyPublicApplicationControllerTest {

    @Mock
    private LegacyPublicApplicationService legacyPublicApplicationService;
    @Mock
    private TextLinkRenderer textLinkRenderer;

    @Test
    void submitReturnsResultViewWhenUnexpectedFailureOccurs() throws Exception {
        ResearchMaster researchMaster = new ResearchMaster();
        researchMaster.setResearchNo(46408L);
        researchMaster.setResearchTitle("테스트 좌담회");

        when(legacyPublicApplicationService.getOpenResearch(46408L)).thenReturn(researchMaster);
        when(legacyPublicApplicationService.submit(eq(46408L), any(PublicApplicationForm.class), any()))
                .thenThrow(new IllegalStateException("DB insert failed"));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new LegacyPublicApplicationController(legacyPublicApplicationService, textLinkRenderer))
                .build();

        mockMvc.perform(post("/research/46408/apply")
                        .param("applicantName", "김테스트")
                        .param("genderCode", "1")
                        .param("birthDate", "1990-01-01")
                        .param("ageText", "36")
                        .param("jobText", "회사원")
                        .param("organizationText", "테스트회사")
                        .param("mobilePhone", "010-1234-5678")
                        .param("address", "서울시 강남구")
                        .param("emailAddress", "applicant@example.com")
                        .param("provideYn", "true"))
                .andExpect(status().isOk())
                .andExpect(view().name("publicform/result"))
                .andExpect(model().attribute("resultTitle", "신청 오류"))
                .andExpect(model().attribute("resultMessage", containsString("신청서 제출 중 문제가 발생했습니다")));
    }
}
