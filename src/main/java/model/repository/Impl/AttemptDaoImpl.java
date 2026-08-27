package repository.impl;

import db.ConnectionPool;
import repository.AttemptDao;
import model.entity.Attempt;
import model.entity.AttemptAnswer;
import model.entity.enums.AttemptStatus;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AttemptDaoImpl implements AttemptDao {

    private final ConnectionPool pool = ConnectionPool.getInstance();

    @Override
    public Attempt create(Attempt attempt) {
        String sql = """
            INSERT INTO attempts (quiz_id, student_id, start_time, status)
            VALUES (?, ?, NOW(), ?)
            """;
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, attempt.getQuizId());
            ps.setLong(2, attempt.getStudentId());
            ps.setString(3, attempt.getStatus().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) attempt.setId(keys.getLong(1));
            }
            return attempt;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create attempt", e);
        } finally {
            pool.release(conn);
        }
    }

    @Override
    public Optional<Attempt> findById(Long id) {
        String sql = "SELECT * FROM attempts WHERE id = ?";
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find attempt", e);
        } finally {
            pool.release(conn);
        }
    }

    @Override
    public List<Attempt> findByStudent(Long studentId) {
        String sql = "SELECT * FROM attempts WHERE student_id = ? ORDER BY start_time DESC";
        return queryList(sql, studentId);
    }

    @Override
    public List<Attempt> findByQuiz(Long quizId) {
        String sql = "SELECT * FROM attempts WHERE quiz_id = ? ORDER BY start_time DESC";
        return queryList(sql, quizId);
    }

    @Override
    public List<Attempt> findByQuizAndStudent(Long quizId, Long studentId) {
        String sql = "SELECT * FROM attempts WHERE quiz_id = ? AND student_id = ? ORDER BY start_time DESC";
        Connection conn = pool.borrow();
        List<Attempt> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, quizId);
            ps.setLong(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query attempts", e);
        } finally {
            pool.release(conn);
        }
    }

    private List<Attempt> queryList(String sql, Long param) {
        Connection conn = pool.borrow();
        List<Attempt> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to query attempts", e);
        } finally {
            pool.release(conn);
        }
    }

    @Override
    public Attempt update(Attempt attempt) {
        String sql = """
            UPDATE attempts SET end_time = ?, status = ?, score = ?, total_marks = ?, percentage = ?, passed = ?
            WHERE id = ?
            """;
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, attempt.getEndTime() == null ? null : Timestamp.valueOf(attempt.getEndTime()));
            ps.setString(2, attempt.getStatus().name());
            ps.setBigDecimal(3, attempt.getScore());
            ps.setBigDecimal(4, attempt.getTotalMarks());
            ps.setBigDecimal(5, attempt.getPercentage());
            ps.setObject(6, attempt.getPassed());
            ps.setLong(7, attempt.getId());
            ps.executeUpdate();
            return attempt;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update attempt", e);
        } finally {
            pool.release(conn);
        }
    }

    @Override
    public void upsertAnswer(AttemptAnswer answer) {
        String sql = """
            INSERT INTO attempt_answers (attempt_id, question_id, selected_option, is_correct)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (attempt_id, question_id)
            DO UPDATE SET selected_option = EXCLUDED.selected_option, is_correct = EXCLUDED.is_correct
            """;
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, answer.getAttemptId());
            ps.setLong(2, answer.getQuestionId());
            if (answer.getSelectedOption() == null) {
                ps.setNull(3, Types.CHAR);
            } else {
                ps.setString(3, String.valueOf(answer.getSelectedOption()));
            }
            ps.setObject(4, answer.getIsCorrect());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to save answer", e);
        } finally {
            pool.release(conn);
        }
    }

    @Override
    public List<AttemptAnswer> getAnswers(Long attemptId) {
        String sql = "SELECT * FROM attempt_answers WHERE attempt_id = ?";
        Connection conn = pool.borrow();
        List<AttemptAnswer> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, attemptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String sel = rs.getString("selected_option");
                    list.add(AttemptAnswer.builder()
                            .attemptId(rs.getLong("attempt_id"))
                            .questionId(rs.getLong("question_id"))
                            .selectedOption(sel == null ? null : sel.charAt(0))
                            .isCorrect((Boolean) rs.getObject("is_correct"))
                            .build());
                }
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to fetch answers", e);
        } finally {
            pool.release(conn);
        }
    }

    private Attempt map(ResultSet rs) throws SQLException {
        Timestamp start = rs.getTimestamp("start_time");
        Timestamp end = rs.getTimestamp("end_time");
        return Attempt.builder()
                .id(rs.getLong("id"))
                .quizId(rs.getLong("quiz_id"))
                .studentId(rs.getLong("student_id"))
                .startTime(start == null ? null : start.toLocalDateTime())
                .endTime(end == null ? null : end.toLocalDateTime())
                .status(AttemptStatus.valueOf(rs.getString("status")))
                .score(rs.getBigDecimal("score"))
                .totalMarks(rs.getBigDecimal("total_marks"))
                .percentage(rs.getBigDecimal("percentage"))
                .passed((Boolean) rs.getObject("passed"))
                .build();
    }
}