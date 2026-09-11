package com.proctor.model.repository;

import com.proctor.model.enums.AttemptStatus;
import com.proctor.config.DatabaseConnection;
import com.proctor.model.entity.Attempt;
import com.proctor.model.entity.AttemptAnswer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AttemptRepository {

    public Attempt createAttempt(int quizId, int studentId) {
        String sql = "INSERT INTO attempts (quiz_id, student_id, started_at, status) VALUES (?, ?, NOW(), 'IN_PROGRESS')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, quizId);
            stmt.setInt(2, studentId);

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return Attempt.builder()
                                .id(rs.getInt(1))
                                .quizId(quizId)
                                .studentId(studentId)
                                .startedAt(new Timestamp(System.currentTimeMillis()))
                                .status(AttemptStatus.IN_PROGRESS)
                                .build();
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating attempt: " + e.getMessage());
        }
        return null;
    }

    public Optional<Attempt> getAttempt(int attemptId) {
        String sql = "SELECT a.id, a.quiz_id, q.title AS quiz_title, a.student_id, a.started_at, a.submitted_at, a.status " +
                     "FROM attempts a LEFT JOIN quizzes q ON a.quiz_id = q.id WHERE a.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, attemptId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding attempt: " + e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<Attempt> findLatestAttempt(int quizId, int studentId) {
        String sql = "SELECT a.id, a.quiz_id, q.title AS quiz_title, a.student_id, a.started_at, a.submitted_at, a.status " +
                     "FROM attempts a LEFT JOIN quizzes q ON a.quiz_id = q.id " +
                     "WHERE a.quiz_id = ? AND a.student_id = ? ORDER BY a.id DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quizId);
            stmt.setInt(2, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding latest attempt: " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<Attempt> getAttemptsByQuiz(int quizId) {
        List<Attempt> list = new ArrayList<>();
        String sql = "SELECT a.id, a.quiz_id, q.title AS quiz_title, a.student_id, u.full_name AS student_name, " +
                     "a.started_at, a.submitted_at, a.status " +
                     "FROM attempts a " +
                     "LEFT JOIN quizzes q ON a.quiz_id = q.id " +
                     "LEFT JOIN users u ON a.student_id = u.id " +
                     "WHERE a.quiz_id = ? ORDER BY a.id DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quizId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Attempt a = mapRow(rs);
                    list.add(a);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error querying quiz attempts: " + e.getMessage());
        }
        return list;
    }

    public List<Attempt> getAllSubmissions(Integer teacherId) {
        List<Attempt> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT a.id, a.quiz_id, q.title AS quiz_title, a.student_id, u.full_name AS student_name, " +
                "a.started_at, a.submitted_at, a.status " +
                "FROM attempts a " +
                "LEFT JOIN quizzes q ON a.quiz_id = q.id " +
                "LEFT JOIN users u ON a.student_id = u.id " +
                "WHERE a.status != 'IN_PROGRESS'"
        );
        if (teacherId != null) {
            sql.append(" AND q.created_by = ?");
        }
        sql.append(" ORDER BY COALESCE(a.submitted_at, a.started_at) DESC, a.id DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            if (teacherId != null) {
                stmt.setInt(1, teacherId);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error querying all submissions: " + e.getMessage());
        }
        return list;
    }

    public boolean saveAnswer(int attemptId, int questionId, Integer selectedOptionId, String textAnswer) {
        String checkSql = "SELECT id FROM attempt_answers WHERE attempt_id = ? AND question_id = ?";
        String updateSql = "UPDATE attempt_answers SET selected_option_id = ?, text_answer = ? WHERE id = ?";
        String insertSql = "INSERT INTO attempt_answers (attempt_id, question_id, selected_option_id, text_answer) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            Integer existingId = null;
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setInt(1, attemptId);
                checkStmt.setInt(2, questionId);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        existingId = rs.getInt("id");
                    }
                }
            }

            if (existingId != null) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    if (selectedOptionId != null) updateStmt.setInt(1, selectedOptionId);
                    else updateStmt.setNull(1, Types.INTEGER);
                    updateStmt.setString(2, textAnswer);
                    updateStmt.setInt(3, existingId);
                    return updateStmt.executeUpdate() > 0;
                }
            } else {
                try (PreparedStatement insStmt = conn.prepareStatement(insertSql)) {
                    insStmt.setInt(1, attemptId);
                    insStmt.setInt(2, questionId);
                    if (selectedOptionId != null) insStmt.setInt(3, selectedOptionId);
                    else insStmt.setNull(3, Types.INTEGER);
                    insStmt.setString(4, textAnswer);
                    return insStmt.executeUpdate() > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error saving answer: " + e.getMessage());
        }
        return false;
    }

    public List<AttemptAnswer> getAttemptAnswers(int attemptId) {
        List<AttemptAnswer> list = new ArrayList<>();
        String sql = "SELECT id, attempt_id, question_id, selected_option_id, text_answer, ai_score, ai_feedback, teacher_feedback, is_correct, points_awarded " +
                     "FROM attempt_answers WHERE attempt_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, attemptId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(AttemptAnswer.builder()
                            .id(rs.getInt("id"))
                            .attemptId(rs.getInt("attempt_id"))
                            .questionId(rs.getInt("question_id"))
                            .selectedOptionId(rs.getObject("selected_option_id") != null ? rs.getInt("selected_option_id") : null)
                            .textAnswer(rs.getString("text_answer"))
                            .aiScore(rs.getObject("ai_score") != null ? rs.getInt("ai_score") : null)
                            .aiFeedback(rs.getString("ai_feedback"))
                            .teacherFeedback(rs.getString("teacher_feedback"))
                            .correct(rs.getObject("is_correct") != null ? rs.getBoolean("is_correct") : null)
                            .pointsAwarded(rs.getDouble("points_awarded"))
                            .build());
                }
            }
        } catch (SQLException e) {
            System.err.println("Error querying attempt answers: " + e.getMessage());
        }
        return list;
    }

    public boolean updateAnswerGrade(int answerId, boolean correct, double points, Integer aiScore, String aiFeedback, String teacherFeedback) {
        String sql = "UPDATE attempt_answers SET is_correct = ?, points_awarded = ?, ai_score = ?, ai_feedback = ?, teacher_feedback = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, correct);
            stmt.setDouble(2, points);
            if (aiScore != null) stmt.setInt(3, aiScore);
            else stmt.setNull(3, Types.INTEGER);
            stmt.setString(4, aiFeedback);
            stmt.setString(5, teacherFeedback);
            stmt.setInt(6, answerId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating answer grade: " + e.getMessage());
        }
        return false;
    }

    public boolean finalizeAttempt(int attemptId, AttemptStatus status) {
        String sql = "UPDATE attempts SET submitted_at = NOW(), status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status.name());
            stmt.setInt(2, attemptId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error finalizing attempt: " + e.getMessage());
        }
        return false;
    }

    public boolean deleteAttemptsForStudent(int quizId, int studentId) {
        String deleteAnswers = "DELETE FROM attempt_answers WHERE attempt_id IN (SELECT id FROM attempts WHERE quiz_id = ? AND student_id = ?)";
        String deleteResults = "DELETE FROM results WHERE attempt_id IN (SELECT id FROM attempts WHERE quiz_id = ? AND student_id = ?)";
        String deleteAttempts = "DELETE FROM attempts WHERE quiz_id = ? AND student_id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement stmt = conn.prepareStatement(deleteAnswers)) {
                stmt.setInt(1, quizId);
                stmt.setInt(2, studentId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(deleteResults)) {
                stmt.setInt(1, quizId);
                stmt.setInt(2, studentId);
                stmt.executeUpdate();
            }
            try (PreparedStatement stmt = conn.prepareStatement(deleteAttempts)) {
                stmt.setInt(1, quizId);
                stmt.setInt(2, studentId);
                return stmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting attempts for student: " + e.getMessage());
        }
        return false;
    }

    private Attempt mapRow(ResultSet rs) throws SQLException {
        String statusStr = rs.getString("status");
        AttemptStatus status;
        try {
            status = AttemptStatus.valueOf(statusStr);
        } catch (Exception e) {
            status = AttemptStatus.TURNED_IN;
        }
        String studentName = null;
        try {
            studentName = rs.getString("student_name");
        } catch (SQLException ignored) {}

        return Attempt.builder()
                .id(rs.getInt("id"))
                .quizId(rs.getInt("quiz_id"))
                .quizTitle(rs.getString("quiz_title"))
                .studentId(rs.getInt("student_id"))
                .studentName(studentName)
                .startedAt(rs.getTimestamp("started_at"))
                .submittedAt(rs.getTimestamp("submitted_at"))
                .status(status)
                .build();
    }
}