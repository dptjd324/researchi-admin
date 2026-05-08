# DEPLOYMENT.md

## Old Admin DB Deployment Notes

The copied old admin database is the primary operational database for postings, applicants, and blacklist data. In local verification, `admin_copy` contained:

- `TB_RESEARCH_MST`
- `TB_RESEARCH_APP`
- `TB_BLACKLIST_MST`

Use a clearly named old-admin datasource configuration when the transition begins. Avoid treating the public XE datasource as the source of truth for manager data.

Recommended environment variable direction:

- `OLD_ADMIN_DB_URL`
- `OLD_ADMIN_DB_USERNAME`
- `OLD_ADMIN_DB_PASSWORD`
- `ADMIN_DB_URL` for supplemental admin tables
- `XE_DB_URL` only for future/manual public website references

Spring property direction:

- `spring.datasource.old-admin.jdbc-url`
- `spring.datasource.old-admin.username`
- `spring.datasource.old-admin.password`

Old-admin mappers and services are activated only when the old-admin datasource
JDBC URL is configured.

Before deployment or schema-sensitive work, verify charset, collation, table engine, and columns in the target environment:

```sql
SHOW CREATE TABLE TB_RESEARCH_MST;
SHOW CREATE TABLE TB_RESEARCH_APP;
SHOW CREATE TABLE TB_BLACKLIST_MST;
SHOW VARIABLES LIKE 'character_set%';
SHOW FULL COLUMNS FROM TB_RESEARCH_MST;
SHOW FULL COLUMNS FROM TB_RESEARCH_APP;
SHOW FULL COLUMNS FROM TB_BLACKLIST_MST;
```

If the old tables are MyISAM, do not rely on transaction rollback. Enable database backup and supplemental revision logs before any update path writes to old admin tables.

Current phase deployment must not enable XE auto-publishing. Publishing support should generate copy-ready content from `TB_RESEARCH_MST` and record the manual publish result separately.

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

## External XE DB Performance Checks
- `xeDataSource` points to the existing XE database. The application reads and writes `xe_documents`, but it must not automatically change XE schema on startup.
- For large XE databases, review and apply these indexes manually during a maintenance window if equivalent indexes do not already exist:

```sql
CREATE INDEX idx_xe_modules_mid_module_srl ON xe_modules (mid, module_srl);
CREATE INDEX idx_xe_documents_module_document ON xe_documents (module_srl, document_srl);
```

- Check for existing indexes first to avoid duplicate indexes with different names.
- Because admin DB and XE DB use separate local transactions, verify failed create/update scenarios in staging after any workflow change that writes both databases.

## Operational Security Checks
- Enforce HTTPS
- Set a strong initial admin password policy
- Review admin access logs
- Monitor repeated login failures
- Review personal data download history
- Verify periodic backups
