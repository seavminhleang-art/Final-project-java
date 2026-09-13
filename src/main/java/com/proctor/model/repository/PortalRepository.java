package com.proctor.model.repository;

import com.proctor.config.DatabaseConnection;
import com.proctor.model.entity.LeaderboardEntry;
import com.proctor.model.entity.Result;
import com.proctor.model.enums.AssessmentType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PortalRepository {

    public List<Result> getStudentHistory(int studentId) {
        List<Result> list = new ArrayList<>();
        String sql = "SELECT r.id AS result_id, a.id AS attempt_id, a.student_id, u.full_name AS student_name, " +
                     "a.quiz_id, q.title AS quiz_title, q.assessment_type, " +
                     "COALESCE(r.total_points, 0) AS total_points, " +
                     "COALESCE(r.max_points, (SELECT COALESCE(SUM(points), 0) FROM questions qq WHERE qq.quiz_id = a.quiz_id OR qq.id IN (SELECT question_id FROM quiz_questions qqq WHERE qqq.quiz_id = a.quiz_id))) AS max_points, " +
                     "COALESCE(r.percentage, 0) AS percentage, " +
                     "COALESCE(r.passed, FALSE) AS passed, " +
                     "r.graded_at, a.submitted_at, a.status AS attempt_status " +
                     "FROM attempts a " +
                     "LEFT JOIN results r ON a.id = r.attempt_id " +
                     "LEFT JOIN users u ON a.student_id = u.id " +
                     "LEFT JOIN quizzes q ON a.quiz_id = q.id " +
                     "WHERE a.student_id = ? AND a.status != 'IN_PROGRESS' " +
                     "ORDER BY COALESCE(r.graded_at, a.submitted_at, a.started_at) DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String typeStr = rs.getString("assessment_type");
                    AssessmentType type = AssessmentType.QUIZ;
                    if (typeStr != null) {
                        try {
                            type = AssessmentType.valueOf(typeStr.trim().toUpperCase());
                        } catch (IllegalArgumentException ignored) {}
                    }
                    boolean isGraded = rs.getObject("result_id") != null && rs.getTimestamp("graded_at") != null;
                    boolean pendingReview = !isGraded;
                    Timestamp displayDate = rs.getTimestamp("graded_at");
                    if (displayDate == null) {
                        displayDate = rs.getTimestamp("submitted_at");
                    }

                    list.add(Result.builder()
                            .id(rs.getObject("result_id") != null ? rs.getInt("result_id") : null)
                            .attemptId(rs.getInt("attempt_id"))
                            .studentId(rs.getInt("student_id"))
                            .studentName(rs.getString("student_name"))
                            .quizId(rs.getInt("quiz_id"))
                            .quizTitle(rs.getString("quiz_title"))
                            .assessmentType(type)
                            .totalPoints(rs.getDouble("total_points"))
                            .maxPoints(rs.getDouble("max_points"))
                            .percentage(rs.getDouble("percentage"))
                            .passed(rs.getBoolean("passed"))
                            .pendingReview(pendingReview)
                            .gradedAt(displayDate)
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
                     "LEFT JOIN quizzes q ON r.quiz_id = q.id " +
                     "INNER JOIN users u ON r.student_id = u.id " +
                     "WHERE (q.assessment_type IS NULL OR q.assessment_type != 'SPEED') AND u.is_enabled = TRUE AND u.role = 'STUDENT' " +
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

    public List<LeaderboardEntry> getSpeedQuizLeaderboard() {
        List<LeaderboardEntry> leaderboard = new ArrayList<>();
        String sql = "SELECT r.student_id, u.full_name, u.username, " +
                     "COUNT(r.id) AS total_runs, " +
                     "COALESCE(MAX(r.total_points), 0) AS highest_score " +
                     "FROM results r " +
                     "INNER JOIN quizzes q ON r.quiz_id = q.id " +
                     "INNER JOIN users u ON r.student_id = u.id " +
                     "WHERE q.assessment_type = 'SPEED' AND u.is_enabled = TRUE AND u.role = 'STUDENT' " +
                     "GROUP BY r.student_id, u.full_name, u.username " +
                     "ORDER BY highest_score DESC, total_runs DESC, r.student_id ASC LIMIT 50";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            int rank = 1;
            while (rs.next()) {
                double highScore = Math.round(rs.getDouble("highest_score") * 10.0) / 10.0;
                leaderboard.add(LeaderboardEntry.builder()
                        .rank(rank++)
                        .studentId(rs.getInt("student_id"))
                        .studentName(rs.getString("full_name"))
                        .username(rs.getString("username"))
                        .totalQuizzes(rs.getInt("total_runs"))
                        .highScore(highScore)
                        .totalPoints(highScore)
                        .build());
            }
        } catch (SQLException e) {
            System.err.println("Error querying speed quiz leaderboard: " + e.getMessage());
        }
        return leaderboard;
    }
}