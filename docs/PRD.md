# PRD.md

## Board Scope Update

Managed XE board mids:
- `notice`: notice/content board
- `newjob`: job/application board
- `additional`: job/application board
- `fast`: job/application board
- `recruit`: job/application board
- `sharing`: content board
- `question`: Q&A board

Applicant/application features are enabled only for `newjob`, `additional`, `fast`, and `recruit`.
The `notice`, `sharing`, and `question` boards are managed as normal website board posts only.
The `page_tjVR38` mid is excluded for now because it is not part of the managed board scope.

## 1. 프로젝트 개요
Researchi 관리자용 통합 시스템은 기존 XE 기반 사용자 사이트와 연동되는 독립 관리자 백오피스다.
관리자 1명이 공고 등록, 신청자 관리, 블랙리스트 관리, 자동발송, 검색발송, 로그 조회를 한 곳에서 처리한다.
또한 공고 상세에서 사용자가 진입하는 신청 폼을 직접 제공하고, 공고마다 다른 추가 질문을 동적으로 구성한다.

## 2. 목표
1. 기존 사용자 사이트 공고를 XE 구조에 맞춰 즉시 반영
2. 사용자가 신청하기 버튼으로 진입하는 신청 폼 제공
3. 공고마다 다른 추가 질문을 설정하고 항목별 답변을 저장
4. 신청 데이터를 체계적으로 저장/검색/발송
5. 블랙리스트 자동 제외 및 자동발송 지원
6. 관심 키워드 기반 재알림 지원
7. 6개월 보존 정책과 개인정보 동의 이력 관리
8. 관리자 로그인과 신청 폼에 강한 보안 적용

## 3. 사용자 구분
### 3.1 관리자
- 1명 전용
- 단일 로그인 계정
- 모든 주요 작업 로그 저장

### 3.2 일반 사용자
- 기존 XE 사용자 사이트에서 공고 열람
- 공고 상세에서 신청하기 버튼 클릭
- 신청 폼 작성 및 제출

## 4. 핵심 기능
### 4.1 공고 관리
- 신규일감 / 추가일감 구분
- 공고 등록 / 수정 / 상태 변경
- 공고 메타 정보 저장
- 거래자(의뢰인) 이메일 저장
- 자동발송 정책 저장

### 4.2 신청 폼
- 공고별 신청 폼 노출
- 공통 항목 + 공고별 추가 질문
- 질문 타입(TEXT, TEXTAREA, RADIO, CHECKBOX, SELECT, NUMBER, DATE) 지원
- 중복 신청 검사
- 블랙리스트 1차 검사
- 신청 완료/중복/제한 안내

### 4.3 지원자 관리
- 공고별 신청자 조회
- 신규 / 기존 구분
- 상태 표시
- 엑셀 / 텍스트 다운로드
- 제공 여부 관리
- 공고별 동적 질문 답변 조회
- 게시글 단위 자동 분리
- 상단 탭 또는 검색창으로 특정 공고 이동

### 4.4 블랙리스트
- 등록 / 해제
- 신청 시 표시
- 발송 전 재검사
- 표시 후 자동삭제(기본)
- 완전 자동삭제
- 표시 유지

### 4.5 자동발송
- 인원 수 도달 시 자동발송
- 예약 시간 자동발송
- 둘 다 사용 가능
- 블랙리스트 제외 후 거래자에게 발송
- 첨부파일(xlsx/txt) 자동 생성
- 발송 로그 기록

### 4.6 동적 다운로드
- 공고별 질문 정의를 기준으로 동적 컬럼 생성
- 거래자에게 보내는 xlsx/txt는 공고별 질문 순서를 반영
- 공고마다 다른 질문이어도 전달 파일 생성 가능

### 4.7 관심 키워드 재알림
- 신청자의 추가설명/답변에서 관심 키워드 추출
- 새 공고의 제목/본문/메타에서 공고 키워드 추출
- 매칭된 사용자에게 이메일/문자 알림
- 동의한 사용자에게만 발송
- 중복 발송 방지 및 로그 저장

### 4.8 검색 / 기간조회
- 이름 / 연락처 / 지역 / 연령 / 직업 / 공고명 / 신청일 기반 검색
- 기간별 게시글 / 신청자 / 발송 / 블랙 로그 조회
- 등록일 / 신청일 / 발송일 기준 검색
- 오늘 / 이번주 / 특정월 / 직접입력
- 검색 결과 대상 발송 지원

### 4.9 보안 / 보존정책
- 관리자 로그인
- HTTPS 전제
- 비밀번호 해시 저장(BCrypt)
- 로그인 실패 횟수 제한
- 세션 타임아웃
- CSRF 방어
- 신청 폼 CAPTCHA/레이트리밋
- 개인정보 암호화 저장 및 마스킹 표시
- 개인정보 동의 이력 저장
- 6개월 경과 데이터 자동 정리 정책 지원

## 5. 비기능 요구사항
- 운영 중인 XE 사이트에 최소 침습
- 공고는 XE DB 연동
- 신청/블랙리스트/로그/발송은 관리자 전용 테이블 사용
- Spring Boot + Thymeleaf + MyBatis + MariaDB/MySQL
- Codex 에이전트가 단계적으로 구현 가능해야 함

## Frontend tasks

When doing frontend design tasks, avoid generic, overbuilt layouts.

**Use these hard rules:**
- One composition: The first viewport must read as one composition, not a dashboard (unless it's a dashboard).
- Brand first: On branded pages, the brand or product name must be a hero-level signal, not just nav text or an eyebrow. No headline should overpower the brand.
- Brand test: If the first viewport could belong to another brand after removing the nav, the branding is too weak.
- Typography: Use expressive, purposeful fonts and avoid default stacks (Inter, Roboto, Arial, system).
- Background: Don't rely on flat, single-color backgrounds; use gradients, images, or subtle patterns to build atmosphere.
- Full-bleed hero only: On landing pages and promotional surfaces, the hero image should be a dominant edge-to-edge visual plane or background by default. Do not use inset hero images, side-panel hero images, rounded media cards, tiled collages, or floating image blocks unless the existing design system clearly requires it.
- Hero budget: The first viewport should usually contain only the brand, one headline, one short supporting sentence, one CTA group, and one dominant image. Do not place stats, schedules, event listings, address blocks, promos, "this week" callouts, metadata rows, or secondary marketing content in the first viewport.
- No hero overlays: Do not place detached labels, floating badges, promo stickers, info chips, or callout boxes on top of hero media.
- Cards: Default: no cards. Never use cards in the hero. Cards are allowed only when they are the container for a user interaction. If removing a border, shadow, background, or radius does not hurt interaction or understanding, it should not be a card.
- One job per section: Each section should have one purpose, one headline, and usually one short supporting sentence.
- Real visual anchor: Imagery should show the product, place, atmosphere, or context. Decorative gradients and abstract backgrounds do not count as the main visual idea.
- Reduce clutter: Avoid pill clusters, stat strips, icon rows, boxed promos, schedule snippets, and multiple competing text blocks.
- Use motion to create presence and hierarchy, not noise. Ship at least 2-3 intentional motions for visually led work.
- Color & Look: Choose a clear visual direction; define CSS variables; avoid purple-on-white defaults. No purple bias or dark mode bias.
- Ensure the page loads properly on both desktop and mobile.
- For React code, prefer modern patterns including useEffectEvent, startTransition, and useDeferredValue when appropriate if used by the team. Do not add useMemo/useCallback by default unless already used; follow the repo's React Compiler guidance.

Exception: If working within an existing website or design system, preserve the established patterns, structure, and visual language.
