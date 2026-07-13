package com.researchi.admin.legacy.matching.service;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.legacy.application.service.LegacyApplicationConsentService;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingIndexJob;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingJob;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingRunTicket;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingSearchCondition;
import com.researchi.admin.legacy.matching.domain.LegacySmsSendLimitExceededException;
import com.researchi.admin.legacy.matching.domain.LegacyStoredMatchingResult;
import com.researchi.admin.legacy.matching.mapper.LegacyApplicationKeywordMapper;
import com.researchi.admin.legacy.matching.mapper.LegacyMatchingIndexJobMapper;
import com.researchi.admin.legacy.matching.mapper.LegacyMatchingJobMapper;
import com.researchi.admin.legacy.research.domain.ResearchApplication;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.legacy.research.service.ResearchApplicationService;
import com.researchi.admin.legacy.research.service.ResearchMasterService;
import com.researchi.admin.mailing.service.MailDispatchGateway;
import com.researchi.admin.notification.config.NotificationProperties;
import com.researchi.admin.notification.config.SmsProperties;
import com.researchi.admin.notification.mapper.AdminNotificationLogMapper;
import com.researchi.admin.notification.service.ApplicantNotificationSmsGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegacyMatchingServiceTest {

    private static final String ADD_COMMENT_FILTER = "\uCD94\uAC00\uAE30\uC7AC\uC0AC\uD56D: vitamin";
    private static final String JOB_FILTER = "\uC9C1\uC5C5: office";

    @Mock
    private ResearchMasterService researchMasterService;
    @Mock
    private ResearchApplicationService researchApplicationService;
    @Mock
    private ApplicantNotificationSmsGateway applicantNotificationSmsGateway;
    @Mock
    private MailDispatchGateway mailDispatchGateway;
    @Mock
    private AdminNotificationLogMapper adminNotificationLogMapper;
    @Mock
    private NotificationProperties notificationProperties;
    @Mock
    private SmsProperties smsProperties;
    @Mock
    private AdminActionLogService adminActionLogService;
    @Mock
    private LegacyApplicationKeywordMapper legacyApplicationKeywordMapper;
    @Mock
    private LegacyMatchingJobMapper legacyMatchingJobMapper;
    @Mock
    private LegacyMatchingIndexJobMapper legacyMatchingIndexJobMapper;
    @Mock
    private LegacyApplicationConsentService legacyApplicationConsentService;

    private LegacyMatchingService legacyMatchingService;

    @BeforeEach
    void setUp() {
        legacyMatchingService = new LegacyMatchingService(
                researchMasterService,
                researchApplicationService,
                applicantNotificationSmsGateway,
                mailDispatchGateway,
                adminNotificationLogMapper,
                notificationProperties,
                smsProperties,
                adminActionLogService,
                legacyApplicationKeywordMapper,
                legacyMatchingJobMapper,
                legacyMatchingIndexJobMapper,
                legacyApplicationConsentService,
                5000
        );
        org.mockito.Mockito.lenient().when(legacyApplicationConsentService.filterActiveFutureRecruitment(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        org.mockito.Mockito.lenient().when(legacyApplicationConsentService.allowsSms(any(), any())).thenReturn(true);
        org.mockito.Mockito.lenient().when(legacyApplicationConsentService.allowsEmail(any(), any())).thenReturn(true);
    }

    @Test
    void matchingResultsExcludeApplicantsWithoutActiveFutureRecruitmentConsent() {
        LegacyMatchingSearchCondition condition = condition(null, null, "office", null, null, null);
        ResearchApplication unconsented = application(1L, 101L, "legacy", "010-1111-2222", "office", "");
        ResearchApplication consented = application(1L, 102L, "consented", "010-3333-4444", "office", "");
        when(legacyMatchingJobMapper.findById(10L)).thenReturn(matchingJob(10L, 46408L, condition));
        when(researchApplicationService.getMatchingIndexCandidatePage(any(LegacyMatchingSearchCondition.class), anyInt(), anyInt()))
                .thenReturn(List.of(unconsented, consented));
        when(legacyApplicationConsentService.filterActiveFutureRecruitment(List.of(unconsented, consented)))
                .thenReturn(List.of(consented));

        legacyMatchingService.executeMatchingJob(10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LegacyStoredMatchingResult>> resultsCaptor = ArgumentCaptor.forClass(List.class);
        verify(legacyMatchingJobMapper).insertResults(resultsCaptor.capture());
        assertThat(resultsCaptor.getValue())
                .extracting(LegacyStoredMatchingResult::getResearchAppSeq)
                .containsExactly(102L);
    }

    @Test
    void fieldConditionsAreUsedWithoutKeywordIndexCandidates() {
        LegacyMatchingSearchCondition condition = condition(null, null, null, null, null, "vitamin");
        when(legacyMatchingJobMapper.findById(10L)).thenReturn(matchingJob(10L, 46408L, condition));
        when(researchApplicationService.getMatchingIndexCandidatePage(any(LegacyMatchingSearchCondition.class), anyInt(), anyInt()))
                .thenReturn(List.of(application(1L, 101L, "person", "010-1111-2222", "office", "vitamin user")));

        legacyMatchingService.executeMatchingJob(10L);

        ArgumentCaptor<LegacyMatchingJob> jobCaptor = ArgumentCaptor.forClass(LegacyMatchingJob.class);
        verify(legacyMatchingJobMapper).markCompleted(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getActiveKeywordText()).isEqualTo(ADD_COMMENT_FILTER);
        assertThat(jobCaptor.getValue().getMatchedCount()).isEqualTo(1);
    }

    @Test
    void additionalAnswerTextIsUsedForAddCommentMatching() {
        LegacyMatchingSearchCondition condition = condition(null, null, null, null, null, "card");
        when(legacyMatchingJobMapper.findById(10L)).thenReturn(matchingJob(10L, 46408L, condition));
        when(researchApplicationService.getApplicationSeqsByAdditionalAnswerTerms(List.of("card"), 1000))
                .thenReturn(List.of(101L));
        when(researchApplicationService.getMatchingIndexCandidatePage(any(LegacyMatchingSearchCondition.class), anyInt(), anyInt()))
                .thenReturn(List.of(application(1L, 101L, "person", "010-1111-2222", "office", "")));
        when(researchApplicationService.getFormattedAdditionalAnswers(1L, 101L))
                .thenReturn("survey answer: card user");

        legacyMatchingService.executeMatchingJob(10L);

        ArgumentCaptor<LegacyMatchingSearchCondition> conditionCaptor = ArgumentCaptor.forClass(LegacyMatchingSearchCondition.class);
        verify(researchApplicationService).getMatchingIndexCandidatePage(conditionCaptor.capture(), anyInt(), anyInt());
        assertThat(conditionCaptor.getValue().getAdditionalAnswerResearchAppSeqs()).containsExactly(101L);

        ArgumentCaptor<LegacyMatchingJob> jobCaptor = ArgumentCaptor.forClass(LegacyMatchingJob.class);
        verify(legacyMatchingJobMapper).markCompleted(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getMatchedCount()).isEqualTo(1);
        assertThat(jobCaptor.getValue().getActiveKeywordText())
                .isEqualTo("\uCD94\uAC00\uAE30\uC7AC\uC0AC\uD56D: card");
    }

    @Test
    void allStructuredConditionsMustMatchBeforeResultIsStored() {
        LegacyMatchingSearchCondition condition = condition("\uB0A8\uC790", null, null, null, null, "\uAE08\uC735");
        ResearchApplication maleWithoutFinance = application(1L, 101L, "male only", "010-1111-2222", "office", "");
        maleWithoutFinance.setAppSex("1");
        ResearchApplication maleWithFinance = application(1L, 102L, "male finance", "010-3333-4444", "office", "\uAE08\uC735 app");
        maleWithFinance.setAppSex("1");
        when(legacyMatchingJobMapper.findById(10L)).thenReturn(matchingJob(10L, 46408L, condition));
        when(researchApplicationService.getApplicationSeqsByAdditionalAnswerTerms(List.of("\uAE08\uC735"), 1000))
                .thenReturn(List.of(102L));
        when(researchApplicationService.getMatchingIndexCandidatePage(any(LegacyMatchingSearchCondition.class), anyInt(), anyInt()))
                .thenReturn(List.of(maleWithoutFinance, maleWithFinance));
        when(researchApplicationService.getFormattedAdditionalAnswers(1L, 101L)).thenReturn("");

        legacyMatchingService.executeMatchingJob(10L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LegacyStoredMatchingResult>> resultsCaptor = ArgumentCaptor.forClass(List.class);
        verify(legacyMatchingJobMapper).insertResults(resultsCaptor.capture());
        assertThat(resultsCaptor.getValue())
                .extracting(LegacyStoredMatchingResult::getResearchAppSeq)
                .containsExactly(102L);
    }

    @Test
    void overviewReadsStoredMatchingResultsForCondition() {
        LegacyMatchingSearchCondition condition = condition(null, null, null, null, null, "vitamin");
        LegacyMatchingJob job = matchingJob(10L, 46408L, condition);
        job.setStatus("COMPLETED");
        job.setCandidatePoolCount(3);
        job.setBlacklistedExcludedCount(1);
        when(researchMasterService.getResearchMaster(46408L)).thenReturn(research("Vitamin research"));
        when(legacyApplicationKeywordMapper.countIndexedApplications()).thenReturn(100);
        when(legacyMatchingJobMapper.findLatestForCriteria(46408L, condition.storageKey(), "")).thenReturn(job);
        when(legacyMatchingJobMapper.findResultsByJobId(10L, 200))
                .thenReturn(List.of(storedResult(1L, 101L, 1, ADD_COMMENT_FILTER)));
        when(researchApplicationService.getApplication(1L, 101L))
                .thenReturn(application(1L, 101L, "person", "010-1111-2222", "office", "vitamin user"));

        var overview = legacyMatchingService.getOverview(46408L, condition);

        assertThat(overview.results()).hasSize(1);
        assertThat(overview.activeKeywords()).containsExactly(ADD_COMMENT_FILTER);
        assertThat(overview.candidatePoolCount()).isEqualTo(3);
        assertThat(overview.blacklistedExcludedCount()).isEqualTo(1);
        assertThat(overview.matchingStatus()).isEqualTo("COMPLETED");
        assertThat(overview.indexLimit()).isEqualTo(5000);
    }

    @Test
    void overviewExcludesStoredResultWhenFutureRecruitmentConsentIsNoLongerActive() {
        LegacyMatchingSearchCondition condition = condition(null, null, null, null, null, "vitamin");
        LegacyMatchingJob job = matchingJob(10L, 46408L, condition);
        job.setStatus("COMPLETED");
        ResearchApplication application = application(1L, 101L, "person", "010-1111-2222", "office", "vitamin user");
        when(researchMasterService.getResearchMaster(46408L)).thenReturn(research("Vitamin research"));
        when(legacyMatchingJobMapper.findLatestForCriteria(46408L, condition.storageKey(), "")).thenReturn(job);
        when(legacyMatchingJobMapper.findResultsByJobId(10L, 200))
                .thenReturn(List.of(storedResult(1L, 101L, 1, ADD_COMMENT_FILTER)));
        when(researchApplicationService.getApplication(1L, 101L)).thenReturn(application);
        when(legacyApplicationConsentService.filterActiveFutureRecruitment(List.of(application))).thenReturn(List.of());

        var overview = legacyMatchingService.getOverview(46408L, condition);

        assertThat(overview.results()).isEmpty();
    }

    @Test
    void overviewMarksRowsWithSuccessfulChannelNotifications() {
        LegacyMatchingSearchCondition condition = condition(null, null, null, null, null, "vitamin");
        LegacyMatchingJob job = matchingJob(10L, 46408L, condition);
        job.setStatus("COMPLETED");
        when(researchMasterService.getResearchMaster(46408L)).thenReturn(research("Vitamin research"));
        when(legacyMatchingJobMapper.findLatestForCriteria(46408L, condition.storageKey(), "")).thenReturn(job);
        when(legacyMatchingJobMapper.findResultsByJobId(10L, 200))
                .thenReturn(List.of(storedResult(1L, 101L, 1, ADD_COMMENT_FILTER)));
        when(researchApplicationService.getApplication(1L, 101L))
                .thenReturn(application(1L, 101L, "person", "010-1111-2222", "office", "vitamin user"));
        when(adminNotificationLogMapper.countSuccessfulDuplicate(46408L, 101L, "LEGACY_SMS", ADD_COMMENT_FILTER))
                .thenReturn(1);
        when(adminNotificationLogMapper.countSuccessfulDuplicate(46408L, 101L, "LEGACY_EMAIL", ADD_COMMENT_FILTER))
                .thenReturn(0);

        var overview = legacyMatchingService.getOverview(46408L, condition);

        assertThat(overview.results()).singleElement()
                .satisfies(result -> {
                    assertThat(result.smsSent()).isTrue();
                    assertThat(result.emailSent()).isFalse();
                });
    }

    @Test
    void overviewAppliesSmsAndEmailConsentIndependently() {
        LegacyMatchingSearchCondition condition = condition(null, null, null, null, null, "vitamin");
        LegacyMatchingJob job = matchingJob(10L, 46408L, condition);
        job.setStatus("COMPLETED");
        when(researchMasterService.getResearchMaster(46408L)).thenReturn(research("Vitamin research"));
        when(legacyMatchingJobMapper.findLatestForCriteria(46408L, condition.storageKey(), "")).thenReturn(job);
        when(legacyMatchingJobMapper.findResultsByJobId(10L, 200))
                .thenReturn(List.of(storedResult(1L, 101L, 1, ADD_COMMENT_FILTER)));
        when(researchApplicationService.getApplication(1L, 101L))
                .thenReturn(application(1L, 101L, "person", "010-1111-2222", "office", "vitamin user"));
        when(legacyApplicationConsentService.allowsSms(1L, 101L)).thenReturn(true);
        when(legacyApplicationConsentService.allowsEmail(1L, 101L)).thenReturn(false);

        var overview = legacyMatchingService.getOverview(46408L, condition);

        assertThat(overview.results()).singleElement().satisfies(result -> {
            assertThat(result.smsAllowed()).isTrue();
            assertThat(result.emailAllowed()).isFalse();
        });
    }

    @Test
    void overviewDeduplicatesStoredResultsByNameAndPhone() {
        LegacyMatchingSearchCondition condition = condition(null, null, null, null, null, "ramen");
        LegacyMatchingJob job = matchingJob(10L, 46408L, condition);
        job.setStatus("COMPLETED");
        when(researchMasterService.getResearchMaster(46408L)).thenReturn(research("Ramen research"));
        when(legacyMatchingJobMapper.findLatestForCriteria(46408L, condition.storageKey(), "")).thenReturn(job);
        when(legacyMatchingJobMapper.findResultsByJobId(10L, 200)).thenReturn(List.of(
                storedResult(1L, 101L, 1, "\uCD94\uAC00\uAE30\uC7AC\uC0AC\uD56D: ramen"),
                storedResult(1L, 102L, 2, "\uCD94\uAC00\uAE30\uC7AC\uC0AC\uD56D: ramen"),
                storedResult(1L, 103L, 3, "\uCD94\uAC00\uAE30\uC7AC\uC0AC\uD56D: ramen")
        ));
        when(researchApplicationService.getApplication(1L, 101L))
                .thenReturn(application(1L, 101L, "same", "010-1111-2222", "office", "ramen"));
        when(researchApplicationService.getApplication(1L, 102L))
                .thenReturn(application(1L, 102L, "same", "01011112222", "office", "ramen"));
        when(researchApplicationService.getApplication(1L, 103L))
                .thenReturn(application(1L, 103L, "other", "010-3333-4444", "office", "ramen"));

        var overview = legacyMatchingService.getOverview(46408L, condition);

        assertThat(overview.results())
                .extracting(result -> result.application().getResearchAppSeq())
                .containsExactly(101L, 103L);
        assertThat(overview.results())
                .extracting("rowNo")
                .containsExactly(1, 2);
    }

    @Test
    void runMatchingCycleStoresConditionAndResetsIndex() {
        LegacyMatchingSearchCondition condition = condition(null, "1990", "office", "company", "seoul", "vitamin");
        when(legacyMatchingIndexJobMapper.findNextCycleNo(46408L)).thenReturn(1);
        when(researchApplicationService.getMatchingIndexCandidatePage(any(LegacyMatchingSearchCondition.class), anyInt(), anyInt()))
                .thenReturn(List.of(application(1L, 101L, "person", "010-1111-2222", "office", "vitamin user")));
        doAnswer(invocation -> {
            LegacyMatchingJob job = invocation.getArgument(0);
            job.setId(10L);
            return null;
        }).when(legacyMatchingJobMapper).insertJob(any());
        when(legacyMatchingJobMapper.findById(10L)).thenReturn(matchingJob(10L, 46408L, condition));

        var result = legacyMatchingService.runMatchingCycle(46408L, condition);

        assertThat(result.indexedApplicationCount()).isEqualTo(1);
        assertThat(result.insertedKeywordCount()).isEqualTo(0);

        ArgumentCaptor<LegacyMatchingIndexJob> jobCaptor = ArgumentCaptor.forClass(LegacyMatchingIndexJob.class);
        verify(legacyMatchingIndexJobMapper).insertJob(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getCycleNo()).isEqualTo(1);
        assertThat(jobCaptor.getValue().getIncludeKeywordText()).isEqualTo(condition.storageKey());
        assertThat(jobCaptor.getValue().getExcludeKeywordText()).isEqualTo("");
        assertThat(jobCaptor.getValue().getIndexLimit()).isEqualTo(LegacyMatchingService.INDEX_BATCH_SIZE);
        verify(legacyMatchingIndexJobMapper).markCompleted(any());
        verify(legacyMatchingJobMapper).insertJob(any());
        verify(legacyApplicationKeywordMapper, times(2)).deleteAll();
    }

    @Test
    void runMatchingCycleReturnsRunningJobWithoutCreatingZeroCountCycle() throws Exception {
        LegacyMatchingSearchCondition condition = condition(null, "1988-1999", null, null, null, null);
        setIndexRunning(true);
        LegacyMatchingIndexJob runningJob = new LegacyMatchingIndexJob();
        runningJob.setCycleNo(16);
        runningJob.setStatus("RUNNING");
        when(legacyMatchingIndexJobMapper.findRunningForCriteria(46408L, condition.storageKey(), ""))
                .thenReturn(runningJob);

        var result = legacyMatchingService.runMatchingCycle(46408L, condition);

        assertThat(result.cycleNo()).isEqualTo(16);
        assertThat(result.alreadyRunning()).isTrue();
        verify(legacyMatchingIndexJobMapper, never()).insertJob(any());
    }

    @Test
    void startMatchingRunCreatesPendingJobWithoutComputingResults() {
        LegacyMatchingSearchCondition condition = condition("남자", "1988-1995", null, null, null, null);
        when(legacyMatchingIndexJobMapper.findRunningForCriteria(46408L, condition.storageKey(), ""))
                .thenReturn(null);
        when(legacyMatchingIndexJobMapper.findNextCycleNo(46408L)).thenReturn(16);
        doAnswer(invocation -> {
            LegacyMatchingIndexJob job = invocation.getArgument(0);
            job.setId(77L);
            return null;
        }).when(legacyMatchingIndexJobMapper).insertJob(any());

        LegacyMatchingRunTicket ticket = legacyMatchingService.startOrReuseMatchingRun(46408L, condition);

        assertThat(ticket.jobId()).isEqualTo(77L);
        assertThat(ticket.cycleNo()).isEqualTo(16);
        assertThat(ticket.status()).isEqualTo("PENDING");
        assertThat(ticket.reused()).isFalse();
        verify(researchApplicationService, never())
                .getMatchingIndexCandidatePage(any(LegacyMatchingSearchCondition.class), anyInt(), anyInt());
    }

    @Test
    void startMatchingRunReusesSameCriteriaJob() {
        LegacyMatchingSearchCondition condition = condition("남자", "1988-1995", null, null, null, null);
        LegacyMatchingIndexJob running = indexJob(77L, 46408L, 16, condition.storageKey(), "RUNNING");
        when(legacyMatchingIndexJobMapper.findRunningForCriteria(46408L, condition.storageKey(), ""))
                .thenReturn(running);

        LegacyMatchingRunTicket ticket = legacyMatchingService.startOrReuseMatchingRun(46408L, condition);

        assertThat(ticket).isEqualTo(new LegacyMatchingRunTicket(77L, 16, "RUNNING", true));
        verify(legacyMatchingIndexJobMapper, never()).insertJob(any());
    }

    @Test
    void matchingRunStatusRejectsJobFromAnotherResearch() {
        when(legacyMatchingIndexJobMapper.findById(77L))
                .thenReturn(indexJob(77L, 99999L, 16, "appSex=male", "RUNNING"));

        assertThatThrownBy(() -> legacyMatchingService.getMatchingRunStatus(46408L, 77L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("매칭 작업");
    }

    @Test
    void completedMatchingRunResultUsesPersistedCounts() {
        LegacyMatchingIndexJob completed = indexJob(77L, 46408L, 16, "appSex=male", "COMPLETED");
        completed.setIndexedApplicationCount(423);
        completed.setInsertedKeywordCount(12);
        completed.setSkippedAlreadyIndexedCount(4);
        when(legacyMatchingIndexJobMapper.findById(77L)).thenReturn(completed);

        var result = legacyMatchingService.getCompletedMatchingRunResult(46408L, 77L);

        assertThat(result.cycleNo()).isEqualTo(16);
        assertThat(result.indexedApplicationCount()).isEqualTo(423);
        assertThat(result.insertedKeywordCount()).isEqualTo(12);
        assertThat(result.skippedAlreadyIndexedCount()).isEqualTo(4);
    }

    @Test
    void executeMatchingRunMarksIndexJobCompleteAfterResultGeneration() {
        LegacyMatchingSearchCondition condition = condition("남자", "1988-1995", null, null, null, null);
        LegacyMatchingIndexJob indexJob = indexJob(77L, 46408L, 16, condition.storageKey(), "PENDING");
        when(legacyMatchingIndexJobMapper.findById(77L)).thenReturn(indexJob);
        when(legacyMatchingIndexJobMapper.markStarted(77L)).thenReturn(1);
        when(researchApplicationService.getMatchingIndexCandidatePage(
                any(LegacyMatchingSearchCondition.class), anyInt(), anyInt()))
                .thenReturn(List.of(application(1L, 101L, "person", "010-1111-2222", "office", "")));
        doAnswer(invocation -> {
            LegacyMatchingJob job = invocation.getArgument(0);
            job.setId(10L);
            return null;
        }).when(legacyMatchingJobMapper).insertJob(any());
        when(legacyMatchingJobMapper.findById(10L)).thenReturn(matchingJob(10L, 46408L, condition));

        legacyMatchingService.executeMatchingRun(77L);

        InOrder completionOrder = inOrder(legacyMatchingJobMapper, legacyMatchingIndexJobMapper);
        completionOrder.verify(legacyMatchingJobMapper).markCompleted(any());
        completionOrder.verify(legacyMatchingIndexJobMapper).markCompleted(any());
        verify(legacyMatchingIndexJobMapper).markStarted(77L);
    }

    @Test
    void executeMatchingRunMarksIndexJobFailedWhenCandidateLookupFails() {
        LegacyMatchingSearchCondition condition = condition("남자", null, null, null, null, null);
        LegacyMatchingIndexJob indexJob = indexJob(77L, 46408L, 16, condition.storageKey(), "PENDING");
        when(legacyMatchingIndexJobMapper.findById(77L)).thenReturn(indexJob);
        when(legacyMatchingIndexJobMapper.markStarted(77L)).thenReturn(1);
        when(researchApplicationService.getMatchingIndexCandidatePage(
                any(LegacyMatchingSearchCondition.class), anyInt(), anyInt()))
                .thenThrow(new IllegalStateException("candidate failure"));

        legacyMatchingService.executeMatchingRun(77L);

        verify(legacyMatchingIndexJobMapper).markFailed(77L, "candidate failure");
        verify(legacyMatchingIndexJobMapper, never()).markCompleted(any());
    }

    @Test
    void executeMatchingRunStopsWhenPendingClaimFails() {
        LegacyMatchingSearchCondition condition = condition("남자", null, null, null, null, null);
        LegacyMatchingIndexJob indexJob = indexJob(77L, 46408L, 16, condition.storageKey(), "PENDING");
        when(legacyMatchingIndexJobMapper.findById(77L)).thenReturn(indexJob);
        when(legacyMatchingIndexJobMapper.markStarted(77L)).thenReturn(0);

        legacyMatchingService.executeMatchingRun(77L);

        verify(researchApplicationService, never())
                .getMatchingIndexCandidatePage(any(LegacyMatchingSearchCondition.class), anyInt(), anyInt());
        verify(legacyMatchingIndexJobMapper, never()).markCompleted(any());
    }

    @Test
    void queueFailureOnlyMarksPendingRunFailed() {
        legacyMatchingService.failMatchingRun(77L, "queue full");

        verify(legacyMatchingIndexJobMapper).markPendingFailed(77L, "queue full");
        verify(legacyMatchingIndexJobMapper, never()).markFailed(77L, "queue full");
    }

    @Test
    void cleanupMatchingLogsAfterClosedDeadlineKeepsSummaryLogs() {
        when(researchMasterService.getClosedResearchNosBefore(any())).thenReturn(List.of(46408L, 46409L));
        when(legacyMatchingJobMapper.deleteResultsByResearchNos(List.of(46408L, 46409L))).thenReturn(3);

        int deleted = legacyMatchingService.cleanupMatchingLogsAfterClosedDeadline();

        assertThat(deleted).isEqualTo(3);
        verify(legacyMatchingJobMapper, never()).deleteJobsByResearchNos(any());
        verify(legacyMatchingIndexJobMapper, never()).deleteByResearchNos(any());
    }

    @Test
    void sendSmsNotificationsStopsBeforeDispatchWhenDailyLimitWouldBeExceeded() throws Exception {
        LegacyMatchingSearchCondition condition = condition(null, null, null, null, null, "limit");
        LegacyMatchingJob job = matchingJob(10L, 46408L, condition);
        job.setStatus("COMPLETED");
        when(researchMasterService.getResearchMaster(46408L)).thenReturn(research("Limit research"));
        when(legacyApplicationKeywordMapper.countIndexedApplications()).thenReturn(100);
        when(legacyMatchingJobMapper.findLatestForCriteria(46408L, condition.storageKey(), "")).thenReturn(job);
        when(legacyMatchingJobMapper.findResultsByJobId(10L, 200))
                .thenReturn(List.of(storedResult(1L, 101L, 1, "\uCD94\uAC00\uAE30\uC7AC\uC0AC\uD56D: limit")));
        when(researchApplicationService.getApplication(1L, 101L))
                .thenReturn(application(1L, 101L, "sms", "010-1111-2222", "office", "limit"));
        when(smsProperties.getDailySendLimit()).thenReturn(500);
        when(smsProperties.getMonthlySendLimit()).thenReturn(10000);
        when(adminNotificationLogMapper.countSentByChannelBetween(eq("LEGACY_SMS"), any(), any()))
                .thenReturn(500, 1000);

        assertThatThrownBy(() -> legacyMatchingService.sendSmsNotifications(46408L, condition, null, null))
                .isInstanceOf(LegacySmsSendLimitExceededException.class);

        verify(applicantNotificationSmsGateway, never()).dispatch(any());
    }

    @Test
    void sendSmsNotificationsSkipsApplicantWithoutSmsConsent() throws Exception {
        LegacyMatchingSearchCondition condition = condition(null, null, null, null, null, "consent");
        LegacyMatchingJob job = matchingJob(10L, 46408L, condition);
        job.setStatus("COMPLETED");
        when(researchMasterService.getResearchMaster(46408L)).thenReturn(research("Consent research"));
        when(legacyMatchingJobMapper.findLatestForCriteria(46408L, condition.storageKey(), "")).thenReturn(job);
        when(legacyMatchingJobMapper.findResultsByJobId(10L, 200))
                .thenReturn(List.of(storedResult(1L, 101L, 1, "추가기재사항: consent")));
        when(researchApplicationService.getApplication(1L, 101L))
                .thenReturn(application(1L, 101L, "sms", "010-1111-2222", "office", "consent"));
        when(legacyApplicationConsentService.allowsSms(1L, 101L)).thenReturn(false);

        var result = legacyMatchingService.sendSmsNotifications(46408L, condition, null, null);

        assertThat(result.sentCount()).isZero();
        assertThat(result.failedCount()).isEqualTo(1);
        verify(applicantNotificationSmsGateway, never()).dispatch(any());
    }

    @Test
    void sendEmailNotificationsSkipsApplicantWithoutEmailConsent() throws Exception {
        LegacyMatchingSearchCondition condition = condition(null, null, null, null, null, "consent");
        LegacyMatchingJob job = matchingJob(10L, 46408L, condition);
        job.setStatus("COMPLETED");
        ResearchApplication application = application(1L, 101L, "email", "010-1111-2222", "office", "consent");
        application.setAppEmail("applicant@example.com");
        when(researchMasterService.getResearchMaster(46408L)).thenReturn(research("Consent research"));
        when(legacyMatchingJobMapper.findLatestForCriteria(46408L, condition.storageKey(), "")).thenReturn(job);
        when(legacyMatchingJobMapper.findResultsByJobId(10L, 200))
                .thenReturn(List.of(storedResult(1L, 101L, 1, "추가기재사항: consent")));
        when(researchApplicationService.getApplication(1L, 101L)).thenReturn(application);
        when(legacyApplicationConsentService.allowsEmail(1L, 101L)).thenReturn(false);

        var result = legacyMatchingService.sendEmailNotifications(46408L, condition, null, null);

        assertThat(result.sentCount()).isZero();
        assertThat(result.failedCount()).isEqualTo(1);
        verify(mailDispatchGateway, never()).dispatch(any());
    }

    @Test
    void sendSmsNotificationsOnlyDispatchesSelectedApplications() throws Exception {
        LegacyMatchingSearchCondition condition = condition(null, null, null, null, null, "limit");
        LegacyMatchingJob job = matchingJob(10L, 46408L, condition);
        job.setStatus("COMPLETED");
        when(researchMasterService.getResearchMaster(46408L)).thenReturn(research("Limit research"));
        when(legacyMatchingJobMapper.findLatestForCriteria(46408L, condition.storageKey(), "")).thenReturn(job);
        when(legacyMatchingJobMapper.findResultsByJobId(10L, 200)).thenReturn(List.of(
                storedResult(1L, 101L, 1, "\uCD94\uAC00\uAE30\uC7AC\uC0AC\uD56D: limit"),
                storedResult(1L, 102L, 2, "\uCD94\uAC00\uAE30\uC7AC\uC0AC\uD56D: limit")
        ));
        when(researchApplicationService.getApplication(1L, 101L))
                .thenReturn(application(1L, 101L, "sms one", "010-1111-2222", "office", "limit"));
        when(researchApplicationService.getApplication(1L, 102L))
                .thenReturn(application(1L, 102L, "sms two", "010-3333-4444", "office", "limit"));
        when(smsProperties.getDailySendLimit()).thenReturn(500);
        when(smsProperties.getMonthlySendLimit()).thenReturn(10000);

        var result = legacyMatchingService.sendSmsNotifications(46408L, condition, Set.of(102L), null, null);

        assertThat(result.targetCount()).isEqualTo(1);
        assertThat(result.sentCount()).isEqualTo(1);
        verify(applicantNotificationSmsGateway).dispatch(argThat(request -> "01033334444".equals(request.recipient())));
        verify(applicantNotificationSmsGateway, times(1)).dispatch(any());
    }

    @Test
    void matchingResultsRemoveDuplicateApplicantsByNameAndPhone() {
        LegacyMatchingSearchCondition condition = condition(null, null, null, null, null, "ramen");
        when(legacyMatchingJobMapper.findById(10L)).thenReturn(matchingJob(10L, 46408L, condition));
        when(researchApplicationService.getMatchingIndexCandidatePage(any(LegacyMatchingSearchCondition.class), anyInt(), anyInt()))
                .thenReturn(List.of(
                        application(1L, 101L, "same", "010-1111-2222", "office", "ramen"),
                        application(1L, 102L, "same", "01011112222", "office", "ramen"),
                        application(1L, 103L, "other", "010-3333-4444", "office", "ramen")
                ));

        legacyMatchingService.executeMatchingJob(10L);

        ArgumentCaptor<LegacyMatchingJob> jobCaptor = ArgumentCaptor.forClass(LegacyMatchingJob.class);
        verify(legacyMatchingJobMapper).markCompleted(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getMatchedCount()).isEqualTo(2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LegacyStoredMatchingResult>> resultsCaptor = ArgumentCaptor.forClass(List.class);
        verify(legacyMatchingJobMapper).insertResults(resultsCaptor.capture());
        assertThat(resultsCaptor.getValue())
                .extracting(LegacyStoredMatchingResult::getResearchAppSeq)
                .containsExactly(103L, 102L);
    }

    @Test
    void matchingResultsExcludeApplicantsAlreadySentForSameCondition() {
        LegacyMatchingSearchCondition condition = condition(null, null, "office", null, null, null);
        when(legacyMatchingJobMapper.findById(10L)).thenReturn(matchingJob(10L, 46408L, condition));
        when(researchApplicationService.getMatchingIndexCandidatePage(any(LegacyMatchingSearchCondition.class), anyInt(), anyInt()))
                .thenReturn(List.of(
                        application(1L, 101L, "sent", "010-1111-2222", "office", ""),
                        application(1L, 102L, "not sent", "010-3333-4444", "office", "")
                ));
        when(adminNotificationLogMapper.countSuccessfulDuplicate(46408L, 101L, "LEGACY_SMS", JOB_FILTER))
                .thenReturn(1);

        legacyMatchingService.executeMatchingJob(10L);

        ArgumentCaptor<LegacyMatchingJob> jobCaptor = ArgumentCaptor.forClass(LegacyMatchingJob.class);
        verify(legacyMatchingJobMapper).markCompleted(jobCaptor.capture());
        assertThat(jobCaptor.getValue().getMatchedCount()).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<LegacyStoredMatchingResult>> resultsCaptor = ArgumentCaptor.forClass(List.class);
        verify(legacyMatchingJobMapper).insertResults(resultsCaptor.capture());
        assertThat(resultsCaptor.getValue())
                .extracting(LegacyStoredMatchingResult::getResearchAppSeq)
                .containsExactly(102L);
    }

    private LegacyMatchingJob matchingJob(Long id, Long researchNo, LegacyMatchingSearchCondition condition) {
        LegacyMatchingJob job = new LegacyMatchingJob();
        job.setId(id);
        job.setResearchNo(researchNo);
        job.setIncludeKeywordText(condition.storageKey());
        job.setExcludeKeywordText("");
        return job;
    }

    private LegacyMatchingIndexJob indexJob(
            Long id,
            Long researchNo,
            int cycleNo,
            String conditionStorageKey,
            String status
    ) {
        LegacyMatchingIndexJob job = new LegacyMatchingIndexJob();
        job.setId(id);
        job.setResearchNo(researchNo);
        job.setCycleNo(cycleNo);
        job.setIncludeKeywordText(conditionStorageKey);
        job.setExcludeKeywordText("");
        job.setStatus(status);
        return job;
    }

    private LegacyMatchingSearchCondition condition(String appSex, String appBirth, String appJob, String appCompany, String appAddr, String addComment) {
        return new LegacyMatchingSearchCondition(appSex, appBirth, appJob, appCompany, appAddr, addComment);
    }

    private void setIndexRunning(boolean value) throws Exception {
        var field = LegacyMatchingService.class.getDeclaredField("indexRunning");
        field.setAccessible(true);
        ((AtomicBoolean) field.get(legacyMatchingService)).set(value);
    }

    private ResearchMaster research(String title) {
        ResearchMaster research = new ResearchMaster();
        research.setResearchNo(46408L);
        research.setResearchTitle(title);
        return research;
    }

    private LegacyStoredMatchingResult storedResult(Long researchNo, Long researchAppSeq, int rowNo, String matchedKeywordText) {
        LegacyStoredMatchingResult stored = new LegacyStoredMatchingResult();
        stored.setMatchingJobId(10L);
        stored.setResearchNo(researchNo);
        stored.setResearchAppSeq(researchAppSeq);
        stored.setRowNo(rowNo);
        stored.setMatchScore(1);
        stored.setMatchedKeywordText(matchedKeywordText);
        return stored;
    }

    private ResearchApplication application(Long researchNo, Long researchAppSeq, String name, String phone, String job, String addComment) {
        ResearchApplication application = new ResearchApplication();
        application.setResearchNo(researchNo);
        application.setResearchAppSeq(researchAppSeq);
        application.setAppName(name);
        application.setAppHphone(phone);
        application.setAppBirth("19900101");
        application.setAppJob(job);
        application.setAppCompany("company");
        application.setAppAddr("seoul");
        application.setAddComment(addComment);
        return application;
    }
}
