package service.impl;

import repository.QuizDao;
import exception.NotFoundException;
import model.entity.Question;
import model.entity.Quiz;
import service.QuizService;

import java.math.BigDecimal;
import java.util.List;

public class QuizServiceImpl implements QuizService {

    private final QuizDao quizDao;

    public QuizServiceImpl(QuizDao quizDao) {
        this.quizDao = quizDao;
    }

    @Override
    public Quiz create(Quiz quiz) {
        if (quiz.getDurationMinutes() <= 0) {
            throw new IllegalArgumentException("Duration must be positive.");
        }
        validateAttemptsAndSchedule(quiz);
        return quizDao.create(quiz);
    }

    @Override
    public Quiz update(Quiz quiz) {
        get(quiz.getId());
        validateAttemptsAndSchedule(quiz);
        return quizDao.update(quiz);
    }

    private void validateAttemptsAndSchedule(Quiz quiz) {
        if (quiz.getMaxAttempts() < 0) {
            throw new IllegalArgumentException("Max attempts cannot be negative (use 0 for unlimited).");
        }
        if (quiz.getExamOpenAt() != null && quiz.getExamCloseAt() != null
                && !quiz.getExamCloseAt().isAfter(quiz.getExamOpenAt())) {
            throw new IllegalArgumentException("Exam close time must be after the open time.");
        }
    }

    @Override
    public void delete(Long id) {
        get(id);
        quizDao.delete(id);
    }

    @Override
    public Quiz get(Long id) {
        return quizDao.findById(id).orElseThrow(() -> new NotFoundException("Quiz not found: " + id));
    }

    @Override
    public List<Quiz> listAll() {
        return quizDao.findAll();
    }

    @Override
    public List<Quiz> listPublished() {
        return quizDao.findPublished();
    }

    @Override
    public void publish(Long quizId) {
        Quiz quiz = get(quizId);
        List<Question> questions = quizDao.getQuestions(quizId);
        if (questions.isEmpty()) {
            throw new IllegalStateException("Cannot publish a quiz with no questions.");
        }
        quizDao.setPublished(quizId, true);
    }

    @Override
    public void unpublish(Long quizId) {
        get(quizId);
        quizDao.setPublished(quizId, false);
    }

    @Override
    public void assignQuestion(Long quizId, Long questionId, BigDecimal marks) {
        get(quizId);
        quizDao.addQuestion(quizId, questionId, marks);
    }

    @Override
    public void removeQuestion(Long quizId, Long questionId) {
        get(quizId);
        quizDao.removeQuestion(quizId, questionId);
    }

    @Override
    public List<Question> questionsFor(Long quizId) {
        return quizDao.getQuestions(quizId);
    }

    @Override
    public BigDecimal totalMarks(Long quizId) {
        return quizDao.getTotalMarks(quizId);
    }
}
