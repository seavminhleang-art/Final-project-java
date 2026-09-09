package com.proctor.model.repository;

import com.proctor.config.DatabaseConnection;
import com.proctor.model.entity.Result;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ResultRepository {

    public boolean saveResult(Result result) {
        String sql = "INSERT INTO results (attempt_id, student_id, quiz_id, total_points, max_points, percentage, passed) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                     "ON CONFLICT (attempt_id) DO UPDATE SET " +
                     "total_points = EXCLUDED.total_points, max_points = EXCLUDED.max_points, " +
                     "percentage = EXCLUDED.percentage, passed = EXCLUDED.passed, graded_at = NOW()";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, result.getAttemptId());
            stmt.setInt(2, result.getStudentId());
            stmt.setInt(3, result.getQuizId());
            stmt.setDouble(4, result.getTotalPoints());
            stmt.setDouble(5, result.getMaxPoints());
            stmt.setDouble(6, result.getPercentage());
            stmt.setBoolean(7, result.isPassed());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        result.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error saving result: " + e.getMessage());
        }
        return false;
    }

    public Optional<Result> findByAttemptId(int attemptId) {
        String sql = "SELECT r.id, r.attempt_id, r.student_id, u.full_name AS student_name, " +
                     "r.quiz_id, q.title AS quiz_title, r.total_points, r.max_points, r.percentage, r.passed, r.graded_at " +
                     "FROM results r " +
                     "LEFT JOIN users u ON r.student_id = u.id " +
                     "LEFT JOIN quizzes q ON r.quiz_id = q.id " +
                     "WHERE r.attempt_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, attemptId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding result by attempt id: " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Result> findByStudent(int studentId) {
        List<Result> list = new ArrayList<>();
        String sql = "SELECT r.id, r.attempt_id, r.student_id, u.full_name AS student_name, " +
                     "r.quiz_id, q.title AS quiz_title, r.total_points, r.max_points, r.percentage, r.passed, r.graded_at " +
                     "FROM results r " +
                     "LEFT JOIN users u ON r.student_id = u.id " +
                     "LEFT JOIN quizzes q ON r.quiz_id = q.id " +
                     "WHERE r.student_id = ? ORDER BY r.graded_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding results by student: " + e.getMessage());
        }
        return list;
    }

    public List<Result> findByQuiz(int quizId) {
        List<Result> list = new ArrayList<>();
        String sql = "SELECT r.id, r.attempt_id, r.student_id, u.full_name AS student_name, " +
                     "r.quiz_id, q.title AS quiz_title, r.total_points, r.max_points, r.percentage, r.passed, r.graded_at " +
                     "FROM results r " +
                     "LEFT JOIN users u ON r.student_id = u.id " +
                     "LEFT JOIN quizzes q ON r.quiz_id = q.id " +
                     "WHERE r.quiz_id = ? ORDER BY r.graded_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quizId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding results by quiz: " + e.getMessage());
        }
        return list;
    }

    private Result mapRow(ResultSet rs) throws SQLException {
        return Result.builder()
                .id(rs.getInt("id"))
                .attemptId(rs.getInt("attempt_id"))
                .studentId(rs.getInt("student_id"))
                .studentName(rs.getString("student_name"))
                .quizId(rs.getInt("quiz_id"))
                .quizTitle(rs.getString("quiz_title"))
                .totalPoints(rs.getDouble("total_points"))
                .maxPoints(rs.getDouble("max_points"))
                .percentage(rs.getDouble("percentage"))
                .passed(rs.getBoolean("passed"))
                .gradedAt(rs.getTimestamp("graded_at"))
                .build();
    }
}