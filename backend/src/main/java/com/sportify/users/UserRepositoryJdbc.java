package com.sportify.users;

import com.sportify.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class UserRepositoryJdbc implements UserRepository {
    private static final Logger logger = Logger.getLogger(UserRepositoryJdbc.class.getName());

    private final DatabaseConfig dbConfig;

    public UserRepositoryJdbc(DatabaseConfig dbConfig) {
        this.dbConfig = dbConfig;
    }

    @Override
    public User save(User user) {
        String sql = "INSERT INTO users (name, email, password_hash, phone, role) VALUES (?, ?, ?, ?, ?) RETURNING id, created_at";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail().toLowerCase().trim());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getPhone());
            ps.setString(5, user.getRole() != null ? user.getRole() : "USER");

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user.setId(rs.getLong("id"));
                    Timestamp ts = rs.getTimestamp("created_at");
                    if (ts != null) {
                        user.setCreatedAt(ts.toInstant().atOffset(ZoneOffset.UTC));
                    }
                }
            }
            return user;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error saving user to DB: " + user.getEmail(), e);
            throw new RuntimeException("Failed to register user in database.", e);
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        String sql = "SELECT id, name, email, password_hash, phone, role, created_at FROM users WHERE id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error fetching user by ID: " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        String normalizedEmail = email.toLowerCase().trim();
        String sql = "SELECT id, name, email, password_hash, phone, role, created_at FROM users WHERE email = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, normalizedEmail);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error fetching user by email: " + normalizedEmail, e);
        }
        return Optional.empty();
    }

    @Override
    public boolean updatePassword(Long userId, String newPasswordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newPasswordHash);
            ps.setLong(2, userId);
            int rowsUpdated = ps.executeUpdate();
            return rowsUpdated > 0;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error updating password for user ID: " + userId, e);
            throw new RuntimeException("Failed to update password in database.", e);
        }
    }

    @Override
    public boolean existsByEmail(String email) {
        if (email == null) return false;
        String normalizedEmail = email.toLowerCase().trim();
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (Connection conn = dbConfig.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, normalizedEmail);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error checking email existence: " + normalizedEmail, e);
        }
        return false;
    }

    private User mapResultSetToUser(ResultSet rs) throws Exception {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setPhone(rs.getString("phone"));
        user.setRole(rs.getString("role"));

        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            user.setCreatedAt(ts.toInstant().atOffset(ZoneOffset.UTC));
        }
        return user;
    }
}
