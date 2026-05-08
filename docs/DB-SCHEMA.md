# DB-SCHEMA.md

## Primary Old Admin DB Schema

The copied old admin DB (`admin_copy`) is the source of truth for current-phase
manager data.

### TB_RESEARCH_MST

Purpose: posting/research master data.

Verified table:
- physical name: `tb_research_mst`
- engine: MyISAM
- collation: `utf8mb4_general_ci`
- row count observed locally: about 46,260
- local character set variables:
  - `character_set_client`: `utf8mb4`
  - `character_set_connection`: `utf8mb4`
  - `character_set_database`: `utf8mb4`
  - `character_set_filesystem`: `binary`
  - `character_set_results`: blank
  - `character_set_server`: `utf8mb4`
  - `character_set_system`: `utf8mb3`

Key columns:
- `RESEARCH_NO`
- `RESEARCH_TITLE`
- `RESEARCH_CONTENTS`
- `ADD_COMMENT`
- `APP_CNT`
- `APP_NEW_CNT`
- `COMPANY_NAME`
- `SERVER_NAME`
- `CONTACT_NO`
- `BROKERAGE_AMT`
- `CALCULATE_YN`
- `REMARK`
- `REGIST_DT`
- `MODIFY_DT`
- `CLOSE_DATE`

Current observed index:
- `KEY RESEARCH_NO (RESEARCH_NO)`

Observed value note:
- `BROKERAGE_AMT` may contain comma-formatted strings such as `10,000`, so map
  it as text unless the old DB column shape and all historical values are
  normalized later.

### TB_RESEARCH_APP

Purpose: applicant data.

Verified table:
- physical name: `tb_research_app`
- engine: MyISAM
- collation: `utf8mb4_general_ci`
- row count observed locally: about 4,120,280

Key columns:
- `RESEARCH_NO`
- `RESEARCH_APP_SEQ`
- `APP_NAME`
- `APP_SEX`
- `APP_BIRTH`
- `APP_AGE`
- `APP_JOB`
- `APP_COMPANY`
- `APP_HPHONE`
- `APP_TELE`
- `APP_ADDR`
- `ADD_COMMENT`
- `ATTEND_RESEARCH`
- `PROVIDE_YN`
- `REGIST_DT`
- `MODIFY_DT`

Current observed index:
- `KEY RESEARCH_NO (RESEARCH_NO, RESEARCH_APP_SEQ)`

Current implementation:
- `ResearchApplication` maps `TB_RESEARCH_APP` values mostly as text to preserve
  old DB values.
- `/research/{researchNo}/applications` reads applicants by `RESEARCH_NO` with
  keyword search and pagination.

Observed `PROVIDE_YN` distribution:
- `Y`: about 4,106,840
- `N`: about 13,440

Business meaning:
- `PROVIDE_YN` is manually changed by an administrator.
- It indicates whether the trader/client has deposited the money.
- Mail/export filtering must not treat this as a generic delivery status.

### TB_BLACKLIST_MST

Purpose: blacklist master data.

Verified table:
- physical name: `tb_blacklist_mst`
- engine: MyISAM
- collation: `utf8mb4_general_ci`
- row count observed locally: about 353

Key columns:
- `BLACKLIST_NO`
- `BLACK_USER_BIRTH`
- `BLACK_USER_NAME`
- `BLACK_USER_CONTACT`
- `BLACK_USER_COMMENT`
- `BLACK_YN`
- `REGIST_DT`
- `MODIFY_DT`

Current observed index:
- `KEY BLACKLIST_NO (BLACKLIST_NO)`

Observed `BLACK_YN` distribution:
- `Y`: about 335
- `N`: about 16
- blank: about 2

Handling rule:
- `Y` means active blacklist.
- `N` means lifted/inactive blacklist.
- Blank values will use the first defined handling rule during migration and can
  be adjusted later after operational review.

Current implementation:
- `Blacklist` maps `TB_BLACKLIST_MST`.
- `/legacy-blacklist` supports list/search/create/update/status changes.
- No hard delete is implemented for `TB_BLACKLIST_MST`.

## Supplemental Admin Tables

The existing newly designed `admin_*` tables are supplemental only. They should
not replace old admin source data.

Keep supplemental tables for:
- audit logs
- mail send jobs
- mail target snapshots
- mail templates and send rules
- dynamic form fields
- export logs
- search logs
- keyword/matching results
- revision backups before old-table updates
- manual publish logs and optional public `document_srl` references

Recommended new supplemental tables for transition:

```text
admin_legacy_revision_log
  id
  legacy_table_name
  legacy_key
  before_json
  action_type
  changed_by
  changed_at

Current implementation creates and uses `admin_legacy_revision_log` before
`TB_RESEARCH_MST` update operations.

admin_manual_publish_log
  id
  research_no
  generated_title
  generated_body
  publish_status
  public_document_srl
  published_by
  published_at
  created_at
  updated_at
```

Do not treat `admin_job_application`, `admin_blacklist`, or `admin_job_meta` as
primary data once the old-admin transition begins. Keep them only as migration
compatibility or supplemental storage until they are retired later.

## Board Configuration

`admin_board_config` is a lightweight admin-side table for board classification.

Columns:
- `xe_mid`
- `board_name`
- `board_type`
- `application_enabled`
- `display_order`
- `active_yn`
- `created_at`
- `updated_at`

Managed board mids: `notice`, `newjob`, `additional`, `fast`, `recruit`, `sharing`, `question`.
Application-enabled board mids: `newjob`, `additional`, `fast`, `recruit`.
Content-only/Q&A board mids: `notice`, `sharing`, `question`.
Excluded mid: `page_tjVR38`.

## 1. 기존 재사용 테이블
### xe_modules
- 공고 게시판 식별(newjob, additional)

### xe_documents
- 실제 공고 게시글 저장

### xe_files (선택)
- 첨부 필요 시 사용

## 2. 신규 관리자 테이블

### admin_user
id, login_id, password_hash, user_name, active_yn, login_fail_count, locked_until, last_login_at, created_at, updated_at

### admin_job_meta
id, document_srl, job_type, reward_text, place_text, age_min, age_max, gender_code, region_text, brand_text, recruit_limit, client_id, client_name, client_email, client_emails, close_date, internal_memo, recruit_status, application_enabled, application_form_notice, auto_send_enabled, auto_send_mode, auto_send_threshold, auto_send_time, auto_send_repeat_yn, auto_send_repeat_unit, auto_send_template_id, auto_send_attachment_type, last_auto_sent_at, next_auto_send_at, created_at, updated_at

### admin_client
id, client_name, department_name, reply_to_email, active_yn, created_at, updated_at

### admin_client_contact
id, client_id, contact_name, email, primary_yn, active_yn, created_at, updated_at

### admin_form_field
id, document_srl, field_key, field_label, field_type, field_order, required_yn, placeholder_text, help_text, options_json, active_yn, created_at, updated_at

### admin_job_application
id, document_srl, applicant_name, gender_code, birth_date, age_text, job_text, organization_text, mobile_phone_enc, mobile_phone_masked, tel_phone_enc, tel_phone_masked, region_text, address_enc, address_masked, extra_comment, prior_research_text, email_address_enc, email_address_masked, notify_email_yn, notify_sms_yn, notify_keyword_yn, application_status, is_new_applicant, is_blacklisted, black_mode_applied, provide_yn, delivery_status, delivered_at, delivery_job_id, applied_at, updated_at

### admin_form_submission_answer
id, application_id, field_id, answer_text, answer_json, created_at, updated_at

### admin_application_duplicate_log
id, document_srl, applicant_name, gender_code, birth_date, mobile_phone_hash, duplicate_found, matched_application_id, checked_at

### admin_blacklist
id, black_name, black_mobile_phone_hash, black_birth_date, black_reason, black_mode, active_yn, created_by, created_at, updated_at, expires_at

### admin_blacklist_match_log
id, application_id, blacklist_id, match_type, action_taken, matched_at

### admin_mail_template
id, template_name, mail_subject, mail_body, active_yn, created_at, updated_at

### admin_mail_send_job
id, document_srl, send_type, trigger_type, template_id, mail_subject_snapshot, mail_body_snapshot, attachment_type, recipient_count, excluded_count, blacklist_excluded_count, send_status, scheduled_at, sent_at, threshold_snapshot, target_snapshot_count, duplicate_prevent_key, created_by, created_at

### admin_mail_send_target
id, send_job_id, application_id, target_email_masked, target_name, send_result, fail_reason, sent_at

### admin_export_log
id, document_srl, export_type, file_name, exported_count, exported_at

### admin_search_log
id, search_type, keyword_text, condition_json, result_count, searched_at

### admin_action_log
id, admin_user_id, action_type, target_type, target_id, action_detail, ip_address, created_at

### admin_privacy_consent
id, application_id, consent_type, consent_version, agreed_yn, ip_address, agreed_at

### admin_application_keyword
id, application_id, keyword, keyword_normalized, source_type, created_at

### admin_job_keyword
id, document_srl, keyword, keyword_normalized, source_type, created_at

### admin_keyword_match_job
id, document_srl, match_status, matched_count, created_at, completed_at

### admin_keyword_match_target
id, match_job_id, application_id, matched_keyword, match_score, notify_email_yn, notify_sms_yn, notify_status, sent_at, fail_reason, created_at

### admin_notification_log
id, application_id, document_srl, channel_type, target_address_masked, keyword_summary, send_status, fail_reason, created_at

## 3. 관계 요약

```text
xe_documents
  1 : 1 admin_job_meta
  1 : N admin_form_field
  1 : N admin_job_application
  1 : N admin_mail_send_job
  1 : N admin_export_log
  1 : N admin_job_keyword
  1 : N admin_keyword_match_job

admin_job_application
  1 : N admin_form_submission_answer
  1 : N admin_blacklist_match_log
  1 : N admin_privacy_consent
  1 : N admin_application_keyword

admin_form_field
  1 : N admin_form_submission_answer

admin_blacklist
  1 : N admin_blacklist_match_log

admin_mail_send_job
  1 : N admin_mail_send_target

admin_client
  1 : N admin_client_contact
  1 : N admin_job_meta

admin_keyword_match_job
  1 : N admin_keyword_match_target

admin_user
  1 : N admin_action_log
```
