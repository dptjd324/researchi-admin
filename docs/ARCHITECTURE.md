# 아키텍처 문서

## 현재 아키텍처

Researchi Admin은 **old-admin-DB-first** 구조입니다.

기존 운영 DB를 source of truth로 유지하고, 신규 관리자 기능은 보조 admin 테이블로 확장합니다. 이 방식은 약 400만 건 규모의 기존 데이터를 유지하면서 운영 흐름을 깨지 않고 백오피스 기능을 추가하기 위한 선택입니다.

## 핵심 데이터 소스

- `TB_RESEARCH_MST`: 리서치/좌담회 공고 마스터
- `TB_RESEARCH_APP`: 신청자 데이터
- `TB_BLACKLIST_MST`: 블랙리스트 데이터

## 보조 admin 테이블의 역할

기존 운영 DB가 제공하지 않는 기능만 `admin_*` 테이블로 관리합니다.

- 관리자 계정과 액션 로그
- 고객사/담당자 관리
- 메일 템플릿, 발송 작업, 발송 대상, 임계치 발송 규칙
- SMS 알림 로그
- export/search 로그
- 매칭 인덱스, 결과, 이력
- 기존 테이블 수정 전 revision backup
- 수동 게시 복사 이력

## 식별자 규칙

- 리서치/좌담회 공고 키: `RESEARCH_NO`
- 신청자 키: `RESEARCH_NO + RESEARCH_APP_SEQ`
- 블랙리스트 키: `BLACKLIST_NO`

메일, export, SMS 로그는 `research_no`를 기준으로 저장합니다. 기존 `document_srl` 호환 컬럼은 active 구조에서 제거했으며 신규 흐름에서는 사용하지 않습니다.

## 활성 모듈

- `legacy.research`: `TB_RESEARCH_MST` 목록, 상세, 수정, 신청자 조회, 메일, export, 수동 게시 복사
- `legacy.application`: `TB_RESEARCH_APP` 기반 공개 신청 폼
- `legacy.blacklist`: `TB_BLACKLIST_MST` 목록, 등록, 수정, 상태 변경
- `legacy.matching`: 신청자 키워드 매칭과 SMS 알림
- `client`: 고객사/담당자 등록과 `RESEARCH_NO` 연결
- `mailing`: 공통 메일 템플릿, 발송 작업, 발송 대상 저장
- `notification`: SMS Gateway와 알림 로그
- `auth`: 관리자 인증과 로그인/로그아웃 로그
- `log`: 액션, 검색, 메일, SMS, export 로그 조회
- `dashboard`: 월별 메일/SMS 사용량과 비용 요약
- `scheduler`: 예약 발송과 임계치 발송 트리거
- `export`: XLSX/TXT 파일 생성

## 제외된 구조

기존 public-board/new-admin/XE 테이블 중심 흐름은 active 구조에서 제외했습니다.

해당 흐름은 현재 운영 기준과 충돌할 수 있으므로, 새로운 프로젝트 단계로 명확히 정의되지 않는 한 다시 추가하지 않습니다.

## 설계 품질 포인트

- 레거시 DB 변경 최소화
- 신규 기능은 보조 테이블로 분리
- `RESEARCH_NO` 기준으로 기능 간 데이터 흐름 통일
- 수정 전 revision log로 운영 데이터 보호
- 대용량 데이터에 대비한 페이지네이션, 조건 검색, 단위별 export 설계
