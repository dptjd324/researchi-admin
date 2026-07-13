# 템플릿 정리 기록

이 문서는 active UI에서 제거한 템플릿과 유지해야 하는 템플릿을 기록합니다.

현재 운영 흐름은 `/research`, `/research/{researchNo}/apply`, `/legacy-blacklist`, `/clients`, `/logs`를 기준으로 합니다.

## 제거한 템플릿

아래 템플릿은 비활성화된 legacy/new-admin route 전용 화면이므로 active UI에서 제거했습니다.

- `src/main/resources/templates/jobs/list.html`
- `src/main/resources/templates/jobs/detail.html`
- `src/main/resources/templates/jobs/form.html`
- `src/main/resources/templates/applications/list.html`
- `src/main/resources/templates/applications/detail.html`
- `src/main/resources/templates/mail/history.html`
- `src/main/resources/templates/mail/templates.html`
- `src/main/resources/templates/matching/detail.html`
- `src/main/resources/templates/search/index.html`
- `src/main/resources/templates/form/fields.html`
- `src/main/resources/templates/blacklist/list.html`
- `src/main/resources/templates/publicform/apply.html`

## 최종 업데이트

preserved-tools page는 active UI에서 제거되었습니다.

기존 route는 특정 legacy 기능을 의도적으로 복원하는 경우가 아니라면 삭제 상태를 유지합니다.

## 유지해야 하는 템플릿

아래 템플릿은 현재 old-admin-first 흐름에서 사용 중이므로 유지합니다.

- `src/main/resources/templates/research/**`
- `src/main/resources/templates/legacy-blacklist/**`
- `src/main/resources/templates/publicform/legacy-apply.html`
- `src/main/resources/templates/publicform/result.html`
- `src/main/resources/templates/clients/**`
- `src/main/resources/templates/logs/**`
- `src/main/resources/templates/auth/**`

## 정리 의도

사용하지 않는 화면을 남겨두면 route 기준이 혼재되고 유지보수 비용이 증가합니다.

현재 운영 기준에 맞지 않는 템플릿을 제거해 관리자 시스템의 흐름을 단순화하고, `RESEARCH_NO` 중심 구조가 명확하게 보이도록 정리했습니다.
