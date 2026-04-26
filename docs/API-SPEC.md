# API-SPEC.md

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
