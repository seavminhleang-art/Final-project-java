package com.proctor.model.repository;

import com.proctor.config.DatabaseConnection;
import com.proctor.exception.DatabaseException;
import com.proctor.model.entity.EmailVerificationToken;

import java.sql.*;
import java.util.Optional;

public class EmailVerificationRepository {

    public EmailVerificationToken saveToken(String email, String code, String purpose, Timestamp expiresAt) {
        String sql = "INSERT INTO email_verification_tokens (email, code, purpose, expires_at) " +
                     "VALUES (?, ?, ?, ?) RETURNING id, created_at, used";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email.trim().toLowerCase());
            stmt.setString(2, code.trim());
            stmt.setString(3, purpose.trim().toUpperCase());
            stmt.setTimestamp(4, expiresAt);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return EmailVerificationToken.builder()
                            .id(rs.getInt("id"))
                            .email(email.trim().toLowerCase())
                            .code(code.trim())
                            .purpose(purpose.trim().toUpperCase())
                            .expiresAt(expiresAt)
                            .used(rs.getBoolean("used"))
                            .createdAt(rs.getTimestamp("created_at"))
                            .build();
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to save email verification token", e);
        }
        return null;
    }

    public Optional<EmailVerificationToken> findLatestValidToken(String email, String code, String purpose) {
        if (email == null || code == null || purpose == null) return Optional.empty();
        String sql = "SELECT id, email, code, purpose, expires_at, used, created_at " +
                     "FROM email_verification_tokens " +
                     "WHERE LOWER(email) = LOWER(?) AND code = ? AND purpose = ? AND used = FALSE AND expires_at > CURRENT_TIMESTAMP " +
                     "ORDER BY id DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email.trim().toLowerCase());
            stmt.setString(2, code.trim());
            stmt.setString(3, purpose.trim().toUpperCase());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find valid verification token for: " + email, e);
        }
        return Optional.empty();
    }

    public boolean markTokenUsed(int id) {
        String sql = "UPDATE email_verification_tokens SET used = TRUE WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to mark verification token as used: " + id, e);
        }
    }

    public void invalidatePendingTokens(String email, String purpose) {
        if (email == null || purpose == null) return;
        String sql = "UPDATE email_verification_tokens SET used = TRUE " +
                     "WHERE LOWER(email) = LOWER(?) AND purpose = ? AND used = FALSE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email.trim().toLowerCase());
            stmt.setString(2, purpose.trim().toUpperCase());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Failed to invalidate pending tokens for: " + email, e);
        }
    }

    private EmailVerificationToken mapRow(ResultSet rs) throws SQLException {
        return EmailVerificationToken.builder()
                .id(rs.getInt("id"))
                .email(rs.getString("email"))
                .code(rs.getString("code"))
                .purpose(rs.getString("purpose"))
                .expiresAt(rs.getTimestamp("expires_at"))
                .used(rs.getBoolean("used"))
                .createdAt(rs.getTimestamp("created_at"))
                .build();
    }
}
