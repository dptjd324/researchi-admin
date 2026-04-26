package com.researchi.admin.matching.service;

import com.researchi.admin.application.domain.ApplicationRecord;
import com.researchi.admin.application.service.ApplicationService;
import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.job.service.JobService;
import com.researchi.admin.keyword.domain.KeywordCandidate;
import com.researchi.admin.keyword.service.KeywordExtractionService;
import com.researchi.admin.matching.domain.AdminKeywordMatchJob;
import com.researchi.admin.matching.domain.AdminKeywordMatchTarget;
import com.researchi.admin.matching.domain.MatchingOverview;
import com.researchi.admin.matching.mapper.AdminKeywordMatchJobMapper;
import com.researchi.admin.matching.mapper.AdminKeywordMatchTargetMapper;
import com.researchi.admin.notification.domain.AdminNotificationLog;
import com.researchi.admin.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private KeywordExtractionService keywordExtractionService;
    @Mock
    private AdminKeywordMatchJobMapper adminKeywordMatchJobMapper;
    @Mock
    private AdminKeywordMatchTargetMapper adminKeywordMatchTargetMapper;
    @Mock
    private ApplicationService applicationService;
    @Mock
    private JobService jobService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AdminActionLogService adminActionLogService;

    @Test
    void runCreatesTargetsOnlyForKeywordMatches() {
        MatchingService matchingService = new MatchingService(
                keywordExtractionService,
                adminKeywordMatchJobMapper,
                adminKeywordMatchTargetMapper,
                applicationService,
                jobService,
                notificationService,
                adminActionLogService
        );

        when(keywordExtractionService.syncJobKeywords(9L)).thenReturn(List.of(
                new KeywordCandidate("survey", "survey", "JOB_TITLE"),
                new KeywordCandidate("panel", "panel", "JOB_META")
        ));
        ApplicationRecord matched = application(101L, 3L, "Y", "Y", "N");
        ApplicationRecord unmatched = application(102L, 4L, "Y", "N", "N");
        when(applicationService.getApplications(null, null)).thenReturn(List.of(matched, unmatched));
        when(keywordExtractionService.getApplicationKeywords(List.of(101L, 102L))).thenReturn(Map.of(
                101L, List.of(new KeywordCandidate("survey", "survey", "APPLICATION_JOB")),
                102L, List.of(new KeywordCandidate("other", "other", "APPLICATION_JOB"))
        ));
        doAnswer(invocation -> {
            AdminKeywordMatchJob matchJob = invocation.getArgument(0);
            matchJob.setId(44L);
            return null;
        }).when(adminKeywordMatchJobMapper).insert(any(AdminKeywordMatchJob.class));

        Long matchJobId = matchingService.run(
                9L,
                new AdminPrincipal(1L, "admin", "hash", "Admin", "Y", LocalDateTime.now().minusMinutes(1)),
                new MockHttpServletRequest()
        );

        assertThat(matchJobId).isEqualTo(44L);
        verify(keywordExtractionService).syncApplicationKeywords(101L);
        verify(keywordExtractionService).syncApplicationKeywords(102L);
        ArgumentCaptor<AdminKeywordMatchTarget> targetCaptor = ArgumentCaptor.forClass(AdminKeywordMatchTarget.class);
        verify(adminKeywordMatchTargetMapper).insert(targetCaptor.capture());
        assertThat(targetCaptor.getValue().getApplicationId()).isEqualTo(101L);
        assertThat(targetCaptor.getValue().getMatchedKeyword()).isEqualTo("survey");
        assertThat(targetCaptor.getValue().getNotifyStatus()).isEqualTo("PENDING");
        verify(adminKeywordMatchJobMapper).update(any(AdminKeywordMatchJob.class));
        verify(adminActionLogService).log(eq(1L), eq("KEYWORD_MATCH_RUN"), eq("JOB"), eq("9"), eq("Keyword match job #44 completed with 1 matches."), any());
    }

    @Test
    void overviewIncludesNotificationHistory() {
        MatchingService matchingService = new MatchingService(
                keywordExtractionService,
                adminKeywordMatchJobMapper,
                adminKeywordMatchTargetMapper,
                applicationService,
                jobService,
                notificationService,
                adminActionLogService
        );
        AdminKeywordMatchJob matchJob = new AdminKeywordMatchJob();
        matchJob.setId(70L);
        matchJob.setDocumentSrl(9L);
        when(adminKeywordMatchJobMapper.findByDocumentSrl(9L)).thenReturn(List.of(matchJob));
        when(keywordExtractionService.getJobKeywords(9L)).thenReturn(List.of(new KeywordCandidate("survey", "survey", "JOB_TITLE")));
        when(adminKeywordMatchTargetMapper.findViewsByMatchJobId(70L)).thenReturn(List.of());
        AdminNotificationLog log = new AdminNotificationLog();
        log.setApplicationId(101L);
        log.setDocumentSrl(9L);
        when(notificationService.getNotificationLogs(9L)).thenReturn(List.of(log));

        MatchingOverview overview = matchingService.getOverview(9L, null);

        assertThat(overview.jobKeywords()).containsExactly("survey");
        assertThat(overview.matchJobs()).hasSize(1);
        assertThat(overview.notificationLogs()).hasSize(1);
    }

    private ApplicationRecord application(Long id, Long sourceDocumentSrl, String notifyKeywordYn, String notifyEmailYn, String notifySmsYn) {
        ApplicationRecord application = new ApplicationRecord();
        application.setId(id);
        application.setDocumentSrl(sourceDocumentSrl);
        application.setNotifyKeywordYn(notifyKeywordYn);
        application.setNotifyEmailYn(notifyEmailYn);
        application.setNotifySmsYn(notifySmsYn);
        application.setProvideYn("Y");
        application.setIsBlacklisted("N");
        application.setApplicationStatus("RECEIVED");
        application.setAppliedAt(LocalDateTime.of(2026, 4, 16, 12, 0));
        return application;
    }
}
