# DEPLOYMENT.md

## Recommended Environment
- Ubuntu 22.04
- Java 17
- Spring Boot
- Nginx
- MariaDB/MySQL

## Recommended Sizing
- AWS Lightsail 2GB or 4GB
- Split the database later if traffic grows
 
## Environment Variables
- `ADMIN_DB_URL`
- `ADMIN_DB_USERNAME`
- `ADMIN_DB_PASSWORD`
- `XE_DB_URL`
- `XE_DB_USERNAME`
- `XE_DB_PASSWORD`
- `SMTP_HOST`
- `SMTP_PORT`
- `SMTP_USERNAME`
- `SMTP_PASSWORD`
- `APP_BASE_URL`
- `APP_SMS_SIMULATE_SEND` (`true`면 실제 문자 발송 없이 성공 처리)
- `APP_SMS_PROVIDER` (`naver-sens`)
- `APP_SMS_ACCESS_KEY`
- `APP_SMS_SECRET_KEY`
- `APP_SMS_SERVICE_ID`
- `APP_SMS_FROM_NUMBER`
- `ENCRYPTION_KEY`
- `APP_SCHEDULER_ENABLED`
- `APP_SCHEDULER_SCHEDULED_SEND_CRON`
- `APP_SCHEDULER_THRESHOLD_CRON`
- `APP_SCHEDULER_CLEANUP_CRON`
- `APP_SCHEDULER_BLACKLIST_EXPIRY_CRON`
- `APP_SCHEDULER_KEYWORD_MATCH_CRON`
- `APP_SCHEDULER_RETENTION_MONTHS`

## Deployment Order
1. Build the application.
2. Set environment variables.
3. Run the admin DB migrations, including `src/main/resources/db/phase12-operations.sql`.
4. Start the application.
5. Verify `/login`.
6. Verify XE DB read integration.
7. Verify the public application form.
8. Verify dynamic question rendering.
9. Verify CAPTCHA or the configured anti-bot flow.
10. Verify XLSX/TXT export generation.
11. Verify mail dispatch.
12. Enable the scheduler by setting `APP_SCHEDULER_ENABLED=true` after checking Phase 12 mail template, threshold, and retention settings.

## Operational Security Checks
- Enforce HTTPS
- Set a strong initial admin password policy
- Review admin access logs
- Monitor repeated login failures
- Review personal data download history
- Verify periodic backups
