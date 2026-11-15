package com.sportify.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.io.File;
import java.util.logging.Logger;

public class AppConfig {
    private static final Logger logger = Logger.getLogger(AppConfig.class.getName());

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;
    private final String jwtSecret;
    private final int jwtExpirationMinutes;
    private final int dbPoolMaxSize;
    private final String smtpHost;
    private final int smtpPort;
    private final String smtpUser;
    private final String smtpPassword;
    private final String smtpFrom;

    public AppConfig() {
        Dotenv dotenv = null;
        File envFile = new File(".env");
        if (envFile.exists()) {
            dotenv = Dotenv.configure().ignoreIfMissing().load();
        }

        this.dbUrl = getEnv(dotenv, "DB_URL", "jdbc:postgresql://localhost:5432/Sportity");
        this.dbUser = getEnv(dotenv, "DB_USER", "postgres");
        this.dbPassword = getEnv(dotenv, "DB_PASSWORD", "12345");

        this.jwtSecret = getEnv(dotenv, "JWT_SECRET", "default_jwt_secret_key_for_sportify_2026");
        if (this.jwtSecret == null || this.jwtSecret.trim().isEmpty()) {
            throw new IllegalStateException("JWT_SECRET environment variable is missing and must be configured.");
        }

        this.jwtExpirationMinutes = Integer.parseInt(getEnv(dotenv, "JWT_EXPIRATION_MINUTES", "60"));
        this.dbPoolMaxSize = Integer.parseInt(getEnv(dotenv, "DB_POOL_MAX_SIZE", "10"));

        this.smtpHost = getEnv(dotenv, "SMTP_HOST", "");
        this.smtpPort = Integer.parseInt(getEnv(dotenv, "SMTP_PORT", "587"));
        this.smtpUser = getEnv(dotenv, "SMTP_USER", "");
        this.smtpPassword = getEnv(dotenv, "SMTP_PASSWORD", "");
        this.smtpFrom = getEnv(dotenv, "SMTP_FROM", "noreply@sportify.com");

        logger.info("Application configuration loaded successfully. Target DB URL: " + dbUrl);
    }

    private static String getEnv(Dotenv dotenv, String key, String defaultValue) {
        String propVal = System.getProperty(key);
        if (propVal != null && !propVal.trim().isEmpty()) {
            return propVal.trim();
        }
        String sysVal = System.getenv(key);
        if (sysVal != null && !sysVal.trim().isEmpty()) {
            return sysVal.trim();
        }
        if (dotenv != null) {
            String dotVal = dotenv.get(key);
            if (dotVal != null && !dotVal.trim().isEmpty()) {
                return dotVal.trim();
            }
        }
        return defaultValue;
    }

    public String getDbUrl() { return dbUrl; }
    public String getDbUser() { return dbUser; }
    public String getDbPassword() { return dbPassword; }
    public String getJwtSecret() { return jwtSecret; }
    public int getJwtExpirationMinutes() { return jwtExpirationMinutes; }
    public int getDbPoolMaxSize() { return dbPoolMaxSize; }
    public String getSmtpHost() { return smtpHost; }
    public int getSmtpPort() { return smtpPort; }
    public String getSmtpUser() { return smtpUser; }
    public String getSmtpPassword() { return smtpPassword; }
    public String getSmtpFrom() { return smtpFrom; }
}
