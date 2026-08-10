# Deterministic Mail Support Clock Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove midnight and boundary races from mail scheduling tests with a single injectable clock.

**Architecture:** Keep the existing public service constructor and delegate it to a package-private constructor using `Clock.systemDefaultZone()`. Route every `LocalDateTime.now()` in the service through the clock and use a fixed Asia/Seoul clock in scheduling tests.

**Tech Stack:** Java 17 `Clock`, JUnit 5, AssertJ, Mockito, Spring, Gradle Wrapper

## Global Constraints

- Preserve the existing public constructor signature and runtime system time zone.
- Do not add Spring configuration or change mail scheduling rules.
- Keep this change in a commit and pull request separate from the line-ending fix.

---

### Task 1: Make mail-support time deterministic

**Files:**
- Modify: `src/main/java/com/researchi/admin/legacy/research/service/mail/LegacyResearchMailSupportService.java`
- Modify: `src/test/java/com/researchi/admin/legacy/research/service/mail/LegacyResearchMailSupportServiceTest.java`

**Interfaces:**
- Produces: package-private constructor `LegacyResearchMailSupportService(..., AdminActionLogService, Clock)`
- Preserves: existing public seven-dependency constructor

- [ ] **Step 1: Preserve the observed RED evidence**

The unmodified focused test was run at 00:05 and failed with expected date 2026-08-12 but actual date 2026-08-11. A second focused run reproduced the same failure.

- [ ] **Step 2: Change scheduling tests to use a fixed clock**

Use `Clock.fixed(Instant.parse("2026-08-10T15:05:00Z"), ZoneId.of("Asia/Seoul"))`. Derive validation inputs with `LocalDateTime.now(clock)`, assert midnight schedules for 2026-08-12, and assert 23:05 schedules for 2026-08-11.

- [ ] **Step 3: Add the minimal clock implementation**

Add a final `Clock`, delegate the public constructor to `Clock.systemDefaultZone()`, add the package-private clock constructor, and replace every `LocalDateTime.now()` in this service with `LocalDateTime.now(clock)`.

- [ ] **Step 4: Verify focused RED becomes GREEN**

```powershell
C:\admin\gradlew.bat -p C:\admin\.worktrees\mail-clock-test test --tests "com.researchi.admin.legacy.research.service.mail.LegacyResearchMailSupportServiceTest"
```

Expected: all tests in the class PASS.

- [ ] **Step 5: Verify the stacked full suite**

```powershell
$env:SPRING_CONFIG_ADDITIONAL_LOCATION='optional:file:C:/admin/src/main/resources/application-local.yml'
C:\admin\gradlew.bat -p C:\admin\.worktrees\mail-clock-test test
```

Expected: all 174 tests PASS.

- [ ] **Step 6: Review and commit**

Run `git diff --check`, inspect the exact production/test diff, stage only the two Java files, and commit with `test: make mail scheduling time deterministic`.

### Task 2: Publish the stacked pull requests

**Files:** No additional source files.

- [ ] **Step 1:** Run Gitleaks over commits not in `master` and push both branches.
- [ ] **Step 2:** Open the line-ending pull request against `master`.
- [ ] **Step 3:** Open the clock pull request against `codex/template-line-ending-test` and verify its diff excludes the line-ending commit.
- [ ] **Step 4:** Merge the line-ending pull request, retarget the clock pull request to `master`, verify its diff, then merge it.
- [ ] **Step 5:** Update the public secret-policy branch from `master` and rerun its policy, Gitleaks, GitHub Actions, and full Gradle gates before merging pull request #1.
