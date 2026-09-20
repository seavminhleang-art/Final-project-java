package com.proctor.model.repository;

import com.proctor.exception.DatabaseException;
import com.proctor.model.enums.Difficulty;
import com.proctor.model.enums.QuestionType;
import com.proctor.config.DatabaseConnection;
import com.proctor.model.entity.Question;
import com.proctor.model.entity.QuestionOption;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QuestionRepository {

    public List<Question> findByQuizId(int quizId) {
        List<Question> list = new ArrayList<>();
        String sql = "SELECT q.id, q.quiz_id, q.subject_id, s.code AS subject_code, q.created_by, q.question_text, " +
                     "q.question_type, q.difficulty, q.points, q.explanation, q.ai_generated, q.is_enabled, q.created_at " +
                     "FROM questions q LEFT JOIN subjects s ON q.subject_id = s.id " +
                     "WHERE q.quiz_id = ? OR q.id IN (SELECT qq.question_id FROM quiz_questions qq WHERE qq.quiz_id = ?) " +
                     "ORDER BY q.id ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, quizId);
            stmt.setInt(2, quizId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Question q = mapRow(rs);
                    q.setOptions(loadOptions(conn, q.getId()));
                    list.add(q);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error querying quiz questions: " + e.getMessage(), e);
        }
        return list;
    }

    public Optional<Question> findById(int id) {
        String sql = "SELECT q.id, q.quiz_id, q.subject_id, s.code AS subject_code, q.created_by, q.question_text, " +
                     "q.question_type, q.difficulty, q.points, q.explanation, q.ai_generated, q.is_enabled, q.created_at " +
                     "FROM questions q LEFT JOIN subjects s ON q.subject_id = s.id WHERE q.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Question q = mapRow(rs);
                    q.setOptions(loadOptions(conn, id));
                    return Optional.of(q);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error finding question by id: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public boolean create(Question question) {
        String sql = "INSERT INTO questions (quiz_id, subject_id, created_by, question_text, question_type, difficulty, points, explanation, ai_generated, is_enabled) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                if (question.getQuizId() != null) stmt.setInt(1, question.getQuizId());
                else stmt.setNull(1, Types.INTEGER);

                if (question.getSubjectId() != null) stmt.setInt(2, question.getSubjectId());
                else stmt.setNull(2, Types.INTEGER);

                if (question.getCreatedBy() != null) stmt.setInt(3, question.getCreatedBy());
                else stmt.setNull(3, Types.INTEGER);

                stmt.setString(4, question.getQuestionText().trim());
                stmt.setString(5, question.getQuestionType().name());
                stmt.setString(6, question.getDifficulty().name());
                stmt.setDouble(7, question.getPoints());
                stmt.setString(8, question.getExplanation());
                stmt.setBoolean(9, question.isAiGenerated());
                stmt.setBoolean(10, question.isEnabled());

                int affected = stmt.executeUpdate();
                if (affected > 0) {
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            question.setId(rs.getInt(1));
                        }
                    }
                    saveOptions(conn, question.getId(), question.getOptions());
                    conn.commit();
                    return true;
                }
                conn.commit();
                return false;
            } catch (SQLException ex) {
                conn.rollback();
                throw new DatabaseException("Error creating question: " + ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Database connection error: " + e.getMessage(), e);
        }
    }

    public boolean update(Question question) {
        String sql = "UPDATE questions SET quiz_id = ?, subject_id = ?, question_text = ?, question_type = ?, " +
                     "difficulty = ?, points = ?, explanation = ?, is_enabled = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                if (question.getQuizId() != null) stmt.setInt(1, question.getQuizId());
                else stmt.setNull(1, Types.INTEGER);

                if (question.getSubjectId() != null) stmt.setInt(2, question.getSubjectId());
                else stmt.setNull(2, Types.INTEGER);

                stmt.setString(3, question.getQuestionText().trim());
                stmt.setString(4, question.getQuestionType().name());
                stmt.setString(5, question.getDifficulty().name());
                stmt.setDouble(6, question.getPoints());
                stmt.setString(7, question.getExplanation());
                stmt.setBoolean(8, question.isEnabled());
                stmt.setInt(9, question.getId());

                int affected = stmt.executeUpdate();
                if (affected > 0) {
                    deleteOptions(conn, question.getId());
                    saveOptions(conn, question.getId(), question.getOptions());
                }
                conn.commit();
                return affected > 0;
            } catch (SQLException ex) {
                conn.rollback();
                throw new DatabaseException("Error updating question: " + ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Database connection error: " + e.getMessage(), e);
        }
    }

    public boolean delete(int questionId) {
        String unlinkOptionsSql = "UPDATE attempt_answers SET selected_option_id = NULL WHERE selected_option_id IN (SELECT id FROM question_options WHERE question_id = ?)";
        String unlinkQuestionSql = "UPDATE attempt_answers SET question_id = NULL WHERE question_id = ?";
        String deleteQuestionSql = "DELETE FROM questions WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement stmtOptions = conn.prepareStatement(unlinkOptionsSql);
                 PreparedStatement stmtQuestion = conn.prepareStatement(unlinkQuestionSql);
                 PreparedStatement stmtDelete = conn.prepareStatement(deleteQuestionSql)) {

                stmtOptions.setInt(1, questionId);
                stmtOptions.executeUpdate();

                stmtQuestion.setInt(1, questionId);
                stmtQuestion.executeUpdate();

                stmtDelete.setInt(1, questionId);
                int affected = stmtDelete.executeUpdate();

                conn.commit();
                return affected > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw new DatabaseException("Error deleting question: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Database connection error: " + e.getMessage(), e);
        }
    }

    public List<Question> findBankQuestions(Integer createdBy, Integer subjectId, QuestionType type, Difficulty difficulty, String search) {
        List<Question> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT q.id, q.quiz_id, q.subject_id, s.code AS subject_code, q.created_by, q.question_text, " +
                "q.question_type, q.difficulty, q.points, q.explanation, q.ai_generated, q.is_enabled, q.created_at " +
                "FROM questions q LEFT JOIN subjects s ON q.subject_id = s.id WHERE q.quiz_id IS NULL"
        );
        List<Object> params = new ArrayList<>();

        if (createdBy != null) {
            sql.append(" AND q.created_by = ?");
            params.add(createdBy);
        }
        if (subjectId != null) {
            sql.append(" AND q.subject_id = ?");
            params.add(subjectId);
        }
        if (type != null) {
            sql.append(" AND q.question_type = ?");
            params.add(type.name());
        }
        if (difficulty != null) {
            sql.append(" AND q.difficulty = ?");
            params.add(difficulty.name());
        }
        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(q.question_text) LIKE ? OR LOWER(COALESCE(q.explanation, '')) LIKE ?)");
            String p = "%" + search.trim().toLowerCase() + "%";
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
                    Question q = mapRow(rs);
                    q.setOptions(loadOptions(conn, q.getId()));
                    list.add(q);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error querying bank questions: " + e.getMessage(), e);
        }
        return list;
    }

    public int copyToQuiz(int bankQuestionId, int quizId) {
        Question source = findById(bankQuestionId).orElseThrow(() ->
                new com.proctor.exception.ValidationException("Bank question not found: " + bankQuestionId));

        String insertQ = "INSERT INTO questions (quiz_id, subject_id, created_by, question_text, question_type, " +
                "difficulty, points, explanation, ai_generated, is_enabled) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection()) {
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try (PreparedStatement stmt = conn.prepareStatement(insertQ, Statement.RETURN_GENERATED_KEYS)) {
                stmt.setInt(1, quizId);
                if (source.getSubjectId() != null) stmt.setInt(2, source.getSubjectId());
                else stmt.setNull(2, Types.INTEGER);
                if (source.getCreatedBy() != null) stmt.setInt(3, source.getCreatedBy());
                else stmt.setNull(3, Types.INTEGER);
                stmt.setString(4, source.getQuestionText());
                stmt.setString(5, source.getQuestionType().name());
                stmt.setString(6, source.getDifficulty().name());
                stmt.setDouble(7, source.getPoints());
                stmt.setString(8, source.getExplanation());
                stmt.setBoolean(9, source.isAiGenerated());
                stmt.setBoolean(10, true);

                stmt.executeUpdate();
                int newId;
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (!rs.next()) throw new DatabaseException("Failed to copy question (no generated ID).");
                    newId = rs.getInt(1);
                }

                saveOptions(conn, newId, source.getOptions());

                String countSql = "SELECT COUNT(*) FROM quiz_questions WHERE quiz_id = ?";
                int order;
                try (PreparedStatement countStmt = conn.prepareStatement(countSql)) {
                    countStmt.setInt(1, quizId);
                    try (ResultSet rs = countStmt.executeQuery()) {
                        order = rs.next() ? rs.getInt(1) + 1 : 1;
                    }
                }
                String qqSql = "INSERT INTO quiz_questions (quiz_id, question_id, question_order) VALUES (?, ?, ?)";
                try (PreparedStatement qqStmt = conn.prepareStatement(qqSql)) {
                    qqStmt.setInt(1, quizId);
                    qqStmt.setInt(2, newId);
                    qqStmt.setInt(3, order);
                    qqStmt.executeUpdate();
                }
                conn.commit();
                return newId;
            } catch (SQLException ex) {
                conn.rollback();
                throw new DatabaseException("Error copying bank question to quiz: " + ex.getMessage(), ex);
            } finally {
                conn.setAutoCommit(originalAutoCommit);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Database connection error: " + e.getMessage(), e);
        }
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

    private void saveOptions(Connection conn, int questionId, List<QuestionOption> options) throws SQLException {
        if (options == null || options.isEmpty()) return;

        String sql = "INSERT INTO question_options (question_id, option_text, is_correct, option_order) VALUES (?, ?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < options.size(); i++) {
                QuestionOption opt = options.get(i);
                stmt.setInt(1, questionId);
                stmt.setString(2, opt.getOptionText().trim());
                stmt.setBoolean(3, opt.isCorrect());
                stmt.setInt(4, i + 1);
                stmt.executeUpdate();
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        opt.setId(rs.getInt(1));
                        opt.setQuestionId(questionId);
                    }
                }
            }
        }
    }

    private void deleteOptions(Connection conn, int questionId) throws SQLException {
        String sql = "DELETE FROM question_options WHERE question_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, questionId);
            stmt.executeUpdate();
        }
    }

    private Question mapRow(ResultSet rs) throws SQLException {
        return Question.builder()
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
                .build();
    }
}