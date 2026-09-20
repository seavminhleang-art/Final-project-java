package com.proctor.model.repository;

import com.proctor.config.DatabaseConnection;
import com.proctor.exception.DatabaseException;
import com.proctor.model.entity.InboxMessage;
import com.proctor.model.enums.InboxMessageType;
import com.proctor.model.enums.InboxStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InboxRepository {

    public boolean create(InboxMessage message) {
        String sql = "INSERT INTO inbox_messages (sender_id, recipient_id, type, title, body, target_id, proposed_password_hash, status, is_read, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (message.getSenderId() != null) stmt.setInt(1, message.getSenderId());
            else stmt.setNull(1, Types.INTEGER);

            stmt.setInt(2, message.getRecipientId());
            stmt.setString(3, message.getType().name());
            stmt.setString(4, message.getTitle());
            stmt.setString(5, message.getBody());

            if (message.getTargetId() != null) stmt.setInt(6, message.getTargetId());
            else stmt.setNull(6, Types.INTEGER);

            if (message.getProposedPasswordHash() != null) stmt.setString(7, message.getProposedPasswordHash());
            else stmt.setNull(7, Types.VARCHAR);

            stmt.setString(8, message.getStatus() != null ? message.getStatus().name() : InboxStatus.PENDING.name());
            stmt.setBoolean(9, message.isRead());
            stmt.setTimestamp(10, message.getCreatedAt() != null ? message.getCreatedAt() : new Timestamp(System.currentTimeMillis()));

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        message.setId(rs.getInt(1));
                    }
                }
                return true;
            }
            return false;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to create inbox message", e);
        }
    }

    public Optional<InboxMessage> findById(int id) {
        String sql = "SELECT m.id, m.sender_id, u.full_name AS sender_name, u.username AS sender_username, " +
                     "m.recipient_id, m.type, m.title, m.body, m.target_id, m.proposed_password_hash, m.status, m.is_read, m.created_at, m.resolved_at " +
                     "FROM inbox_messages m " +
                     "LEFT JOIN users u ON m.sender_id = u.id " +
                     "WHERE m.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to find inbox message by id: " + id, e);
        }
        return Optional.empty();
    }

    public List<InboxMessage> findByRecipientId(int recipientId) {
        List<InboxMessage> list = new ArrayList<>();
        String sql = "SELECT m.id, m.sender_id, u.full_name AS sender_name, u.username AS sender_username, " +
                     "m.recipient_id, m.type, m.title, m.body, m.target_id, m.proposed_password_hash, m.status, m.is_read, m.created_at, m.resolved_at " +
                     "FROM inbox_messages m " +
                     "LEFT JOIN users u ON m.sender_id = u.id " +
                     "WHERE m.recipient_id = ? " +
                     "ORDER BY m.created_at DESC, m.id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, recipientId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to query inbox messages for recipient id: " + recipientId, e);
        }
        return list;
    }

    public int countUnreadByRecipientId(int recipientId) {
        String sql = "SELECT COUNT(*) FROM inbox_messages WHERE recipient_id = ? AND is_read = FALSE";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, recipientId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to count unread inbox messages for recipient id: " + recipientId, e);
        }
        return 0;
    }

    public boolean markAsRead(int id) {
        String sql = "UPDATE inbox_messages SET is_read = TRUE WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to mark inbox message as read for id: " + id, e);
        }
    }

    public boolean markAllAsRead(int recipientId) {
        String sql = "UPDATE inbox_messages SET is_read = TRUE WHERE recipient_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, recipientId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to mark all inbox messages as read for recipient id: " + recipientId, e);
        }
    }

    public boolean updateStatus(int id, InboxStatus status, Timestamp resolvedAt) {
        String sql = "UPDATE inbox_messages SET status = ?, resolved_at = ?, is_read = TRUE WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setTimestamp(2, resolvedAt);
            stmt.setInt(3, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to update status for inbox message id: " + id, e);
        }
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM inbox_messages WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to delete inbox message id: " + id, e);
        }
    }

    public boolean hasPendingRequest(int senderId, InboxMessageType type, int targetId) {
        String sql = "SELECT COUNT(*) FROM inbox_messages WHERE sender_id = ? AND type = ? AND target_id = ? AND status = 'PENDING'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, senderId);
            stmt.setString(2, type.name());
            stmt.setInt(3, targetId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Failed to check pending request for sender id: " + senderId, e);
        }
        return false;
    }

    private InboxMessage mapRow(ResultSet rs) throws SQLException {
        String senderName = rs.getString("sender_name");
        String senderUsername = rs.getString("sender_username");
        String displaySender = senderUsername != null ? "@" + senderUsername : (senderName != null ? senderName : "System");
        String proposedHash = rs.getString("proposed_password_hash");

        return InboxMessage.builder()
                .id(rs.getInt("id"))
                .senderId(rs.getObject("sender_id") != null ? rs.getInt("sender_id") : null)
                .senderName(displaySender)
                .recipientId(rs.getInt("recipient_id"))
                .type(InboxMessageType.valueOf(rs.getString("type")))
                .title(rs.getString("title"))
                .body(rs.getString("body"))
                .targetId(rs.getObject("target_id") != null ? rs.getInt("target_id") : null)
                .proposedPasswordHash(proposedHash)
                .status(InboxStatus.valueOf(rs.getString("status")))
                .read(rs.getBoolean("is_read"))
                .createdAt(rs.getTimestamp("created_at"))
                .resolvedAt(rs.getTimestamp("resolved_at"))
                .build();
    }
}