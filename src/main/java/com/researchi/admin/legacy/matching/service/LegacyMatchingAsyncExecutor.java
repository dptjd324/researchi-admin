package com.researchi.admin.legacy.matching.service;

import com.researchi.admin.legacy.matching.mapper.LegacyMatchingIndexJobMapper;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.time.LocalDateTime;

@Service
public class LegacyMatchingAsyncExecutor {

    static final String INTERRUPTED_REASON = "서버 재시작으로 매칭 작업이 중단되었습니다.";

    private final LegacyMatchingService legacyMatchingService;
    private final LegacyMatchingIndexJobMapper legacyMatchingIndexJobMapper;
    private final Executor taskExecutor;
    private final Set<Long> queuedJobIds = ConcurrentHashMap.newKeySet();
    private final LocalDateTime executorStartedAt = LocalDateTime.now();

    public LegacyMatchingAsyncExecutor(
            LegacyMatchingService legacyMatchingService,
            LegacyMatchingIndexJobMapper legacyMatchingIndexJobMapper,
            @Qualifier("legacyMatchingTaskExecutor") Executor taskExecutor
    ) {
        this.legacyMatchingService = legacyMatchingService;
        this.legacyMatchingIndexJobMapper = legacyMatchingIndexJobMapper;
        this.taskExecutor = taskExecutor;
    }

    public boolean submit(Long jobId) {
        if (!queuedJobIds.add(jobId)) {
            return false;
        }
        try {
            taskExecutor.execute(() -> {
                try {
                    legacyMatchingService.executeMatchingRun(jobId);
                } finally {
                    queuedJobIds.remove(jobId);
                }
            });
            return true;
        } catch (RuntimeException ex) {
            queuedJobIds.remove(jobId);
            throw ex;
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedRuns() {
        legacyMatchingIndexJobMapper.markInterruptedRunsFailed(INTERRUPTED_REASON, executorStartedAt);
    }
}
