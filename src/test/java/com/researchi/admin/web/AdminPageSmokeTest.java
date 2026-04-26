package com.researchi.admin.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.hamcrest.Matchers.containsString;

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
    void applicationsPageRenders() throws Exception {
        mockMvc.perform(get("/applications").with(user("admin")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("전체 지원서")))
                .andExpect(content().string(containsString("공고별 목록, 빠른 검색, 상태 변경, 응답 상세 확인을 지원하는 지원서 관리 화면입니다.")));
    }

    @Test
    void mailHistoryPageRenders() throws Exception {
        mockMvc.perform(get("/mail/send/history").with(user("admin")))
                .andExpect(status().isOk());
    }
}
