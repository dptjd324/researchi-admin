# ARCHITECTURE.md

## Current Architecture

The admin program is old-admin-DB-first.

Primary data source:

- `TB_RESEARCH_MST`: focus group/survey master data
- `TB_RESEARCH_APP`: applicant data
- `TB_BLACKLIST_MST`: blacklist data

Supplemental admin tables remain only for features the old DB does not provide:

- admin accounts and action logs
- client/contact management
- mail templates, send jobs, send targets, threshold rules
- SMS notification logs
- export/search logs
- matching index/result/history
- revision backups before old-table updates
- manual publish copy history

## Key Identity Rules

- Focus group/survey key: `RESEARCH_NO`
- Applicant key: `RESEARCH_NO + RESEARCH_APP_SEQ`
- Blacklist key: `BLACKLIST_NO`

Supplemental mail/export/notification logs use `research_no`. The old `document_srl` compatibility columns have been removed from the local schema and are no longer written by the application.

## Active Modules

- `legacy.research`: `TB_RESEARCH_MST` list/detail/edit, applicant lookup, mail, export, manual publish copy
- `legacy.application`: public application form backed by `TB_RESEARCH_APP`
- `legacy.blacklist`: `TB_BLACKLIST_MST` list/create/update/status
- `legacy.matching`: applicant keyword matching and SMS notification
- `client`: client/contact registration and `RESEARCH_NO` client link
- `mailing`: shared mail template/job/target storage
- `notification`: SMS gateway and notification logs
- `auth`, `log`, `dashboard`, `scheduler`, `export`

## Removed From Active Architecture

The old public-board integration and new-admin/XE table flow are no longer active. Do not add it back unless it becomes a new project phase.
