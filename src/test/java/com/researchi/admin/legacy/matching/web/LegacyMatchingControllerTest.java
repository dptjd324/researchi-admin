package com.researchi.admin.legacy.matching.web;

import com.researchi.admin.legacy.matching.domain.LegacyMatchingRunStatus;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingRunTicket;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingSearchCondition;
import com.researchi.admin.legacy.matching.service.LegacyMatchingAsyncExecutor;
import com.researchi.admin.legacy.matching.service.LegacyMatchingService;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.legacy.research.service.ResearchMasterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.core.task.TaskRejectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class LegacyMatchingControllerTest {

    private LegacyMatchingService matchingService;
    private ResearchMasterService researchMasterService;
    private LegacyMatchingAsyncExecutor asyncExecutor;
    private LegacyMatchingController controller;

    @BeforeEach
    void setUp() {
        matchingService = mock(LegacyMatchingService.class);
        researchMasterService = mock(ResearchMasterService.class);
        asyncExecutor = mock(LegacyMatchingAsyncExecutor.class);
        controller = new LegacyMatchingController(matchingService, researchMasterService, asyncExecutor);
        when(researchMasterService.isHidden(46408L)).thenReturn(false);
    }

    @Test
    void runWindowReturnsProgressViewAndQueuesNewRun() {
        LegacyMatchingSearchForm form = searchForm();
        LegacyMatchingRunTicket ticket = new LegacyMatchingRunTicket(77L, 16, "PENDING", false);
        when(matchingService.startOrReuseMatchingRun(eq(46408L), any(LegacyMatchingSearchCondition.class)))
                .thenReturn(ticket);
        when(researchMasterService.getResearchMaster(46408L)).thenReturn(research());
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.runMatchingWindow(
                46408L,
                form,
                true,
                model,
                new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token")
        );

        assertThat(view).isEqualTo("research/matching-progress-window");
        assertThat(model.get("runTicket")).isEqualTo(ticket);
        verify(asyncExecutor).submit(77L);
    }

    @Test
    void runWindowReconnectsPendingRunThroughDatabaseClaim() {
        LegacyMatchingSearchForm form = searchForm();
        LegacyMatchingRunTicket ticket = new LegacyMatchingRunTicket(77L, 16, "PENDING", true);
        when(matchingService.startOrReuseMatchingRun(eq(46408L), any(LegacyMatchingSearchCondition.class)))
                .thenReturn(ticket);
        when(researchMasterService.getResearchMaster(46408L)).thenReturn(research());

        String view = controller.runMatchingWindow(
                46408L,
                form,
                true,
                new ExtendedModelMap(),
                new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token")
        );

        assertThat(view).isEqualTo("research/matching-progress-window");
        verify(asyncExecutor).submit(77L);
    }

    @Test
    void runWindowDoesNotQueueAlreadyRunningRun() {
        LegacyMatchingSearchForm form = searchForm();
        LegacyMatchingRunTicket ticket = new LegacyMatchingRunTicket(77L, 16, "RUNNING", true);
        when(matchingService.startOrReuseMatchingRun(eq(46408L), any(LegacyMatchingSearchCondition.class)))
                .thenReturn(ticket);
        when(researchMasterService.getResearchMaster(46408L)).thenReturn(research());

        String view = controller.runMatchingWindow(
                46408L,
                form,
                true,
                new ExtendedModelMap(),
                new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token")
        );

        assertThat(view).isEqualTo("research/matching-progress-window");
        verify(asyncExecutor, never()).submit(77L);
    }

    @Test
    void runWindowMarksRunFailedWhenQueueRejectsExecution() {
        LegacyMatchingSearchForm form = searchForm();
        LegacyMatchingRunTicket ticket = new LegacyMatchingRunTicket(77L, 16, "PENDING", false);
        when(matchingService.startOrReuseMatchingRun(eq(46408L), any(LegacyMatchingSearchCondition.class)))
                .thenReturn(ticket);
        when(researchMasterService.getResearchMaster(46408L)).thenReturn(research());
        when(asyncExecutor.submit(77L)).thenThrow(new TaskRejectedException("queue full"));

        String view = controller.runMatchingWindow(
                46408L,
                form,
                true,
                new ExtendedModelMap(),
                new DefaultCsrfToken("X-CSRF-TOKEN", "_csrf", "token")
        );

        assertThat(view).isEqualTo("research/matching-progress-window");
        verify(matchingService).failMatchingRun(77L, "매칭 실행 대기열이 가득 차 작업을 시작하지 못했습니다.");
    }

    @Test
    void statusResponseIncludesCompletedResultUrl() {
        LegacyMatchingSearchCondition condition = searchForm().toCondition();
        when(matchingService.getMatchingRunStatus(46408L, 77L)).thenReturn(new LegacyMatchingRunStatus(
                77L,
                46408L,
                16,
                "COMPLETED",
                null,
                condition.storageKey()
        ));

        ResponseEntity<LegacyMatchingRunStatusResponse> entity = controller.matchingRunStatus(46408L, 77L);
        LegacyMatchingRunStatusResponse response = entity.getBody();

        assertThat(entity.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.resultUrl())
                .isEqualTo("/research/46408/matching/run-window-result?jobId=77&" + condition.storageKey());
    }

    @Test
    void statusResponseReturnsNotFoundForUnknownRun() {
        when(matchingService.getMatchingRunStatus(46408L, 77L))
                .thenThrow(new IllegalArgumentException("매칭 작업을 찾을 수 없습니다."));

        ResponseEntity<LegacyMatchingRunStatusResponse> response = controller.matchingRunStatus(46408L, 77L);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNull();
    }

    private LegacyMatchingSearchForm searchForm() {
        LegacyMatchingSearchForm form = new LegacyMatchingSearchForm();
        form.setAppSex("남자");
        form.setAppBirth("1988-1995");
        return form;
    }

    private ResearchMaster research() {
        ResearchMaster research = new ResearchMaster();
        research.setResearchNo(46408L);
        research.setResearchTitle("금융 앱 사용자 조사");
        return research;
    }
}
