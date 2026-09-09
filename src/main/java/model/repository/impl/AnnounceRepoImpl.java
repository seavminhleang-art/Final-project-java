package model.repository.impl;

import db.DbConnection;
import model.entity.Announcement;
import model.entity.enums.AnnouncementType;
import model.entity.enums.Role;
import model.repository.AnnounceRepo;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AnnounceRepoImpl implements AnnounceRepo {
private final DbConnection dbConnection = DbConnection.getInstance();
    @Override
    public Announcement create(Announcement announcement) {
        String sql = """
        INSERT INTO announcements
            (user_id, role_target, quiz_id, attempt_id, message, type, posted_by, is_read, created_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, FALSE, ?)
        """;
        Connection conn = dbConnection.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, announcement.getUserId());
            ps.setString(2, announcement.getRoleTarget() == null ? null : announcement.getRoleTarget().name());
            ps.setObject(3, announcement.getQuizId());
            ps.setObject(4, announcement.getAttemptId());
            ps.setString(5, announcement.getMessage());
            ps.setString(6, announcement.getType().name());
            ps.setObject(7, announcement.getPostedBy());
            ps.setTimestamp(8, Timestamp.valueOf(java.time.LocalDateTime.now()));

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return announcement.toBuilder().id(keys.getLong(1)).build();
                }
            }
            return announcement;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create announcement", e);
        } finally {
            dbConnection.release(conn);
        }
    }

    @Override
    public List<Announcement> findForUser(Long userId, Role role) {
        String sql = """
        SELECT * FROM announcements
        WHERE user_id = ?
           OR (user_id IS NULL AND (role_target IS NULL OR role_target = ?))
        ORDER BY created_at DESC
        """;
        Connection conn = dbConnection.borrow();
        List<Announcement> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to fetch announcements", e);
        } finally {
            dbConnection.release(conn);
        }
    }

    @Override
    public List<Announcement> findPostedBy(Long postedBy) {
        String sql = "SELECT * FROM announcements WHERE posted_by = ? ORDER BY created_at DESC";
        Connection conn = dbConnection.borrow();
        List<Announcement> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, postedBy);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to fetch posted announcements", e);
        } finally {
            dbConnection.release(conn);
        }
    }

    @Override
    public boolean markRead(Long announcementId, Long userId) {
        String sql = "UPDATE announcements SET is_read = TRUE WHERE id = ? AND user_id = ?";
        Connection conn = dbConnection.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, announcementId);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to mark announcement read", e);
        } finally {
            dbConnection.release(conn);
        }
    }

    @Override
    public int countUnreadForUser(Long userId, Role role) {
        String sql = """
            SELECT COUNT(*) AS cnt FROM announcements
            WHERE (user_id = ? AND is_read = FALSE)
               OR (user_id IS NULL AND (role_target IS NULL OR role_target = ?))
            """;
        Connection conn = dbConnection.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, role.name());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("cnt");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to count unread announcements", e);
        } finally {
            dbConnection.release(conn);
        }
    }
    private Announcement map(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        String roleTarget = rs.getString("role_target");
        return Announcement.builder()
                .id(rs.getLong("id"))
                .userId((Long) rs.getObject("user_id"))
                .roleTarget(roleTarget == null ? null : Role.valueOf(roleTarget))
                .quizId((Long) rs.getObject("quiz_id"))
                .attemptId((Long) rs.getObject("attempt_id"))
                .message(rs.getString("message"))
                .type(AnnouncementType.valueOf(rs.getString("type")))
                .postedBy((Long) rs.getObject("posted_by"))
                .read(rs.getBoolean("is_read"))
                .createdAt(created == null ? null : created.toLocalDateTime())
                .build();
    }
}
