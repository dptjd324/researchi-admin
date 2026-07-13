# Public Application Consent Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Store auditable application consent and enforce future-recruitment and channel-specific consent in matching, SMS, and email flows.

**Architecture:** A new admin-database consent record is keyed by the immutable legacy application identity. The public form writes the record after legacy application creation, matching removes applicants without active future-recruitment consent, and dispatch rechecks channel consent immediately before provider calls.

**Tech Stack:** Java 17, Spring Boot 4, MyBatis, Thymeleaf, MySQL, JUnit 5, Mockito, AssertJ

## Global Constraints

- Keep `TB_RESEARCH_APP.PROVIDE_YN` as delivery/provision state only.
- Do not backfill existing applications as consented.
- Keep the existing two-year application-age filter, blacklist filter, successful-send exclusion, duplicate prevention, and keyword behavior unchanged.
- Current-research data retention notice is two years after the research ends.
- Future recruitment consent expires two years after consent or on withdrawal, whichever occurs first.
- Optional consent must default to false and must not block the current application.
- The form identifies `리서치아이` and `spirit2@naver.com`.

---

### Task 1: Consent Persistence Model

**Files:**
- Create: `src/main/java/com/researchi/admin/legacy/application/domain/LegacyApplicationConsent.java`
- Create: `src/main/java/com/researchi/admin/legacy/application/mapper/LegacyApplicationConsentMapper.java`
- Create: `src/main/resources/mapper/admin/legacy/LegacyApplicationConsentMapper.xml`
- Modify: `src/main/java/com/researchi/admin/config/AdminSchemaBootstrap.java`
- Test: `src/test/java/com/researchi/admin/legacy/application/domain/LegacyApplicationConsentTest.java`

**Interfaces:**
- Produces: `LegacyApplicationConsent.activeFutureRecruitmentAt(LocalDateTime)`, `allowsSmsAt(LocalDateTime)`, and `allowsEmailAt(LocalDateTime)`.
- Produces: mapper operations `insert`, `findByApplication`, and `findActiveFutureRecruitmentApplicationSeqs`.

- [ ] **Step 1: Write failing consent-state tests**

Cover active, expired, withdrawn, future-denied, SMS-denied, and email-denied records with a fixed `LocalDateTime`.

- [ ] **Step 2: Run the domain test and verify RED**

Run: `./gradlew.bat test --tests "*LegacyApplicationConsentTest"`

Expected: compilation failure because `LegacyApplicationConsent` does not exist.

- [ ] **Step 3: Implement the domain, mapper, and schema**

Create `admin_legacy_application_consent` with a unique `(research_no, research_app_seq)` key and indexes for active consent lookup. Store required, future, SMS, email flags, consent version, notice snapshot, consent and expiration timestamps, withdrawal timestamp, and creation timestamp.

- [ ] **Step 4: Run the domain test and verify GREEN**

Run: `./gradlew.bat test --tests "*LegacyApplicationConsentTest"`

Expected: PASS.

### Task 2: Public Form Contract and Copy

**Files:**
- Modify: `src/main/java/com/researchi/admin/publicform/web/PublicApplicationForm.java`
- Modify: `src/main/resources/templates/publicform/legacy-apply.html`
- Modify: `src/main/resources/static/css/public-ui.css`
- Modify: `src/test/java/com/researchi/admin/publicform/web/PublicApplicationFormValidationTest.java`
- Create: `src/test/java/com/researchi/admin/legacy/matching/web/PublicConsentTemplateTest.java`

**Interfaces:**
- Produces: required `provideYn`, optional `futureRecruitmentYn`, `notifySmsYn`, and `notifyEmailYn` form properties.

- [ ] **Step 1: Write failing validation and template tests**

Assert that missing `provideYn` fails validation, all optional false values pass that constraint, and the template includes the four independent fields plus the final retention and contact copy.

- [ ] **Step 2: Run focused tests and verify RED**

Run: `./gradlew.bat test --tests "*PublicApplicationFormValidationTest" --tests "*PublicConsentTemplateTest"`

Expected: FAIL because `futureRecruitmentYn` and the new template copy do not exist.

- [ ] **Step 3: Implement form fields, Korean notice, and restrained layout styles**

Keep one explanatory box and separate required/optional checkbox rows. Do not make optional fields HTML-required.

- [ ] **Step 4: Run focused tests and verify GREEN**

Run: `./gradlew.bat test --tests "*PublicApplicationFormValidationTest" --tests "*PublicConsentTemplateTest"`

Expected: PASS.

### Task 3: Submission Consent Write

**Files:**
- Create: `src/main/java/com/researchi/admin/legacy/application/service/LegacyApplicationConsentService.java`
- Modify: `src/main/java/com/researchi/admin/legacy/application/service/LegacyPublicApplicationService.java`
- Modify: `src/test/java/com/researchi/admin/legacy/application/service/LegacyPublicApplicationServiceTest.java`

**Interfaces:**
- Consumes: `LegacyApplicationConsentMapper.insert` and the four form flags.
- Produces: `LegacyApplicationConsentService.recordSubmissionConsent(Long, Long, PublicApplicationForm)`.

- [ ] **Step 1: Write a failing submission test**

Capture the consent passed after application insertion and assert the identifiers, four flags, version, consent time, and two-year expiration.

- [ ] **Step 2: Run the service test and verify RED**

Run: `./gradlew.bat test --tests "*LegacyPublicApplicationServiceTest"`

Expected: FAIL because the consent service is not invoked.

- [ ] **Step 3: Implement mandatory consent persistence**

Record consent after legacy application and structured-answer writes and before best-effort indexing. Let consent persistence exceptions propagate to the controller error flow.

- [ ] **Step 4: Run the service test and verify GREEN**

Run: `./gradlew.bat test --tests "*LegacyPublicApplicationServiceTest"`

Expected: PASS.

### Task 4: Matching Candidate Enforcement

**Files:**
- Modify: `src/main/java/com/researchi/admin/legacy/application/service/LegacyApplicationConsentService.java`
- Modify: `src/main/java/com/researchi/admin/legacy/matching/service/LegacyMatchingService.java`
- Modify: `src/test/java/com/researchi/admin/legacy/matching/service/LegacyMatchingServiceTest.java`

**Interfaces:**
- Produces: `filterActiveFutureRecruitment(List<ResearchApplication>, LocalDateTime)` preserving input order.

- [ ] **Step 1: Write failing matching tests**

Assert that a candidate without consent, with denied consent, with expired consent, or with withdrawn consent is absent, while an active consented candidate remains.

- [ ] **Step 2: Run matching tests and verify RED**

Run: `./gradlew.bat test --tests "*LegacyMatchingServiceTest"`

Expected: FAIL because unconsented candidates remain in results.

- [ ] **Step 3: Filter candidates before stored result generation**

Apply consent filtering without changing keyword scoring, two-year age filtering, blacklist exclusion, prior-success exclusion, or duplicate removal.

- [ ] **Step 4: Run matching tests and verify GREEN**

Run: `./gradlew.bat test --tests "*LegacyMatchingServiceTest"`

Expected: PASS.

### Task 5: Dispatch-Time Channel Enforcement

**Files:**
- Modify: `src/main/java/com/researchi/admin/legacy/application/service/LegacyApplicationConsentService.java`
- Modify: `src/main/java/com/researchi/admin/legacy/matching/service/LegacyMatchingService.java`
- Modify: `src/test/java/com/researchi/admin/legacy/matching/service/LegacyMatchingServiceTest.java`

**Interfaces:**
- Produces: `allowsSms(Long researchNo, Long researchAppSeq, LocalDateTime now)` and equivalent `allowsEmail`.

- [ ] **Step 1: Write failing SMS and email guard tests**

Assert that provider gateways are not called when channel consent is missing, expired, or withdrawn, and are called once when the corresponding channel is active.

- [ ] **Step 2: Run matching tests and verify RED**

Run: `./gradlew.bat test --tests "*LegacyMatchingServiceTest"`

Expected: FAIL because dispatch currently ignores consent.

- [ ] **Step 3: Add immediate pre-dispatch consent checks**

Recheck consent inside each result loop and log `SKIPPED_CONSENT` with a Korean explanation when blocked. Include the same checks in SMS send-limit counting so blocked rows do not consume quota calculations.

- [ ] **Step 4: Run matching tests and verify GREEN**

Run: `./gradlew.bat test --tests "*LegacyMatchingServiceTest"`

Expected: PASS.

### Task 6: Integrated Verification

**Files:**
- Modify only files required by failures attributable to this feature.

**Interfaces:**
- Consumes all preceding tasks.
- Produces a deployable consent-enforced application.

- [ ] **Step 1: Run focused public-form and matching tests**

Run: `./gradlew.bat test --tests "*PublicApplicationFormValidationTest" --tests "*PublicConsentTemplateTest" --tests "*LegacyPublicApplicationServiceTest" --tests "*LegacyMatchingServiceTest"`

Expected: PASS.

- [ ] **Step 2: Run the full test suite**

Run: `./gradlew.bat test`

Expected: PASS with no failing tests.

- [ ] **Step 3: Build the production artifact**

Run: `./gradlew.bat clean bootJar`

Expected: `BUILD SUCCESSFUL` and an executable jar under `build/libs` without profile secrets bundled.

- [ ] **Step 4: Verify the public application visually**

Start the existing local profile, open a real public application URL, and verify desktop and mobile layouts. Confirm all four checkboxes are readable, optional boxes default unchecked, and form submission is blocked only when the required checkbox is missing.

### Task 7: 상위·하위 선택 동의 검증

**Files:**
- Modify: `src/main/java/com/researchi/admin/publicform/web/PublicApplicationForm.java`
- Modify: `src/main/java/com/researchi/admin/legacy/application/service/LegacyPublicApplicationService.java`
- Modify: `src/main/resources/templates/publicform/legacy-apply.html`
- Modify: `src/main/resources/static/css/public-ui.css`
- Modify: `src/test/java/com/researchi/admin/publicform/web/PublicApplicationFormValidationTest.java`
- Modify: `src/test/java/com/researchi/admin/publicform/web/PublicConsentTemplateTest.java`

**Interfaces:**
- Produces: `isFutureRecruitmentChannelAccepted()` 교차 필드 검증.
- Produces: 향후 모집 체크 시 채널 활성화, 채널 체크 시 상위 동의 자동 선택, 상위 동의 해제 시 채널 초기화.

- [ ] 실패 테스트에서 향후 모집만 선택한 제출을 거부하고 SMS만, 이메일만, 두 채널 모두 선택한 제출을 허용한다.
- [ ] 템플릿 테스트에서 상위·하위 그룹 속성과 자동 선택 스크립트를 검증한다.
- [ ] 최소 구현 후 두 테스트를 다시 실행해 통과시킨다.

### Task 8: 매칭 결과 채널 허용 상태

**Files:**
- Modify: `src/main/java/com/researchi/admin/legacy/application/service/LegacyApplicationConsentService.java`
- Modify: `src/main/java/com/researchi/admin/legacy/matching/domain/LegacyMatchingResult.java`
- Modify: `src/main/java/com/researchi/admin/legacy/matching/service/LegacyMatchingService.java`
- Modify: `src/main/resources/templates/research/matching-run-window.html`
- Modify: `src/test/java/com/researchi/admin/legacy/matching/service/LegacyMatchingServiceTest.java`
- Modify: `src/test/java/com/researchi/admin/legacy/matching/web/LegacyMatchingTemplateTest.java`

**Interfaces:**
- Produces: `smsAllowed`, `emailAllowed` 상태를 가진 `LegacyMatchingResult`.
- Produces: SMS만 동의한 행에는 SMS 버튼만, 이메일만 동의한 행에는 이메일 버튼만 표시.

- [ ] 결과 상태와 템플릿 표시 조건의 실패 테스트를 작성한다.
- [ ] 동의 조회 결과를 행 상태에 결합하고 허용되지 않은 폼을 렌더링하지 않는다.
- [ ] 서비스와 템플릿 테스트를 다시 실행해 통과시킨다.

### Task 9: 혼합 선택 채널별 대량 발송

**Files:**
- Modify: `src/main/resources/templates/research/matching-run-window.html`
- Modify: `src/test/java/com/researchi/admin/legacy/matching/web/LegacyMatchingTemplateTest.java`

**Interfaces:**
- Consumes: 각 행의 `data-sms-allowed`, `data-email-allowed`.
- Produces: 선택 행 중 채널별 허용된 ID만 폼에 추가하고 실제 인원을 버튼과 확인창에 표시.

- [ ] 혼합 선택 시 채널별 ID 필터와 버튼 문구를 요구하는 실패 테스트를 작성한다.
- [ ] SMS와 이메일 폼별로 허용된 선택 행만 집계하도록 스크립트를 수정한다.
- [ ] 채널 인원이 0명이면 해당 버튼을 비활성화하고 확인창에 실제 인원을 표시한다.
- [ ] 템플릿 테스트와 전체 테스트, `bootJar`를 실행한다.

### Task 10: 로컬 가상 매칭 미리보기

**Files:**
- Create: `src/main/java/com/researchi/admin/legacy/matching/web/LocalMatchingConsentPreviewController.java`
- Create: `src/test/java/com/researchi/admin/legacy/matching/web/LocalMatchingConsentPreviewControllerTest.java`
- Modify: `src/main/resources/templates/research/matching-run-window.html`
- Modify: `src/test/java/com/researchi/admin/legacy/matching/web/LegacyMatchingTemplateTest.java`

**Interfaces:**
- Produces: `GET /dev/matching-consent-preview` local 프로필 전용 경로.
- Produces: SMS 전용, 이메일 전용, 두 채널 동의 가상 행과 `previewMode=true` 모델.
- Produces: 미리보기에서 모든 폼 제출을 차단하는 템플릿 이벤트 처리.

- [ ] 컨트롤러 프로필, 경로, 음수 식별자와 채널 조합을 검증하는 실패 테스트를 작성한다.
- [ ] 실제 서비스나 매퍼 의존성 없이 가상 모델을 생성하는 컨트롤러를 구현한다.
- [ ] 미리보기 모드 폼 제출 차단 실패 테스트를 작성한다.
- [ ] 기존 템플릿에 제출 차단 로직을 추가하고 관련 테스트를 통과시킨다.
- [ ] 전체 테스트와 `bootJar`를 실행한다.
