package repository;

import model.entity.Question;
import model.entity.Quiz;
import model.entity.QuizQuestion;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface QuizDao {
    Quiz create(Quiz quiz);
    Optional<Quiz> findById(Long id);
    List<Quiz> findAll();
    List<Quiz> findPublished();
    Quiz update(Quiz quiz);
    boolean delete(Long id);
    boolean setPublished(Long quizId, boolean published);

    void addQuestion(Long quizId, Long questionId, BigDecimal marks);
    void removeQuestion(Long quizId, Long questionId);
    List<Question> getQuestions(Long quizId);
    List<QuizQuestion> getQuizQuestionLinks(Long quizId);
    BigDecimal getTotalMarks(Long quizId);
}
