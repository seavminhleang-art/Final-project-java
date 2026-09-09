package model.repository.impl;

import db.DbConnection;
import model.entity.Subject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SubjectDaoImpl implements repository.SubjectDao {

    private final DbConnection dbConnection = DbConnection.getInstance();

    @Override
    public Subject create(Subject subject) {
        String sql = "INSERT INTO subjects (name, description, created_by, created_at) VALUES (?, ?, ?, NOW())";
        Connection conn = dbConnection.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, subject.getName());
            ps.setString(2, subject.getDescription());
            ps.setObject(3, subject.getCreatedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) subject.setId(keys.getLong(1));
            }
            return subject;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create subject", e);
        } finally {
            dbConnection.release(conn);
        }
    }

    @Override
    public Optional<Subject> findById(Long id) {
        String sql = "SELECT * FROM subjects WHERE id = ?";
        Connection conn = dbConnection.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find subject", e);
        } finally {
            dbConnection.release(conn);
        }
    }

    @Override
    public List<Subject> findAll() {
        String sql = "SELECT * FROM subjects ORDER BY id";
        Connection conn = dbConnection.borrow();
        List<Subject> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to list subjects", e);
        } finally {
            dbConnection.release(conn);
        }
    }

    @Override
    public Subject update(Subject subject) {
        String sql = "UPDATE subjects SET name = ?, description = ? WHERE id = ?";
        Connection conn = dbConnection.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, subject.getName());
            ps.setString(2, subject.getDescription());
            ps.setLong(3, subject.getId());
            ps.executeUpdate();
            return subject;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update subject", e);
        } finally {
            dbConnection.release(conn);
        }
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM subjects WHERE id = ?";
        Connection conn = dbConnection.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete subject", e);
        } finally {
            dbConnection.release(conn);
        }
    }

    private Subject map(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        return Subject.builder()
                .id(rs.getLong("id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .createdBy((Long) rs.getObject("created_by"))
                .createdAt(created == null ? null : created.toLocalDateTime())
                .build();
    }
}
