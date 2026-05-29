# DB 스키마 문서

## 설계 원칙

Researchi Admin은 기존 운영 DB를 source of truth로 유지합니다.

신규 관리자 기능에 필요한 로그, 메일, SMS, 고객사, export, 매칭 데이터는 별도 `admin_*` 테이블에 저장합니다. 이를 통해 기존 약 400만 건 규모의 운영 데이터를 보존하면서 신규 기능을 확장할 수 있습니다.

## 기존 운영 DB 핵심 테이블

### `TB_RESEARCH_MST`

리서치/좌담회 공고 마스터 테이블입니다.

주요 컬럼:

- `RESEARCH_NO`: 공고 식별자
- `RESEARCH_TITLE`: 공고 제목
- `RESEARCH_CONTENTS`: 공고 내용
- `ADD_COMMENT`: 추가 안내/질문 내용
- `APP_CNT`: 전체 신청 수
- `APP_NEW_CNT`: 신규 신청 수
- `COMPANY_NAME`: 고객사명
- `SERVER_NAME`: 담당/서버 구분
- `CONTACT_NO`: 연락처
- `BROKERAGE_AMT`: 중개 금액
- `CALCULATE_YN`: 정산 여부
- `REMARK`: 내부 메모
- `REGIST_DT`: 등록일
- `MODIFY_DT`: 수정일
- `CLOSE_DATE`: 마감일

### `TB_RESEARCH_APP`

신청자 정보 테이블입니다.

주요 컬럼:

- `RESEARCH_NO`: 공고 식별자
- `RESEARCH_APP_SEQ`: 공고별 신청자 순번
- `APP_NAME`: 신청자 이름
- `APP_SEX`: 성별
- `APP_BIRTH`: 생년월일
- `APP_AGE`: 나이
- `APP_JOB`: 직업
- `APP_COMPANY`: 회사
- `APP_HPHONE`: 휴대폰 번호
- `APP_TELE`: 전화번호
- `APP_ADDR`: 주소
- `ADD_COMMENT`: 추가 응답
- `ATTEND_RESEARCH`: 참석 여부
- `PROVIDE_YN`: 고객사 제공 여부
- `REGIST_DT`: 등록일
- `MODIFY_DT`: 수정일

`PROVIDE_YN`은 결제/입금 여부가 아니라, 신청자 정보가 고객사에 제공되었는지를 의미합니다.

### `TB_BLACKLIST_MST`

블랙리스트 테이블입니다.

주요 컬럼:

- `BLACKLIST_NO`: 블랙리스트 식별자
- `BLACK_USER_BIRTH`: 생년월일
- `BLACK_USER_NAME`: 이름
- `BLACK_USER_CONTACT`: 연락처
- `BLACK_USER_COMMENT`: 사유/메모
- `BLACK_YN`: 활성 여부
- `REGIST_DT`: 등록일
- `MODIFY_DT`: 수정일

## 유지해야 하는 보조 테이블

- `admin_user`
- `admin_action_log`
- `admin_search_log`
- `admin_client`
- `admin_client_contact`
- `admin_research_client_link`
- `admin_mail_template`
- `admin_mail_send_job`
- `admin_mail_send_target`
- `admin_legacy_mail_rule`
- `admin_legacy_mail_rule_item`
- `admin_legacy_revision_log`
- `admin_legacy_application_extra_answer`
- `admin_legacy_application_keyword`
- `admin_legacy_matching_job`
- `admin_legacy_matching_result`
- `admin_legacy_matching_index_job`
- `admin_manual_publish_log`
- `admin_export_log`
- `admin_notification_log`

메일, export, SMS 알림 로그는 `research_no`를 기준으로 저장합니다.

## 품질 및 개선 포인트

- 자주 조회하는 `RESEARCH_NO`, `RESEARCH_APP_SEQ`, `BLACKLIST_NO` 기준 조회 성능을 우선 고려합니다.
- 검색 조건이 많은 화면은 실행 계획과 인덱스 전략을 별도 점검하는 것이 좋습니다.
- 개인정보가 포함된 테이블은 export/log 조회 권한과 마스킹 정책을 강화할 여지가 있습니다.
