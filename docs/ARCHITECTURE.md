# ARCHITECTURE.md

## Architecture Correction: Old Admin DB Is Primary

Verified local `admin_copy` sample data shows the old admin database must be the
primary source of truth.

Primary old admin tables:
- `TB_RESEARCH_MST` (`tb_research_mst`): posting/research master data
- `TB_RESEARCH_APP` (`tb_research_app`): applicant data
- `TB_BLACKLIST_MST` (`tb_blacklist_mst`): blacklist data

Verified facts from `admin_copy`:
- `TB_RESEARCH_MST`: MyISAM, `utf8mb4_general_ci`, about 46k rows
- `TB_RESEARCH_APP`: MyISAM, `utf8mb4_general_ci`, about 4.12M rows
- `TB_BLACKLIST_MST`: MyISAM, `utf8mb4_general_ci`, about 353 rows

Because the old tables are MyISAM, application code must not rely on transaction
rollback for old-table writes. Before each old-table update, write a supplemental
revision backup row.

Target source-of-truth model:

```text
old admin DB / admin_copy
  TB_RESEARCH_MST       -> ResearchMaster
  TB_RESEARCH_APP       -> ResearchApplication
  TB_BLACKLIST_MST      -> Blacklist

new admin supplemental DB/tables
  audit logs
  mail send jobs and send target snapshots
  mail templates and send rules
  dynamic form fields
  revision backups
  manual publish logs
  optional public homepage document_srl reference

public XE DB
  future publishing target only
  no direct join assumption with old admin DB
```

Important identity rule:
- Use `RESEARCH_NO` as the posting/research key.
- Use `RESEARCH_NO + RESEARCH_APP_SEQ` as the applicant key.
- Use `BLACKLIST_NO` as the blacklist key.
- Treat `document_srl` as optional supplemental metadata only.

Current architecture gap:
The existing implementation still uses `xe_documents`, `admin_job_meta`,
`admin_job_application`, and `admin_blacklist` as operational sources in many
flows. These should be migrated gradually, without removing current features, so
that the old admin tables become the read/write source of truth.

Transition principle:
Add old-admin-table-backed mappers/services beside the existing implementation,
move screens and workflows one by one, and only remove legacy-new-admin paths in
a later cleanup phase.

First transition slice:
- `oldAdminDataSource` reads the copied old admin DB.
- `ResearchMasterMapper` reads `TB_RESEARCH_MST`.
- `/research` provides a read-only list for validating old posting data before
  replacing the existing `/jobs` workflow.
- `/research/{researchNo}` provides an edit view for old posting fields.
- `POST /research/{researchNo}` writes a supplemental `admin_legacy_revision_log`
  row before updating `TB_RESEARCH_MST`.
- `/research/{researchNo}/applications` reads applicants from `TB_RESEARCH_APP`
  by `RESEARCH_NO` without relying on `document_srl`.
- Applicant search for `/research/{researchNo}/applications` filters old fields
  directly, including name, sex, birth, age, job, company/school, phones,
  address, additional comment, attendance, and provide status.
- `/research/{researchNo}/applications/{researchAppSeq}` reads a single old
  applicant row by `RESEARCH_NO + RESEARCH_APP_SEQ`.
- `POST /research/{researchNo}/applications/{researchAppSeq}/provide` updates
  only `PROVIDE_YN` after writing a revision backup for the old applicant row.
- `/legacy-blacklist` reads and writes `TB_BLACKLIST_MST` without hard delete.
- `POST /legacy-blacklist/{blacklistNo}/status` updates only `BLACK_YN` after
  writing a revision backup for the old blacklist row.

## Board Classification

The admin app classifies managed XE mids before choosing available features.

- JOB boards: `newjob`, `additional`, `fast`, `recruit`
- CONTENT boards: `notice`, `sharing`
- QNA boards: `question`

Board management queries include `notice`, `newjob`, `additional`, `fast`, `recruit`, `sharing`, and `question`.
Applicant, application form, blacklist filtering, export, matching, and mail sending flows are limited to JOB boards.
`page_tjVR38` is excluded because it is outside the managed board set.

## 1. 아키텍처 개요
시스템은 세 부분으로 구성된다.

1. 기존 사용자 사이트(XE, 카페24 0419)
2. 신청 폼(public form)
3. 관리자 백오피스(admin app)

공고는 기존 XE 게시판 구조를 그대로 사용하고,
신청/블랙리스트/발송/로그는 새 관리자 시스템이 담당한다.
신청 폼은 공통 항목 + 공고별 동적 질문 구조로 동작한다.

## 2. 전체 구조

```text
[사용자 브라우저]
  ├─ 기존 XE 공고 목록/상세
  └─ 신청하기 클릭

                ↓

[Admin App / Public Form]
Spring Boot + Thymeleaf + MyBatis + Spring Security

  ├─ auth
  ├─ dashboard
  ├─ job
  ├─ publicform
  ├─ form
  ├─ application
  ├─ blacklist
  ├─ mailing
  ├─ export
  ├─ keyword
  ├─ matching
  ├─ notification
  ├─ log
  ├─ consent
  ├─ scheduler
  ├─ xe
  └─ common

                ↓
   ┌──────────────────────────────┬──────────────────────────────┬──────────────────┐
   │                              │                              │                  │
[xeDataSource]               [adminDataSource]              [SMTP / SMS]       [Scheduler]
0419 XE DB                   Admin DB                       mail/sms sender     예약/배치
xe_modules                   admin_user
xe_documents                 admin_job_meta
xe_files(optional)           admin_job_application
                             admin_form_field
                             admin_form_submission_answer
                             admin_blacklist
                             admin_mail_send_job
                             admin_application_keyword
                             admin_job_keyword
                             admin_keyword_match_target
                             admin_action_log
                             admin_privacy_consent
```

## 3. 데이터소스 전략

### 3.1 xeDataSource
- 대상: 0419 XE DB
- 역할:
  - 공고 게시글 조회
  - 공고 등록/수정/상태변경
  - 게시판(mid) 조회
- 사용 테이블:
  - xe_modules
  - xe_documents
  - xe_files (선택)

### 3.2 adminDataSource
- 대상: 관리자 전용 DB 또는 관리자 전용 테이블 영역
- 역할:
  - 신청자 저장
  - 동적 질문 정의/답변 저장
  - 블랙리스트
  - 자동발송
  - 다운로드 로그
  - 키워드 매칭/알림
  - 작업 로그
  - 개인정보 동의

## 4. 모듈 설명
### auth
- 관리자 로그인
- 세션 유지
- 비밀번호 변경
- 로그인 실패 횟수 제한
- 세션 타임아웃

### dashboard
- 오늘 신청 수
- 모집중 공고 수
- 자동발송 예정
- 최근 로그 요약
- 키워드 매칭 후보 수

### job
- 공고 목록/등록/수정/상태변경
- XE 게시글 연동
- 공고 메타 관리
- 거래자 정보 관리(공고 단위)

### publicform
- 사용자 신청 폼
- 공고 상태/신청 가능 여부 확인
- 중복 신청 검사
- 블랙리스트 1차 검사
- CAPTCHA/레이트리밋
- 동의 저장
- 신청 완료 화면

### form
- 공고별 동적 질문 정의
- 질문 타입/순서/필수 여부 관리
- 질문 옵션 저장

### application
- 신청자 목록
- 공고별 조회
- 신규/기존 구분
- 발송 상태/제공 여부 관리
- 질문별 답변 조회
- 공고 단위 자동 분리

### blacklist
- 블랙 등록/해제
- 매칭/제외 로직
- 블랙 처리 로그
- 표시 후 자동삭제 / 완전 자동삭제 / 표시 유지 정책 적용

### mailing
- 자동발송
- 예약발송
- 수동발송
- 템플릿 관리
- 발송 결과 저장

### export
- 공고별 질문 정의를 기준으로 동적 xlsx/txt 생성
- 다운로드 기록 저장

### keyword
- 신청자의 추가설명/답변에서 키워드 추출
- 공고 제목/본문/메타에서 키워드 추출

### matching
- 신청자 관심 키워드와 공고 키워드 매칭
- 후보 사용자 생성

### notification
- 키워드 기반 이메일/문자 알림
- 중복 발송 방지
- 알림 로그 저장

### log
- 작업 로그
- 검색 로그
- 발송 로그
- 블랙 처리 로그

### consent
- 동의 버전 관리
- 신청 동의 이력 저장
- 키워드 재알림 동의 저장

### scheduler
- 예약발송 실행
- 인원 도달 발송 확인
- 6개월 삭제 정책
- 블랙 만료 처리
- 키워드 매칭 배치 실행

### xe
- XE 게시판 연동 전용 서비스/매퍼
- 공고 게시판만 담당

## 5. UI 구조

### 5.1 관리자 화면
- 상단 메뉴: 게시글 / 지원자 / 블랙리스트 / 기간조회 / 검색발송 / 로그 / 설정
- 좌측 필터: 상태(모집중/마감/대기), 날짜, 검색창, 빠른 기간 선택
- 중앙 리스트: 게시글·지원자·검색 결과 표시, 정렬 및 선택
- 우측 패널: 세부정보 / 수정 / 발송 / 삭제 버튼
- 하단 상태창: 자동발송·검색발송 로그, 진행상태, 최근 성공/실패 알림

### 5.2 사용자 신청 폼 화면
- /apply/{documentSrl}
- 공고 요약 표시
- 공통 항목 표시
- 공고별 추가 질문 동적 렌더링
- 개인정보 동의 / 재알림 동의
- 제출
- 결과 화면

## 6. 자동발송 아키텍처
### 6.1 인원 도달 발송
- 신청 저장 후 유효 신청자 수 계산
- 임계값 도달 시 발송 job 생성
- 블랙리스트 재검사 후 발송

### 6.2 예약 시간 발송
- scheduler가 발송 시각 도달 공고 조회
- 블랙리스트 재검사
- 대상 snapshot 생성
- 동적 xlsx/txt 생성 후 발송

### 6.3 중복 방지
- send_job 단위 상태 관리
- duplicate_prevent_key 사용
- 이미 발송된 대상은 delivery_status로 구분

## 7. 동적 폼 다운로드 문제 해결
### 문제 1: 공고마다 질문이 다름
- 해결: `admin_form_field` 로 공고별 질문 정의 저장

### 문제 2: 거래자에게 xlsx/txt로 보내야 함
- 해결: `export` 모듈이 공고의 질문 정의를 읽고 동적 컬럼 생성
- 각 신청자의 답변은 `admin_form_submission_answer`에서 질문별로 조회하여 헤더 순서에 맞춰 파일 생성

## 8. 보안 아키텍처
### 8.1 관리자 로그인
- Spring Security
- BCrypt 해시
- CSRF 활성화
- 로그인 실패 횟수 제한
- 일정 횟수 초과 시 잠금 또는 지연
- 세션 고정 방지
- 세션 타임아웃
- 관리자 작업/로그인 로그 저장
- 선택적으로 IP 허용 목록 및 2차 인증 확장 가능

### 8.2 신청 폼
- 서버측 검증
- XSS 방지
- MyBatis 바인딩으로 SQL Injection 방지
- CAPTCHA(hCaptcha/reCAPTCHA) 또는 대체 봇 방지
- IP/공고 기준 레이트리밋
- 중복 제출 방지 토큰
- 개인정보 동의 없으면 저장 금지

### 8.3 개인정보
- 휴대폰/이메일/주소 암호화 저장 권장
- 화면 표시 시 마스킹
- 다운로드/발송 이력 기록
- 환경변수/시크릿 분리

## 9. 운영 원칙
- XE 운영 DB는 최소 침습
- 공고만 XE에 반영
- 신청/운영 데이터는 admin 영역에서 독립 관리
- 개발/테스트는 운영 DB 직접 변경 없이 진행
