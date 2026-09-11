package com.proctor.model.repository;

import com.proctor.config.DatabaseConnection;
import com.proctor.model.entity.Subject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class SubjectRepository {

    public List<Subject> findAll(String search) {
        List<Subject> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT id, code, name, description, is_enabled, created_at FROM subjects WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(code) LIKE ? OR LOWER(name) LIKE ?)");
            String pattern = "%" + search.trim().toLowerCase() + "%";
            params.add(pattern);
            params.add(pattern);
        }

        sql.append(" ORDER BY code ASC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error querying subjects: " + e.getMessage());
        }
        return list;
    }

    public Optional<Subject> findById(int id) {
        String sql = "SELECT id, code, name, description, is_enabled, created_at FROM subjects WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding subject by id: " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<Subject> findByCode(String code) {
        String sql = "SELECT id, code, name, description, is_enabled, created_at FROM subjects WHERE LOWER(code) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, code);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding subject by code: " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<Subject> findByName(String name) {
        String sql = "SELECT id, code, name, description, is_enabled, created_at FROM subjects WHERE LOWER(name) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding subject by name: " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean create(Subject subject) {
        String sql = "INSERT INTO subjects (code, name, description, is_enabled) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, subject.getCode().toUpperCase().trim());
            stmt.setString(2, subject.getName().trim());
            stmt.setString(3, subject.getDescription());
            stmt.setBoolean(4, subject.isEnabled());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        subject.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error creating subject: " + e.getMessage());
        }
        return false;
    }

    public boolean update(Subject subject) {
        String sql = "UPDATE subjects SET code = ?, name = ?, description = ?, is_enabled = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, subject.getCode().toUpperCase().trim());
            stmt.setString(2, subject.getName().trim());
            stmt.setString(3, subject.getDescription());
            stmt.setBoolean(4, subject.isEnabled());
            stmt.setInt(5, subject.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating subject: " + e.getMessage());
        }
        return false;
    }

    public boolean toggleEnabled(int subjectId) {
        String sql = "UPDATE subjects SET is_enabled = NOT is_enabled WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, subjectId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error toggling subject status: " + e.getMessage());
        }
        return false;
    }

    public boolean delete(int subjectId) {
        String unlinkQuizzes = "UPDATE quizzes SET subject_id = NULL WHERE subject_id = ?";
        String unlinkQuestions = "UPDATE questions SET subject_id = NULL WHERE subject_id = ?";
        String delSubject = "DELETE FROM subjects WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement s1 = conn.prepareStatement(unlinkQuizzes);
                 PreparedStatement s2 = conn.prepareStatement(unlinkQuestions);
                 PreparedStatement s3 = conn.prepareStatement(delSubject)) {
                s1.setInt(1, subjectId);
                s1.executeUpdate();

                s2.setInt(1, subjectId);
                s2.executeUpdate();

                s3.setInt(1, subjectId);
                int affected = s3.executeUpdate();
                conn.commit();
                conn.setAutoCommit(true);
                return affected > 0;
            } catch (SQLException e) {
                conn.rollback();
                conn.setAutoCommit(true);
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting subject: " + e.getMessage());
        }
        return false;
    }

    private Subject mapRow(ResultSet rs) throws SQLException {
        return Subject.builder()
                .id(rs.getInt("id"))
                .code(rs.getString("code"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .enabled(rs.getBoolean("is_enabled"))
                .createdAt(rs.getTimestamp("created_at"))
                .build();
    }
}