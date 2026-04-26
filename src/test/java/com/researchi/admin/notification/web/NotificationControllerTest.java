package com.researchi.admin.notification.web;

import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController notificationController;

    @Test
    void notifyEmailRedirectsToMatchingPage() {
        String viewName = notificationController.notifyEmail(
                9L,
                44L,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        assertThat(viewName).isEqualTo("redirect:/matching/jobs/9?matchJobId=44&emailSent");
        verify(notificationService).sendEmailNotifications(any(), any(), any(), any());
    }
}
