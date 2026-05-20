# API-SPEC.md

## Active Admin Routes

Authentication:

- `GET /login`
- `POST /login`
- `POST /logout`
- `GET /account/password`
- `POST /account/password`

Dashboard:

- `GET /`
- `GET /dashboard`

Focus group/survey:

- `GET /research`
- `GET /research/new`
- `POST /research`
- `GET /research/{researchNo}`
- `POST /research/{researchNo}`
- `GET /research/{researchNo}/publish-copy`
- `POST /research/{researchNo}/manual-publish-log`

Applicants:

- `GET /research/{researchNo}/applications`
- `GET /research/{researchNo}/applications/{researchAppSeq}`
- `POST /research/{researchNo}/applications/{researchAppSeq}/provide`
- `GET /research/{researchNo}/applications/provide-preview`
- `POST /research/{researchNo}/applications/provide-complete`
- `GET /research/{researchNo}/applications/duplicates`

Public application form:

- `GET /research/{researchNo}/apply`
- `POST /research/{researchNo}/apply`
- `GET /research/{researchNo}/apply/complete`

Blacklist:

- `GET /legacy-blacklist`
- `GET /legacy-blacklist/new`
- `GET /legacy-blacklist/{blacklistNo}/edit`
- `POST /legacy-blacklist`
- `POST /legacy-blacklist/{blacklistNo}/status`

Export:

- `POST /research/{researchNo}/export/xlsx`
- `POST /research/{researchNo}/export/provide-xlsx`
- `POST /research/{researchNo}/export/txt`
- `POST /research/{researchNo}/export/provide-txt`

Mail:

- `GET /research/{researchNo}/mail`
- `POST /research/{researchNo}/mail`
- `POST /research/{researchNo}/mail/schedule`
- `POST /research/{researchNo}/mail/scheduled/{sendJobId}/cancel`
- `POST /research/{researchNo}/mail/threshold-settings`
- `POST /research/{researchNo}/mail/threshold-cancel`
- `POST /research/{researchNo}/mail/threshold-trigger`
- `POST /research/{researchNo}/mail/threshold-rules`
- `POST /research/{researchNo}/mail/threshold-rules/{ruleId}/trigger`
- `POST /research/{researchNo}/mail/threshold-rules/{ruleId}/delete`

Matching/SMS:

- `GET /research/{researchNo}/matching`
- `POST /research/{researchNo}/matching/run-window`
- `POST /research/{researchNo}/matching/refresh`
- `POST /research/{researchNo}/matching/sms`
- `GET /research/{researchNo}/matching/history`

Clients:

- `GET /clients`
- `POST /clients`
- `POST /clients/{clientId}/delete`

Logs:

- `GET /logs/actions`
- `GET /logs/mail`
- `GET /logs/search`
- `GET /logs/notifications`

## Route Key Rule

Use `RESEARCH_NO` routes for focus group/survey flows.
