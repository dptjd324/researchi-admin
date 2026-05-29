# API/Route 명세

이 문서는 Researchi Admin의 active route를 정리합니다.

관리자 화면은 서버 렌더링 기반이며, 핵심 식별자는 `RESEARCH_NO`입니다. 공고, 신청자, 메일, SMS, export, 고객사 연결 흐름이 같은 키를 사용하도록 설계했습니다.

## 인증

- `GET /login`: 로그인 화면
- `POST /login`: 로그인 처리
- `POST /logout`: 로그아웃
- `GET /account/password`: 비밀번호 변경 화면
- `POST /account/password`: 비밀번호 변경 처리

## 대시보드

- `GET /`: 대시보드 진입
- `GET /dashboard`: 월별 메일/SMS 사용량과 운영 요약

## 리서치/좌담회 공고

- `GET /research`: 공고 목록
- `GET /research/new`: 공고 등록 화면
- `POST /research`: 공고 등록
- `GET /research/{researchNo}`: 공고 상세
- `POST /research/{researchNo}`: 공고 수정
- `GET /research/{researchNo}/publish-copy`: 홈페이지 게시용 복사 문구
- `POST /research/{researchNo}/manual-publish-log`: 수동 게시 이력 저장

## 신청자 관리

- `GET /research/{researchNo}/applications`: 공고별 신청자 목록
- `GET /research/{researchNo}/applications/{researchAppSeq}`: 신청자 상세
- `POST /research/{researchNo}/applications/{researchAppSeq}/provide`: 개별 제공 처리
- `GET /research/{researchNo}/applications/provide-preview`: 제공 대상 미리보기
- `POST /research/{researchNo}/applications/provide-complete`: 제공 완료 처리
- `GET /research/{researchNo}/applications/duplicates`: 중복 신청자 확인

## 공개 신청 폼

- `GET /research/{researchNo}/apply`: 공개 신청 화면
- `POST /research/{researchNo}/apply`: 신청 제출
- `GET /research/{researchNo}/apply/complete`: 신청 완료 화면

## 블랙리스트

- `GET /legacy-blacklist`: 블랙리스트 목록
- `GET /legacy-blacklist/new`: 블랙리스트 등록 화면
- `GET /legacy-blacklist/{blacklistNo}/edit`: 블랙리스트 수정 화면
- `POST /legacy-blacklist`: 등록/수정 처리
- `POST /legacy-blacklist/{blacklistNo}/status`: 상태 변경

## Export

- `POST /research/{researchNo}/export/xlsx`: 전체 신청자 XLSX export
- `POST /research/{researchNo}/export/provide-xlsx`: 제공 대상 XLSX export
- `POST /research/{researchNo}/export/txt`: 전체 신청자 TXT export
- `POST /research/{researchNo}/export/provide-txt`: 제공 대상 TXT export

## 메일

- `GET /research/{researchNo}/mail`: 메일 화면
- `POST /research/{researchNo}/mail`: 수동 메일 발송
- `POST /research/{researchNo}/mail/schedule`: 예약 메일 등록
- `POST /research/{researchNo}/mail/scheduled/{sendJobId}/cancel`: 예약 발송 취소
- `POST /research/{researchNo}/mail/threshold-settings`: 임계치 발송 설정
- `POST /research/{researchNo}/mail/threshold-cancel`: 임계치 발송 취소
- `POST /research/{researchNo}/mail/threshold-trigger`: 임계치 발송 수동 실행
- `POST /research/{researchNo}/mail/threshold-rules`: 임계치 규칙 저장
- `POST /research/{researchNo}/mail/threshold-rules/{ruleId}/trigger`: 특정 규칙 실행
- `POST /research/{researchNo}/mail/threshold-rules/{ruleId}/delete`: 특정 규칙 삭제

## 매칭/SMS

- `GET /research/{researchNo}/matching`: 키워드 매칭 화면
- `POST /research/{researchNo}/matching/run-window`: 매칭 실행
- `POST /research/{researchNo}/matching/refresh`: 매칭 결과 새로고침
- `POST /research/{researchNo}/matching/sms`: SMS 발송
- `GET /research/{researchNo}/matching/history`: 매칭 이력

## 고객사

- `GET /clients`: 고객사 목록
- `POST /clients`: 고객사 등록/수정
- `POST /clients/{clientId}/delete`: 고객사 삭제

## 로그

- `GET /logs/actions`: 관리자 액션 로그
- `GET /logs/mail`: 메일 로그
- `GET /logs/search`: 검색 로그
- `GET /logs/notifications`: SMS 알림 로그

## Route 설계 원칙

- 리서치/좌담회 흐름은 `RESEARCH_NO`를 기준으로 연결합니다.
- 개인정보 처리, export, 메일/SMS 발송처럼 운영 리스크가 있는 route는 로그를 남깁니다.
- 공개 신청 폼은 관리자 인증이 필요 없는 사용자 진입점이므로 입력 검증과 중복 방지를 강화합니다.
