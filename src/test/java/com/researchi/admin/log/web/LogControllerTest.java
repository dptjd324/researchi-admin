package com.researchi.admin.log.web;

import com.researchi.admin.log.service.AdminLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogControllerTest {

    @Mock
    private AdminLogService adminLogService;

    @InjectMocks
    private LogController logController;

    @Test
    void actionsPopulatesActionLogScreen() {
        when(adminLogService.getActionLogs()).thenReturn(List.of());

        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = logController.actions(null, request("/logs/actions"), model);

        assertThat(viewName).isEqualTo("logs/actions");
        assertThat(model.get("actionLogs")).isEqualTo(List.of());
        assertThat(model.get("pageTitle")).isEqualTo("액션 로그");
        assertThat(model.get("pageSize")).isEqualTo(20);
    }

    @Test
    void mailPopulatesMailLogScreen() {
        when(adminLogService.getMailLogs()).thenReturn(List.of());

        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = logController.mail(null, request("/logs/mail"), model);

        assertThat(viewName).isEqualTo("logs/mail");
        assertThat(model.get("mailLogs")).isEqualTo(List.of());
        assertThat(model.get("pageTitle")).isEqualTo("메일 로그");
        assertThat(model.get("pageSize")).isEqualTo(20);
    }

    @Test
    void searchPopulatesSearchLogScreen() {
        when(adminLogService.getSearchLogs()).thenReturn(List.of());

        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = logController.search(null, request("/logs/search"), model);

        assertThat(viewName).isEqualTo("logs/search");
        assertThat(model.get("searchLogs")).isEqualTo(List.of());
        assertThat(model.get("pageTitle")).isEqualTo("검색 로그");
        assertThat(model.get("pageSize")).isEqualTo(20);
    }

    @Test
    void notificationsPopulatesNotificationLogScreen() {
        when(adminLogService.getNotificationLogs()).thenReturn(List.of());

        ExtendedModelMap model = new ExtendedModelMap();

        String viewName = logController.notifications(null, request("/logs/notifications"), model);

        assertThat(viewName).isEqualTo("logs/notifications");
        assertThat(model.get("notificationLogs")).isEqualTo(List.of());
        assertThat(model.get("pageTitle")).isEqualTo("알림 로그");
        assertThat(model.get("pageSize")).isEqualTo(20);
    }

    private MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        return request;
    }
}
