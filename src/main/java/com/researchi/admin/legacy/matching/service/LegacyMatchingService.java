package com.researchi.admin.legacy.matching.service;

import com.researchi.admin.auth.service.AdminActionLogService;
import com.researchi.admin.auth.service.AdminPrincipal;
import com.researchi.admin.legacy.application.service.LegacyApplicationConsentService;
import com.researchi.admin.common.support.PhoneNumberFormatter;
import com.researchi.admin.export.domain.ExportPayload;
import com.researchi.admin.legacy.matching.domain.LegacyApplicationKeyword;
import com.researchi.admin.legacy.matching.domain.LegacyEmailSendResult;
import com.researchi.admin.legacy.matching.domain.LegacyKeywordIndexResult;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingHistory;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingCandidate;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingIndexJob;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingJob;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingOverview;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingResult;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingRunStatus;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingRunTicket;
import com.researchi.admin.legacy.matching.domain.LegacyMatchingSearchCondition;
import com.researchi.admin.legacy.matching.domain.LegacySmsSendResult;
import com.researchi.admin.legacy.matching.domain.LegacySmsSendLimitExceededException;
import com.researchi.admin.legacy.matching.domain.LegacyStoredMatchingResult;
import com.researchi.admin.legacy.matching.mapper.LegacyApplicationKeywordMapper;
import com.researchi.admin.legacy.matching.mapper.LegacyMatchingIndexJobMapper;
import com.researchi.admin.legacy.matching.mapper.LegacyMatchingJobMapper;
import com.researchi.admin.legacy.research.domain.ResearchApplication;
import com.researchi.admin.legacy.research.domain.ResearchMaster;
import com.researchi.admin.legacy.research.service.ResearchApplicationService;
import com.researchi.admin.legacy.research.service.ResearchMasterService;
import com.researchi.admin.mailing.domain.MailDispatchRequest;
import com.researchi.admin.mailing.service.MailDispatchGateway;
import com.researchi.admin.notification.config.NotificationProperties;
import com.researchi.admin.notification.config.SmsProperties;
import com.researchi.admin.notification.domain.AdminNotificationLog;
import com.researchi.admin.notification.domain.NotificationSmsRequest;
import com.researchi.admin.notification.mapper.AdminNotificationLogMapper;
import com.researchi.admin.notification.service.ApplicantNotificationSmsGateway;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LegacyMatchingService {

    private static final int MATCH_CANDIDATE_LIMIT = 1000;
    private static final int STORED_RESULT_LIMIT = 200;
    private static final int DISPLAY_RESULT_LIMIT = 200;
    static final int INDEX_BATCH_SIZE = 500;
    static final int DEFAULT_INDEX_TOTAL_LIMIT = 5000;
    private static final int MAX_INDEX_TOTAL_LIMIT = 50000;
    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String TXT_CONTENT_TYPE = "text/plain; charset=UTF-8";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("[\\p{IsHangul}A-Za-z0-9]{2,}");
    private static final Set<String> STOP_WORDS = Set.of(
            "좌담회", "설문", "조사", "대상", "일정",
            "방법", "소요시간", "사례비", "소개비",
            "남자", "여성", "남성", "서울", "수도권",
            "거주", "진행", "관련", "가능", "참여",
            "만세", "만원", "이상", "이하", "이외",
            "최근", "개월", "사용", "구매", "브랜드"
    );

    private final ResearchMasterService researchMasterService;
    private final ResearchApplicationService researchApplicationService;
    private final ApplicantNotificationSmsGateway applicantNotificationSmsGateway;
    private final MailDispatchGateway mailDispatchGateway;
    private final AdminNotificationLogMapper adminNotificationLogMapper;
    private final NotificationProperties notificationProperties;
    private final SmsProperties smsProperties;
    private final AdminActionLogService adminActionLogService;
    private final LegacyApplicationKeywordMapper legacyApplicationKeywordMapper;
    private final LegacyMatchingJobMapper legacyMatchingJobMapper;
    private final LegacyMatchingIndexJobMapper legacyMatchingIndexJobMapper;
    private final LegacyApplicationConsentService legacyApplicationConsentService;
    private final int defaultIndexLimit;
    private final AtomicBoolean indexRunning = new AtomicBoolean(false);

    public LegacyMatchingService(
            ResearchMasterService researchMasterService,
            ResearchApplicationService researchApplicationService,
            ApplicantNotificationSmsGateway applicantNotificationSmsGateway,
            MailDispatchGateway mailDispatchGateway,
            AdminNotificationLogMapper adminNotificationLogMapper,
            NotificationProperties notificationProperties,
            SmsProperties smsProperties,
            AdminActionLogService adminActionLogService,
            LegacyApplicationKeywordMapper legacyApplicationKeywordMapper,
            LegacyMatchingJobMapper legacyMatchingJobMapper,
            LegacyMatchingIndexJobMapper legacyMatchingIndexJobMapper,
            LegacyApplicationConsentService legacyApplicationConsentService,
            @Value("${researchi.legacy.matching.index.total-limit:5000}") int defaultIndexLimit
    ) {
        this.researchMasterService = researchMasterService;
        this.researchApplicationService = researchApplicationService;
        this.applicantNotificationSmsGateway = applicantNotificationSmsGateway;
        this.mailDispatchGateway = mailDispatchGateway;
        this.adminNotificationLogMapper = adminNotificationLogMapper;
        this.notificationProperties = notificationProperties;
        this.smsProperties = smsProperties;
        this.adminActionLogService = adminActionLogService;
        this.legacyApplicationKeywordMapper = legacyApplicationKeywordMapper;
        this.legacyMatchingJobMapper = legacyMatchingJobMapper;
        this.legacyMatchingIndexJobMapper = legacyMatchingIndexJobMapper;
        this.legacyApplicationConsentService = legacyApplicationConsentService;
        this.defaultIndexLimit = safeIndexTotalLimit(defaultIndexLimit);
    }

    public LegacyMatchingOverview getOverview(Long researchNo, String includeKeywords, String excludeKeywords) {
        return getOverview(researchNo, LegacyMatchingSearchCondition.fromStorageKey(trimToEmpty(includeKeywords)));
    }

    public LegacyMatchingOverview getOverview(Long researchNo, LegacyMatchingSearchCondition condition) {
        LegacyMatchingSearchCondition normalizedCondition = normalizeCondition(condition);
        ResearchMaster research = researchMasterService.getResearchMaster(researchNo);
        int indexedApplicationCount = legacyApplicationKeywordMapper.countIndexedApplications();
        int nextCycleNo = safeNextCycleNo(researchNo);
        LegacyMatchingJob matchingJob = legacyMatchingJobMapper.findLatestForCriteria(
                researchNo,
                normalizedCondition.storageKey(),
                ""
        );
        List<LegacyMatchingResult> results = applyNotificationStatuses(
                researchNo,
                applyConsentStatuses(filterActiveConsentResults(storedResults(matchingJob)))
        );
        List<String> activeKeywords = normalizedCondition.displayFilters();

        return new LegacyMatchingOverview(
                research,
                normalizedCondition.storageKey(),
                "",
                normalizedCondition,
                defaultIndexLimit,
                activeKeywords,
                results,
                matchingJob == null || matchingJob.getCandidatePoolCount() == null ? 0 : matchingJob.getCandidatePoolCount(),
                indexedApplicationCount,
                matchingJob == null || matchingJob.getBlacklistedExcludedCount() == null ? 0 : matchingJob.getBlacklistedExcludedCount(),
                Math.max(0, nextCycleNo - 1),
                nextCycleNo,
                INDEX_BATCH_SIZE,
                matchingJob == null ? null : matchingJob.getId(),
                matchingJob == null ? "NO_RESULT" : matchingJob.getStatus(),
                matchingJob == null ? null : matchingJob.getFailReason(),
                matchingJob == null ? null : matchingJob.getFinishedAt()
        );
    }

    public ExportPayload prepareMatchingXlsx(Long researchNo, String includeKeywords, String excludeKeywords) {
        return prepareMatchingXlsx(researchNo, LegacyMatchingSearchCondition.fromStorageKey(trimToEmpty(includeKeywords)));
    }

    public ExportPayload prepareMatchingXlsx(Long researchNo, LegacyMatchingSearchCondition condition) {
        LegacyMatchingOverview overview = getOverview(researchNo, condition);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Matching");
            String[] headers = {"No", "성명", "성별", "나이", "직업", "회사/학교", "휴대폰", "주소", "추가기재", "점수", "매칭 키워드"};
            Row headerRow = sheet.createRow(0);
            for (int index = 0; index < headers.length; index++) {
                headerRow.createCell(index).setCellValue(headers[index]);
            }
            int rowIndex = 1;
            for (LegacyMatchingResult result : overview.results()) {
                ResearchApplication application = result.application();
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(result.rowNo());
                row.createCell(1).setCellValue(excelValue(application.getAppName()));
                row.createCell(2).setCellValue(excelValue(application.getAppSexLabel()));
                row.createCell(3).setCellValue(excelValue(application.getAppAge()));
                row.createCell(4).setCellValue(excelValue(application.getAppJob()));
                row.createCell(5).setCellValue(excelValue(application.getAppCompany()));
                row.createCell(6).setCellValue(excelValue(application.getAppHphoneLabel()));
                row.createCell(7).setCellValue(excelValue(application.getAppAddr()));
                row.createCell(8).setCellValue(excelValue(application.getAddComment()));
                row.createCell(9).setCellValue(result.matchScore());
                row.createCell(10).setCellValue(excelValue(result.matchedKeywordText()));
            }
            for (int index = 0; index < headers.length; index++) {
                sheet.autoSizeColumn(index);
            }
            workbook.write(outputStream);
            return new ExportPayload(matchingFileName(overview.research().getResearchTitle(), "xlsx"), XLSX_CONTENT_TYPE, outputStream.toByteArray(), overview.results().size());
        } catch (IOException ex) {
            throw new IllegalStateException("매칭 결과 엑셀 파일을 생성하지 못했습니다.", ex);
        }
    }

    public ExportPayload prepareMatchingTxt(Long researchNo, String includeKeywords, String excludeKeywords) {
        return prepareMatchingTxt(researchNo, LegacyMatchingSearchCondition.fromStorageKey(trimToEmpty(includeKeywords)));
    }

    public ExportPayload prepareMatchingTxt(Long researchNo, LegacyMatchingSearchCondition condition) {
        LegacyMatchingOverview overview = getOverview(researchNo, condition);
        StringBuilder builder = new StringBuilder();
        builder.append(safeText(overview.research().getResearchTitle())).append(System.lineSeparator());
        builder.append(overview.results().size()).append("건").append(System.lineSeparator()).append(System.lineSeparator());
        for (LegacyMatchingResult result : overview.results()) {
            ResearchApplication application = result.application();
            builder.append(result.rowNo()).append(". ")
                    .append(safeText(application.getAppName())).append(" / ")
                    .append(safeText(application.getAppSexLabel())).append(" / ")
                    .append(safeText(application.getAppAge())).append(" / ")
                    .append(safeText(application.getAppJob())).append(" / ")
                    .append(safeText(application.getAppCompany())).append(" / ")
                    .append(safeText(application.getAppHphoneLabel())).append(" / ")
                    .append(safeText(application.getAppAddr())).append(" / ")
                    .append(safeText(application.getAddComment())).append(" / ")
                    .append("점수 ").append(result.matchScore()).append(" / ")
                    .append(safeText(result.matchedKeywordText()))
                    .append(System.lineSeparator()).append(System.lineSeparator());
        }
        return new ExportPayload(matchingFileName(overview.research().getResearchTitle(), "txt"), TXT_CONTENT_TYPE, builder.toString().getBytes(StandardCharsets.UTF_8), overview.results().size());
    }

    @Transactional("adminTransactionManager")
    public LegacyKeywordIndexResult runMatchingCycle(Long researchNo, String includeKeywords, String excludeKeywords) {
        return runMatchingCycle(researchNo, LegacyMatchingSearchCondition.fromStorageKey(trimToEmpty(includeKeywords)));
    }

    public synchronized LegacyMatchingRunTicket startOrReuseMatchingRun(
            Long researchNo,
            LegacyMatchingSearchCondition condition
    ) {
        LegacyMatchingSearchCondition normalizedCondition = normalizeCondition(condition);
        LegacyMatchingIndexJob runningJob = legacyMatchingIndexJobMapper.findRunningForCriteria(
                researchNo,
                normalizedCondition.storageKey(),
                ""
        );
        if (runningJob != null) {
            return new LegacyMatchingRunTicket(
                    runningJob.getId(),
                    runningJob.getCycleNo() == null ? 0 : runningJob.getCycleNo(),
                    runningJob.getStatus(),
                    true
            );
        }

        LegacyMatchingIndexJob indexJob = new LegacyMatchingIndexJob();
        indexJob.setResearchNo(researchNo);
        indexJob.setCycleNo(legacyMatchingIndexJobMapper.findNextCycleNo(researchNo));
        indexJob.setIncludeKeywordText(normalizedCondition.storageKey());
        indexJob.setExcludeKeywordText("");
        indexJob.setAppliedYears(2);
        indexJob.setIndexLimit(INDEX_BATCH_SIZE);
        indexJob.setBatchSize(INDEX_BATCH_SIZE);
        indexJob.setRequireContactYn("Y");
        indexJob.setExcludeBlacklistYn("Y");
        indexJob.setResetBeforeRunYn("Y");
        indexJob.setStatus("PENDING");
        legacyMatchingIndexJobMapper.insertJob(indexJob);
        return new LegacyMatchingRunTicket(indexJob.getId(), indexJob.getCycleNo(), indexJob.getStatus(), false);
    }

    @Transactional(value = "adminTransactionManager", readOnly = true)
    public LegacyMatchingRunStatus getMatchingRunStatus(Long researchNo, Long jobId) {
        LegacyMatchingIndexJob job = legacyMatchingIndexJobMapper.findById(jobId);
        if (job == null || !researchNo.equals(job.getResearchNo())) {
            throw new IllegalArgumentException("매칭 작업을 찾을 수 없습니다.");
        }
        return new LegacyMatchingRunStatus(
                job.getId(),
                job.getResearchNo(),
                job.getCycleNo() == null ? 0 : job.getCycleNo(),
                job.getStatus(),
                job.getFailReason(),
                trimToEmpty(job.getIncludeKeywordText())
        );
    }

    @Transactional(value = "adminTransactionManager", readOnly = true)
    public LegacyKeywordIndexResult getCompletedMatchingRunResult(Long researchNo, Long jobId) {
        LegacyMatchingIndexJob job = legacyMatchingIndexJobMapper.findById(jobId);
        if (job == null || !researchNo.equals(job.getResearchNo()) || !"COMPLETED".equals(job.getStatus())) {
            throw new IllegalArgumentException("완료된 매칭 작업을 찾을 수 없습니다.");
        }
        return new LegacyKeywordIndexResult(
                job.getCycleNo() == null ? 0 : job.getCycleNo(),
                job.getIndexedApplicationCount() == null ? 0 : job.getIndexedApplicationCount(),
                job.getInsertedKeywordCount() == null ? 0 : job.getInsertedKeywordCount(),
                false,
                job.getSkippedAlreadyIndexedCount() == null ? 0 : job.getSkippedAlreadyIndexedCount(),
                false
        );
    }

    public void executeMatchingRun(Long indexJobId) {
        LegacyMatchingIndexJob indexJob = legacyMatchingIndexJobMapper.findById(indexJobId);
        if (indexJob == null || !("PENDING".equals(indexJob.getStatus()) || "RUNNING".equals(indexJob.getStatus()))) {
            return;
        }

        if (legacyMatchingIndexJobMapper.markStarted(indexJobId) != 1) {
            return;
        }
        try {
            legacyApplicationKeywordMapper.deleteAll();
            LegacyKeywordIndexResult indexResult = indexMatchingCycle(indexJob);
            LegacyMatchingSearchCondition condition = LegacyMatchingSearchCondition.fromStorageKey(
                    indexJob.getIncludeKeywordText()
            );
            Long matchingJobId = requestMatchingRefresh(indexJob.getResearchNo(), condition);
            executeMatchingJob(matchingJobId);

            indexJob.setStatus("COMPLETED");
            indexJob.setIndexedApplicationCount(indexResult.indexedApplicationCount());
            indexJob.setInsertedKeywordCount(indexResult.insertedKeywordCount());
            indexJob.setSkippedAlreadyIndexedCount(indexResult.skippedAlreadyIndexedCount());
            legacyMatchingIndexJobMapper.markCompleted(indexJob);
        } catch (RuntimeException ex) {
            legacyMatchingIndexJobMapper.markFailed(indexJobId, trimFailureReason(ex.getMessage()));
        } finally {
            legacyApplicationKeywordMapper.deleteAll();
        }
    }

    public void failMatchingRun(Long indexJobId, String failReason) {
        legacyMatchingIndexJobMapper.markPendingFailed(indexJobId, trimFailureReason(failReason));
    }

    @Transactional("adminTransactionManager")
    public LegacyKeywordIndexResult runMatchingCycle(Long researchNo, LegacyMatchingSearchCondition condition) {
        LegacyMatchingSearchCondition normalizedCondition = normalizeCondition(condition);
        if (!indexRunning.compareAndSet(false, true)) {
            LegacyMatchingIndexJob runningJob = legacyMatchingIndexJobMapper.findRunningForCriteria(
                    researchNo,
                    normalizedCondition.storageKey(),
                    ""
            );
            int runningCycleNo = runningJob == null || runningJob.getCycleNo() == null ? Math.max(0, safeNextCycleNo(researchNo) - 1) : runningJob.getCycleNo();
            return LegacyKeywordIndexResult.alreadyRunning(runningCycleNo);
        }
        LegacyMatchingIndexJob indexJob = new LegacyMatchingIndexJob();
        indexJob.setResearchNo(researchNo);
        indexJob.setCycleNo(legacyMatchingIndexJobMapper.findNextCycleNo(researchNo));
        indexJob.setIncludeKeywordText(normalizedCondition.storageKey());
        indexJob.setExcludeKeywordText("");
        indexJob.setAppliedYears(2);
        indexJob.setIndexLimit(INDEX_BATCH_SIZE);
        indexJob.setBatchSize(INDEX_BATCH_SIZE);
        indexJob.setRequireContactYn("Y");
        indexJob.setExcludeBlacklistYn("Y");
        indexJob.setResetBeforeRunYn("Y");
        indexJob.setStatus("PENDING");
        legacyMatchingIndexJobMapper.insertJob(indexJob);
        try {
            legacyMatchingIndexJobMapper.markStarted(indexJob.getId());
            legacyApplicationKeywordMapper.deleteAll();
            LegacyKeywordIndexResult indexResult = indexMatchingCycle(indexJob);
            indexJob.setStatus("COMPLETED");
            indexJob.setIndexedApplicationCount(indexResult.indexedApplicationCount());
            indexJob.setInsertedKeywordCount(indexResult.insertedKeywordCount());
            indexJob.setSkippedAlreadyIndexedCount(indexResult.skippedAlreadyIndexedCount());
            legacyMatchingIndexJobMapper.markCompleted(indexJob);

            Long matchingJobId = requestMatchingRefresh(researchNo, normalizedCondition);
            executeMatchingJob(matchingJobId);
            legacyApplicationKeywordMapper.deleteAll();
            return indexResult.withCycleNo(indexJob.getCycleNo());
        } catch (RuntimeException ex) {
            legacyMatchingIndexJobMapper.markFailed(indexJob.getId(), trimFailureReason(ex.getMessage()));
            legacyApplicationKeywordMapper.deleteAll();
            return new LegacyKeywordIndexResult(0, 0).withCycleNo(indexJob.getCycleNo());
        } finally {
            indexRunning.set(false);
        }
    }

    @Transactional("adminTransactionManager")
    public Long requestMatchingRefresh(Long researchNo, String includeKeywords, String excludeKeywords) {
        return requestMatchingRefresh(researchNo, LegacyMatchingSearchCondition.fromStorageKey(trimToEmpty(includeKeywords)));
    }

    @Transactional("adminTransactionManager")
    public Long requestMatchingRefresh(Long researchNo, LegacyMatchingSearchCondition condition) {
        LegacyMatchingSearchCondition normalizedCondition = normalizeCondition(condition);
        LegacyMatchingJob job = new LegacyMatchingJob();
        job.setResearchNo(researchNo);
        job.setIncludeKeywordText(normalizedCondition.storageKey());
        job.setExcludeKeywordText("");
        job.setStatus("PENDING");
        legacyMatchingJobMapper.insertJob(job);
        return job.getId();
    }

    private LegacyKeywordIndexResult indexMatchingCycle(LegacyMatchingIndexJob job) {
        LegacyMatchingSearchCondition condition = conditionWithAdditionalAnswerMatches(
                LegacyMatchingSearchCondition.fromStorageKey(job.getIncludeKeywordText())
        );
        if (!condition.hasInput()) {
            return new LegacyKeywordIndexResult(0, 0, false);
        }
        int cycleNo = job.getCycleNo() == null || job.getCycleNo() < 1 ? 1 : job.getCycleNo();
        int offset = (cycleNo - 1) * INDEX_BATCH_SIZE;
        List<ResearchApplication> applications = researchApplicationService.getMatchingIndexCandidatePage(
                condition,
                INDEX_BATCH_SIZE,
                offset
        );
        return new LegacyKeywordIndexResult(applications.size(), 0);
    }

    @Transactional("adminTransactionManager")
    public void executeMatchingJob(Long matchingJobId) {
        LegacyMatchingJob job = legacyMatchingJobMapper.findById(matchingJobId);
        if (job == null) {
            return;
        }
        legacyMatchingJobMapper.markStarted(matchingJobId);
        try {
            MatchingComputation computation = computeMatching(
                    job.getResearchNo(),
                    LegacyMatchingSearchCondition.fromStorageKey(job.getIncludeKeywordText())
            );
            job.setActiveKeywordText(String.join(", ", computation.activeKeywords()));
            job.setCandidatePoolCount(computation.candidatePoolCount());
            job.setMatchedCount(computation.results().size());
            job.setBlacklistedExcludedCount(computation.blacklistedExcludedCount());
            legacyMatchingJobMapper.markCompleted(job);
            List<LegacyStoredMatchingResult> storedResults = toStoredResults(matchingJobId, computation.results());
            if (!storedResults.isEmpty()) {
                legacyMatchingJobMapper.insertResults(storedResults);
            }
        } catch (RuntimeException ex) {
            legacyMatchingJobMapper.markFailed(matchingJobId, trimFailureReason(ex.getMessage()));
            throw ex;
        }
    }

    private MatchingComputation computeMatching(Long researchNo, LegacyMatchingSearchCondition condition) {
        LegacyMatchingSearchCondition normalizedCondition = conditionWithAdditionalAnswerMatches(normalizeCondition(condition));
        List<ResearchApplication> candidates = normalizedCondition.hasInput()
                ? researchApplicationService.getMatchingIndexCandidatePage(
                normalizedCondition,
                MATCH_CANDIDATE_LIMIT,
                0
        )
                : List.of();
        candidates = legacyApplicationConsentService.filterActiveFutureRecruitment(candidates);
        List<LegacyMatchingResult> results = new ArrayList<>();
        int blacklistedExcludedCount = 0;
        for (ResearchApplication application : candidates) {
            if (application.isBlacklisted()) {
                blacklistedExcludedCount++;
                continue;
            }
            List<String> matched = matchedFilters(application, normalizedCondition);
            if (matched.size() < normalizedCondition.requiredFilterCount()) {
                continue;
            }
            if (hasSuccessfulMatchingNotification(researchNo, application.getResearchAppSeq(), String.join(", ", matched))) {
                continue;
            }
            results.add(new LegacyMatchingResult(results.size() + 1, application, matched.size(), matched, List.of()));
        }
        List<LegacyMatchingResult> sortedResults = results.stream()
                .sorted(Comparator.comparing(LegacyMatchingResult::matchScore).reversed()
                        .thenComparing(result -> result.application().getResearchAppSeq(), Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        List<LegacyMatchingResult> renumberedResults = renumberResults(removeDuplicateNamePhoneMatches(sortedResults));
        return new MatchingComputation(
                normalizedCondition.displayFilters(),
                renumberedResults,
                candidates.size(),
                blacklistedExcludedCount
        );
    }

    private List<LegacyMatchingResult> filterActiveConsentResults(List<LegacyMatchingResult> results) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        Set<Long> activeApplicationSeqs = legacyApplicationConsentService
                .filterActiveFutureRecruitment(results.stream().map(LegacyMatchingResult::application).toList())
                .stream()
                .map(ResearchApplication::getResearchAppSeq)
                .collect(java.util.stream.Collectors.toSet());
        return results.stream()
                .filter(result -> activeApplicationSeqs.contains(result.application().getResearchAppSeq()))
                .toList();
    }

    private List<LegacyMatchingResult> applyConsentStatuses(List<LegacyMatchingResult> results) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }
        return results.stream()
                .map(result -> {
                    ResearchApplication application = result.application();
                    return result.withConsentStatus(
                            legacyApplicationConsentService.allowsSms(application.getResearchNo(), application.getResearchAppSeq()),
                            legacyApplicationConsentService.allowsEmail(application.getResearchNo(), application.getResearchAppSeq())
                    );
                })
                .filter(result -> result.smsAllowed() || result.emailAllowed())
                .toList();
    }

    private LegacyMatchingSearchCondition conditionWithAdditionalAnswerMatches(LegacyMatchingSearchCondition condition) {
        LegacyMatchingSearchCondition normalizedCondition = normalizeCondition(condition);
        if (!normalizedCondition.hasAddCommentTerms()) {
            return normalizedCondition;
        }
        List<Long> researchAppSeqs = researchApplicationService.getApplicationSeqsByAdditionalAnswerTerms(
                normalizedCondition.getAddCommentTerms(),
                MATCH_CANDIDATE_LIMIT
        );
        return normalizedCondition.withAdditionalAnswerResearchAppSeqs(researchAppSeqs);
    }

    private List<String> matchedFilters(ResearchApplication application, LegacyMatchingSearchCondition condition) {
        List<String> matched = new ArrayList<>(condition.matchedFilters(application));
        if (!condition.hasAddCommentTerms() || matched.contains(condition.addCommentDisplayFilter())) {
            return matched;
        }
        String formattedAdditionalAnswers = researchApplicationService.getFormattedAdditionalAnswers(
                application.getResearchNo(),
                application.getResearchAppSeq()
        );
        if (condition.addCommentMatches(formattedAdditionalAnswers)) {
            matched.add(condition.addCommentDisplayFilter());
        }
        return matched;
    }

    @Transactional("adminTransactionManager")
    public LegacyKeywordIndexResult indexApplicationIfIdle(ResearchApplication application) {
        if (application == null || application.getResearchNo() == null || application.getResearchAppSeq() == null) {
            return new LegacyKeywordIndexResult(0, 0);
        }
        if (!indexRunning.compareAndSet(false, true)) {
            return new LegacyKeywordIndexResult(0, 0);
        }
        try {
            return indexApplications(List.of(application));
        } finally {
            indexRunning.set(false);
        }
    }

    private LegacyKeywordIndexResult indexApplications(List<ResearchApplication> applications) {
        int insertedKeywordCount = 0;
        for (ResearchApplication application : applications) {
            List<LegacyApplicationKeyword> keywords = extractApplicationKeywords(application);
            legacyApplicationKeywordMapper.deleteByApplication(application.getResearchNo(), application.getResearchAppSeq());
            if (keywords.isEmpty()) {
                keywords = List.of(indexMarkerKeyword(application));
            }
            insertedKeywordCount += legacyApplicationKeywordMapper.insertBatch(keywords);
        }
        return new LegacyKeywordIndexResult(applications.size(), insertedKeywordCount);
    }

    @Transactional("adminTransactionManager")
    public LegacySmsSendResult sendSmsNotifications(
            Long researchNo,
            String includeKeywords,
            String excludeKeywords,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        return sendSmsNotifications(
                researchNo,
                LegacyMatchingSearchCondition.fromStorageKey(trimToEmpty(includeKeywords)),
                principal,
                request
        );
    }

    @Transactional("adminTransactionManager")
    public LegacySmsSendResult sendSmsNotifications(
            Long researchNo,
            LegacyMatchingSearchCondition condition,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        return sendSmsNotifications(researchNo, condition, null, principal, request);
    }

    @Transactional("adminTransactionManager")
    public LegacySmsSendResult sendSmsNotifications(
            Long researchNo,
            LegacyMatchingSearchCondition condition,
            Set<Long> selectedApplicationIds,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        LegacyMatchingOverview overview = filterOverview(getOverview(researchNo, condition), selectedApplicationIds);
        enforceSmsSendLimit(overview);
        int sentCount = 0;
        int skippedDuplicateCount = 0;
        int failedCount = 0;
        Set<String> recipientIdentityKeys = new LinkedHashSet<>();
        for (LegacyMatchingResult result : overview.results()) {
            ResearchApplication application = result.application();
            String keywordSummary = trimSummary(result.matchedKeywordText());
            String phone = firstPhone(application);
            String maskedPhone = maskPhone(phone);
            if (!legacyApplicationConsentService.allowsSms(application.getResearchNo(), application.getResearchAppSeq())) {
                insertLog(researchNo, application.getResearchAppSeq(), maskedPhone, keywordSummary, "SKIPPED_CONSENT", "SMS 수신 동의가 없거나 만료 또는 철회되었습니다.");
                failedCount++;
                continue;
            }
            String recipientIdentityKey = recipientIdentityKey(application);
            if (recipientIdentityKey != null && !recipientIdentityKeys.add(recipientIdentityKey)) {
                insertLog(researchNo, application.getResearchAppSeq(), maskedPhone, keywordSummary, "SKIPPED_DUPLICATE", "동일 이름과 휴대폰 번호가 같아 발송 대상에서 제외했습니다.");
                skippedDuplicateCount++;
                continue;
            }
            if (adminNotificationLogMapper.countSuccessfulDuplicate(researchNo, application.getResearchAppSeq(), "LEGACY_SMS", keywordSummary) > 0) {
                insertLog(researchNo, application.getResearchAppSeq(), maskedPhone, keywordSummary, "SKIPPED_DUPLICATE", null);
                skippedDuplicateCount++;
                continue;
            }
            if (phone == null || phone.isBlank()) {
                insertLog(researchNo, application.getResearchAppSeq(), maskedPhone, keywordSummary, "FAILED", "휴대폰 번호가 확인되지 않습니다.");
                failedCount++;
                continue;
            }
            try {
                applicantNotificationSmsGateway.dispatch(new NotificationSmsRequest(
                        phone,
                        buildConfigurableLegacySmsMessage(overview.research(), keywordSummary)
                ));
                insertLog(researchNo, application.getResearchAppSeq(), maskedPhone, keywordSummary, "SENT", null);
                sentCount++;
            } catch (Exception ex) {
                String reason = trimFailureReason(ex.getMessage());
                insertLog(researchNo, application.getResearchAppSeq(), maskedPhone, keywordSummary, "FAILED", reason);
                failedCount++;
            }
        }
        adminActionLogService.log(
                principal == null ? null : principal.getId(),
                "LEGACY_KEYWORD_NOTIFICATION_SMS",
                "RESEARCH",
                String.valueOf(researchNo),
                "구 DB 키워드 문자 알림 완료: 대상 " + overview.matchedCount()
                        + "건, 발송 " + sentCount
                        + "건, 중복 제외 " + skippedDuplicateCount
                        + "건, 실패 " + failedCount + "건",
                request
        );
        return new LegacySmsSendResult(overview.matchedCount(), sentCount, skippedDuplicateCount, failedCount);
    }

    private void enforceSmsSendLimit(LegacyMatchingOverview overview) {
        int requestedCount = countDispatchableSmsTargets(overview);
        if (requestedCount == 0) {
            return;
        }

        SmsUsageWindow usageWindow = currentSmsUsageWindow();
        int dailyLimit = smsProperties.getDailySendLimit();
        int monthlyLimit = smsProperties.getMonthlySendLimit();

        boolean dailyExceeded = dailyLimit > 0 && usageWindow.dailySentCount() + requestedCount > dailyLimit;
        boolean monthlyExceeded = monthlyLimit > 0 && usageWindow.monthlySentCount() + requestedCount > monthlyLimit;
        if (!dailyExceeded && !monthlyExceeded) {
            return;
        }

        String message;
        if (dailyExceeded && monthlyExceeded) {
            message = "일일/월간 SMS 발송 한도를 초과하여 발송을 중단했습니다.";
        } else if (dailyExceeded) {
            message = "일일 SMS 발송 한도를 초과하여 발송을 중단했습니다.";
        } else {
            message = "월간 SMS 발송 한도를 초과하여 발송을 중단했습니다.";
        }
        throw new LegacySmsSendLimitExceededException(
                message,
                requestedCount,
                usageWindow.dailySentCount(),
                dailyLimit,
                usageWindow.monthlySentCount(),
                monthlyLimit
        );
    }

    private int countDispatchableSmsTargets(LegacyMatchingOverview overview) {
        int requestedCount = 0;
        Set<String> recipientIdentityKeys = new LinkedHashSet<>();
        for (LegacyMatchingResult result : overview.results()) {
            ResearchApplication application = result.application();
            if (!legacyApplicationConsentService.allowsSms(application.getResearchNo(), application.getResearchAppSeq())) {
                continue;
            }
            String recipientIdentityKey = recipientIdentityKey(application);
            if (recipientIdentityKey != null && !recipientIdentityKeys.add(recipientIdentityKey)) {
                continue;
            }
            String keywordSummary = trimSummary(result.matchedKeywordText());
            if (adminNotificationLogMapper.countSuccessfulDuplicate(
                    overview.research().getResearchNo(),
                    application.getResearchAppSeq(),
                    "LEGACY_SMS",
                    keywordSummary
            ) > 0) {
                continue;
            }
            String phone = firstPhone(application);
            if (phone == null || phone.isBlank()) {
                continue;
            }
            requestedCount++;
        }
        return requestedCount;
    }

    private boolean hasSuccessfulMatchingNotification(Long researchNo, Long researchAppSeq, String keywordSummary) {
        String normalizedKeywordSummary = trimSummary(keywordSummary);
        return adminNotificationLogMapper.countSuccessfulDuplicate(researchNo, researchAppSeq, "LEGACY_SMS", normalizedKeywordSummary) > 0
                || adminNotificationLogMapper.countSuccessfulDuplicate(researchNo, researchAppSeq, "LEGACY_EMAIL", normalizedKeywordSummary) > 0;
    }

    private SmsUsageWindow currentSmsUsageWindow() {
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDateTime monthStart = firstDayOfMonth.atStartOfDay();
        LocalDateTime monthEnd = firstDayOfMonth.plusMonths(1).atStartOfDay();
        int dailySentCount = adminNotificationLogMapper.countSentByChannelBetween("LEGACY_SMS", dayStart, dayEnd);
        int monthlySentCount = adminNotificationLogMapper.countSentByChannelBetween("LEGACY_SMS", monthStart, monthEnd);
        return new SmsUsageWindow(dailySentCount, monthlySentCount);
    }

    private record SmsUsageWindow(
            int dailySentCount,
            int monthlySentCount
    ) {
    }

    @Transactional("adminTransactionManager")
    public LegacyEmailSendResult sendEmailNotifications(
            Long researchNo,
            String includeKeywords,
            String excludeKeywords,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        return sendEmailNotifications(
                researchNo,
                LegacyMatchingSearchCondition.fromStorageKey(trimToEmpty(includeKeywords)),
                principal,
                request
        );
    }

    @Transactional("adminTransactionManager")
    public LegacyEmailSendResult sendEmailNotifications(
            Long researchNo,
            LegacyMatchingSearchCondition condition,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        return sendEmailNotifications(researchNo, condition, null, principal, request);
    }

    @Transactional("adminTransactionManager")
    public LegacyEmailSendResult sendEmailNotifications(
            Long researchNo,
            LegacyMatchingSearchCondition condition,
            Set<Long> selectedApplicationIds,
            AdminPrincipal principal,
            HttpServletRequest request
    ) {
        LegacyMatchingOverview overview = filterOverview(getOverview(researchNo, condition), selectedApplicationIds);
        int sentCount = 0;
        int skippedDuplicateCount = 0;
        int failedCount = 0;
        for (LegacyMatchingResult result : overview.results()) {
            ResearchApplication application = result.application();
            String keywordSummary = trimSummary(result.matchedKeywordText());
            String email = normalizeEmail(application.getAppEmail());
            String maskedEmail = maskEmail(email);
            LegacyEmailSendCounts emailResult = sendLegacyMatchingEmail(
                    researchNo,
                    application,
                    overview.research(),
                    email,
                    maskedEmail,
                    keywordSummary
            );
            sentCount += emailResult.sentCount();
            skippedDuplicateCount += emailResult.skippedDuplicateCount();
            failedCount += emailResult.failedCount();
        }
        adminActionLogService.log(
                principal == null ? null : principal.getId(),
                "LEGACY_KEYWORD_NOTIFICATION_EMAIL",
                "RESEARCH",
                String.valueOf(researchNo),
                "구 DB 키워드 이메일 알림 완료: 대상 " + overview.matchedCount()
                        + "건, 발송 " + sentCount
                        + "건, 중복 제외 " + skippedDuplicateCount
                        + "건, 실패 " + failedCount + "건",
                request
        );
        return new LegacyEmailSendResult(overview.matchedCount(), sentCount, skippedDuplicateCount, failedCount);
    }
    public LegacyMatchingHistory getHistory(Long researchNo) {
        return new LegacyMatchingHistory(
                legacyMatchingJobMapper.findRecentByResearchNo(researchNo, 30),
                legacyMatchingIndexJobMapper.findRecentByResearchNo(researchNo, 30),
                adminNotificationLogMapper.findMatchingSummariesByResearchNo(researchNo)
        );
    }

    private LegacyEmailSendCounts sendLegacyMatchingEmail(
            Long researchNo,
            ResearchApplication application,
            ResearchMaster research,
            String email,
            String maskedEmail,
            String keywordSummary
    ) {
        if (!legacyApplicationConsentService.allowsEmail(application.getResearchNo(), application.getResearchAppSeq())) {
            insertLog(researchNo, application.getResearchAppSeq(), maskedEmail, keywordSummary, "LEGACY_EMAIL", "SKIPPED_CONSENT", "이메일 수신 동의가 없거나 만료 또는 철회되었습니다.");
            return new LegacyEmailSendCounts(0, 0, 1);
        }
        if (email == null) {
            insertLog(researchNo, application.getResearchAppSeq(), maskedEmail, keywordSummary, "LEGACY_EMAIL", "FAILED", "수신 이메일을 찾을 수 없습니다.");
            return new LegacyEmailSendCounts(0, 0, 1);
        }
        if (adminNotificationLogMapper.countSuccessfulDuplicate(researchNo, application.getResearchAppSeq(), "LEGACY_EMAIL", keywordSummary) > 0) {
            insertLog(researchNo, application.getResearchAppSeq(), maskedEmail, keywordSummary, "LEGACY_EMAIL", "SKIPPED_DUPLICATE", null);
            return new LegacyEmailSendCounts(0, 1, 0);
        }
        try {
            mailDispatchGateway.dispatch(new MailDispatchRequest(
                    List.of(email),
                    null,
                    buildLegacyMatchingEmailSubject(research, keywordSummary),
                    buildLegacyMatchingEmailBody(research, keywordSummary),
                    null,
                    null,
                    null
            ));
            insertLog(researchNo, application.getResearchAppSeq(), maskedEmail, keywordSummary, "LEGACY_EMAIL", "SENT", null);
            return new LegacyEmailSendCounts(1, 0, 0);
        } catch (Exception ex) {
            String reason = trimFailureReason(ex.getMessage());
            insertLog(researchNo, application.getResearchAppSeq(), maskedEmail, keywordSummary, "LEGACY_EMAIL", "FAILED", reason);
            return new LegacyEmailSendCounts(0, 0, 1);
        }
    }

    private record LegacyEmailSendCounts(
            int sentCount,
            int skippedDuplicateCount,
            int failedCount
    ) {
    }

    @Transactional("adminTransactionManager")
    public int cleanupMatchingLogsAfterClosedDeadline() {
        List<Long> closedResearchNos = researchMasterService.getClosedResearchNosBefore(LocalDate.now());
        if (closedResearchNos.isEmpty()) {
            return 0;
        }
        int deleted = 0;
        deleted += legacyMatchingJobMapper.deleteResultsByResearchNos(closedResearchNos);
        return deleted;
    }

    private int safeNextCycleNo(Long researchNo) {
        Integer nextCycleNo = legacyMatchingIndexJobMapper.findNextCycleNo(researchNo);
        if (nextCycleNo == null || nextCycleNo < 1) {
            return 1;
        }
        return nextCycleNo;
    }

    private LegacyMatchingSearchCondition normalizeCondition(LegacyMatchingSearchCondition condition) {
        return condition == null ? LegacyMatchingSearchCondition.empty() : condition;
    }

    private void insertLog(
            Long researchNo,
            Long researchAppSeq,
            String targetPhone,
            String keywordSummary,
            String sendStatus,
            String failReason
    ) {
        insertLog(researchNo, researchAppSeq, targetPhone, keywordSummary, "LEGACY_SMS", sendStatus, failReason);
    }

    private void insertLog(
            Long researchNo,
            Long researchAppSeq,
            String targetAddress,
            String keywordSummary,
            String channelType,
            String sendStatus,
            String failReason
    ) {
        AdminNotificationLog log = new AdminNotificationLog();
        log.setResearchNo(researchNo);
        log.setApplicationId(researchAppSeq);
        log.setChannelType(channelType);
        log.setTargetAddressMasked(targetAddress);
        log.setKeywordSummary(keywordSummary);
        log.setSendStatus(sendStatus);
        log.setFailReason(failReason);
        adminNotificationLogMapper.insert(log);
    }

    private String buildConfigurableLegacySmsMessage(ResearchMaster research, String keywordSummary) {
        String title = trimMessagePart(research.getResearchTitle(), 120);
        String details = trimMessagePart(firstNonBlank(research.getResearchContents(), research.getAddComment()), 500);
        String applyUrl = trimTrailingSlash(notificationProperties.getBaseUrl()) + "/research/" + research.getResearchNo() + "/apply";
        String trimmedKeywordSummary = trimMessagePart(keywordSummary, 120);
        String keywordLine = trimmedKeywordSummary.isBlank()
                ? ""
                : "\uad00\ub828 \ud0a4\uc6cc\ub4dc: " + trimmedKeywordSummary;
        String template = firstNonBlank(notificationProperties.getLegacyMatchingSmsMessage(), defaultLegacySmsMessage());
        return template
                .replace("{{researchNo}}", String.valueOf(research.getResearchNo()))
                .replace("{{researchTitle}}", title)
                .replace("{{researchContent}}", details)
                .replace("{{applyUrl}}", applyUrl)
                .replace("{{keywordSummary}}", trimmedKeywordSummary)
                .replace("{{keywordLine}}", keywordLine)
                .trim();
    }

    private String buildLegacyMatchingEmailSubject(ResearchMaster research, String keywordSummary) {
        String template = firstNonBlank(notificationProperties.getLegacyMatchingEmailSubject(), "[Researchi] {{researchTitle}}");
        return renderLegacyMatchingNotificationTemplate(template, research, keywordSummary);
    }

    private String buildLegacyMatchingEmailBody(ResearchMaster research, String keywordSummary) {
        String template = firstNonBlank(notificationProperties.getLegacyMatchingEmailBody(), defaultLegacyEmailBody());
        return renderLegacyMatchingNotificationTemplate(template, research, keywordSummary);
    }

    private String renderLegacyMatchingNotificationTemplate(String template, ResearchMaster research, String keywordSummary) {
        String title = trimMessagePart(research.getResearchTitle(), 120);
        String details = trimMessagePart(firstNonBlank(research.getResearchContents(), research.getAddComment()), 1000);
        String applyUrl = trimTrailingSlash(notificationProperties.getBaseUrl()) + "/research/" + research.getResearchNo() + "/apply";
        String trimmedKeywordSummary = trimMessagePart(keywordSummary, 120);
        String keywordLine = trimmedKeywordSummary.isBlank()
                ? ""
                : "\uad00\ub828 \ud0a4\uc6cc\ub4dc: " + trimmedKeywordSummary;
        return template
                .replace("{{researchNo}}", String.valueOf(research.getResearchNo()))
                .replace("{{researchTitle}}", title)
                .replace("{{researchContent}}", details)
                .replace("{{applyUrl}}", applyUrl)
                .replace("{{keywordSummary}}", trimmedKeywordSummary)
                .replace("{{keywordLine}}", keywordLine)
                .trim();
    }

    private String defaultLegacySmsMessage() {
        return """
                [Researchi] \uc88c\ub2f4\ud68c/\uc124\ubb38 \ucc38\uc5ec \uc548\ub0b4

                {{researchTitle}}

                \uc870\uac74\uc5d0 \ub9de\ub294 \uc88c\ub2f4\ud68c/\uc124\ubb38\uc774 \uc788\uc5b4 \uc548\ub0b4\ub4dc\ub9bd\ub2c8\ub2e4.
                \uc2e0\uccad \ub9c1\ud06c: {{applyUrl}}
                {{keywordLine}}
                """;
    }

    private String defaultLegacyEmailBody() {
        return """
                \uc548\ub155\ud558\uc138\uc694.

                \uc870\uac74\uc5d0 \ub9de\ub294 \uc88c\ub2f4\ud68c/\uc124\ubb38\uc774 \uc788\uc5b4 \uc548\ub0b4\ub4dc\ub9bd\ub2c8\ub2e4.

                \uc88c\ub2f4\ud68c/\uc124\ubb38: {{researchTitle}}
                \uc2e0\uccad \ub9c1\ud06c: {{applyUrl}}
                {{keywordLine}}

                \ucc38\uc5ec\ub97c \uc6d0\ud558\uc2dc\uba74 \uc704 \ub9c1\ud06c\uc5d0\uc11c \uc790\uc138\ud55c \ub0b4\uc6a9\uc744 \ud655\uc778\ud574 \uc8fc\uc138\uc694.
                """;
    }

    private String buildLegacySmsMessage(ResearchMaster research, String keywordSummary) {
        String title = trimMessagePart(research.getResearchTitle(), 120);
        String details = trimMessagePart(firstNonBlank(research.getResearchContents(), research.getAddComment()), 500);
        String applyUrl = trimTrailingSlash(notificationProperties.getBaseUrl()) + "/research/" + research.getResearchNo() + "/apply";
        return "[Researchi] 좌담회/설문 안내\n"
                + "제목: " + title + "\n"
                + (details.isBlank() ? "" : "내용: " + details + "\n")
                + "신청: " + applyUrl
                + (keywordSummary.isBlank() ? "" : "\n관련 키워드: " + trimMessagePart(keywordSummary, 120));
    }

    private List<String> parseManualKeywords(String value) {
        String normalized = trimToEmpty(value);
        if (normalized.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        for (String keyword : normalized.split("[,\\r\\n/]+")) {
            String trimmed = keyword.trim();
            if (!trimmed.isBlank()) {
                keywords.add(trimmed);
            }
        }
        return List.copyOf(keywords);
    }

    private List<String> normalizedKeywords(List<String> keywords) {
        return keywords.stream()
                .map(this::normalize)
                .filter(keyword -> !keyword.isBlank())
                .distinct()
                .toList();
    }

    private List<String> parseStoredKeywords(String value) {
        String trimmed = trimToEmpty(value);
        if (trimmed.isBlank()) {
            return List.of();
        }
        return List.of(trimmed.split("\\s*,\\s*")).stream()
                .filter(keyword -> !keyword.isBlank())
                .toList();
    }

    private List<LegacyMatchingResult> removeDuplicateNamePhoneMatches(List<LegacyMatchingResult> sortedResults) {
        List<LegacyMatchingResult> uniqueResults = new ArrayList<>();
        Set<String> identityKeys = new LinkedHashSet<>();
        for (LegacyMatchingResult result : sortedResults) {
            String identityKey = recipientIdentityKey(result.application());
            if (identityKey != null && !identityKeys.add(identityKey)) {
                continue;
            }
            uniqueResults.add(result);
        }
        return uniqueResults;
    }

    private List<LegacyMatchingResult> storedResults(LegacyMatchingJob matchingJob) {
        if (matchingJob == null || !"COMPLETED".equalsIgnoreCase(matchingJob.getStatus())) {
            return List.of();
        }
        List<LegacyMatchingResult> results = new ArrayList<>();
        for (LegacyStoredMatchingResult stored : legacyMatchingJobMapper.findResultsByJobId(matchingJob.getId(), DISPLAY_RESULT_LIMIT)) {
            ResearchApplication application = researchApplicationService.getApplication(stored.getResearchNo(), stored.getResearchAppSeq());
            results.add(new LegacyMatchingResult(
                    stored.getRowNo() == null ? results.size() + 1 : stored.getRowNo(),
                    application,
                    stored.getMatchScore() == null ? 0 : stored.getMatchScore(),
                    parseStoredKeywords(stored.getMatchedKeywordText()),
                    List.of()
            ));
        }
        return renumberResults(removeDuplicateNamePhoneMatches(results));
    }

    private List<LegacyMatchingResult> renumberResults(List<LegacyMatchingResult> results) {
        List<LegacyMatchingResult> renumberedResults = new ArrayList<>();
        for (int index = 0; index < results.size(); index++) {
            LegacyMatchingResult result = results.get(index);
            renumberedResults.add(new LegacyMatchingResult(
                    index + 1,
                    result.application(),
                    result.matchScore(),
                    result.matchedKeywords(),
                    result.excludedKeywords(),
                    result.smsAllowed(),
                    result.emailAllowed(),
                    result.smsSent(),
                    result.emailSent()
            ));
        }
        return renumberedResults;
    }

    private List<LegacyMatchingResult> applyNotificationStatuses(Long researchNo, List<LegacyMatchingResult> results) {
        if (results.isEmpty()) {
            return results;
        }
        List<LegacyMatchingResult> updated = new ArrayList<>();
        for (LegacyMatchingResult result : results) {
            Long applicationId = result.application().getResearchAppSeq();
            String keywordSummary = trimSummary(result.matchedKeywordText());
            boolean smsSent = adminNotificationLogMapper.countSuccessfulDuplicate(researchNo, applicationId, "LEGACY_SMS", keywordSummary) > 0;
            boolean emailSent = adminNotificationLogMapper.countSuccessfulDuplicate(researchNo, applicationId, "LEGACY_EMAIL", keywordSummary) > 0;
            updated.add(result.withNotificationStatus(smsSent, emailSent));
        }
        return updated;
    }

    private LegacyMatchingOverview filterOverview(LegacyMatchingOverview overview, Set<Long> selectedApplicationIds) {
        if (selectedApplicationIds == null) {
            return overview;
        }
        List<LegacyMatchingResult> selectedResults = overview.results().stream()
                .filter(result -> selectedApplicationIds.contains(result.application().getResearchAppSeq()))
                .toList();
        return new LegacyMatchingOverview(
                overview.research(),
                overview.includeKeywordText(),
                overview.excludeKeywordText(),
                overview.searchCondition(),
                overview.indexLimit(),
                overview.activeKeywords(),
                selectedResults,
                overview.candidatePoolCount(),
                overview.indexedApplicationCount(),
                overview.blacklistedExcludedCount(),
                overview.latestCycleNo(),
                overview.nextCycleNo(),
                overview.cycleBatchSize(),
                overview.matchingJobId(),
                overview.matchingStatus(),
                overview.failReason(),
                overview.finishedAt()
        );
    }

    private List<LegacyStoredMatchingResult> toStoredResults(Long matchingJobId, List<LegacyMatchingResult> results) {
        return results.stream()
                .limit(STORED_RESULT_LIMIT)
                .map(result -> {
                    LegacyStoredMatchingResult stored = new LegacyStoredMatchingResult();
                    stored.setMatchingJobId(matchingJobId);
                    stored.setResearchNo(result.application().getResearchNo());
                    stored.setResearchAppSeq(result.application().getResearchAppSeq());
                    stored.setRowNo(result.rowNo());
                    stored.setMatchScore(result.matchScore());
                    stored.setMatchedKeywordText(result.matchedKeywordText());
                    return stored;
                })
                .toList();
    }

    private List<LegacyApplicationKeyword> extractApplicationKeywords(ResearchApplication application) {
        Map<String, LegacyApplicationKeyword> keywords = new LinkedHashMap<>();
        addApplicationKeywords(keywords, application, application.getAppName(), "APP_NAME");
        addApplicationKeywords(keywords, application, application.getAppSexLabel(), "APP_SEX");
        addApplicationKeywords(keywords, application, application.getAppAge(), "APP_AGE");
        addApplicationKeywords(keywords, application, application.getAppJob(), "APP_JOB");
        addApplicationKeywords(keywords, application, application.getAppCompany(), "APP_COMPANY");
        addApplicationKeywords(keywords, application, application.getAppAddr(), "APP_ADDR");
        addApplicationKeywords(keywords, application, application.getAddComment(), "ADD_COMMENT");
        addApplicationKeywords(keywords, application, application.getAttendResearch(), "ATTEND_RESEARCH");
        return List.copyOf(keywords.values());
    }

    private void addApplicationKeywords(
            Map<String, LegacyApplicationKeyword> keywords,
            ResearchApplication application,
            String text,
            String sourceType
    ) {
        if (text == null || text.isBlank()) {
            return;
        }
        Matcher matcher = TOKEN_PATTERN.matcher(Normalizer.normalize(text, Normalizer.Form.NFKC));
        while (matcher.find()) {
            String keyword = matcher.group().trim();
            String normalized = normalize(keyword);
            if (normalized.length() < 2 || STOP_WORDS.contains(normalized) || normalized.chars().allMatch(Character::isDigit)) {
                continue;
            }
            String key = normalized + ":" + sourceType;
            keywords.computeIfAbsent(key, ignored -> {
                LegacyApplicationKeyword row = new LegacyApplicationKeyword();
                row.setResearchNo(application.getResearchNo());
                row.setResearchAppSeq(application.getResearchAppSeq());
                row.setApplicationRegistDt(application.getRegistDt());
                row.setKeywordNormalized(normalized);
                row.setKeyword(keyword);
                row.setSourceType(sourceType);
                return row;
            });
        }
    }

    private LegacyApplicationKeyword indexMarkerKeyword(ResearchApplication application) {
        LegacyApplicationKeyword row = new LegacyApplicationKeyword();
        row.setResearchNo(application.getResearchNo());
        row.setResearchAppSeq(application.getResearchAppSeq());
        row.setApplicationRegistDt(application.getRegistDt());
        row.setKeywordNormalized("__indexed__");
        row.setKeyword("__indexed__");
        row.setSourceType("INDEX_MARKER");
        return row;
    }

    private List<String> matchedKeywords(ResearchApplication application, List<String> keywords) {
        if (keywords.isEmpty()) {
            return List.of();
        }
        String haystack = normalize(String.join(" ",
                safe(application.getAppName()),
                safe(application.getAppSexLabel()),
                safe(application.getAppAge()),
                safe(application.getAppJob()),
                safe(application.getAppCompany()),
                safe(application.getAppAddr()),
                safe(application.getAddComment()),
                safe(application.getAttendResearch())
        ));
        List<String> matched = new ArrayList<>();
        for (String keyword : keywords) {
            if (haystack.contains(normalize(keyword))) {
                matched.add(keyword);
            }
        }
        return matched;
    }

    private String firstPhone(ResearchApplication application) {
        String phone = digitsOnly(application.getAppHphone());
        if (phone != null) {
            return phone;
        }
        return digitsOnly(application.getAppTele());
    }

    private String recipientIdentityKey(ResearchApplication application) {
        if (application == null) {
            return null;
        }
        String name = normalize(application.getAppName());
        String phone = firstPhone(application);
        if (name.isBlank() || phone == null || phone.isBlank()) {
            return null;
        }
        return name + ":" + phone;
    }

    private String digitsOnly(String value) {
        return PhoneNumberFormatter.digitsOnly(value);
    }

    private String normalizeEmail(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        return trimmed.isBlank() ? null : trimmed;
    }

    private String maskPhone(String value) {
        String digits = digitsOnly(value);
        if (digits == null || digits.length() < 7) {
            return digits == null ? "" : digits;
        }
        return digits.substring(0, 3) + "-****-" + digits.substring(digits.length() - 4);
    }

    private String maskEmail(String value) {
        String email = normalizeEmail(value);
        if (email == null) {
            return "";
        }
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + (at >= 0 ? email.substring(at) : "");
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    private String trimSummary(String value) {
        String trimmed = trimToEmpty(value).replaceAll("\\s+", " ");
        return trimmed.length() <= 120 ? trimmed : trimmed.substring(0, 120);
    }

    private String trimMessagePart(String value, int maxLength) {
        String trimmed = trimToEmpty(value)
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private String firstNonBlank(String first, String second) {
        String normalizedFirst = trimToEmpty(first);
        return normalizedFirst.isBlank() ? trimToEmpty(second) : normalizedFirst;
    }

    private String trimFailureReason(String value) {
        String trimmed = trimToEmpty(value);
        if (trimmed.isBlank()) {
            return "문자 발송에 실패했습니다.";
        }
        return trimmed.length() <= 500 ? trimmed : trimmed.substring(0, 500);
    }

    private String trimTrailingSlash(String value) {
        String baseUrl = trimToEmpty(value);
        if (baseUrl.isBlank()) {
            baseUrl = "http://localhost:8082";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String normalize(String value) {
        return Normalizer.normalize(safe(value), Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "");
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String safeText(String value) {
        return safe(value).replaceAll("[\\r\\n\\t]+", " ").trim();
    }

    private String excelValue(String value) {
        String safeValue = safeText(value);
        return safeValue.matches("^[=+\\-@].*") ? "'" + safeValue : safeValue;
    }

    private String matchingFileName(String title, String extension) {
        String safeTitle = safeText(title).replaceAll("[\\\\/:*?\"<>|]", " ");
        safeTitle = safeTitle.replaceAll("\\s+", " ").trim();
        if (safeTitle.isBlank()) {
            safeTitle = "matching";
        }
        if (safeTitle.length() > 80) {
            safeTitle = safeTitle.substring(0, 80).trim();
        }
        return safeTitle + " 매칭결과." + extension;
    }

    private int safeIndexTotalLimit(Integer value) {
        if (value == null || value < 1) {
            return DEFAULT_INDEX_TOTAL_LIMIT;
        }
        return Math.min(value, MAX_INDEX_TOTAL_LIMIT);
    }

    private record MatchingComputation(
            List<String> activeKeywords,
            List<LegacyMatchingResult> results,
            int candidatePoolCount,
            int blacklistedExcludedCount
    ) {
    }

}
