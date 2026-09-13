package com.proctor.model.repository;

import com.proctor.model.enums.AssessmentType;
import com.proctor.model.enums.Difficulty;
import com.proctor.model.enums.QuestionType;
import com.proctor.model.enums.Role;
import com.proctor.config.DatabaseConnection;
import com.proctor.model.entity.Question;
import com.proctor.model.entity.QuestionOption;
import com.proctor.model.entity.Quiz;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QuizRepository {

    public List<Quiz> findAll(AssessmentType assessmentType, Integer subjectId, Integer createdBy, Boolean published, String search, boolean activeOnly) {
        return queryDatabase(assessmentType, subjectId, createdBy, published, search, activeOnly);
    }

    private List<Quiz> queryDatabase(AssessmentType assessmentType, Integer subjectId, Integer createdBy, Boolean published, String search, boolean activeOnly) {
        List<Quiz> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT q.id, q.subject_id, s.code AS subject_code, q.created_by, u.full_name AS creator_name, u.gender AS creator_gender, u.role AS creator_role, q.assessment_type, q.quiz_question_type, q.title, q.topic, q.description, " +
                "q.time_limit_mins, q.speed_quiz_seconds_per_question, q.pass_score, q.randomize_questions, q.randomize_answers, q.show_answers_after, " +
                "q.is_published, q.expires_at, q.created_at, q.updated_at, " +
                "(SELECT COUNT(*) FROM questions qu WHERE qu.quiz_id = q.id OR qu.id IN (SELECT qq.question_id FROM quiz_questions qq WHERE qq.quiz_id = q.id)) AS q_count, " +
                "(SELECT COALESCE(SUM(qu.points), 0) FROM questions qu WHERE qu.quiz_id = q.id OR qu.id IN (SELECT qq.question_id FROM quiz_questions qq WHERE qq.quiz_id = q.id)) AS total_pts " +
                "FROM quizzes q LEFT JOIN subjects s ON q.subject_id = s.id LEFT JOIN users u ON q.created_by = u.id WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (assessmentType != null) {
            if (assessmentType == AssessmentType.QUIZ) {
                sql.append(" AND (q.assessment_type = ? OR q.assessment_type IS NULL)");
            } else {
                sql.append(" AND q.assessment_type = ?");
            }
            params.add(assessmentType.name());
        }

        if (subjectId != null) {
            sql.append(" AND q.subject_id = ?");
            params.add(subjectId);
        }

        if (createdBy != null) {
            sql.append(" AND q.created_by = ?");
            params.add(createdBy);
        }

        if (published != null) {
            sql.append(" AND q.is_published = ?");
            params.add(published);
        }

        if (activeOnly) {
            sql.append(" AND (q.expires_at IS NULL OR q.expires_at > NOW())");
        }

        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(q.title) LIKE ? OR LOWER(COALESCE(q.topic, '')) LIKE ? OR LOWER(COALESCE(q.description, '')) LIKE ?)");
            String p = "%" + search.trim().toLowerCase() + "%";
            params.add(p);
            params.add(p);
            params.add(p);
        }

        sql.append(" ORDER BY q.id DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Quiz quiz = mapRow(rs);
                    quiz.setQuestionCount(rs.getInt("q_count"));
                    quiz.setTotalPoints(rs.getDouble("total_pts"));
                    list.add(quiz);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error querying quizzes: " + e.getMessage());
        }
        return list;
    }

    public Optional<Quiz> findById(int id) {
        String sql = "SELECT q.id, q.subject_id, s.code AS subject_code, q.created_by, u.full_name AS creator_name, u.gender AS creator_gender, u.role AS creator_role, q.assessment_type, q.quiz_question_type, q.title, q.topic, q.description, " +
                     "q.time_limit_mins, q.speed_quiz_seconds_per_question, q.pass_score, q.randomize_questions, q.randomize_answers, q.show_answers_after, " +
                     "q.is_published, q.expires_at, q.created_at, q.updated_at, " +
                     "(SELECT COUNT(*) FROM questions qu WHERE qu.quiz_id = q.id OR qu.id IN (SELECT qq.question_id FROM quiz_questions qq WHERE qq.quiz_id = q.id)) AS q_count, " +
                     "(SELECT COALESCE(SUM(qu.points), 0) FROM questions qu WHERE qu.quiz_id = q.id OR qu.id IN (SELECT qq.question_id FROM quiz_questions qq WHERE qq.quiz_id = q.id)) AS total_pts " +
                     "FROM quizzes q LEFT JOIN subjects s ON q.subject_id = s.id LEFT JOIN users u ON q.created_by = u.id WHERE q.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Quiz quiz = mapRow(rs);
                    quiz.setQuestionCount(rs.getInt("q_count"));
                    quiz.setTotalPoints(rs.getDouble("total_pts"));
                    quiz.setQuestions(loadAssignedQuestions(conn, id));
                    return Optional.of(quiz);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding quiz by id: " + e.getMessage());
        }
        return Optional.empty();
    }

    public boolean create(Quiz quiz) {
        String sql = "INSERT INTO quizzes (subject_id, created_by, assessment_type, quiz_question_type, title, topic, description, time_limit_mins, speed_quiz_seconds_per_question, pass_score, " +
                     "randomize_questions, randomize_answers, show_answers_after, is_published, expires_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (quiz.getSubjectId() != null) stmt.setInt(1, quiz.getSubjectId());
            else stmt.setNull(1, Types.INTEGER);

            if (quiz.getCreatedBy() != null) stmt.setInt(2, quiz.getCreatedBy());
            else stmt.setNull(2, Types.INTEGER);

            stmt.setString(3, quiz.getAssessmentType() != null ? quiz.getAssessmentType().name() : AssessmentType.QUIZ.name());
            if (quiz.getQuizQuestionType() != null) stmt.setString(4, quiz.getQuizQuestionType().name());
            else stmt.setNull(4, Types.VARCHAR);

            stmt.setString(5, quiz.getTitle().trim());
            stmt.setString(6, quiz.getTopic() != null ? quiz.getTopic().trim() : null);
            stmt.setString(7, quiz.getDescription());
            if (quiz.getTimeLimitMins() != null && quiz.getTimeLimitMins() > 0) {
                stmt.setInt(8, quiz.getTimeLimitMins());
            } else {
                stmt.setNull(8, Types.INTEGER);
            }
            if (quiz.getSpeedSecondsPerQuestion() != null && quiz.getSpeedSecondsPerQuestion() > 0) {
                stmt.setInt(9, quiz.getSpeedSecondsPerQuestion());
            } else {
                stmt.setNull(9, Types.INTEGER);
            }
            stmt.setInt(10, quiz.getPassScore());
            stmt.setBoolean(11, quiz.isRandomizeQuestions());
            stmt.setBoolean(12, quiz.isRandomizeAnswers());
            stmt.setBoolean(13, quiz.isShowAnswersAfter());
            stmt.setBoolean(14, quiz.isPublished());
            stmt.setTimestamp(15, quiz.getExpiresAt());

            int affected = stmt.executeUpdate();
            if (affected > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        quiz.setId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error creating quiz: " + e.getMessage());
        }
        return false;
    }

    public boolean update(Quiz quiz) {
        String sql = "UPDATE quizzes SET subject_id = ?, assessment_type = ?, quiz_question_type = ?, title = ?, topic = ?, description = ?, time_limit_mins = ?, speed_quiz_seconds_per_question = ?, " +
                     "pass_score = ?, randomize_questions = ?, randomize_answers = ?, show_answers_after = ?, " +
                     "is_published = ?, expires_at = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            if (quiz.getSubjectId() != null) stmt.setInt(1, quiz.getSubjectId());
            else stmt.setNull(1, Types.INTEGER);

            stmt.setString(2, quiz.getAssessmentType() != null ? quiz.getAssessmentType().name() : AssessmentType.QUIZ.name());
            if (quiz.getQuizQuestionType() != null) stmt.setString(3, quiz.getQuizQuestionType().name());
            else stmt.setNull(3, Types.VARCHAR);

            stmt.setString(4, quiz.getTitle().trim());
            stmt.setString(5, quiz.getTopic() != null ? quiz.getTopic().trim() : null);
            stmt.setString(6, quiz.getDescription());
            if (quiz.getTimeLimitMins() != null && quiz.getTimeLimitMins() > 0) {
                stmt.setInt(7, quiz.getTimeLimitMins());
            } else {
                stmt.setNull(7, Types.INTEGER);
            }
            if (quiz.getSpeedSecondsPerQuestion() != null && quiz.getSpeedSecondsPerQuestion() > 0) {
                stmt.setInt(8, quiz.getSpeedSecondsPerQuestion());
            } else {
                stmt.setNull(8, Types.INTEGER);
            }
            stmt.setInt(9, quiz.getPassScore());
            stmt.setBoolean(10, quiz.isRandomizeQuestions());
            stmt.setBoolean(11, quiz.isRandomizeAnswers());
            stmt.setBoolean(12, quiz.isShowAnswersAfter());
            stmt.setBoolean(13, quiz.isPublished());
            stmt.setTimestamp(14, quiz.getExpiresAt());
            stmt.setInt(15, quiz.getId());

            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating quiz: " + e.getMessage());
        }
        return false;
    }

    public boolean delete(int quizId) {
        String delAnswersSql = "DELETE FROM attempt_answers WHERE attempt_id IN (SELECT id FROM attempts WHERE quiz_id = ?)";
        String delResultsSql = "DELETE FROM results WHERE quiz_id = ? OR attempt_id IN (SELECT id FROM attempts WHERE quiz_id = ?)";
        String delAttemptsSql = "DELETE FROM attempts WHERE quiz_id = ?";
        String delAssignedSql = "DELETE FROM quiz_questions WHERE quiz_id = ?";
        String delOptionsSql = "DELETE FROM question_options WHERE question_id IN (SELECT id FROM questions WHERE quiz_id = ?)";
        String delQuestionsSql = "DELETE FROM questions WHERE quiz_id = ?";
        String delQuizSql = "DELETE FROM quizzes WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement s1 = conn.prepareStatement(delAnswersSql);
                 PreparedStatement s2 = conn.prepareStatement(delResultsSql);
                 PreparedStatement s3 = conn.prepareStatement(delAttemptsSql);
                 PreparedStatement s4 = conn.prepareStatement(delAssignedSql);
                 PreparedStatement s5 = conn.prepareStatement(delOptionsSql);
                 PreparedStatement s6 = conn.prepareStatement(delQuestionsSql);
                 PreparedStatement s7 = conn.prepareStatement(delQuizSql)) {

                s1.setInt(1, quizId);
                s1.executeUpdate();

                s2.setInt(1, quizId);
                s2.setInt(2, quizId);
                s2.executeUpdate();

                s3.setInt(1, quizId);
                s3.executeUpdate();

                s4.setInt(1, quizId);
                s4.executeUpdate();

                s5.setInt(1, quizId);
                s5.executeUpdate();

                s6.setInt(1, quizId);
                s6.executeUpdate();

                s7.setInt(1, quizId);
                int affected = s7.executeUpdate();

                conn.commit();
                conn.setAutoCommit(true);
                return affected > 0;
            } catch (SQLException e) {
                conn.rollback();
                conn.setAutoCommit(true);
                throw e;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting quiz: " + e.getMessage());
        }
        return false;
    }

    public boolean togglePublished(int quizId) {
        String sql = "UPDATE quizzes SET is_published = NOT is_published, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quizId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error toggling quiz published status: " + e.getMessage());
        }
        return false;
    }

    public List<Integer> getAssignedQuestionIds(int quizId) {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM questions WHERE quiz_id = ? " +
                     "UNION " +
                     "SELECT question_id FROM quiz_questions WHERE quiz_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quizId);
            stmt.setInt(2, quizId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getInt(1));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error querying assigned question ids: " + e.getMessage());
        }
        return ids;
    }

    public boolean assignQuestions(int quizId, List<Integer> questionIds) {
        String delSql = "DELETE FROM quiz_questions WHERE quiz_id = ?";
        String insSql = "INSERT INTO quiz_questions (quiz_id, question_id, question_order) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement delStmt = conn.prepareStatement(delSql)) {
                delStmt.setInt(1, quizId);
                delStmt.executeUpdate();
            }
            if (questionIds != null && !questionIds.isEmpty()) {
                try (PreparedStatement insStmt = conn.prepareStatement(insSql)) {
                    for (int i = 0; i < questionIds.size(); i++) {
                        insStmt.setInt(1, quizId);
                        insStmt.setInt(2, questionIds.get(i));
                        insStmt.setInt(3, i + 1);
                        insStmt.addBatch();
                    }
                    insStmt.executeBatch();
                }
            }
            conn.commit();
            conn.setAutoCommit(true);
            return true;
        } catch (SQLException e) {
            System.err.println("Error assigning questions to quiz: " + e.getMessage());
        }
        return false;
    }

    private List<Question> loadAssignedQuestions(Connection conn, int quizId) throws SQLException {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT q.id, q.quiz_id, q.subject_id, s.code AS subject_code, q.created_by, q.question_text, " +
                     "q.question_type, q.difficulty, q.points, q.explanation, q.ai_generated, q.is_enabled, q.created_at " +
                     "FROM questions q LEFT JOIN subjects s ON q.subject_id = s.id " +
                     "WHERE q.quiz_id = ? OR q.id IN (SELECT qq.question_id FROM quiz_questions qq WHERE qq.quiz_id = ?) " +
                     "ORDER BY q.id ASC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quizId);
            stmt.setInt(2, quizId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Question q = Question.builder()
                            .id(rs.getInt("id"))
                            .quizId(rs.getObject("quiz_id") != null ? rs.getInt("quiz_id") : null)
                            .subjectId(rs.getObject("subject_id") != null ? rs.getInt("subject_id") : null)
                            .subjectCode(rs.getString("subject_code"))
                            .createdBy(rs.getObject("created_by") != null ? rs.getInt("created_by") : null)
                            .questionText(rs.getString("question_text"))
                            .questionType(QuestionType.valueOf(rs.getString("question_type")))
                            .difficulty(Difficulty.valueOf(rs.getString("difficulty")))
                            .points(rs.getDouble("points"))
                            .explanation(rs.getString("explanation"))
                            .aiGenerated(rs.getBoolean("ai_generated"))
                            .enabled(rs.getBoolean("is_enabled"))
                            .createdAt(rs.getTimestamp("created_at"))
                            .options(loadOptions(conn, rs.getInt("id")))
                            .build();
                    questions.add(q);
                }
            }
        }
        return questions;
    }

    private List<QuestionOption> loadOptions(Connection conn, int questionId) throws SQLException {
        List<QuestionOption> options = new ArrayList<>();
        String sql = "SELECT id, question_id, option_text, is_correct, option_order FROM question_options " +
                     "WHERE question_id = ? ORDER BY option_order ASC";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, questionId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    options.add(QuestionOption.builder()
                            .id(rs.getInt("id"))
                            .questionId(rs.getInt("question_id"))
                            .optionText(rs.getString("option_text"))
                            .correct(rs.getBoolean("is_correct"))
                            .optionOrder(rs.getInt("option_order"))
                            .build());
                }
            }
        }
        return options;
    }

    private Quiz mapRow(ResultSet rs) throws SQLException {
        String aTypeStr = rs.getString("assessment_type");
        AssessmentType aType = aTypeStr != null ? AssessmentType.valueOf(aTypeStr) : AssessmentType.QUIZ;
        String qTypeStr = rs.getString("quiz_question_type");
        QuestionType qType = qTypeStr != null ? QuestionType.valueOf(qTypeStr) : null;
        String creatorName = null;
        try {
            creatorName = rs.getString("creator_name");
        } catch (SQLException ignored) {}
        String creatorGender = null;
        try {
            creatorGender = rs.getString("creator_gender");
        } catch (SQLException ignored) {}
        Role creatorRole = null;
        try {
            String roleStr = rs.getString("creator_role");
            if (roleStr != null) creatorRole = Role.valueOf(roleStr);
        } catch (Exception ignored) {}

        Integer speedSeconds = null;
        try {
            if (rs.getObject("speed_quiz_seconds_per_question") != null) {
                speedSeconds = rs.getInt("speed_quiz_seconds_per_question");
            }
        } catch (SQLException ignored) {}

        return Quiz.builder()
                .id(rs.getInt("id"))
                .subjectId(rs.getObject("subject_id") != null ? rs.getInt("subject_id") : null)
                .subjectCode(rs.getString("subject_code"))
                .createdBy(rs.getObject("created_by") != null ? rs.getInt("created_by") : null)
                .creatorName(creatorName)
                .creatorGender(creatorGender)
                .creatorRole(creatorRole)
                .assessmentType(aType)
                .quizQuestionType(qType)
                .title(rs.getString("title"))
                .topic(rs.getString("topic"))
                .description(rs.getString("description"))
                .timeLimitMins(rs.getObject("time_limit_mins") != null ? rs.getInt("time_limit_mins") : null)
                .speedSecondsPerQuestion(speedSeconds)
                .passScore(rs.getInt("pass_score"))
                .randomizeQuestions(rs.getBoolean("randomize_questions"))
                .randomizeAnswers(rs.getBoolean("randomize_answers"))
                .showAnswersAfter(rs.getBoolean("show_answers_after"))
                .published(rs.getBoolean("is_published"))
                .expiresAt(rs.getTimestamp("expires_at"))
                .createdAt(rs.getTimestamp("created_at"))
                .updatedAt(rs.getTimestamp("updated_at"))
                .build();
    }
}