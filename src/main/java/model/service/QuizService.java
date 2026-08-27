package service;

import model.entity.Question;
import model.entity.Quiz;

import java.math.BigDecimal;
import java.util.List;

public interface QuizService {
    Quiz create(Quiz quiz);
    Quiz update(Quiz quiz);
    void delete(Long id);
    Quiz get(Long id);
    List<Quiz> listAll();
    List<Quiz> listPublished();
    void publish(Long quizId);
    void unpublish(Long quizId);

    void assignQuestion(Long quizId, Long questionId, BigDecimal marks);
    void removeQuestion(Long quizId, Long questionId);
    List<Question> questionsFor(Long quizId);
    BigDecimal totalMarks(Long quizId);
}
