package model.repository.impl;


import db.DbConnection;
import model.entity.User;
import model.entity.enums.Role;
import model.repository.UserRepo;


import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepoImpl implements UserRepo {
    private final DbConnection pool = DbConnection.getInstance();

    @Override
    public User create(User user) {
        String sql = """
            INSERT INTO users (username, password_hash, salt, full_name, email, role, active, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            """;
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getSalt());
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getEmail());
            ps.setString(6, user.getRole().name());
            ps.setBoolean(7, user.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getLong(1));
                }
            }
            return user;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create user", e);
        } finally {
            pool.release(conn);
        }
    }

    @Override
    public Optional<User> findById(Long id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find user by id", e);
        } finally {
            pool.release(conn);
        }
    }

    @Override
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find user by username", e);
        } finally {
            pool.release(conn);
        }
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users ORDER BY id";
        Connection conn = pool.borrow();
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(map(rs));
            }
            return users;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to list users", e);
        } finally {
            pool.release(conn);
        }
    }



    @Override
    public List<User> search(String keyword, Role roleFilter) {
        StringBuilder sql = new StringBuilder(
                "SELECT * FROM users WHERE (LOWER(username) LIKE ? OR LOWER(full_name) LIKE ? OR LOWER(email) LIKE ?)");
        if (roleFilter != null) {
            sql.append(" AND role = ?");
        }
        sql.append(" ORDER BY id");

        Connection conn = pool.borrow();
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            String like = "%" + keyword.toLowerCase() + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);
            if (roleFilter != null) {
                ps.setString(4, roleFilter.name());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    users.add(map(rs));
                }
            }
            return users;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to search users", e);
        } finally {
            pool.release(conn);
        }
    }

    @Override
    public User update(User user) {
        String sql = """
            UPDATE users SET full_name = ?, email = ?, role = ?, active = ?, updated_at = NOW()
            WHERE id = ?
            """;
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getRole().name());
            ps.setBoolean(4, user.isActive());
            ps.setLong(5, user.getId());
            ps.executeUpdate();
            return user;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update user", e);
        } finally {
            pool.release(conn);
        }
    }

    @Override
    public boolean updatePassword(Long userId, String newHash, String newSalt) {
        String sql = "UPDATE users SET password_hash = ?, salt = ?, updated_at = NOW() WHERE id = ?";
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setString(2, newSalt);
            ps.setLong(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to reset password", e);
        } finally {
            pool.release(conn);
        }
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete user", e);
        } finally {
            pool.release(conn);
        }
    }

    private User map(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp updated = rs.getTimestamp("updated_at");
        return User.builder()
                .id(rs.getLong("id"))
                .username(rs.getString("username"))
                .passwordHash(rs.getString("password_hash"))
                .salt(rs.getString("salt"))
                .fullName(rs.getString("full_name"))
                .email(rs.getString("email"))
                .role(Role.valueOf(rs.getString("role")))
                .active(rs.getBoolean("active"))
                .createdAt(created == null ? null : created.toLocalDateTime())
                .updatedAt(updated == null ? null : updated.toLocalDateTime())
                .build();
    }
}
