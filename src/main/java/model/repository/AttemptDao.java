package repository;

import model.entity.Attempt;
import model.entity.AttemptAnswer;

import java.util.List;
import java.util.Optional;

public interface AttemptDao {
    Attempt create(Attempt attempt);
    Optional<Attempt> findById(Long id);
    List<Attempt> findByStudent(Long studentId);
    List<Attempt> findByQuiz(Long quizId);
    List<Attempt> findByQuizAndStudent(Long quizId, Long studentId);
    Attempt update(Attempt attempt);

    void upsertAnswer(AttemptAnswer answer);
    List<AttemptAnswer> getAnswers(Long attemptId);
}