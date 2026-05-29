# src 디렉터리 구조

이 애플리케이션은 기존 운영 DB를 우선으로 사용하는 **old-admin-DB-first** 구조입니다.

핵심 라우트는 모두 `RESEARCH_NO`를 기준으로 연결됩니다.

- `/research`: 리서치/좌담회 공고 목록
- `/research/{researchNo}`: 공고 상세 및 수정
- `/research/{researchNo}/applications`: 공고별 신청자 관리
- `/research/{researchNo}/apply`: 공개 신청 폼

## 주요 패키지

- `auth`: 관리자 인증, 세션, 로그인/로그아웃 로그
- `legacy.research`: 기존 `TB_RESEARCH_MST` 기반 공고 관리
- `legacy.application`: 기존 `TB_RESEARCH_APP` 기반 신청 폼과 신청자 저장
- `legacy.blacklist`: 기존 `TB_BLACKLIST_MST` 기반 블랙리스트 관리
- `legacy.matching`: 신청자 키워드 매칭과 SMS 알림
- `client`: 고객사와 담당자 관리
- `mailing`: 메일 템플릿, 발송 작업, 발송 대상 관리
- `notification`: SMS Gateway와 알림 로그
- `dashboard`: 월별 메일/SMS 사용량과 비용 요약
- `export`: XLSX/TXT 내보내기
- `log`: 액션, 검색, 메일, SMS, export 로그 조회

## 설계 의도

기존 운영 테이블을 무리하게 새 도메인 모델로 바꾸지 않고, 레거시 테이블은 그대로 source of truth로 유지했습니다. 신규 관리자 기능에 필요한 고객사, 메일, SMS, export, 매칭, 로그 데이터만 보조 `admin_*` 테이블에 저장합니다.

이 구조를 통해 약 400만 건 규모의 기존 데이터를 유지하면서도 신규 백오피스 기능을 점진적으로 확장할 수 있도록 했습니다.
