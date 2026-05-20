# DB-SCHEMA.md

## Primary Old Admin Tables

### `TB_RESEARCH_MST`

Focus group/survey master table.

Important columns:

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

### `TB_RESEARCH_APP`

Applicant table.

Important columns:

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

`PROVIDE_YN` means applicant information was provided to the client by email. It is not a payment/deposit flag.

### `TB_BLACKLIST_MST`

Blacklist table.

Important columns:

- `BLACKLIST_NO`
- `BLACK_USER_BIRTH`
- `BLACK_USER_NAME`
- `BLACK_USER_CONTACT`
- `BLACK_USER_COMMENT`
- `BLACK_YN`
- `REGIST_DT`
- `MODIFY_DT`

## Supplemental Tables To Keep

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

Mail/export/notification logs use `research_no`.
