# DEPLOYMENT.md

## Current Deployment Direction

The application uses:

- `ADMIN_DB_URL`: supplemental admin tables
- `OLD_ADMIN_DB_URL`: copied old admin DB containing `TB_RESEARCH_MST`, `TB_RESEARCH_APP`, `TB_BLACKLIST_MST`
- SMTP settings for mail
- Naver SENS settings for SMS

## Required Environment Variables

- `ADMIN_DB_URL`
- `ADMIN_DB_USERNAME`
- `ADMIN_DB_PASSWORD`
- `OLD_ADMIN_DB_URL`
- `OLD_ADMIN_DB_USERNAME`
- `OLD_ADMIN_DB_PASSWORD`
- `SMTP_HOST`
- `SMTP_PORT`
- `SMTP_USERNAME`
- `SMTP_PASSWORD`
- `APP_BASE_URL`
- `APP_SMS_SIMULATE_SEND`
- `APP_SMS_PROVIDER`
- `APP_SMS_ACCESS_KEY`
- `APP_SMS_SECRET_KEY`
- `APP_SMS_SERVICE_ID`
- `APP_SMS_FROM_NUMBER`
- `ENCRYPTION_KEY`
- `PHONE_HASH_KEY`
- `APP_SCHEDULER_ENABLED`
- `APP_SCHEDULER_SCHEDULED_SEND_CRON`
- `APP_SCHEDULER_THRESHOLD_CRON`
- `APP_SCHEDULER_CLEANUP_CRON`

## Deployment Order

1. Back up admin DB and old admin DB.
2. Build the application.
3. Configure environment variables.
4. Run required admin supplemental schema scripts.
5. Start the application.
6. Verify `/login`.
7. Verify `/dashboard`.
8. Verify `/research`.
9. Verify one public application link: `/research/{researchNo}/apply`.
10. Verify mail, SMS, export, matching, and blacklist flows.

## Operational Checks

- Enforce HTTPS.
- Review admin access logs.
- Review monthly mail/SMS counts on `/dashboard`.
- Verify periodic DB backups.
- Keep scheduler enabled only after mail/SMS settings are verified.
