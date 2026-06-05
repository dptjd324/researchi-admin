package com.researchi.admin.web;

import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.legacy.research.service.ResearchMasterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class AdminPageSmokeTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ResearchMasterService researchMasterService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void currentOperationPagesRender() throws Exception {
        mockMvc.perform(get("/dashboard").with(user("admin")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/research").with(user("admin")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/clients").with(user("admin")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/clients/new").with(user("admin")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/legacy-blacklist").with(user("admin")))
                .andExpect(status().isOk());
    }

    @Test
    void localStyleAndScriptAssetsAreServed() throws Exception {
        mockMvc.perform(get("/webjars/bootstrap/5.3.3/css/bootstrap.min.css"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/webjars/bootstrap/5.3.3/js/bootstrap.bundle.min.js"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/css/admin-ui.css"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/css/public-ui.css"))
                .andExpect(status().isOk());
    }

    @Test
    void researchDetailEditorRendersOriginalContents() throws Exception {
        ResearchMaster research = researchMasterService.getResearchMasterPage(
                        null, null, null, null, null, null, 100, 0
                )
                .stream()
                .filter(item -> item.getResearchContents() != null && !item.getResearchContents().trim().isEmpty())
                .findFirst()
                .orElseThrow();

        MvcResult result = mockMvc.perform(get("/research/{researchNo}", research.getResearchNo()).with(user("admin")))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        String firstContentLine = research.getResearchContents()
                .lines()
                .filter(line -> !line.trim().isEmpty())
                .findFirst()
                .orElseThrow();

        assertThat(html).contains("id=\"researchContentsEditor\"");
        assertThat(html).contains("data-initial-value=");
        assertThat(html).contains(HtmlUtils.htmlEscape(firstContentLine));
    }
}
