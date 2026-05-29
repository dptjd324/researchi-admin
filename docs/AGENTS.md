# AI 협업 가이드

이 문서는 Codex 같은 AI 개발 도구와 협업할 때 지켜야 할 프로젝트 규칙을 정리합니다.

AI를 단순 코드 생성 도구로 사용하지 않고, 프로젝트 문맥을 이해한 상태에서 작은 단위로 검토와 구현을 수행하도록 제한하기 위해 작성했습니다.

## 작업 방향

Researchi Admin은 **old-admin-DB-first** 구조입니다.

AI가 작업하기 전 반드시 이 방향을 유지해야 합니다.

운영 기준 키:

- `RESEARCH_NO`
- `RESEARCH_NO + RESEARCH_APP_SEQ`
- `BLACKLIST_NO`

명시적인 새 프로젝트 단계가 아니라면 public-board 중심 통합을 다시 추가하지 않습니다.

## 현재 활성 영역

- `legacy.research`
- `legacy.application`
- `legacy.blacklist`
- `legacy.matching`
- `client`
- `mailing`
- `notification`
- `dashboard`
- `log`
- `auth`

## 리팩토링 규칙

- 기존 운영 DB 데이터는 훼손하지 않습니다.
- 보조 로그와 snapshot은 사용자가 명시적으로 요청하지 않는 한 삭제하지 않습니다.
- retired code는 active route/service 의존성이 없을 때만 제거합니다.
- 작은 단위로 수정하고 검증 가능한 변경을 우선합니다.
- 구조 변경 후 compile/test를 실행합니다.

## AI 활용 품질 기준

- PRD, 아키텍처, DB 스키마, API 문서를 먼저 읽고 작업합니다.
- 한 번에 여러 phase를 구현하지 않습니다.
- 관련 없는 파일을 수정하지 않습니다.
- 실제 운영 credential이나 secret을 코드에 넣지 않습니다.
- 변경 파일, 구현 내용, 테스트 명령, 보안 설정 변경 여부를 보고합니다.

## 포트폴리오 관점

이 문서는 AI를 무분별하게 사용하는 것이 아니라, AI에게도 프로젝트 제약과 품질 기준을 부여해 통제된 방식으로 협업했다는 근거입니다.
