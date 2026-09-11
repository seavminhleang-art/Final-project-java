package com.proctor.model.repository;

import com.proctor.config.DatabaseConnection;
import com.proctor.model.entity.QuizPerformanceDTO;
import com.proctor.model.entity.SubjectReportDTO;
import com.proctor.model.entity.SystemOverviewDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReportRepository {

    public List<QuizPerformanceDTO> getQuizPerformanceData(Integer subjectId) {
        List<QuizPerformanceDTO> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT q.id, q.title, s.code AS subject_code, " +
                "COUNT(r.id) AS total_attempts, " +
                "COALESCE(AVG(r.percentage), 0) AS avg_score, " +
                "COALESCE(SUM(CASE WHEN r.passed THEN 1 ELSE 0 END)::FLOAT / NULLIF(COUNT(r.id), 0) * 100, 0) AS pass_rate, " +
                "COALESCE(MAX(r.total_points), 0) AS top_score " +
                "FROM quizzes q " +
                "LEFT JOIN subjects s ON q.subject_id = s.id " +
                "LEFT JOIN results r ON q.id = r.quiz_id " +
                "WHERE 1=1"
        );
        if (subjectId != null) {
            sql.append(" AND q.subject_id = ?");
        }
        sql.append(" GROUP BY q.id, q.title, s.code ORDER BY q.id ASC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            if (subjectId != null) {
                stmt.setInt(1, subjectId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(QuizPerformanceDTO.builder()
                            .quizId(rs.getInt("id"))
                            .quizTitle(rs.getString("title"))
                            .subjectCode(rs.getString("subject_code"))
                            .totalAttempts(rs.getInt("total_attempts"))
                            .avgScore(Math.round(rs.getDouble("avg_score") * 10.0) / 10.0)
                            .passRate(Math.round(rs.getDouble("pass_rate") * 10.0) / 10.0)
                            .topScore(rs.getDouble("top_score"))
                            .build());
                }
            }
        } catch (SQLException e) {
            System.err.println("Error querying quiz performance report: " + e.getMessage());
        }
        return list;
    }

    public SystemOverviewDTO getSystemOverviewData() {
        String sql = "SELECT " +
                     "(SELECT COUNT(*) FROM users) AS total_users, " +
                     "(SELECT COUNT(*) FROM users WHERE role = 'TEACHER') AS total_teachers, " +
                     "(SELECT COUNT(*) FROM users WHERE role = 'STUDENT') AS total_students, " +
                     "(SELECT COUNT(*) FROM subjects) AS total_subjects, " +
                     "(SELECT COUNT(*) FROM quizzes) AS total_quizzes, " +
                     "(SELECT COUNT(*) FROM attempts) AS total_attempts, " +
                     "(SELECT COALESCE(SUM(CASE WHEN passed THEN 1 ELSE 0 END)::FLOAT / NULLIF(COUNT(*), 0) * 100, 0) FROM results) AS overall_pass_rate";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return SystemOverviewDTO.builder()
                        .totalUsers(rs.getInt("total_users"))
                        .totalTeachers(rs.getInt("total_teachers"))
                        .totalStudents(rs.getInt("total_students"))
                        .totalSubjects(rs.getInt("total_subjects"))
                        .totalQuizzes(rs.getInt("total_quizzes"))
                        .totalAttempts(rs.getInt("total_attempts"))
                        .overallPassRate(Math.round(rs.getDouble("overall_pass_rate") * 10.0) / 10.0)
                        .build();
            }
        } catch (SQLException e) {
            System.err.println("Error querying system overview report: " + e.getMessage());
        }
        return new SystemOverviewDTO();
    }

    public List<SubjectReportDTO> getSubjectSummaryData() {
        List<SubjectReportDTO> list = new ArrayList<>();
        String sql = "SELECT s.id, s.code, s.name, " +
                     "(SELECT COUNT(DISTINCT q.created_by) FROM quizzes q WHERE q.subject_id = s.id) AS teacher_count, " +
                     "(SELECT COUNT(*) FROM quizzes q WHERE q.subject_id = s.id) AS quiz_count, " +
                     "COUNT(r.id) AS total_attempts, " +
                     "COALESCE(AVG(r.percentage), 0) AS avg_score " +
                     "FROM subjects s " +
                     "LEFT JOIN quizzes q ON s.id = q.subject_id " +
                     "LEFT JOIN results r ON q.id = r.quiz_id " +
                     "GROUP BY s.id, s.code, s.name ORDER BY s.code ASC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                list.add(SubjectReportDTO.builder()
                        .subjectId(rs.getInt("id"))
                        .subjectCode(rs.getString("code"))
                        .subjectName(rs.getString("name"))
                        .teacherCount(rs.getInt("teacher_count"))
                        .quizCount(rs.getInt("quiz_count"))
                        .totalAttempts(rs.getInt("total_attempts"))
                        .avgScore(Math.round(rs.getDouble("avg_score") * 10.0) / 10.0)
                        .build());
            }
        } catch (SQLException e) {
            System.err.println("Error querying subject report: " + e.getMessage());
        }
        return list;
    }
}