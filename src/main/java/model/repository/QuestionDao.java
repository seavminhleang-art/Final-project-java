package repository;

import model.entity.enums.Difficulty;
import model.entity.Question;

import java.util.List;
import java.util.Optional;

public interface QuestionDao {
    Question create(Question question);
    Optional<Question> findById(Long id);
    List<Question> findBySubject(Long subjectId);
    List<Question> filter(Long subjectId, Difficulty difficulty, String keyword);
    List<Question> findRandomBySubject(Long subjectId, int count);
    Question update(Question question);
    boolean delete(Long id);
}
