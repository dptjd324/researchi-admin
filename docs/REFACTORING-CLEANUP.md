# 리팩토링 및 정리 기록

마지막 검토일: 2026-05-19

이 문서는 active 구조에서 제외한 흐름과 유지해야 하는 테이블을 정리합니다.

포트폴리오 관점에서는 단순 기능 구현뿐 아니라, 레거시 흐름을 분석하고 현재 운영에 필요한 구조만 남긴 리팩토링 기록입니다.

## 현재 Source Of Truth

현재 관리자 흐름은 **old-admin DB first**입니다.

- `TB_RESEARCH_MST`: 리서치/좌담회 공고 마스터
- `TB_RESEARCH_APP`: 신청자 데이터
- `TB_BLACKLIST_MST`: 블랙리스트 데이터

신규 `admin_*` 테이블은 보조 기능만 담당합니다.

## 제거 또는 비활성화한 흐름

기존 public-board/new-admin 흐름은 active application에서 제외했습니다.

제거 또는 비활성화 대상:

- `admin_job_meta` 기반 기존 공고 관리
- `admin_job_application` 기반 기존 공개 신청 저장
- `admin_form_field` 기반 기존 동적 폼 저장
- `admin_blacklist` 기반 기존 블랙리스트 저장
- new-admin application ID 기반 기존 키워드/매칭 저장
- public-board datasource와 route 가정

## 유지해야 하는 DB 테이블

- `admin_client`
- `admin_client_contact`
- `admin_research_client_link`
- `admin_mail_template`
- `admin_mail_send_job`
- `admin_mail_send_target`
- `admin_legacy_mail_rule`
- `admin_legacy_mail_rule_item`
- `admin_legacy_application_extra_answer`
- `admin_legacy_application_keyword`
- `admin_legacy_matching_job`
- `admin_legacy_matching_result`
- `admin_legacy_matching_index_job`
- `admin_manual_publish_log`
- `admin_legacy_revision_log`
- `admin_action_log`
- `admin_search_log`
- `admin_notification_log`
- `admin_export_log`

## 정리 결정

- 메일, export, SMS 알림 로그는 `research_no`를 기준으로 저장합니다.
- 기존 `document_srl` 호환 컬럼은 local DB에서 제거했습니다.
- retired 구조가 다시 유입되지 않도록 오래된 migration/cleanup SQL을 제거했습니다.

## 남은 리팩토링 작업

- production DB에만 남아 있는 retired table이 있는지 별도 확인
- IDE에 빈 디렉터리가 남아 있으면 정리
- `public_document_srl`은 외부 게시글 번호 메모로만 유지
- 해당 메모 필드가 더 이상 필요 없다고 판단되면 이후 제거

## 품질 포인트

- 운영 기준 키를 `RESEARCH_NO`로 통일해 데이터 흐름을 단순화했습니다.
- 사용하지 않는 legacy route와 template을 제거해 유지보수 부담을 줄였습니다.
- 로그와 revision backup은 보존해 운영 추적성을 유지했습니다.
