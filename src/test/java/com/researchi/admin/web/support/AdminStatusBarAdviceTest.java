package com.researchi.admin.web.support;

import com.researchi.admin.log.domain.StatusBarSummary;
import com.researchi.admin.log.service.AdminLogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStatusBarAdviceTest {

    @Mock
    private AdminLogService adminLogService;

    @InjectMocks
    private AdminStatusBarAdvice adminStatusBarAdvice;

    @Test
    void statusBarSummaryUsesSharedLogSummary() {
        StatusBarSummary summary = new StatusBarSummary(
                1,
                2,
                3,
                4,
                LocalDateTime.now().minusMinutes(4),
                LocalDateTime.now().minusMinutes(3),
                LocalDateTime.now().minusMinutes(2),
                LocalDateTime.now().minusMinutes(1)
        );
        when(adminLogService.getStatusBarSummary()).thenReturn(summary);

        assertThat(adminStatusBarAdvice.statusBarSummary()).isEqualTo(summary);
    }
}
