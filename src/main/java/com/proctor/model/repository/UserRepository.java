package com.proctor.model.repository;

import com.proctor.model.entity.User;
import com.proctor.model.enums.Role;
import com.proctor.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserRepository {

    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        String sql = "SELECT id, email, username, password_hash, full_name, role, is_enabled, date_of_birth, gender, academic_degree, education_background, specialization, created_at, updated_at " +
                     "FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by email: " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) return Optional.empty();
        String sql = "SELECT id, email, username, password_hash, full_name, role, is_enabled, date_of_birth, gender, academic_degree, education_background, specialization, created_at, updated_at " +
                     "FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username.trim());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by username: " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<User> findByEmailOrUsername(String identifier) {
        if (identifier == null || identifier.isBlank()) return Optional.empty();
        String clean = identifier.trim();
        String sql = "SELECT id, email, username, password_hash, full_name, role, is_enabled, date_of_birth, gender, academic_degree, education_background, specialization, created_at, updated_at " +
                     "FROM users WHERE LOWER(email) = LOWER(?) OR LOWER(username) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, clean);
            stmt.setString(2, clean);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding user: " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<User> findById(int id) {
        String sql = "SELECT id, email, username, password_hash, full_name, role, is_enabled, date_of_birth, gender, academic_degree, education_background, specialization, created_at, updated_at " +
                     "FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by id: " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<User> findAll(String search, Role role) {
        List<User> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT id, email, username, password_hash, full_name, role, is_enabled, date_of_birth, gender, academic_degree, education_background, specialization, created_at, updated_at FROM users WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(email) LIKE ? OR LOWER(username) LIKE ? OR LOWER(full_name) LIKE ?)");
            String pattern = "%" + search.trim().toLowerCase() + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (role != null) {
            sql.append(" AND role = ?");
            params.add(role.name());
        }

        sql.append(" ORDER BY id ASC");

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
            System.err.println("Error querying users: " + e.getMessage());
        }
        return list;
    }

    public boolean create(User user) {
        String email = user.getEmail() != null ? user.getEmail().trim() : "";
        String username = user.getUsername() != null ? user.getUsername().trim() : "";

        if (email.isBlank() && !username.isBlank()) email = username;
        if (username.isBlank() && !email.isBlank()) username = email.contains("@") ? email.split("@")[0] : email;

        String sql = "INSERT INTO users (email, username, password_hash, full_name, role, is_enabled, date_of_birth, gender, academic_degree, education_background, specialization) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, email);
            stmt.setString(2, username);
            stmt.setString(3, user.getPasswordHash());
            stmt.setString(4, user.getFullName());
            stmt.setString(5, user.getRole().name());
            stmt.setBoolean(6, user.isEnabled());
            if (user.getDateOfBirth() != null) {
                stmt.setDate(7, java.sql.Date.valueOf(user.getDateOfBirth()));
            } else {
                stmt.setNull(7, java.sql.Types.DATE);
            }
            stmt.setString(8, user.getGender());
            stmt.setString(9, user.getAcademicDegree());
            stmt.setString(10, user.getEducationBackground());
            stmt.setString(11, user.getSpecialization());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
        }
        return false;
    }

    public boolean update(User user) {
        String sql = "UPDATE users SET full_name = ?, role = ?, is_enabled = ?, date_of_birth = ?, gender = ?, academic_degree = ?, education_background = ?, specialization = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getFullName());
            stmt.setString(2, user.getRole().name());
            stmt.setBoolean(3, user.isEnabled());
            if (user.getDateOfBirth() != null) {
                stmt.setDate(4, java.sql.Date.valueOf(user.getDateOfBirth()));
            } else {
                stmt.setNull(4, java.sql.Types.DATE);
            }
            stmt.setString(5, user.getGender());
            stmt.setString(6, user.getAcademicDegree());
            stmt.setString(7, user.getEducationBackground());
            stmt.setString(8, user.getSpecialization());
            stmt.setInt(9, user.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
        }
        return false;
    }

    public boolean updatePassword(int userId, String newPasswordHash) {
        String sql = "UPDATE users SET password_hash = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newPasswordHash);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating password: " + e.getMessage());
        }
        return false;
    }

    public boolean toggleEnabled(int userId) {
        String sql = "UPDATE users SET is_enabled = NOT is_enabled, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error toggling user status: " + e.getMessage());
        }
        return false;
    }

    public boolean delete(int userId) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
        }
        return false;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        String email = null;
        try {
            email = rs.getString("email");
        } catch (SQLException ignored) {}

        String username = null;
        try {
            username = rs.getString("username");
        } catch (SQLException ignored) {}

        String gender = null;
        try {
            gender = rs.getString("gender");
        } catch (SQLException ignored) {}

        String academicDegree = null;
        try {
            academicDegree = rs.getString("academic_degree");
        } catch (SQLException ignored) {}

        String educationBackground = null;
        try {
            educationBackground = rs.getString("education_background");
        } catch (SQLException ignored) {}

        String specialization = null;
        try {
            specialization = rs.getString("specialization");
        } catch (SQLException ignored) {}

        return User.builder()
                .id(rs.getInt("id"))
                .email(email != null ? email : username)
                .username(username != null ? username : email)
                .passwordHash(rs.getString("password_hash"))
                .fullName(rs.getString("full_name"))
                .dateOfBirth(rs.getDate("date_of_birth") != null ? rs.getDate("date_of_birth").toLocalDate() : null)
                .gender(gender)
                .role(Role.valueOf(rs.getString("role")))
                .enabled(rs.getBoolean("is_enabled"))
                .academicDegree(academicDegree)
                .educationBackground(educationBackground)
                .specialization(specialization)
                .createdAt(rs.getTimestamp("created_at"))
                .updatedAt(rs.getTimestamp("updated_at"))
                .build();
    }
}