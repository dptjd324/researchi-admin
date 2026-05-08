# TASKS.md

## Old Admin DB First Transition
- [x] Verify copied old admin tables exist in `admin_copy`: `TB_RESEARCH_MST`, `TB_RESEARCH_APP`, `TB_BLACKLIST_MST`
- [x] Verify local sample table engine/collation: MyISAM, `utf8mb4_general_ci`
- [x] Confirm local character set variables: client/connection/database/server `utf8mb4`, filesystem `binary`, system `utf8mb3`
- [x] Confirm `PROVIDE_YN` business meaning: admin-managed trader/client deposit status
- [x] Confirm `APP_CNT` and `APP_NEW_CNT` display meaning: total applicants and unverified new applicants
- [ ] Confirm `ADD_COMMENT` parsing rules before deriving structured fields from old free text
- [x] Define first-pass `BLACK_YN` handling: `Y` active, `N` inactive, blanks handled by initial migration rule and revisited later
- [x] Add first read-only legacy domain/mapper/service for `TB_RESEARCH_MST`
- [x] Add first read-only `/research` screen backed by `TB_RESEARCH_MST`
- [x] Add read-only `/research/{researchNo}` detail screen backed by `TB_RESEARCH_MST`
- [x] Convert `/research/{researchNo}` to an edit screen with pre-update revision backup
- [x] Add first read-only legacy domain/mapper/service for `TB_RESEARCH_APP`
- [x] Add read-only `/research/{researchNo}/applications` screen backed by `TB_RESEARCH_APP`
- [x] Add read-only `/research/{researchNo}/applications/{researchAppSeq}` detail screen backed by `TB_RESEARCH_APP`
- [x] Add per-announcement applicant search filters for core `TB_RESEARCH_APP` fields
- [x] Add `PROVIDE_YN` update on applicant detail with pre-update revision backup
- [x] Add first legacy blacklist domain/mapper/service for `TB_BLACKLIST_MST`
- [x] Add `/legacy-blacklist` old DB blacklist list/create/update/status screen
- [ ] Add legacy domain naming around old tables: `ResearchMaster`, `ResearchApplication`, `Blacklist`
- [x] Add MyBatis mappers for `TB_RESEARCH_MST`, `TB_RESEARCH_APP`, and `TB_BLACKLIST_MST`
- [ ] Migrate posting list/detail/create/update to `TB_RESEARCH_MST` first; keep no-delete policy
- [ ] Migrate applicant management to `TB_RESEARCH_APP` with `RESEARCH_NO` search, pagination, and existing filters
- [x] Migrate blacklist management to `TB_BLACKLIST_MST` with active-status updates and no hard delete
- [x] Add pre-update revision backup logs before modifying old tables because rollback cannot be assumed
- [ ] Add manual homepage publish view that generates copy-ready title/body from `TB_RESEARCH_MST`
- [ ] Add manual publish log in supplemental admin tables, with optional public `document_srl` recording
- [ ] Reconnect mail/send/export flows to old-admin keys while preserving existing send logs and snapshots
- [ ] Keep XE auto-publishing out of the current phase

## Board Scope Correction
- [x] Include managed XE board mids: `notice`, `newjob`, `additional`, `fast`, `recruit`, `sharing`, `question`
- [x] Keep applicant/application features limited to: `newjob`, `additional`, `fast`, `recruit`
- [x] Add board classification for JOB, CONTENT, and QNA boards
- [x] Add lightweight `admin_board_config` schema support
- [x] Hide application form, applicant, matching, and mail controls for `notice`, `sharing`, and `question`
- [x] Keep `page_tjVR38` excluded

## Phase 1. 프로젝트 부트스트랩
- [x] Spring Boot 프로젝트 초기화
- [x] Gradle 설정
- [x] Java 17 설정
- [x] Thymeleaf 설정
- [x] MyBatis 설정
- [x] `xeDataSource` / `adminDataSource` 설정
- [x] 공통 레이아웃 생성

## Phase 2. 인증 / 세션 / 보안
- [x] 관리자 로그인 페이지
- [x] 로그인 처리
- [x] BCrypt 지원
- [x] 세션 체크 필터 또는 Spring Security 설정
- [x] 로그아웃
- [x] 비밀번호 변경
- [x] 로그인 실패 횟수 / 잠금 처리
- [x] CSRF 활성화
- [x] 세션 타임아웃
- [x] 로그인 / 로그아웃 액션 로그 저장

## Phase 3. 공고 관리
- [x] XE 공고 조회 서비스
- [x] 신규 공고 / 추가 공고 목록 페이지
- [x] 공고 등록 페이지
- [x] 공고 수정 페이지
- [x] 공고 상태 변경
- [x] `admin_job_meta` insert / update
- [x] 공고 액션 로그 저장

## Phase 4. 동적 폼 항목
- [x] `admin_form_field` CRUD
- [x] 질문 순서 / 필수 여부 관리
- [x] 공고별 질문 설정 페이지

## Phase 5. 공개 지원 폼
- [x] `GET /apply/{documentSrl}`
- [x] 공고 상태 / 지원 가능 여부 검증
- [x] 공통 필드 + 동적 질문 렌더링
- [x] 입력값 검증
- [x] CSRF / anti-bot / CAPTCHA
- [x] 개인정보 마스킹
- [x] 중복 지원 검사
- [x] 1차 블랙리스트 검사
- [x] 개인정보 보호 / 마스킹 / 암호화 저장
- [x] 지원서 저장
- [x] 질문 답변 저장
- [x] 동의 이력 저장
- [x] 완료 / 중복 / 차단 결과 페이지

## Phase 6. 지원자 관리
- [x] 공고별 지원자 목록
- [x] 전체 지원자 목록
- [x] 신규 / 기존 지원자 구분
- [x] 발송 상태 표시
- [x] 상태 변경
- [x] 지원 상세 + 동적 답변 조회
- [x] 공고 단위 분리 UI
- [x] 상단 빠른 검색 네비게이션

## Phase 7. 블랙리스트
- [x] 블랙리스트 목록
- [x] 등록 / 수정 / 활성화 토글
- [x] 지원 시 즉시 반영
- [x] 즉시 차단 / 임시 차단 / 수동 검토 모드
- [x] 블랙리스트 매칭 로그 저장
- [x] 블랙리스트 액션 로그 저장

## Phase 8. 동적 다운로드
- [x] 공고별 필드 정의 조회
- [x] 질문 순서 기준 XLSX 컬럼 생성
- [x] 질문 순서 기준 TXT 생성
- [x] 공고 단위 다운로드
- [x] 다운로드 이력 저장

## Phase 9. 수동 / 자동 메일 발송
- [x] 메일 템플릿 CRUD
- [x] 수동 발송
- [x] 예약 발송
- [x] 임계치 도달 시 발송
- [x] 발송 시 블랙리스트 제외
- [x] 발송 스냅샷 저장
- [x] 발송 결과 저장
- [x] `provide` / `delivery` 상태 갱신

## Phase 10. 키워드 알림
- [x] 지원자 키워드 추출
- [x] 공고 키워드 추출
- [x] 매칭 공고 생성
- [x] 알림 이력 조회
- [x] 관리자 확인 후 메일 발송
- [x] 자동 후속 메일 발송(2차 발송)
- [x] SMS 확장 시나리오 지원

## Phase 11. 검색 / 기간 조회 / 로그 / UI
- [x] 기간 검색 페이지
- [x] 등록일 / 지원일 / 발송일 필터
- [x] 오늘 / 이번 주 / 특정일 / 직접 입력
- [x] 다중 조건 검색
- [x] 검색 결과 발송
- [x] 액션 로그 페이지
- [x] 메일 로그 페이지
- [x] 검색 로그 페이지
- [x] 알림 로그 페이지
- [x] 하단 상태 바
- [x] Phase 11 항목별 검증 완료 (2026-04-16)

## Phase 12. 배치 / 운영
- [x] 예약 발송 스케줄러
- [x] 임계치 검증 스케줄러
- [x] 6개월 정리 스케줄러
- [x] 블랙리스트 만료 처리
- [x] 키워드 매칭 배치
- [x] Phase 12 항목별 검증 완료 (2026-04-16)
