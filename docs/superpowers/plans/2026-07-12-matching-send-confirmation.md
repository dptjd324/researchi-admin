# Matching Send Confirmation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 매칭 결과의 선택 및 개별 SMS·이메일 발송 전에 사용자가 승인해야만 폼이 제출되도록 한다.

**Architecture:** `matching-run-window.html`의 발송 폼에 채널과 발송 범위를 나타내는 `data-*` 속성을 추가한다. 기존 선택 발송 제출 핸들러를 공통 확인 함수로 정리하고, 행별 발송 폼에도 같은 함수가 작동하도록 연결한다.

**Tech Stack:** Thymeleaf HTML, browser JavaScript `confirm()`, JUnit 5, AssertJ

## Global Constraints

- 선택 SMS 문구는 `선택한 N명에게 SMS를 발송하시겠습니까?`이다.
- 선택 이메일 문구는 `선택한 N명에게 이메일을 발송하시겠습니까?`이다.
- 개별 SMS 문구는 `SMS를 보내시겠습니까?`이다.
- 개별 이메일 문구는 `이메일을 보내시겠습니까?`이다.
- 취소 시 `preventDefault()`로 서버 요청을 차단한다.
- 다운로드와 서버 발송 로직은 변경하지 않는다.

---

### Task 1: 모든 매칭 발송 폼에 확인창 적용

**Files:**
- Modify: `src/main/resources/templates/research/matching-run-window.html`
- Modify: `src/test/java/com/researchi/admin/legacy/matching/web/LegacyMatchingTemplateTest.java`

**Interfaces:**
- Consumes: 기존 `data-selected-send-form`, `data-selected-channel`, `selectedValues()`, `appendSelectedInputs()`
- Produces: `data-send-confirm-form`, `data-send-channel`, `data-send-scope` 템플릿 계약과 공통 제출 확인 동작

- [ ] **Step 1: Write the failing template test**

완료 결과 모델을 사용해 `matching-run-window`를 렌더링하고 다음 계약을 검증한다.

```java
assertThat(rendered)
        .contains("data-send-confirm-form")
        .contains("data-send-channel=\"SMS\"")
        .contains("data-send-channel=\"이메일\"")
        .contains("data-send-scope=\"selected\"")
        .contains("data-send-scope=\"single\"")
        .contains("SMS를 보내시겠습니까?")
        .contains("이메일을 보내시겠습니까?")
        .contains("명에게 SMS를 발송하시겠습니까?")
        .contains("명에게 이메일을 발송하시겠습니까?");
```

- [ ] **Step 2: Run the test and verify RED**

Run: `.\gradlew.bat test --tests com.researchi.admin.legacy.matching.web.LegacyMatchingTemplateTest`

Expected: assertion failure because row forms do not have confirmation metadata or individual confirmation messages.

- [ ] **Step 3: Implement the common confirmation handler**

선택·행별 SMS 및 이메일 폼에 `data-send-confirm-form`, `data-send-channel`, `data-send-scope`를 지정한다. 제출 핸들러는 선택 범위일 때 선택 인원 검증과 hidden input 생성을 수행하고, 범위별 문구로 `confirm()`을 호출한다.

```javascript
document.querySelectorAll('[data-send-confirm-form]').forEach(function (form) {
    form.addEventListener('submit', function (event) {
        const scope = form.dataset.sendScope;
        const channel = form.dataset.sendChannel;
        let message = channel === 'SMS'
            ? 'SMS를 보내시겠습니까?'
            : '이메일을 보내시겠습니까?';

        if (scope === 'selected') {
            const selected = selectedValues();
            if (selected.length === 0) {
                event.preventDefault();
                alert('발송할 신청자를 선택해 주세요.');
                return;
            }
            message = '선택한 ' + selected.length + '명에게 ' + channel + '을 발송하시겠습니까?';
            appendSelectedInputs(form, selected);
        }

        if (!confirm(message)) {
            event.preventDefault();
        }
    });
});
```

- [ ] **Step 4: Run focused and full tests**

Run: `.\gradlew.bat test --tests com.researchi.admin.legacy.matching.web.LegacyMatchingTemplateTest`

Expected: `BUILD SUCCESSFUL`.

Run: `.\gradlew.bat test --rerun-tasks`

Expected: `BUILD SUCCESSFUL` with zero failed tests.
