package com.proctor.model.repository;

import com.proctor.config.DatabaseConnection;
import com.proctor.model.entity.LeaderboardEntry;
import com.proctor.model.entity.Result;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PortalRepository {

    public List<Result> getStudentHistory(int studentId) {
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
                    list.add(Result.builder()
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
                            .build());
                }
            }
        } catch (SQLException e) {
            System.err.println("Error querying student history: " + e.getMessage());
        }
        return list;
    }

    public List<LeaderboardEntry> getGlobalLeaderboard() {
        List<LeaderboardEntry> leaderboard = new ArrayList<>();
        String sql = "SELECT r.student_id, u.full_name, u.username, " +
                     "COUNT(r.id) AS total_quizzes, " +
                     "COALESCE(SUM(r.total_points), 0) AS total_points, " +
                     "COALESCE(AVG(r.percentage), 0) AS avg_percentage " +
                     "FROM results r " +
                     "INNER JOIN users u ON r.student_id = u.id " +
                     "WHERE u.is_enabled = TRUE AND u.role = 'STUDENT' " +
                     "GROUP BY r.student_id, u.full_name, u.username " +
                     "ORDER BY total_points DESC, avg_percentage DESC, r.student_id ASC LIMIT 50";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            int rank = 1;
            while (rs.next()) {
                leaderboard.add(LeaderboardEntry.builder()
                        .rank(rank++)
                        .studentId(rs.getInt("student_id"))
                        .studentName(rs.getString("full_name"))
                        .username(rs.getString("username"))
                        .totalQuizzes(rs.getInt("total_quizzes"))
                        .totalPoints(Math.round(rs.getDouble("total_points") * 10.0) / 10.0)
                        .avgPercentage(Math.round(rs.getDouble("avg_percentage") * 10.0) / 10.0)
                        .build());
            }
        } catch (SQLException e) {
            System.err.println("Error querying global leaderboard: " + e.getMessage());
        }
        return leaderboard;
    }
}