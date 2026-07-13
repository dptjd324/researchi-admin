package com.researchi.admin.legacy.matching.service;

import com.researchi.admin.legacy.matching.mapper.LegacyMatchingIndexJobMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class LegacyMatchingAsyncExecutorTest {

    @Test
    void submitQueuesEachJobIdOnlyOnceUntilExecutionFinishes() {
        LegacyMatchingService matchingService = mock(LegacyMatchingService.class);
        LegacyMatchingIndexJobMapper indexJobMapper = mock(LegacyMatchingIndexJobMapper.class);
        List<Runnable> queuedTasks = new ArrayList<>();
        Executor taskExecutor = queuedTasks::add;
        LegacyMatchingAsyncExecutor executor = new LegacyMatchingAsyncExecutor(
                matchingService,
                indexJobMapper,
                taskExecutor
        );

        boolean firstSubmission = executor.submit(77L);
        boolean duplicateSubmission = executor.submit(77L);

        assertThat(firstSubmission).isTrue();
        assertThat(duplicateSubmission).isFalse();
        assertThat(queuedTasks).hasSize(1);

        queuedTasks.get(0).run();

        verify(matchingService).executeMatchingRun(77L);
    }

    @Test
    void applicationReadyMarksInterruptedRunsFailed() {
        LegacyMatchingService matchingService = mock(LegacyMatchingService.class);
        LegacyMatchingIndexJobMapper indexJobMapper = mock(LegacyMatchingIndexJobMapper.class);
        LegacyMatchingAsyncExecutor executor = new LegacyMatchingAsyncExecutor(
                matchingService,
                indexJobMapper,
                Runnable::run
        );

        executor.recoverInterruptedRuns();

        verify(indexJobMapper).markInterruptedRunsFailed(
                eq("서버 재시작으로 매칭 작업이 중단되었습니다."),
                any(LocalDateTime.class)
        );
    }
}
