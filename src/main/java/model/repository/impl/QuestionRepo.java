package model.repository.impl;

import db.DbConnection;
import model.entity.Question;
import model.entity.enums.Difficulty;
import model.repository.QuestionRepoService;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class QuestionRepo implements QuestionRepoService {
    private final DbConnection dbConnection = DbConnection.getInstance();

    @Override
    public Question create(Question q) {
        String sql = """
            INSERT INTO questions (subject_id, question_text, option_a, option_b, option_c, option_d,
                                    correct_option, difficulty, created_by, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())
            """;
        Connection conn = dbConnection.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindForInsert(ps, q);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) q.setId(keys.getLong(1));
            }
            return q;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to create question", e);
        } finally {
            pool.release(conn);
        }
    }

    private void bindForInsert(PreparedStatement ps, Question q) throws SQLException {
        ps.setLong(1, q.getSubjectId());
        ps.setString(2, q.getQuestionText());
        ps.setString(3, q.getOptionA());
        ps.setString(4, q.getOptionB());
        ps.setString(5, q.getOptionC());
        ps.setString(6, q.getOptionD());
        ps.setString(7, String.valueOf(q.getCorrectOption()));
        ps.setString(8, q.getDifficulty().name());
        ps.setObject(9, q.getCreatedBy());
    }

    @Override
    public Optional<Question> findById(Long id) {
        String sql = "SELECT * FROM questions WHERE id = ?";
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to find question", e);
        } finally {
            pool.release(conn);
        }
    }

    @Override
    public List<Question> findBySubject(Long subjectId) {
        return filter(subjectId, null, null);
    }

    @Override
    public List<Question> filter(Long subjectId, Difficulty difficulty, String keyword) {
        StringBuilder sql = new StringBuilder("SELECT * FROM questions WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (subjectId != null) {
            sql.append(" AND subject_id = ?");
            params.add(subjectId);
        }
        if (difficulty != null) {
            sql.append(" AND difficulty = ?");
            params.add(difficulty.name());
        }
        if (keyword != null && !keyword.isBlank()) {
            sql.append(" AND LOWER(question_text) LIKE ?");
            params.add("%" + keyword.toLowerCase() + "%");
        }
        sql.append(" ORDER BY id");

        Connection conn = pool.borrow();
        List<Question> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to filter questions", e);
        } finally {
            pool.release(conn);
        }
    }

    @Override
    public List<Question> findRandomBySubject(Long subjectId, int count) {
        String sql = "SELECT * FROM questions WHERE subject_id = ? ORDER BY RANDOM() LIMIT ?";
        Connection conn = pool.borrow();
        List<Question> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, subjectId);
            ps.setInt(2, count);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to fetch random questions", e);
        } finally {
            pool.release(conn);
        }
    }

    @Override
    public Question update(Question q) {
        String sql = """
            UPDATE questions SET question_text = ?, option_a = ?, option_b = ?, option_c = ?, option_d = ?,
                                  correct_option = ?, difficulty = ?, subject_id = ?
            WHERE id = ?
            """;
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q.getQuestionText());
            ps.setString(2, q.getOptionA());
            ps.setString(3, q.getOptionB());
            ps.setString(4, q.getOptionC());
            ps.setString(5, q.getOptionD());
            ps.setString(6, String.valueOf(q.getCorrectOption()));
            ps.setString(7, q.getDifficulty().name());
            ps.setLong(8, q.getSubjectId());
            ps.setLong(9, q.getId());
            ps.executeUpdate();
            return q;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to update question", e);
        } finally {
            pool.release(conn);
        }
    }

    @Override
    public boolean delete(Long id) {
        String sql = "DELETE FROM questions WHERE id = ?";
        Connection conn = pool.borrow();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to delete question", e);
        } finally {
            pool.release(conn);
        }
    }

    private Question map(ResultSet rs) throws SQLException {
        Timestamp created = rs.getTimestamp("created_at");
        return Question.builder()
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
                .createdAt(created == null ? null : created.toLocalDateTime())
                .build();
    }
}
