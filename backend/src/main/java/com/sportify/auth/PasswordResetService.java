package com.sportify.auth;

import com.sportify.config.DatabaseConfig;
import com.sportify.email.EmailService;
import com.sportify.users.User;
import com.sportify.users.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PasswordResetService {
    private static final Logger logger = Logger.getLogger(PasswordResetService.class.getName());
    private static final SecureRandom secureRandom = new SecureRandom();

    private final UserRepository userRepository;
    private final DatabaseConfig dbConfig;
    private final EmailService emailService;

    public PasswordResetService(UserRepository userRepository, DatabaseConfig dbConfig, EmailService emailService) {
        this.userRepository = userRepository;
        this.dbConfig = dbConfig;
        this.emailService = emailService;
    }

    public void requestPasswordReset(String email) {
        if (email == null || email.trim().isEmpty()) {
            return;
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            logger.info("Password reset requested for non-existent email address.");
            return;
        }

        User user = userOpt.get();
        String rawToken = generateSecureToken();
        String tokenHash = hashToken(rawToken);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(15, ChronoUnit.MINUTES);

        String sql = "INSERT INTO password_reset_tokens (user_id, token_hash, expires_at) VALUES (?, ?, ?)";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, user.getId());
            ps.setString(2, tokenHash);
            ps.setTimestamp(3, Timestamp.from(expiresAt));
            ps.executeUpdate();

            emailService.sendPasswordResetEmail(user.getEmail(), rawToken);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to create password reset token for user ID: " + user.getId(), e);
        }
    }

    public boolean resetPassword(String rawToken, String newPassword) {
        if (rawToken == null || rawToken.trim().isEmpty() || newPassword == null || newPassword.trim().length() < 8) {
            return false;
        }

        String tokenHash = hashToken(rawToken);
        String newPasswordHash = PasswordHasher.hashPassword(newPassword);

        String selectSql = "SELECT id, user_id, expires_at, used FROM password_reset_tokens WHERE token_hash = ? FOR UPDATE";
        String updateUserSql = "UPDATE users SET password_hash = ? WHERE id = ?";
        String updateTokenSql = "UPDATE password_reset_tokens SET used = TRUE WHERE id = ?";

        Connection conn = null;
        try {
            conn = dbConfig.getConnection();
            conn.setAutoCommit(false);

            Long tokenId = null;
            Long userId = null;
            Instant expiresAt = null;
            boolean used = true;

            try (PreparedStatement psSelect = conn.prepareStatement(selectSql)) {
                psSelect.setString(1, tokenHash);
                try (ResultSet rs = psSelect.executeQuery()) {
                    if (rs.next()) {
                        tokenId = rs.getLong("id");
                        userId = rs.getLong("user_id");
                        Timestamp ts = rs.getTimestamp("expires_at");
                        if (ts != null) expiresAt = ts.toInstant();
                        used = rs.getBoolean("used");
                    }
                }
            }

            if (tokenId == null || used || expiresAt == null || Instant.now().isAfter(expiresAt)) {
                conn.rollback();
                return false;
            }

            try (PreparedStatement psUser = conn.prepareStatement(updateUserSql)) {
                psUser.setString(1, newPasswordHash);
                psUser.setLong(2, userId);
                psUser.executeUpdate();
            }

            try (PreparedStatement psToken = conn.prepareStatement(updateTokenSql)) {
                psToken.setLong(1, tokenId);
                psToken.executeUpdate();
            }

            conn.commit();
            logger.info("Successfully reset password for user ID: " + userId);
            return true;
        } catch (Exception e) {
            if (conn != null) {
                try { conn.rollback(); } catch (Exception ex) { /* ignore */ }
            }
            logger.log(Level.SEVERE, "Error resetting password with token.", e);
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (Exception e) { /* ignore */ }
            }
        }
    }

    public static String generateSecureToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
    }

    public static String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}
