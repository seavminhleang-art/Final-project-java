package model.repository.impl;

import db.DbConnection;
import model.entity.Question;
import model.entity.Quiz;
import model.entity.QuizQuestion;
import model.entity.enums.Difficulty;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QuizDaoImpl implements repository.QuizDao {
    private final DbConnection dbConnection = DbConnection.getInstance();

    @Override
    public Quiz create(Quiz quiz) {
        String sql = """
            INSERT INTO quizzes (title, subject_id, duration_minutes, pass_percentage, is_published,
                                  max_attempts, exam_open_at, exam_close_at, review_allowed, created_by, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
            """;
        Connection conn = dbConnection.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, quiz.getTitle());
            ps.setLong(2, quiz.getSubjectId());
            ps.setInt(3, quiz.getDurationMinutes());
            ps.setBigDecimal(4, quiz.getPassPercentage());
            ps.setBoolean(5, quiz.isPublished());
            ps.setInt(6, quiz.getMaxAttempts());
            ps.setTimestamp(7, quiz.getExamOpenAt() == null ? null : Timestamp.valueOf(quiz.getExamOpenAt()));
            ps.setTimestamp(8, quiz.getExamCloseAt() == null ? null : Timestamp.valueOf(quiz.getExamCloseAt()));
            ps.setBoolean(9, quiz.isReviewAllowed());
            ps.setObject(10, quiz.getCreatedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) quiz.setId(keys.getLong(1));
            }
            return quiz;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create quiz", e);
        } finally {
            dbConnection.release(conn);
        }
    }

    @Override
    public Optional<Quiz> findById(Long id) {
        String sql = "SELECT * FROM quizzes WHERE id = ?";
        Connection conn = dbConnection.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find quiz", e);
        } finally {
            dbConnection.release(conn);
        }
    }

    @Override
    public List<Quiz> findAll() {
        return query("SELECT * FROM quizzes ORDER BY id");
    }

    @Override
    public List<Quiz> findPublished() {
        return query("SELECT * FROM quizzes WHERE is_published = TRUE ORDER BY id");
    }

    private List<Quiz> query(String sql) {
        Connection conn = dbConnection.borrow();
        List<Quiz> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query quizzes", e);
        } finally {
            dbConnection.release(conn);
        }
    }

    @Override
    public Quiz update(Quiz quiz) {
        String sql = """
            UPDATE quizzes SET title = ?, duration_minutes = ?, pass_percentage = ?,
                                max_attempts = ?, exam_open_at = ?, exam_close_at = ?, review_allowed = ?
            WHERE id = ?
            """;
        Connection conn = dbConnection.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, quiz.getTitle());
            ps.setInt(2, quiz.getDurationMinutes());
            ps.setBigDecimal(3, quiz.getPassPercentage());
            ps.setInt(4, quiz.getMaxAttempts());
            ps.setTimestamp(5, quiz.getExamOpenAt() == null ? null : Timestamp.valueOf(quiz.getExamOpenAt()));
            ps.setTimestamp(6, quiz.getExamCloseAt() == null ? null : Timestamp.valueOf(quiz.getExamCloseAt()));
            ps.setBoolean(7, quiz.isReviewAllowed());
            ps.setLong(8, quiz.getId());
            ps.executeUpdate();
            return quiz;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update quiz", e);
        } finally {
            dbConnection.release(conn);
        }
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM quizzes WHERE id = ?";
        Connection conn = dbConnection.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete quiz", e);
        } finally {
            dbConnection.release(conn);
        }
    }

    @Override
    public boolean setPublished(Long quizId, boolean published) {
        String sql = "UPDATE quizzes SET is_published = ? WHERE id = ?";
        Connection conn = dbConnection.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, published);
            ps.setLong(2, quizId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to publish quiz", e);
        } finally {
            dbConnection.release(conn);
        }
    }



    @Override
    public void addQuestion(Long quizId, Long questionId, BigDecimal marks) {
        String sql = """
            INSERT INTO quiz_questions (quiz_id, question_id, marks) VALUES (?, ?, ?)
            ON CONFLICT (quiz_id, question_id) DO UPDATE SET marks = EXCLUDED.marks
            """;
        Connection conn = dbConnection.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, quizId);
            ps.setLong(2, questionId);
            ps.setBigDecimal(3, marks);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to assign question to quiz", e);
        } finally {
            dbConnection.release(conn);
        }
    }

    @Override
    public void removeQuestion(Long quizId, Long questionId) {
        String sql = "DELETE FROM quiz_questions WHERE quiz_id = ? AND question_id = ?";
        Connection conn = dbConnection.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, quizId);
            ps.setLong(2, questionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to remove question from quiz", e);
        } finally {
            dbConnection.release(conn);
        }
    }

    @Override
    public List<Question> getQuestions(Long quizId) {
        String sql = """
            SELECT q.* FROM questions q
            JOIN quiz_questions qq ON qq.question_id = q.id
            WHERE qq.quiz_id = ?
            ORDER BY q.id
            """;
        Connection conn = dbConnection.borrow();
        List<Question> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(Question.builder()
                            .id(rs.getLong("id"))
                            .subjectId(rs.getLong("subject_id"))
                            .questionText(rs.getString("question_text"))
                            .optionA(rs.getString("option_a"))
                            .optionB(rs.getString("option_b"))
                            .optionC(rs.getString("option_c"))
                            .optionD(rs.getString("option_d"))
                            .correctOption(rs.getString("correct_option").charAt(0))
                            .difficulty(Difficulty.valueOf(rs.getString("difficulty")))
                            .createdBy((Long) rs.getObject("created_by"))
                            .build());
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to fetch quiz questions", e);
        } finally {
            dbConnection.release(conn);
        }
    }

    @Override
    public List<QuizQuestion> getQuizQuestionLinks(Long quizId) {
        String sql = "SELECT * FROM quiz_questions WHERE quiz_id = ?";
        Connection conn = dbConnection.borrow();
        List<QuizQuestion> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(QuizQuestion.builder()
                            .quizId(rs.getLong("quiz_id"))
                            .questionId(rs.getLong("question_id"))
                            .marks(rs.getBigDecimal("marks"))
                            .build());
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to fetch quiz question links", e);
        } finally {
            dbConnection.release(conn);
        }
    }

    @Override
    public BigDecimal getTotalMarks(Long quizId) {
        String sql = "SELECT COALESCE(SUM(marks), 0) AS total FROM quiz_questions WHERE quiz_id = ?";
        Connection conn = dbConnection.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, quizId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal("total");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to compute total marks", e);
        } finally {
            dbConnection.release(conn);
        }
    }

    private Quiz map(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        Timestamp openAt = rs.getTimestamp("exam_open_at");
        Timestamp closeAt = rs.getTimestamp("exam_close_at");
        return Quiz.builder()
                .id(rs.getLong("id"))
                .title(rs.getString("title"))
                .subjectId(rs.getLong("subject_id"))
                .durationMinutes(rs.getInt("duration_minutes"))
                .passPercentage(rs.getBigDecimal("pass_percentage"))
                .published(rs.getBoolean("is_published"))
                .maxAttempts(rs.getInt("max_attempts"))
                .examOpenAt(openAt == null ? null : openAt.toLocalDateTime())
                .examCloseAt(closeAt == null ? null : closeAt.toLocalDateTime())
                .reviewAllowed(rs.getBoolean("review_allowed"))
                .createdBy((Long) rs.getObject("created_by"))
                .createdAt(created == null ? null : created.toLocalDateTime())
                .build();
    }
}
