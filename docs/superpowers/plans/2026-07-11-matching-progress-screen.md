# Matching Progress Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 매칭 실행 요청을 즉시 진행 전용 화면으로 응답하고, DB 작업 상태를 조회해 완료 시 결과 화면으로 자동 전환한다.

**Architecture:** `LegacyMatchingService`는 작업 생성·재사용과 실제 계산을 분리한다. 단일 스레드 비동기 실행기가 DB에 저장된 작업 ID를 처리하고, 진행 화면은 약 2초마다 읽기 전용 상태 API를 조회한다. 동일 공고·동일 조건의 `PENDING` 또는 `RUNNING` 작업은 재사용한다.

**Tech Stack:** Java 17, Spring Boot 4, Spring MVC, Spring `@Async`, MyBatis, Thymeleaf, JUnit 5, Mockito, AssertJ

## Global Constraints

- 최근 2년 이내 신청자 조회 조건을 유지한다.
- 같은 공고와 같은 매칭 조건으로 SMS 또는 메일 발송에 성공한 신청자는 다음 결과에서 제외한다.
- 블랙리스트, 동일 이름·전화번호 중복 제거, 구조화 검색 AND/OR 규칙을 변경하지 않는다.
- 진행 중에는 `0명` 결과와 빈 결과 문구를 표시하지 않는다.
- 정확한 처리율을 계산할 수 없으므로 퍼센트를 표시하지 않는다.
- 동일 조건 재실행은 새 회차를 만들지 않고 기존 작업에 연결한다.

---

### Task 1: 작업 티켓과 DB 상태 조회

**Files:**
- Create: `src/main/java/com/researchi/admin/legacy/matching/domain/LegacyMatchingRunTicket.java`
- Create: `src/main/java/com/researchi/admin/legacy/matching/domain/LegacyMatchingRunStatus.java`
- Modify: `src/main/java/com/researchi/admin/legacy/matching/mapper/LegacyMatchingIndexJobMapper.java`
- Modify: `src/main/resources/mapper/admin/legacy/matching/LegacyMatchingIndexJobMapper.xml`
- Modify: `src/main/java/com/researchi/admin/legacy/matching/service/LegacyMatchingService.java`
- Test: `src/test/java/com/researchi/admin/legacy/matching/service/LegacyMatchingServiceTest.java`

**Interfaces:**
- Produces: `LegacyMatchingRunTicket startOrReuseMatchingRun(Long researchNo, LegacyMatchingSearchCondition condition)`
- Produces: `LegacyMatchingRunStatus getMatchingRunStatus(Long researchNo, Long jobId)`
- Produces: `LegacyMatchingIndexJob findById(Long id)` mapper query

- [ ] **Step 1: Write failing service tests**

Add tests proving a new request stores a `PENDING` job without calculating candidates, an identical running request reuses its job ID and cycle, and a status request rejects a job belonging to another research.

```java
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
    assertThat(ticket.reused()).isFalse();
    verify(researchApplicationService, never()).getMatchingIndexCandidatePage(any(), anyInt(), anyInt());
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
```

- [ ] **Step 2: Run tests and confirm RED**

Run: `.\gradlew.bat test --tests com.researchi.admin.legacy.matching.service.LegacyMatchingServiceTest`

Expected: compilation fails because `LegacyMatchingRunTicket`, `startOrReuseMatchingRun`, and `getMatchingRunStatus` do not exist.

- [ ] **Step 3: Add minimal domain records and mapper query**

```java
public record LegacyMatchingRunTicket(Long jobId, int cycleNo, String status, boolean reused) {}

public record LegacyMatchingRunStatus(
        Long jobId,
        Long researchNo,
        int cycleNo,
        String status,
        String failReason,
        String conditionStorageKey
) {}
```

Add `findById` to the mapper interface and XML:

```xml
<select id="findById" resultMap="legacyMatchingIndexJobResultMap">
    SELECT * FROM admin_legacy_matching_index_job WHERE id = #{id}
</select>
```

Implement `startOrReuseMatchingRun` as a synchronized, short admin transaction. Normalize the condition, query `findRunningForCriteria`, otherwise insert one `PENDING` job using the existing two-year and batch configuration. Implement `getMatchingRunStatus` by loading the job and validating `researchNo` equality.

- [ ] **Step 4: Run tests and confirm GREEN**

Run the Task 1 test command. Expected: all `LegacyMatchingServiceTest` tests pass.

- [ ] **Step 5: Commit Task 1**

Stage only the Task 1 files and commit with `feat: add persistent matching run status`.

### Task 2: 단일 큐 비동기 실행과 재시작 복구

**Files:**
- Create: `src/main/java/com/researchi/admin/legacy/matching/config/LegacyMatchingAsyncConfig.java`
- Create: `src/main/java/com/researchi/admin/legacy/matching/service/LegacyMatchingAsyncExecutor.java`
- Modify: `src/main/java/com/researchi/admin/legacy/matching/service/LegacyMatchingService.java`
- Modify: `src/main/java/com/researchi/admin/legacy/matching/mapper/LegacyMatchingIndexJobMapper.java`
- Modify: `src/main/resources/mapper/admin/legacy/matching/LegacyMatchingIndexJobMapper.xml`
- Test: `src/test/java/com/researchi/admin/legacy/matching/service/LegacyMatchingServiceTest.java`
- Test: `src/test/java/com/researchi/admin/legacy/matching/service/LegacyMatchingAsyncExecutorTest.java`

**Interfaces:**
- Consumes: `LegacyMatchingRunTicket` from Task 1
- Produces: `void LegacyMatchingAsyncExecutor.execute(Long jobId)`
- Produces: `void LegacyMatchingService.executeMatchingRun(Long jobId)`
- Produces: `int markInterruptedRunsFailed(String failReason)`

- [ ] **Step 1: Write failing execution tests**

Add tests proving the executor delegates by job ID, the index job is marked complete only after result generation, exceptions mark it failed, and startup recovery marks leftover `PENDING`/`RUNNING` jobs failed.

```java
@Test
void asyncExecutorDelegatesMatchingRunByJobId() {
    LegacyMatchingAsyncExecutor executor = new LegacyMatchingAsyncExecutor(legacyMatchingService, legacyMatchingIndexJobMapper);
    executor.execute(77L);
    verify(legacyMatchingService).executeMatchingRun(77L);
}

@Test
void executeMatchingRunMarksIndexJobCompleteAfterResultGeneration() {
    LegacyMatchingIndexJob job = indexJob(77L, 46408L, 16, condition.storageKey(), "PENDING");
    when(legacyMatchingIndexJobMapper.findById(77L)).thenReturn(job);

    legacyMatchingService.executeMatchingRun(77L);

    InOrder order = inOrder(legacyMatchingJobMapper, legacyMatchingIndexJobMapper);
    order.verify(legacyMatchingJobMapper).markCompleted(any());
    order.verify(legacyMatchingIndexJobMapper).markCompleted(any());
}
```

- [ ] **Step 2: Run focused tests and confirm RED**

Run: `.\gradlew.bat test --tests com.researchi.admin.legacy.matching.service.LegacyMatchingServiceTest --tests com.researchi.admin.legacy.matching.service.LegacyMatchingAsyncExecutorTest`

Expected: compilation fails because the executor and `executeMatchingRun` do not exist.

- [ ] **Step 3: Implement the single-thread executor**

```java
@Bean("legacyMatchingTaskExecutor")
public Executor legacyMatchingTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(1);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("legacy-matching-");
    executor.initialize();
    return executor;
}
```

`LegacyMatchingAsyncExecutor.execute` uses `@Async("legacyMatchingTaskExecutor")`. On `ApplicationReadyEvent`, call `markInterruptedRunsFailed("서버 재시작으로 매칭 작업이 중단되었습니다.")`. Move the synchronous calculation body into `executeMatchingRun(jobId)`: load the stored job, mark started, calculate candidates and results, then mark the index job complete. Runtime exceptions mark the index job failed, and the temporary keyword index is cleared in `finally`.

- [ ] **Step 4: Run focused tests and confirm GREEN**

Run the Task 2 test command. Expected: all selected tests pass.

- [ ] **Step 5: Commit Task 2**

Stage only the Task 2 files and commit with `feat: execute matching runs asynchronously`.

### Task 3: 상태 API와 A안 진행 전용 화면

**Files:**
- Create: `src/main/java/com/researchi/admin/legacy/matching/web/LegacyMatchingRunStatusResponse.java`
- Create: `src/main/resources/templates/research/matching-progress-window.html`
- Modify: `src/main/java/com/researchi/admin/legacy/matching/web/LegacyMatchingController.java`
- Modify: `src/main/resources/templates/research/matching.html`
- Modify: `src/main/resources/templates/research/matching-run-window.html`
- Create: `src/test/java/com/researchi/admin/legacy/matching/web/LegacyMatchingControllerTest.java`
- Modify: `src/test/java/com/researchi/admin/legacy/matching/web/LegacyMatchingTemplateTest.java`

**Interfaces:**
- Consumes: `startOrReuseMatchingRun`, `LegacyMatchingAsyncExecutor.execute`, `getMatchingRunStatus`
- Produces: `GET /research/{researchNo}/matching/run-window/status?jobId={jobId}` JSON
- Produces: `research/matching-progress-window` Thymeleaf view

- [ ] **Step 1: Write failing controller and template tests**

Use standalone `MockMvc` with mocked services. Assert that POST `/run-window` returns the progress template, queues only a newly created ticket, and the status endpoint returns job state and an encoded result URL. Extend the template test to process the progress template and assert the existing result template still parses.

```java
mockMvc.perform(post("/research/46408/matching/run-window")
        .param("conditionChecked", "true")
        .param("appSex", "남자")
        .param("appBirth", "1988-1995"))
    .andExpect(status().isOk())
    .andExpect(view().name("research/matching-progress-window"))
    .andExpect(model().attribute("runTicket", new LegacyMatchingRunTicket(77L, 16, "PENDING", false)));

mockMvc.perform(get("/research/46408/matching/run-window/status").param("jobId", "77"))
    .andExpect(status().isOk())
    .andExpect(jsonPath("$.status").value("RUNNING"))
    .andExpect(jsonPath("$.cycleNo").value(16));
```

- [ ] **Step 2: Run web tests and confirm RED**

Run: `.\gradlew.bat test --tests com.researchi.admin.legacy.matching.web.LegacyMatchingControllerTest --tests com.researchi.admin.legacy.matching.web.LegacyMatchingTemplateTest`

Expected: compilation or assertion failure because the endpoint and progress template do not exist.

- [ ] **Step 3: Implement controller flow and progress template**

```java
LegacyMatchingRunTicket ticket = legacyMatchingService.startOrReuseMatchingRun(researchNo, searchForm.toCondition());
if (!ticket.reused()) {
    legacyMatchingAsyncExecutor.execute(ticket.jobId());
}
model.addAttribute("runTicket", ticket);
model.addAttribute("research", researchMasterService.getResearchMaster(researchNo));
model.addAttribute("searchForm", searchForm);
return "research/matching-progress-window";
```

The progress template displays only the research title, cycle, active condition text, indeterminate spinner, and status copy. JavaScript polls every 2 seconds. `COMPLETED` assigns `window.location.href = response.resultUrl`; `FAILED` stops polling and replaces the spinner with a Korean failure panel. Network errors show a reconnecting message and retry without classifying the server job as failed.

Remove the temporary `document.write` loader from `matching.html`; keep popup creation and form targeting. The server-rendered progress template becomes the authoritative first screen. Keep `matching-run-window.html` as the completed results view and remove its running-state `0` metrics branch.

- [ ] **Step 4: Run web tests and confirm GREEN**

Run the Task 3 test command. Expected: all selected web tests pass.

- [ ] **Step 5: Commit Task 3**

Stage only the Task 3 files and commit with `feat: show dedicated matching progress screen`.

### Task 4: 정책 회귀와 전체 검증

**Files:**
- Modify only if a regression is found: matching files from Tasks 1-3
- Test: `src/test/java/com/researchi/admin/legacy/matching/service/LegacyMatchingServiceTest.java`
- Test: `src/test/java/com/researchi/admin/legacy/matching/web/LegacyMatchingControllerTest.java`

**Interfaces:**
- Verifies all interfaces produced by Tasks 1-3

- [ ] **Step 1: Run matching regression tests**

Run: `.\gradlew.bat test --tests "com.researchi.admin.legacy.matching.*"`

Expected: all matching tests pass, including `matchingResultsExcludeApplicantsAlreadySentForSameCondition` and existing birth-range tests.

- [ ] **Step 2: Verify two-year SQL remains present**

Run: `rg -n "DATE_SUB\(CURDATE\(\), INTERVAL 2 YEAR\)" src/main/resources/mapper/oldadmin/research/ResearchApplicationMapper.xml src/main/resources/mapper/admin/legacy/matching/LegacyApplicationKeywordMapper.xml`

Expected: both candidate-query paths still contain the two-year condition.

- [ ] **Step 3: Run the full test suite**

Run: `.\gradlew.bat test`

Expected: `BUILD SUCCESSFUL` with zero failed tests.

- [ ] **Step 4: Review the final diff**

Run `git diff --check`, `git status --short`, and a path-scoped `git diff` for matching Java, mapper, template, and test files. Confirm only the approved progress flow changed and unrelated dirty files remain untouched.

- [ ] **Step 5: Commit final test adjustments if needed**

Stage only matching files changed during Task 4 and commit with `test: verify matching progress workflow`.
