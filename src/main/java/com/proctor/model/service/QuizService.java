package com.proctor.model.service;

import com.proctor.model.entity.User;
import com.proctor.model.enums.AssessmentType;
import com.proctor.model.enums.QuestionType;
import com.proctor.model.enums.Role;
import com.proctor.exception.ValidationException;
import com.proctor.model.entity.Question;
import com.proctor.model.repository.QuestionRepository;
import com.proctor.model.entity.Quiz;
import com.proctor.model.repository.QuizRepository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class QuizService {
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;

    public QuizService(QuizRepository quizRepository) {
        this(quizRepository, new QuestionRepository());
    }

    public QuizService(QuizRepository quizRepository, QuestionRepository questionRepository) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
    }

    public List<Quiz> getAssessments(AssessmentType type, Integer subjectId, Boolean published, String search) {
        return quizRepository.findAll(type, subjectId, null, published, search, false);
    }

    public List<Quiz> getAssessments(AssessmentType type, Integer subjectId, Integer createdBy, Boolean published, String search) {
        return quizRepository.findAll(type, subjectId, createdBy, published, search, false);
    }

    public Optional<Quiz> getQuizById(int id) {
        return quizRepository.findById(id);
    }

    public Quiz createQuiz(Quiz quiz) {
        validateQuiz(quiz);
        applyExpiration(quiz);
        boolean created = quizRepository.create(quiz);
        if (!created) {
            throw new ValidationException("Failed to create quiz.");
        }
        return quiz;
    }

    public Quiz updateQuiz(Quiz quiz) {
        if (quiz.getId() == null) {
            throw new ValidationException("Quiz ID is required for update.");
        }
        validateQuiz(quiz);
        applyExpiration(quiz);
        boolean updated = quizRepository.update(quiz);
        if (!updated) {
            throw new ValidationException("Failed to update quiz.");
        }
        return quiz;
    }

    public boolean deleteQuiz(int quizId, User requestingUser) {
        Optional<Quiz> opt = quizRepository.findById(quizId);
        if (opt.isEmpty()) {
            throw new ValidationException("Quiz not found.");
        }
        Quiz q = opt.get();
        if (requestingUser != null && requestingUser.getRole() != Role.ADMIN) {
            if (q.getCreatedBy() != null && !q.getCreatedBy().equals(requestingUser.getId())) {
                throw new ValidationException("You can only delete quizzes that you created.");
            }
        }
        boolean deleted = quizRepository.delete(quizId);
        if (!deleted) {
            throw new ValidationException("Failed to delete quiz.");
        }
        return true;
    }

    public boolean togglePublishStatus(int quizId) {
        Optional<Quiz> opt = quizRepository.findById(quizId);
        if (opt.isEmpty()) {
            throw new ValidationException("Quiz not found.");
        }

        Quiz q = opt.get();
        if (!q.isPublished()) {

            List<Integer> assignedIds = quizRepository.getAssignedQuestionIds(quizId);
            if (assignedIds.isEmpty()) {
                throw new ValidationException("Cannot publish quiz with 0 questions. Add questions first.");
            }
        }

        return quizRepository.togglePublished(quizId);
    }

    public List<Integer> getAssignedQuestionIds(int quizId) {
        return quizRepository.getAssignedQuestionIds(quizId);
    }

    public boolean assignQuestions(int quizId, List<Integer> questionIds) {
        Optional<Quiz> quizOpt = quizRepository.findById(quizId);
        if (quizOpt.isPresent() && questionRepository != null) {
            Quiz quiz = quizOpt.get();
            if (quiz.getAssessmentType() == AssessmentType.QUIZ && quiz.getQuizQuestionType() != null && questionIds != null) {
                for (int qId : questionIds) {
                    try {
                        Optional<Question> qOpt = questionRepository.findById(qId);
                        if (qOpt.isPresent() && qOpt.get().getQuestionType() != quiz.getQuizQuestionType()) {
                            throw new ValidationException(String.format(
                                    "Cannot assign question #%d (%s) to a %s quiz. Quizzes are strictly confined to %s questions.",
                                    qId, qOpt.get().getQuestionType(), quiz.getQuizQuestionType(), quiz.getQuizQuestionType()));
                        }
                    } catch (Exception e) {
                        if (e instanceof ValidationException) throw e;
                    }
                }
            }
        }
        return quizRepository.assignQuestions(quizId, questionIds);
    }

    private void applyExpiration(Quiz quiz) {
        if (quiz.getActiveDurationHours() != null && quiz.getActiveDurationHours() > 0) {
            long millis = System.currentTimeMillis() + ((long) quiz.getActiveDurationHours() * 3600 * 1000);
            quiz.setExpiresAt(new Timestamp(millis));
        } else if (quiz.getActiveDurationHours() != null && quiz.getActiveDurationHours() == 0) {
            quiz.setExpiresAt(null);
        }
    }

    private void validateQuiz(Quiz quiz) {
        if (quiz.getTitle() == null || quiz.getTitle().isBlank()) {
            throw new ValidationException("Title is required.");
        }
        if (quiz.getAssessmentType() == null) {
            quiz.setAssessmentType(AssessmentType.QUIZ);
        }
        if (quiz.getAssessmentType() == AssessmentType.QUIZ) {
            if (quiz.getQuizQuestionType() == null) {
                quiz.setQuizQuestionType(QuestionType.MCQ);
            }
        } else {
            quiz.setQuizQuestionType(null);
        }
        if (quiz.getPassScore() < 1 || quiz.getPassScore() > 100) {
            quiz.setPassScore(60);
        }
    }
}