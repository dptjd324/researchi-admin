# DEPLOYMENT.md

## Current Deployment Direction

The application uses:

- `ADMIN_DB_URL`: supplemental admin tables
- `OLD_ADMIN_DB_URL`: copied old admin DB containing `TB_RESEARCH_MST`, `TB_RESEARCH_APP`, `TB_BLACKLIST_MST`
- SMTP settings for mail
- Naver SENS settings for SMS

Use `prod` on the cloud server. Keep `local` only for the developer machine.
Keep production secrets outside the JAR. Create a private server-only file at
`/etc/researchi-admin/application-prod.yml` from
`src/main/resources/application-prod.yml.example`.

## Do Not Upload

- `src/main/resources/application-local.yml`
- `.env` or `.env.*`
- DB dumps such as `*.sql`, `*.dump`, `*.bak`
- private keys such as `*.pem`, `*.key`, `*.p12`, `*.jks`
- local `uploads/`, `exports/`, `build/`, `.gradle/`, `.idea/`, `.vs/`

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
- `APP_EXPORT_PATH`
- `APP_UPLOAD_PATH`

See `src/main/resources/application-prod.yml.example` for a complete production template.

## Deployment Order

1. Back up admin DB and old admin DB.
2. Run tests locally and record any known failures.
3. Build the application.
4. Create `/etc/researchi-admin/application-prod.yml` on the cloud server.
5. Create `APP_EXPORT_PATH` and `APP_UPLOAD_PATH`, then grant write permission to the app user.
6. Run required admin supplemental schema scripts.
7. Start the application with `spring.profiles.active=prod` and the external prod config file.
8. Verify `/login`.
9. Verify `/dashboard`.
10. Verify `/research`.
11. Verify one public application link: `/research/{researchNo}/apply`.
12. Verify matching and blacklist flows.
13. Keep mail/SMS simulation enabled for the first smoke test.
14. Send one mail/SMS to a controlled recipient, then switch real sending on only after success.

## Start Command Example

```powershell
java -jar researchi-admin.jar --spring.profiles.active=prod --spring.config.additional-location=file:/etc/researchi-admin/application-prod.yml
```

Use the server's process manager or service runner after this command has been verified manually.

## First Release Safety Switches

Start with:

```yaml
app:
  mail:
    simulate-send: true
  sms:
    simulate-send: true
  scheduler:
    enabled: false
```

After login, dashboard, research list, public application, export path, matching, and blacklist checks pass:

1. Set mail real sending for one controlled recipient.
2. Set SMS real sending for one controlled recipient.
3. Enable scheduler only after both are verified.

## Operational Checks

- Enforce HTTPS.
- Review admin access logs.
- Review monthly mail/SMS counts on `/dashboard`.
- Verify periodic DB backups.
- Keep scheduler enabled only after mail/SMS settings are verified.

## Rollback Preparation

- Keep the previous JAR available on the server.
- Keep the DB backup taken immediately before deployment.
- Record the exact environment variables used for the release.
- If SMS or mail misfires, first switch `APP_SMS_SIMULATE_SEND=true`, `APP_MAIL_SIMULATE_SEND=true`, and `APP_SCHEDULER_ENABLED=false`.
