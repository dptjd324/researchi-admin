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
            ensureAdminJobMetaColumns(connection);
            ensureAdminMailSendJobColumns(connection);
            ensureAdminJobApplicationExtraAnswerTable(connection);
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
    }

    private void ensureAdminMailSendJobColumns(Connection connection) throws Exception {
        addColumnIfMissing(
                connection,
                "admin_mail_send_job",
                "attachment_type",
                "ALTER TABLE admin_mail_send_job ADD COLUMN attachment_type VARCHAR(20) NOT NULL DEFAULT 'XLSX' AFTER template_id"
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
}
