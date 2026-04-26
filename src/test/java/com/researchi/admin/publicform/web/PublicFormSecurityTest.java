package com.researchi.admin.publicform.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PublicFormSecurityTest {

    private MockMvc mockMvc;
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestApplyEndpoint())
                .addFilters(new CsrfFilter(new HttpSessionCsrfTokenRepository()))
                .build();
    }

    @Test
    void postApplyRequiresCsrfToken() throws Exception {
        mockMvc.perform(post("/apply/9")
                        .session(new MockHttpSession())
                        .param("applicantName", "Applicant"))
                .andExpect(status().isForbidden());
    }

    @RestController
    static class TestApplyEndpoint {

        @PostMapping("/apply/9")
        ResponseEntity<String> submit() {
            return ResponseEntity.ok("ok");
        }
    }
}
