# 작업 현황

이 문서는 Researchi Admin의 구현 상태와 남은 개선 포인트를 정리합니다.

## 완료된 작업

- [x] old-admin DB first 아키텍처 확정
- [x] `TB_RESEARCH_MST` 기반 리서치/좌담회 공고 관리
- [x] `TB_RESEARCH_APP` 기반 신청자 관리
- [x] `TB_BLACKLIST_MST` 기반 블랙리스트 관리
- [x] `/research/{researchNo}/apply` 공개 신청 폼 구현
- [x] `RESEARCH_NO` 기준 메일, 예약 메일, 임계치 메일, export, SMS, matching 연결
- [x] 고객사/담당자 관리를 기존 public-board 데이터와 분리
- [x] 월별 메일/SMS 사용량과 예상 비용 대시보드 구현
- [x] retired public-board/new-admin route와 테이블 흐름을 active code에서 제거
- [x] local log 테이블에서 `document_srl` 호환 컬럼 제거
- [x] 기존 메일 템플릿 변수 `documentSrl` 제거
- [x] 불필요한 cleanup/migration SQL 제거
- [x] 메일/SMS simulation mode 기반 안전 발송 구조 정리
- [x] 관리자 액션, 검색, 메일, SMS, export 로그 구성

## 남은 작업

- [ ] production-only retired table이 실제 운영 DB에 남아 있는지 별도 확인
- [ ] 삭제된 모듈의 빈 디렉터리가 IDE에 남아 있으면 정리
- [ ] 수동 게시 메모용 `public_document_srl` 유지 여부 결정
- [ ] `ADD_COMMENT` 질문/답변 파싱을 구조화 데이터로 전환할지 검토
- [ ] 대용량 검색 쿼리의 실행 계획과 인덱스 전략 문서화
- [ ] 대량 export에 streaming 방식 적용 검토
- [ ] 관리자 권한 등급, 2FA, IP 제한 등 보안 강화 검토

## 품질 평가

현재 구현은 레거시 DB와 운영 흐름을 유지하면서 관리자 기능을 통합하는 데 초점을 맞췄습니다.

추가 개선은 성능 검증, 보안 강화, 테스트 범위 확대, 대량 export 최적화에 집중하는 것이 좋습니다.
