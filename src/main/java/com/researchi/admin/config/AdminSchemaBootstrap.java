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
            ensureAdminResearchClientLinkTable(connection);
            ensureAdminMailSendJobColumns(connection);
            ensureAdminExportLogColumns(connection);
            ensureAdminNotificationLogColumns(connection);
            ensureAdminLegacyRevisionLogTable(connection);
            ensureAdminLegacyApplicationExtraAnswerTable(connection);
            ensureAdminLegacyApplicationKeywordTable(connection);
            ensureAdminLegacyMatchingTables(connection);
            ensureAdminManualPublishLogTable(connection);
            ensureAdminLegacyMailRuleTable(connection);
            ensureAdminLegacyMailRuleItemTable(connection);
            ensureAdminPerformanceIndexes(connection);
        }
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
                    contact_no VARCHAR(50) NULL,
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
        addColumnIfMissing(
                connection,
                "admin_client_contact",
                "contact_no",
                "ALTER TABLE admin_client_contact ADD COLUMN contact_no VARCHAR(50) NULL AFTER email"
        );
    }

    private void ensureAdminResearchClientLinkTable(Connection connection) throws Exception {
        createTableIfMissing(
                connection,
                "admin_research_client_link",
                """
                CREATE TABLE admin_research_client_link (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    research_no BIGINT NOT NULL,
                    client_id BIGINT NOT NULL,
                    client_name VARCHAR(200) NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    UNIQUE KEY uk_admin_research_client_link_research_no (research_no)
                )
                """
        );
    }

    private void ensureAdminMailSendJobColumns(Connection connection) throws Exception {
        addColumnIfMissing(
                connection,
                "admin_mail_send_job",
                "research_no",
                "ALTER TABLE admin_mail_send_job ADD COLUMN research_no BIGINT NULL AFTER id"
        );
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

    private void ensureAdminExportLogColumns(Connection connection) throws Exception {
        addColumnIfMissing(
                connection,
                "admin_export_log",
                "research_no",
                "ALTER TABLE admin_export_log ADD COLUMN research_no BIGINT NULL AFTER id"
        );
        alterColumnIfVarcharShorter(
                connection,
                "admin_export_log",
                "export_type",
                50,
                "ALTER TABLE admin_export_log MODIFY COLUMN export_type VARCHAR(50) NOT NULL"
        );
    }

    private void ensureAdminNotificationLogColumns(Connection connection) throws Exception {
        addColumnIfMissing(
                connection,
                "admin_notification_log",
                "research_no",
                "ALTER TABLE admin_notification_log ADD COLUMN research_no BIGINT NULL AFTER id"
        );
    }

    private void ensureAdminLegacyRevisionLogTable(Connection connection) throws Exception {
        createTableIfMissing(
                connection,
                "admin_legacy_revision_log",
                """
                CREATE TABLE admin_legacy_revision_log (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    legacy_table_name VARCHAR(100) NOT NULL,
                    legacy_key VARCHAR(100) NOT NULL,
                    before_json LONGTEXT NOT NULL,
                    action_type VARCHAR(50) NOT NULL,
                    changed_by BIGINT NULL,
                    changed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """
        );
    }

    private void ensureAdminLegacyApplicationExtraAnswerTable(Connection connection) throws Exception {
        createTableIfMissing(
                connection,
                "admin_legacy_application_extra_answer",
                """
                CREATE TABLE admin_legacy_application_extra_answer (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    research_no BIGINT NOT NULL,
                    research_app_seq BIGINT NOT NULL,
                    answer_order INT NOT NULL,
                    question_label VARCHAR(500) NOT NULL,
                    answer_text TEXT NOT NULL,
                    raw_answer_text LONGTEXT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """
        );
    }

    private void ensureAdminLegacyApplicationKeywordTable(Connection connection) throws Exception {
        createTableIfMissing(
                connection,
                "admin_legacy_application_keyword",
                """
                CREATE TABLE admin_legacy_application_keyword (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    research_no BIGINT NOT NULL,
                    research_app_seq BIGINT NOT NULL,
                    application_regist_dt VARCHAR(30) NULL,
                    keyword_normalized VARCHAR(120) NOT NULL,
                    keyword VARCHAR(120) NOT NULL,
                    source_type VARCHAR(50) NOT NULL,
                    indexed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """
        );
        addColumnIfMissing(
                connection,
                "admin_legacy_application_keyword",
                "application_regist_dt",
                "ALTER TABLE admin_legacy_application_keyword ADD COLUMN application_regist_dt VARCHAR(30) NULL AFTER research_app_seq"
        );
    }

    private void ensureAdminLegacyMatchingTables(Connection connection) throws Exception {
        createTableIfMissing(
                connection,
                "admin_legacy_matching_job",
                """
                CREATE TABLE admin_legacy_matching_job (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    research_no BIGINT NOT NULL,
                    include_keyword_text TEXT NULL,
                    exclude_keyword_text TEXT NULL,
                    use_auto_keywords CHAR(1) NOT NULL DEFAULT 'Y',
                    active_keyword_text TEXT NULL,
                    status VARCHAR(30) NOT NULL,
                    candidate_pool_count INT NOT NULL DEFAULT 0,
                    matched_count INT NOT NULL DEFAULT 0,
                    blacklisted_excluded_count INT NOT NULL DEFAULT 0,
                    fail_reason VARCHAR(500) NULL,
                    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    started_at DATETIME NULL,
                    finished_at DATETIME NULL
                )
                """
        );
        createTableIfMissing(
                connection,
                "admin_legacy_matching_result",
                """
                CREATE TABLE admin_legacy_matching_result (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    matching_job_id BIGINT NOT NULL,
                    research_no BIGINT NOT NULL,
                    research_app_seq BIGINT NOT NULL,
                    row_no INT NOT NULL,
                    match_score INT NOT NULL,
                    matched_keyword_text TEXT NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """
        );
        createTableIfMissing(
                connection,
                "admin_legacy_matching_index_job",
                """
                CREATE TABLE admin_legacy_matching_index_job (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    research_no BIGINT NOT NULL,
                    cycle_no INT NOT NULL DEFAULT 1,
                    include_keyword_text TEXT NULL,
                    exclude_keyword_text TEXT NULL,
                    applied_years INT NOT NULL DEFAULT 2,
                    index_limit INT NOT NULL DEFAULT 5000,
                    batch_size INT NOT NULL DEFAULT 500,
                    require_contact_yn CHAR(1) NOT NULL DEFAULT 'Y',
                    exclude_blacklist_yn CHAR(1) NOT NULL DEFAULT 'Y',
                    reset_before_run_yn CHAR(1) NOT NULL DEFAULT 'N',
                    status VARCHAR(30) NOT NULL,
                    indexed_application_count INT NOT NULL DEFAULT 0,
                    inserted_keyword_count INT NOT NULL DEFAULT 0,
                    skipped_already_indexed_count INT NOT NULL DEFAULT 0,
                    fail_reason VARCHAR(500) NULL,
                    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    started_at DATETIME NULL,
                    finished_at DATETIME NULL
                )
                """
        );
        addColumnIfMissing(
                connection,
                "admin_legacy_matching_index_job",
                "cycle_no",
                "ALTER TABLE admin_legacy_matching_index_job ADD COLUMN cycle_no INT NOT NULL DEFAULT 1 AFTER research_no"
        );
    }

    private void ensureAdminManualPublishLogTable(Connection connection) throws Exception {
        createTableIfMissing(
                connection,
                "admin_manual_publish_log",
                """
                CREATE TABLE admin_manual_publish_log (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    research_no BIGINT NOT NULL,
                    generated_title VARCHAR(500) NOT NULL,
                    generated_body LONGTEXT NOT NULL,
                    publish_status VARCHAR(50) NOT NULL,
                    public_document_srl BIGINT NULL,
                    published_by BIGINT NULL,
                    published_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """
        );
    }

    private void ensureAdminLegacyMailRuleTable(Connection connection) throws Exception {
        createTableIfMissing(
                connection,
                "admin_legacy_mail_rule",
                """
                CREATE TABLE admin_legacy_mail_rule (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    research_no BIGINT NOT NULL,
                    threshold_count INT NULL,
                    template_id BIGINT NULL,
                    direct_mail_subject VARCHAR(255) NULL,
                    direct_mail_body TEXT NULL,
                    attachment_type VARCHAR(20) NOT NULL DEFAULT 'XLSX',
                    enabled_yn CHAR(1) NOT NULL DEFAULT 'N',
                    last_triggered_at DATETIME NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """
        );
    }

    private void ensureAdminLegacyMailRuleItemTable(Connection connection) throws Exception {
        createTableIfMissing(
                connection,
                "admin_legacy_mail_rule_item",
                """
                CREATE TABLE admin_legacy_mail_rule_item (
                    id BIGINT PRIMARY KEY AUTO_INCREMENT,
                    research_no BIGINT NOT NULL,
                    threshold_count INT NULL,
                    template_id BIGINT NULL,
                    direct_mail_subject VARCHAR(255) NULL,
                    direct_mail_body TEXT NULL,
                    attachment_type VARCHAR(20) NOT NULL DEFAULT 'XLSX',
                    enabled_yn CHAR(1) NOT NULL DEFAULT 'N',
                    last_triggered_at DATETIME NULL,
                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """
        );
    }

    private void ensureAdminPerformanceIndexes(Connection connection) throws Exception {
        createIndexIfMissing(
                connection,
                "admin_legacy_application_extra_answer",
                "idx_admin_legacy_extra_answer_research_app",
                List.of("research_no", "research_app_seq", "answer_order", "id"),
                "CREATE INDEX idx_admin_legacy_extra_answer_research_app ON admin_legacy_application_extra_answer (research_no, research_app_seq, answer_order, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_legacy_application_keyword",
                "idx_admin_legacy_keyword_lookup",
                List.of("keyword_normalized", "application_regist_dt", "research_no", "research_app_seq"),
                "CREATE INDEX idx_admin_legacy_keyword_lookup ON admin_legacy_application_keyword (keyword_normalized, application_regist_dt, research_no, research_app_seq)"
        );
        createIndexIfMissing(
                connection,
                "admin_legacy_application_keyword",
                "idx_admin_legacy_keyword_app",
                List.of("research_no", "research_app_seq"),
                "CREATE INDEX idx_admin_legacy_keyword_app ON admin_legacy_application_keyword (research_no, research_app_seq)"
        );
        createIndexIfMissing(
                connection,
                "admin_legacy_matching_job",
                "idx_admin_legacy_matching_job_lookup",
                List.of("research_no", "status", "finished_at"),
                "CREATE INDEX idx_admin_legacy_matching_job_lookup ON admin_legacy_matching_job (research_no, status, finished_at)"
        );
        createIndexIfMissing(
                connection,
                "admin_legacy_matching_result",
                "idx_admin_legacy_matching_result_job",
                List.of("matching_job_id", "row_no"),
                "CREATE INDEX idx_admin_legacy_matching_result_job ON admin_legacy_matching_result (matching_job_id, row_no)"
        );
        createIndexIfMissing(
                connection,
                "admin_legacy_matching_index_job",
                "idx_admin_legacy_matching_index_job_lookup",
                List.of("research_no", "status", "requested_at"),
                "CREATE INDEX idx_admin_legacy_matching_index_job_lookup ON admin_legacy_matching_index_job (research_no, status, requested_at)"
        );
        createIndexIfMissing(
                connection,
                "admin_mail_send_job",
                "idx_admin_mail_send_job_research_id",
                List.of("research_no", "id"),
                "CREATE INDEX idx_admin_mail_send_job_research_id ON admin_mail_send_job (research_no, id)"
        );
        createIndexIfMissing(
                connection,
                "admin_manual_publish_log",
                "idx_admin_manual_publish_log_research_created",
                List.of("research_no", "created_at", "id"),
                "CREATE INDEX idx_admin_manual_publish_log_research_created ON admin_manual_publish_log (research_no, created_at, id)"
        );
        createUniqueIndexIfMissingAndNoDuplicates(
                connection,
                "admin_legacy_mail_rule",
                "uk_admin_legacy_mail_rule_research_no",
                "research_no",
                "CREATE UNIQUE INDEX uk_admin_legacy_mail_rule_research_no ON admin_legacy_mail_rule (research_no)"
        );
        createIndexIfMissing(
                connection,
                "admin_legacy_mail_rule_item",
                "idx_admin_legacy_mail_rule_item_enabled",
                List.of("enabled_yn", "research_no", "threshold_count", "id"),
                "CREATE INDEX idx_admin_legacy_mail_rule_item_enabled ON admin_legacy_mail_rule_item (enabled_yn, research_no, threshold_count, id)"
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
                "idx_admin_notification_log_research_created",
                List.of("research_no", "created_at", "id"),
                "CREATE INDEX idx_admin_notification_log_research_created ON admin_notification_log (research_no, created_at, id)"
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
                "idx_admin_notification_log_duplicate_research",
                List.of("research_no", "application_id", "channel_type", "send_status"),
                "CREATE INDEX idx_admin_notification_log_duplicate_research ON admin_notification_log (research_no, application_id, channel_type, send_status)"
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
                "admin_research_client_link",
                "idx_admin_research_client_link_client_research",
                List.of("client_id", "research_no"),
                "CREATE INDEX idx_admin_research_client_link_client_research ON admin_research_client_link (client_id, research_no)"
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

    private void alterColumnIfVarcharShorter(
            Connection connection,
            String tableName,
            String columnName,
            int minimumLength,
            String ddl
    ) throws Exception {
        if (!hasColumn(connection, tableName, columnName) || getColumnSize(connection, tableName, columnName) >= minimumLength) {
            return;
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(ddl);
            log.info("Expanded column length {}.{} to at least {}.", tableName, columnName, minimumLength);
        }
    }

    private int getColumnSize(Connection connection, String tableName, String columnName) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet columns = metaData.getColumns(connection.getCatalog(), null, tableName, columnName)) {
            if (columns.next()) {
                return columns.getInt("COLUMN_SIZE");
            }
            return 0;
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
