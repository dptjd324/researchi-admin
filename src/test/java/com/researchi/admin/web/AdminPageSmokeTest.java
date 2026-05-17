package com.researchi.admin.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class AdminPageSmokeTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void currentOperationPagesRender() throws Exception {
        mockMvc.perform(get("/research").with(user("admin")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/clients").with(user("admin")))
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
}
