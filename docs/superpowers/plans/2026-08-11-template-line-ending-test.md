# Platform-Independent Template Test Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `LegacyMatchingTemplateTest` independent of Windows CRLF versus Unix LF without changing production templates.

**Architecture:** Load `matching-run-window.html` through one test-only helper that normalizes CRLF and lone CR to LF. Both tests that inspect the raw resource consume this helper; production resources and repository-wide line-ending rules remain unchanged.

**Tech Stack:** Java 17, JUnit 5, AssertJ, Spring `ClassPathResource`, Gradle Wrapper

## Global Constraints

- Modify only the test class and this branch's design/plan documentation.
- Do not modify production HTML, runtime configuration, or `.gitattributes`.
- Keep this fix in a pull request separate from the public secret-policy pull request.
- Merge only after the focused test and full Gradle suite pass.

---

### Task 1: Normalize the matching template in the test

**Files:**
- Modify: `src/test/java/com/researchi/admin/legacy/matching/web/LegacyMatchingTemplateTest.java:27-29,73-75`
- Test: `src/test/java/com/researchi/admin/legacy/matching/web/LegacyMatchingTemplateTest.java`

**Interfaces:**
- Consumes: `ClassPathResource.getContentAsString(StandardCharsets.UTF_8)`
- Produces: `private String matchingRunWindowTemplate() throws IOException`, returning template text with all line endings normalized to LF

- [ ] **Step 1: Run the existing focused test to reproduce the platform-sensitive failure**

```powershell
$env:SPRING_CONFIG_ADDITIONAL_LOCATION='optional:file:C:/admin/src/main/resources/application-local.yml'
C:\admin\gradlew.bat -p C:\admin\.worktrees\template-line-ending-test test --tests "com.researchi.admin.legacy.matching.web.LegacyMatchingTemplateTest.matchingRunWindowConfirmsSelectedAndSingleNotifications"
```

Expected: FAIL at the assertion containing `if (!confirm(message)) {\n` when the resource contains CRLF.

- [ ] **Step 2: Add the minimal test-only helper and route both raw template reads through it**

Add `java.io.IOException` and implement:

```java
private String matchingRunWindowTemplate() throws IOException {
    return new ClassPathResource("templates/research/matching-run-window.html")
            .getContentAsString(StandardCharsets.UTF_8)
            .replace("\r\n", "\n")
            .replace('\r', '\n');
}
```

In both raw-resource tests, replace the direct `ClassPathResource` expression with:

```java
String template = matchingRunWindowTemplate();
```

- [ ] **Step 3: Run the focused class to verify all template assertions pass**

```powershell
$env:SPRING_CONFIG_ADDITIONAL_LOCATION='optional:file:C:/admin/src/main/resources/application-local.yml'
C:\admin\gradlew.bat -p C:\admin\.worktrees\template-line-ending-test test --tests "com.researchi.admin.legacy.matching.web.LegacyMatchingTemplateTest"
```

Expected: PASS for every test in `LegacyMatchingTemplateTest`.

- [ ] **Step 4: Run the full regression suite**

```powershell
$env:SPRING_CONFIG_ADDITIONAL_LOCATION='optional:file:C:/admin/src/main/resources/application-local.yml'
C:\admin\gradlew.bat -p C:\admin\.worktrees\template-line-ending-test test
```

Expected: all 174 tests PASS.

- [ ] **Step 5: Review and commit only the test change**

```powershell
git diff --check
git diff -- src/test/java/com/researchi/admin/legacy/matching/web/LegacyMatchingTemplateTest.java
git add -- src/test/java/com/researchi/admin/legacy/matching/web/LegacyMatchingTemplateTest.java
git commit -m "test: normalize template line endings"
```

Expected: the commit contains only the test file.

### Task 2: Deliver the isolated fix and revalidate the security policy

**Files:**
- No additional source files
- Verify: `.github/workflows/secret-policy.yml`
- Verify: `scripts/secrets/Test-SecretPolicy.Tests.ps1`

**Interfaces:**
- Consumes: merged `master` containing the test-only normalization
- Produces: updated `codex/public-secret-policy` branch with a green full suite and policy checks

- [ ] **Step 1: Scan and publish the test-fix branch**

```powershell
gitleaks git --no-banner --redact --log-opts="master..HEAD"
git push -u origin codex/template-line-ending-test
```

Expected: no leak findings; push succeeds.

- [ ] **Step 2: Open and merge the separate test-fix pull request**

Create a pull request from `codex/template-line-ending-test` to `master`, verify its diff contains only documentation plus the test file, and squash-merge it after checks pass.

- [ ] **Step 3: Update the public secret-policy branch from the new master**

```powershell
git fetch origin
git merge origin/master
```

Expected: merge succeeds without changing the four policy files unexpectedly.

- [ ] **Step 4: Re-run the policy and application gates**

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\secrets\Test-SecretPolicy.Tests.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\secrets\Test-SecretPolicy.ps1
gitleaks git --no-banner --redact --log-opts="origin/master..HEAD"
$env:SPRING_CONFIG_ADDITIONAL_LOCATION='optional:file:C:/admin/src/main/resources/application-local.yml'
C:\admin\gradlew.bat -p C:\admin\.worktrees\public-secret-policy test
```

Expected: both PowerShell suites PASS, Gitleaks reports no leaks, and all 174 Gradle tests PASS.

- [ ] **Step 5: Publish and merge the public secret-policy pull request**

Push the updated `codex/public-secret-policy` branch, wait for GitHub Actions to pass, mark pull request #1 ready, verify its four-file policy diff, and squash-merge it into `master`.
