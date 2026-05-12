# API-SPEC.md

## Old Admin DB First API Direction

The transition target is old-admin-DB-first. Existing routes may stay during migration for compatibility, but new or refactored behavior must use old admin keys:

- Posting key: `RESEARCH_NO` from `TB_RESEARCH_MST`
- Applicant key: `RESEARCH_NO` + `RESEARCH_APP_SEQ` from `TB_RESEARCH_APP`
- Blacklist key: `BLACKLIST_NO` from `TB_BLACKLIST_MST`
- Public XE key: optional `document_srl` only for manually recorded homepage links

Do not assume `RESEARCH_NO` equals `xe_documents.document_srl`.

Suggested transition routes:

- GET /research
- GET /research/new
- POST /research
- GET /research/{researchNo}/edit
- POST /research/{researchNo}
- POST /research/{researchNo}/status
- GET /research/{researchNo}/applications
- GET /research/{researchNo}/applications/{researchAppSeq}
- GET /research/{researchNo}/publish-copy
- POST /research/{researchNo}/manual-publish-log
- GET /blacklist
- POST /blacklist
- POST /blacklist/{blacklistNo}/toggle

Implemented transition route:

- GET /research: read-only `TB_RESEARCH_MST` list with keyword search and pagination
- GET /research/{researchNo}: `TB_RESEARCH_MST` edit screen
- POST /research/{researchNo}: update `TB_RESEARCH_MST` after writing `admin_legacy_revision_log`
- GET /research/{researchNo}/applications: read-only `TB_RESEARCH_APP` list with per-field search and pagination
- GET /research/{researchNo}/applications/{researchAppSeq}: read-only `TB_RESEARCH_APP` detail
- POST /research/{researchNo}/applications/{researchAppSeq}/provide: update `TB_RESEARCH_APP.PROVIDE_YN` after writing `admin_legacy_revision_log`; `Y` means applicant information was provided to the trader/client by email, `N` means not yet provided
- GET /legacy-blacklist: old DB `TB_BLACKLIST_MST` list/search/edit screen
- POST /legacy-blacklist: create or update `TB_BLACKLIST_MST` row
- POST /legacy-blacklist/{blacklistNo}/status: update `TB_BLACKLIST_MST.BLACK_YN`

Current phase manual publishing only:

- Generate copy-ready homepage title/body from `TB_RESEARCH_MST`.
- Record manual publish status in a supplemental admin table.
- Allow optional `document_srl` input after an admin manually creates a public website post.
- Do not implement XE auto-insert or XE auto-update endpoints in this phase.

## Board Scope

`GET /jobs` manages these XE mids:
`notice`, `newjob`, `additional`, `fast`, `recruit`, `sharing`, `question`.

Application endpoints and application-related controls are limited to:
`newjob`, `additional`, `fast`, `recruit`.

Content-only/Q&A boards are:
`notice`, `sharing`, `question`.

`page_tjVR38` is excluded from the API scope for now.

## Admin Pages
- GET /login
- POST /login
- POST /logout
- GET /dashboard

## Jobs
- GET /jobs
- GET /jobs/new
- POST /jobs
- GET /jobs/{documentSrl}/edit
- POST /jobs/{documentSrl}
- POST /jobs/{documentSrl}/status

## Dynamic Fields
- GET /jobs/{documentSrl}/fields
- POST /jobs/{documentSrl}/fields
- POST /jobs/{documentSrl}/fields/{fieldId}
- POST /jobs/{documentSrl}/fields/{fieldId}/delete

## Applications
- GET /applications
- GET /jobs/{documentSrl}/applications
- GET /applications/{id}

## Blacklist
- GET /blacklist
- POST /blacklist
- POST /blacklist/{id}/toggle

## Export
- GET /jobs/{documentSrl}/export/xlsx
- GET /jobs/{documentSrl}/export/txt

## Mail / Send
- GET /mail/templates
- POST /mail/templates
- GET /mail/send/history
- POST /mail/send/manual
- POST /mail/send/schedule
- POST /mail/send/threshold-trigger

## Keyword Notification
- GET /matching/jobs/{documentSrl}
- POST /matching/jobs/{documentSrl}/run
- POST /notifications/email
- POST /notifications/sms

## Search / Period Query
- GET /search

## Logs
- GET /logs/actions
- GET /logs/mail
- GET /logs/search
- GET /logs/notifications

## Public Form
- GET /apply/{documentSrl}
- POST /apply/{documentSrl}
- GET /apply/{documentSrl}/complete
- GET /apply/{documentSrl}/duplicate
- GET /apply/{documentSrl}/blocked
