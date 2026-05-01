package com.researchi.admin.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class AdminSchemaBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSchemaBootstrap.class);

    private final DataSource adminDataSource;

    public AdminSchemaBootstrap(@Qualifier("adminDataSource") DataSource adminDataSource) {
        this.adminDataSource = adminDataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = adminDataSource.getConnection()) {
            ensureAdminClientTables(connection);
            ensureAdminBoardConfigTable(connection);
            ensureAdminJobMetaColumns(connection);
            ensureAdminMailSendJobColumns(connection);
            ensureAdminJobApplicationExtraAnswerTable(connection);
            ensureAdminPerformanceIndexes(connection);
        }
    }

    private void ensureAdminBoardConfigTable(Connection connection) throws Exception {
        createTableIfMissing(
                connection,
                "admin_board_config",
                """
                CREATE TABLE admin_board_config (
                    xe_mid VARCHAR(80) PRIMARY KEY,
                    board_name VARCHAR(100) NOT NULL,
                    board_type VARCHAR(20) NOT NULL,
                    application_enabled CHAR(1) NOT NULL DEFAULT 'N',
                    display_order INT NOT NULL DEFAULT 0,
                    active_yn CHAR(1) NOT NULL DEFAULT 'Y',
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """
        );
    }

    private void ensureAdminClientTables(Connection connection) throws Exception {
        createTableIfMissing(
                connection,
                "admin_client",
                """
                CREATE TABLE admin_client (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    client_name VARCHAR(200) NOT NULL,
                    department_name VARCHAR(100) NULL,
                    reply_to_email VARCHAR(255) NULL,
                    active_yn CHAR(1) NOT NULL DEFAULT 'Y',
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """
        );
        createTableIfMissing(
                connection,
                "admin_client_contact",
                """
                CREATE TABLE admin_client_contact (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    client_id BIGINT NOT NULL,
                    contact_name VARCHAR(100) NULL,
                    email VARCHAR(255) NOT NULL,
                    primary_yn CHAR(1) NOT NULL DEFAULT 'N',
                    active_yn CHAR(1) NOT NULL DEFAULT 'Y',
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """
        );
        addColumnIfMissing(
                connection,
                "admin_client",
                "department_name",
                "ALTER TABLE admin_client ADD COLUMN department_name VARCHAR(100) NULL AFTER client_name"
        );
        addColumnIfMissing(
                connection,
                "admin_client",
                "reply_to_email",
                "ALTER TABLE admin_client ADD COLUMN reply_to_email VARCHAR(255) NULL AFTER department_name"
        );
    }

    private void ensureAdminJobMetaColumns(Connection connection) throws Exception {
        addColumnIfMissing(
                connection,
                "admin_job_meta",
                "client_id",
                "ALTER TABLE admin_job_meta ADD COLUMN client_id BIGINT NULL AFTER recruit_limit"
        );
        addColumnIfMissing(
                connection,
                "admin_job_meta",
                "auto_send_template_id",
                "ALTER TABLE admin_job_meta ADD COLUMN auto_send_template_id BIGINT NULL AFTER auto_send_repeat_unit"
        );
        addColumnIfMissing(
                connection,
                "admin_job_meta",
                "auto_send_attachment_type",
                "ALTER TABLE admin_job_meta ADD COLUMN auto_send_attachment_type VARCHAR(20) NULL AFTER auto_send_template_id"
        );
        addColumnIfMissing(
                connection,
                "admin_job_meta",
                "deleted_yn",
                "ALTER TABLE admin_job_meta ADD COLUMN deleted_yn CHAR(1) NOT NULL DEFAULT 'N' AFTER next_auto_send_at"
        );
        addColumnIfMissing(
                connection,
                "admin_job_meta",
                "delete_reason",
                "ALTER TABLE admin_job_meta ADD COLUMN delete_reason VARCHAR(500) NULL AFTER deleted_yn"
        );
        addColumnIfMissing(
                connection,
                "admin_job_meta",
                "deleted_at",
                "ALTER TABLE admin_job_meta ADD COLUMN deleted_at DATETIME NULL AFTER delete_reason"
        );
        addColumnIfMissing(
                connection,
                "admin_job_meta",
                "permanent_delete_after",
                "ALTER TABLE admin_job_meta ADD COLUMN permanent_delete_after DATETIME NULL AFTER deleted_at"
        );
    }

    private void ensureAdminMailSendJobColumns(Connection connection) throws Exception {
        alterColumnIfNotNullable(
                connection,
                "admin_mail_send_job",
                "template_id",
                "ALTER TABLE admin_mail_send_job MODIFY COLUMN template_id BIGINT NULL"
        );
        addColumnIfMissing(
                connection,
                "admin_mail_send_job",
                "mail_subject_snapshot",
                "ALTER TABLE admin_mail_send_job ADD COLUMN mail_subject_snapshot VARCHAR(255) NULL AFTER template_id"
        );
        addColumnIfMissing(
                connection,
                "admin_mail_send_job",
                "mail_body_snapshot",
                "ALTER TABLE admin_mail_send_job ADD COLUMN mail_body_snapshot TEXT NULL AFTER mail_subject_snapshot"
        );
        addColumnIfMissing(
                connection,
                "admin_mail_send_job",
                "attachment_type",
                "ALTER TABLE admin_mail_send_job ADD COLUMN attachment_type VARCHAR(20) NOT NULL DEFAULT 'XLSX' AFTER template_id"
        );
        addColumnIfMissing(
                connection,
                "admin_mail_send_job",
                "repeat_yn",
                "ALTER TABLE admin_mail_send_job ADD COLUMN repeat_yn CHAR(1) NOT NULL DEFAULT 'N' AFTER duplicate_prevent_key"
        );
        addColumnIfMissing(
                connection,
                "admin_mail_send_job",
                "repeat_unit",
                "ALTER TABLE admin_mail_send_job ADD COLUMN repeat_unit VARCHAR(20) NULL AFTER repeat_yn"
        );
    }

    private void ensureAdminJobApplicationExtraAnswerTable(Connection connection) throws Exception {
        createTableIfMissing(
                connection,
                "admin_job_application_extra_answer",
                """
                CREATE TABLE admin_job_application_extra_answer (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    application_id BIGINT NOT NULL,
                    answer_order INT NOT NULL,
                    question_label VARCHAR(255) NOT NULL,
                    answer_text TEXT NOT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """
        );
    }

    private void ensureAdminPerformanceIndexes(Connection connection) throws Exception {
        createUniqueIndexIfMissingAndNoDuplicates(
                connection,
                "admin_job_meta",
                "uk_admin_job_meta_document_srl",
                "document_srl",
                "CREATE UNIQUE INDEX uk_admin_job_meta_document_srl ON admin_job_meta (document_srl)"
        );
        createIndexIfMissing(
                connection,
                "admin_job_meta",
                "idx_admin_job_meta_document_srl",
                List.of("document_srl"),
                "CREATE INDEX idx_admin_job_meta_document_srl ON admin_job_meta (document_srl)"
        );
        createIndexIfMissing(
                connection,
                "admin_job_meta",
                "idx_admin_job_meta_client_document",
                List.of("client_id", "document_srl"),
                "CREATE INDEX idx_admin_job_meta_client_document ON admin_job_meta (client_id, document_srl)"
        );
        createIndexIfMissing(
                connection,
                "admin_job_meta",
                "idx_admin_job_meta_apply_recruit_document",
                List.of("application_enabled", "recruit_status", "document_srl"),
                "CREATE INDEX idx_admin_job_meta_apply_recruit_document ON admin_job_meta (application_enabled, recruit_status, document_srl)"
        );
        createIndexIfMissing(
                connection,
                "admin_job_meta",
                "idx_admin_job_meta_deleted_due",
                List.of("deleted_yn", "permanent_delete_after", "document_srl"),
                "CREATE INDEX idx_admin_job_meta_deleted_due ON admin_job_meta (deleted_yn, permanent_delete_after, document_srl)"
        );
        createIndexIfMissing(
                connection,
                "admin_job_application",
                "idx_admin_job_application_document_applied",
                List.of("document_srl", "applied_at", "id"),
                "CREATE INDEX idx_admin_job_application_document_applied ON admin_job_application (document_srl, applied_at, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_job_application",
                "idx_admin_job_application_applied",
                List.of("applied_at", "id"),
                "CREATE INDEX idx_admin_job_application_applied ON admin_job_application (applied_at, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_job_application",
                "idx_admin_job_application_status_applied",
                List.of("application_status", "applied_at", "id"),
                "CREATE INDEX idx_admin_job_application_status_applied ON admin_job_application (application_status, applied_at, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_job_application",
                "idx_admin_job_application_delivery_job",
                List.of("delivery_job_id"),
                "CREATE INDEX idx_admin_job_application_delivery_job ON admin_job_application (delivery_job_id)"
        );
        createIndexIfMissing(
                connection,
                "admin_form_field",
                "idx_admin_form_field_document_order",
                List.of("document_srl", "field_order", "id"),
                "CREATE INDEX idx_admin_form_field_document_order ON admin_form_field (document_srl, field_order, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_form_field",
                "idx_admin_form_field_document_key",
                List.of("document_srl", "field_key"),
                "CREATE INDEX idx_admin_form_field_document_key ON admin_form_field (document_srl, field_key)"
        );
        createIndexIfMissing(
                connection,
                "admin_form_submission_answer",
                "idx_admin_form_answer_application",
                List.of("application_id", "id"),
                "CREATE INDEX idx_admin_form_answer_application ON admin_form_submission_answer (application_id, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_job_application_extra_answer",
                "idx_admin_extra_answer_application_order",
                List.of("application_id", "answer_order", "id"),
                "CREATE INDEX idx_admin_extra_answer_application_order ON admin_job_application_extra_answer (application_id, answer_order, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_mail_send_job",
                "idx_admin_mail_send_job_document_id",
                List.of("document_srl", "id"),
                "CREATE INDEX idx_admin_mail_send_job_document_id ON admin_mail_send_job (document_srl, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_mail_send_job",
                "idx_admin_mail_send_job_status_schedule",
                List.of("send_status", "scheduled_at", "id"),
                "CREATE INDEX idx_admin_mail_send_job_status_schedule ON admin_mail_send_job (send_status, scheduled_at, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_mail_send_job",
                "idx_admin_mail_send_job_duplicate_key",
                List.of("duplicate_prevent_key"),
                "CREATE INDEX idx_admin_mail_send_job_duplicate_key ON admin_mail_send_job (duplicate_prevent_key)"
        );
        createIndexIfMissing(
                connection,
                "admin_mail_send_target",
                "idx_admin_mail_send_target_job_application",
                List.of("send_job_id", "application_id", "id"),
                "CREATE INDEX idx_admin_mail_send_target_job_application ON admin_mail_send_target (send_job_id, application_id, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_action_log",
                "idx_admin_action_log_created",
                List.of("created_at", "id"),
                "CREATE INDEX idx_admin_action_log_created ON admin_action_log (created_at, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_action_log",
                "idx_admin_action_log_target_created",
                List.of("target_type", "target_id", "created_at", "id"),
                "CREATE INDEX idx_admin_action_log_target_created ON admin_action_log (target_type, target_id, created_at, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_search_log",
                "idx_admin_search_log_searched",
                List.of("searched_at", "id"),
                "CREATE INDEX idx_admin_search_log_searched ON admin_search_log (searched_at, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_notification_log",
                "idx_admin_notification_log_document_created",
                List.of("document_srl", "created_at", "id"),
                "CREATE INDEX idx_admin_notification_log_document_created ON admin_notification_log (document_srl, created_at, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_notification_log",
                "idx_admin_notification_log_created",
                List.of("created_at", "id"),
                "CREATE INDEX idx_admin_notification_log_created ON admin_notification_log (created_at, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_notification_log",
                "idx_admin_notification_log_duplicate",
                List.of("document_srl", "application_id", "channel_type", "send_status"),
                "CREATE INDEX idx_admin_notification_log_duplicate ON admin_notification_log (document_srl, application_id, channel_type, send_status)"
        );
        createIndexIfMissing(
                connection,
                "admin_keyword_match_job",
                "idx_admin_keyword_match_job_document_created",
                List.of("document_srl", "created_at", "id"),
                "CREATE INDEX idx_admin_keyword_match_job_document_created ON admin_keyword_match_job (document_srl, created_at, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_keyword_match_target",
                "idx_admin_keyword_match_target_job_score",
                List.of("match_job_id", "match_score", "id"),
                "CREATE INDEX idx_admin_keyword_match_target_job_score ON admin_keyword_match_target (match_job_id, match_score, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_keyword_match_target",
                "idx_admin_keyword_match_target_application",
                List.of("application_id"),
                "CREATE INDEX idx_admin_keyword_match_target_application ON admin_keyword_match_target (application_id)"
        );
        createIndexIfMissing(
                connection,
                "admin_job_keyword",
                "idx_admin_job_keyword_document",
                List.of("document_srl", "id"),
                "CREATE INDEX idx_admin_job_keyword_document ON admin_job_keyword (document_srl, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_application_keyword",
                "idx_admin_application_keyword_application",
                List.of("application_id", "id"),
                "CREATE INDEX idx_admin_application_keyword_application ON admin_application_keyword (application_id, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_client",
                "idx_admin_client_active_name",
                List.of("active_yn", "client_name", "id"),
                "CREATE INDEX idx_admin_client_active_name ON admin_client (active_yn, client_name, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_client_contact",
                "idx_admin_client_contact_client_primary",
                List.of("client_id", "primary_yn", "id"),
                "CREATE INDEX idx_admin_client_contact_client_primary ON admin_client_contact (client_id, primary_yn, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_application_duplicate_log",
                "idx_admin_duplicate_log_document_phone_checked",
                List.of("document_srl", "mobile_phone_hash", "checked_at", "id"),
                "CREATE INDEX idx_admin_duplicate_log_document_phone_checked ON admin_application_duplicate_log (document_srl, mobile_phone_hash, checked_at, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_application_duplicate_log",
                "idx_admin_duplicate_log_phone_found_match",
                List.of("mobile_phone_hash", "duplicate_found", "matched_application_id"),
                "CREATE INDEX idx_admin_duplicate_log_phone_found_match ON admin_application_duplicate_log (mobile_phone_hash, duplicate_found, matched_application_id)"
        );
        createIndexIfMissing(
                connection,
                "admin_blacklist",
                "idx_admin_blacklist_active_phone_expiry",
                List.of("active_yn", "black_mobile_phone_hash", "expires_at", "id"),
                "CREATE INDEX idx_admin_blacklist_active_phone_expiry ON admin_blacklist (active_yn, black_mobile_phone_hash, expires_at, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_blacklist",
                "idx_admin_blacklist_active_name_birth_expiry",
                List.of("active_yn", "black_name", "black_birth_date", "expires_at", "id"),
                "CREATE INDEX idx_admin_blacklist_active_name_birth_expiry ON admin_blacklist (active_yn, black_name, black_birth_date, expires_at, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_blacklist",
                "idx_admin_blacklist_active_expiry",
                List.of("active_yn", "expires_at", "id"),
                "CREATE INDEX idx_admin_blacklist_active_expiry ON admin_blacklist (active_yn, expires_at, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_blacklist_match_log",
                "idx_admin_blacklist_match_blacklist",
                List.of("blacklist_id", "matched_at", "id"),
                "CREATE INDEX idx_admin_blacklist_match_blacklist ON admin_blacklist_match_log (blacklist_id, matched_at, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_blacklist_match_log",
                "idx_admin_blacklist_match_application",
                List.of("application_id"),
                "CREATE INDEX idx_admin_blacklist_match_application ON admin_blacklist_match_log (application_id)"
        );
    }

    private void addColumnIfMissing(
            Connection connection,
            String tableName,
            String columnName,
            String ddl
    ) throws Exception {
        if (hasColumn(connection, tableName, columnName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(ddl);
            log.info("Added missing column {}.{}.", tableName, columnName);
        }
    }

    private void createTableIfMissing(Connection connection, String tableName, String ddl) throws Exception {
        if (hasTable(connection, tableName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(ddl);
            log.info("Created missing table {}.", tableName);
        }
    }

    private void createIndexIfMissing(
            Connection connection,
            String tableName,
            String indexName,
            List<String> columns,
            String ddl
    ) throws Exception {
        if (!hasTable(connection, tableName)
                || hasIndex(connection, tableName, indexName)
                || hasIndexStartingWith(connection, tableName, columns)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(ddl);
            log.info("Created missing index {} on {}.", indexName, tableName);
        }
    }

    private void createUniqueIndexIfMissingAndNoDuplicates(
            Connection connection,
            String tableName,
            String indexName,
            String columnName,
            String ddl
    ) throws Exception {
        if (!hasTable(connection, tableName) || hasIndex(connection, tableName, indexName)) {
            return;
        }
        if (hasDuplicateValues(connection, tableName, columnName)) {
            log.warn(
                    "Skipped unique index {} on {}.{} because duplicate values already exist. Resolve duplicates before production.",
                    indexName,
                    tableName,
                    columnName
            );
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(ddl);
            log.info("Created missing unique index {} on {}.", indexName, tableName);
        }
    }

    private void alterColumnIfNotNullable(
            Connection connection,
            String tableName,
            String columnName,
            String ddl
    ) throws Exception {
        if (!hasColumn(connection, tableName, columnName) || isColumnNullable(connection, tableName, columnName)) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(ddl);
            log.info("Ensured column shape {}.{}.", tableName, columnName);
        }
    }

    private boolean isColumnNullable(Connection connection, String tableName, String columnName) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet columns = metaData.getColumns(connection.getCatalog(), null, tableName, columnName)) {
            if (columns.next()) {
                return columns.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
            }
            return false;
        }
    }

    private boolean hasTable(Connection connection, String tableName) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet tables = metaData.getTables(connection.getCatalog(), null, tableName, null)) {
            return tables.next();
        }
    }

    private boolean hasColumn(Connection connection, String tableName, String columnName) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet columns = metaData.getColumns(connection.getCatalog(), null, tableName, columnName)) {
            return columns.next();
        }
    }

    private boolean hasIndex(Connection connection, String tableName, String indexName) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        String normalizedIndexName = indexName.toLowerCase(Locale.ROOT);
        try (ResultSet indexes = metaData.getIndexInfo(connection.getCatalog(), null, tableName, false, false)) {
            while (indexes.next()) {
                String currentIndexName = indexes.getString("INDEX_NAME");
                if (currentIndexName != null && normalizedIndexName.equals(currentIndexName.toLowerCase(Locale.ROOT))) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean hasDuplicateValues(Connection connection, String tableName, String columnName) throws Exception {
        String sql = """
                SELECT %s
                FROM %s
                WHERE %s IS NOT NULL
                GROUP BY %s
                HAVING COUNT(*) > 1
                LIMIT 1
                """.formatted(columnName, tableName, columnName, columnName);
        try (Statement statement = connection.createStatement();
             ResultSet duplicates = statement.executeQuery(sql)) {
            return duplicates.next();
        }
    }

    private boolean hasIndexStartingWith(Connection connection, String tableName, List<String> columns) throws Exception {
        if (columns.isEmpty()) {
            return true;
        }
        List<String> normalizedColumns = columns.stream()
                .map(column -> column.toLowerCase(Locale.ROOT))
                .toList();
        DatabaseMetaData metaData = connection.getMetaData();
        Map<String, List<String>> indexColumns = new LinkedHashMap<>();
        try (ResultSet indexes = metaData.getIndexInfo(connection.getCatalog(), null, tableName, false, false)) {
            while (indexes.next()) {
                String currentIndexName = indexes.getString("INDEX_NAME");
                String columnName = indexes.getString("COLUMN_NAME");
                short ordinalPosition = indexes.getShort("ORDINAL_POSITION");
                if (currentIndexName == null || columnName == null || ordinalPosition < 1) {
                    continue;
                }
                indexColumns.computeIfAbsent(currentIndexName, ignored -> new ArrayList<>());
                List<String> currentColumns = indexColumns.get(currentIndexName);
                while (currentColumns.size() < ordinalPosition) {
                    currentColumns.add("");
                }
                currentColumns.set(ordinalPosition - 1, columnName.toLowerCase(Locale.ROOT));
            }
        }
        return indexColumns.values().stream()
                .anyMatch(currentColumns -> startsWith(currentColumns, normalizedColumns));
    }

    private boolean startsWith(List<String> currentColumns, List<String> requiredColumns) {
        if (currentColumns.size() < requiredColumns.size()) {
            return false;
        }
        return Arrays.equals(
                currentColumns.subList(0, requiredColumns.size()).toArray(),
                requiredColumns.toArray()
        );
    }
}
