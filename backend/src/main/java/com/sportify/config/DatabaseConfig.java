package com.sportify.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConfig {
    private static final Logger logger = Logger.getLogger(DatabaseConfig.class.getName());

    private final HikariDataSource dataSource;

    public DatabaseConfig(AppConfig appConfig) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(appConfig.getDbUrl());
        hikariConfig.setUsername(appConfig.getDbUser());
        hikariConfig.setPassword(appConfig.getDbPassword());
        hikariConfig.setMaximumPoolSize(appConfig.getDbPoolMaxSize());
        hikariConfig.setMinimumIdle(2);
        hikariConfig.setIdleTimeout(30000);
        hikariConfig.setConnectionTimeout(10000);
        hikariConfig.setDriverClassName("org.postgresql.Driver");

        this.dataSource = new HikariDataSource(hikariConfig);
        verifyConnection();
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public Connection getConnection() throws Exception {
        return dataSource.getConnection();
    }

    public void verifyConnection() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            if (rs.next()) {
                logger.info("Database connection pool initialized and verified successfully.");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to connect to PostgreSQL database.", e);
            throw new RuntimeException("Database connection verification failed.", e);
        }
    }

    public void runMigrations() {
        logger.info("Running SQL migrations...");
        String[] migrationFiles = {
            "db/migrations/V1__create_users_table.sql",
            "db/migrations/V2__create_password_reset_tokens_table.sql"
        };

        for (String filePath : migrationFiles) {
            runSqlMigrationFile(filePath);
        }
    }

    private void runSqlMigrationFile(String relativePath) {
        File file = new File(relativePath);
        StringBuilder sql = new StringBuilder();

        try {
            if (file.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        sql.append(line).append("\n");
                    }
                }
            } else {
                InputStream is = getClass().getClassLoader().getResourceAsStream(relativePath);
                if (is != null) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            sql.append(line).append("\n");
                        }
                    }
                } else {
                    logger.warning("Migration file not found: " + relativePath);
                    return;
                }
            }

            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql.toString());
                logger.info("Successfully executed migration: " + relativePath);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error executing migration " + relativePath, e);
            throw new RuntimeException("Migration failed: " + relativePath, e);
        }
    }

    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed successfully.");
        }
    }
}
