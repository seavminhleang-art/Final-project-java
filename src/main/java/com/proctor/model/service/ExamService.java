package com.proctor.model.service;

import com.proctor.model.entity.AIGradeResult;
import com.proctor.model.enums.AttemptStatus;
import com.proctor.model.enums.QuestionType;
import com.proctor.exception.ValidationException;
import com.proctor.model.entity.Attempt;
import com.proctor.model.entity.AttemptAnswer;
import com.proctor.model.entity.ExamSession;
import com.proctor.model.repository.AttemptRepository;
import com.proctor.model.entity.Question;
import com.proctor.model.entity.QuestionOption;
import com.proctor.model.entity.Quiz;
import com.proctor.model.repository.QuizRepository;
import com.proctor.model.entity.Result;
import com.proctor.model.repository.ResultRepository;

import java.util.*;

import com.proctor.model.enums.AssessmentType;

public class ExamService {
    private final QuizRepository quizRepository;
    private final AttemptRepository attemptRepository;
    private final ResultRepository resultRepository;
    private final AIService aiService;

    public ExamService(QuizRepository quizRepository, AttemptRepository attemptRepository, ResultRepository resultRepository) {
        this(quizRepository, attemptRepository, resultRepository, new AIService());
    }

    public ExamService(QuizRepository quizRepository, AttemptRepository attemptRepository, ResultRepository resultRepository, AIService aiService) {
        this.quizRepository = quizRepository;
        this.attemptRepository = attemptRepository;
        this.resultRepository = resultRepository;
        this.aiService = aiService;
    }

    public List<Quiz> getAvailableQuizzes(int studentId) {
        return quizRepository.findAll(AssessmentType.QUIZ, null, null, true, null, false);
    }

    public List<Quiz> getAvailableExams(int studentId) {
        return quizRepository.findAll(AssessmentType.EXAM, null, null, true, null, false);
    }

    public Optional<Attempt> getStudentAttempt(int quizId, int studentId) {
        return attemptRepository.findLatestAttempt(quizId, studentId);
    }

    public Optional<Result> getResultByAttempt(int attemptId) {
        return resultRepository.findByAttemptId(attemptId);
    }

    public List<Attempt> getSubmissionsForQuiz(int quizId) {
        return attemptRepository.getAttemptsByQuiz(quizId);
    }

    public List<Attempt> getAllSubmissions(Integer teacherId) {
        return attemptRepository.getAllSubmissions(teacherId);
    }

    public List<AttemptAnswer> getAttemptAnswers(int attemptId) {
        return attemptRepository.getAttemptAnswers(attemptId);
    }

    public ExamSession startExam(int quizId, int studentId) {
        Optional<Quiz> quizOpt = quizRepository.findById(quizId);
        if (quizOpt.isEmpty() || !quizOpt.get().isPublished()) {
            throw new ValidationException("Quiz not found or is not published.");
        }

        Quiz quiz = quizOpt.get();
        if (quiz.isExpired()) {
            throw new ValidationException("This assessment has expired.");
        }

        Optional<Attempt> existingOpt = attemptRepository.findLatestAttempt(quizId, studentId);
        Attempt attempt;

        if (existingOpt.isPresent()) {
            Attempt latest = existingOpt.get();
            if (latest.getStatus() == AttemptStatus.TURNED_IN || latest.getStatus() == AttemptStatus.GRADED || latest.getStatus() == AttemptStatus.AUTO_SUBMITTED) {
                String itemType = quiz.getAssessmentType() == AssessmentType.EXAM ? "exam" : "quiz";
                throw new ValidationException("You have already turned in this " + itemType + ". View your scorecard in History.");
            }
            attempt = latest;
        } else {
            attempt = attemptRepository.createAttempt(quizId, studentId);
            if (attempt == null) {
                throw new ValidationException("Failed to initiate examination session.");
            }
        }

        List<Question> questions = new ArrayList<>(quiz.getQuestions());
        if (quiz.isRandomizeQuestions()) {
            Collections.shuffle(questions);
        }

        if (quiz.isRandomizeAnswers()) {
            for (Question q : questions) {
                if (q.getQuestionType() == QuestionType.MCQ && q.getOptions() != null) {
                    List<QuestionOption> shuffled = new ArrayList<>(q.getOptions());
                    Collections.shuffle(shuffled);
                    q.setOptions(shuffled);
                }
            }
        }

        Map<Integer, Integer> selectedOpts = new HashMap<>();
        Map<Integer, String> textAns = new HashMap<>();
        List<AttemptAnswer> saved = attemptRepository.getAttemptAnswers(attempt.getId());
        for (AttemptAnswer a : saved) {
            if (a.getSelectedOptionId() != null) selectedOpts.put(a.getQuestionId(), a.getSelectedOptionId());
            if (a.getTextAnswer() != null) textAns.put(a.getQuestionId(), a.getTextAnswer());
        }

        boolean isTimed = quiz.getTimeLimitMins() != null && quiz.getTimeLimitMins() > 0;
        int remainingSecs = isTimed ? quiz.getTimeLimitMins() * 60 : 0;

        return ExamSession.builder()
                .attempt(attempt)
                .quiz(quiz)
                .questions(questions)
                .selectedOptions(selectedOpts)
                .textAnswers(textAns)
                .remainingSeconds(remainingSecs)
                .isTimed(isTimed)
                .build();
    }

    public void recordAnswer(int attemptId, int questionId, Integer selectedOptionId, String textAnswer) {
        attemptRepository.saveAnswer(attemptId, questionId, selectedOptionId, textAnswer);
    }

    public Result submitExam(ExamSession session, boolean autoSubmitted) {
        Optional<Attempt> existingOpt = attemptRepository.getAttempt(session.getAttempt().getId());
        if (existingOpt.isPresent()) {
            Attempt existing = existingOpt.get();
            if (existing.getStatus() == AttemptStatus.GRADED || existing.getStatus() == AttemptStatus.TURNED_IN || existing.getStatus() == AttemptStatus.AUTO_SUBMITTED) {
                Optional<Result> existingRes = resultRepository.findByAttemptId(session.getAttempt().getId());
                if (existingRes.isPresent()) {
                    return existingRes.get();
                }
                double maxPoints = session.getQuestions().stream().mapToDouble(Question::getPoints).sum();
                return Result.builder()
                        .attemptId(session.getAttempt().getId())
                        .studentId(session.getAttempt().getStudentId())
                        .quizId(session.getQuiz().getId())
                        .quizTitle(session.getQuiz().getTitle())
                        .assessmentType(session.getQuiz().getAssessmentType())
                        .totalPoints(0.0)
                        .maxPoints(Math.round(maxPoints * 10.0) / 10.0)
                        .percentage(0.0)
                        .passed(false)
                        .pendingReview(true)
                        .gradedAt(null)
                        .build();
            }
        }

        AttemptStatus finalStatus = autoSubmitted ? AttemptStatus.AUTO_SUBMITTED : AttemptStatus.TURNED_IN;
        attemptRepository.finalizeAttempt(session.getAttempt().getId(), finalStatus);

        List<AttemptAnswer> savedAnswers = attemptRepository.getAttemptAnswers(session.getAttempt().getId());
        Map<Integer, AttemptAnswer> answerMap = new HashMap<>();
        for (AttemptAnswer a : savedAnswers) {
            answerMap.put(a.getQuestionId(), a);
        }

        boolean hasShortAnswer = false;
        double totalAwarded = 0.0;
        double maxPoints = 0.0;

        for (Question q : session.getQuestions()) {
            maxPoints += q.getPoints();
            AttemptAnswer ans = answerMap.get(q.getId());

            if (q.getQuestionType() == QuestionType.SHORT_ANSWER) {
                hasShortAnswer = true;
                continue;
            }

            if (ans != null && (q.getQuestionType() == QuestionType.MCQ || q.getQuestionType() == QuestionType.TRUE_FALSE)) {
                boolean isCorrect = false;
                if (ans.getSelectedOptionId() != null && q.getOptions() != null) {
                    for (QuestionOption opt : q.getOptions()) {
                        if (opt.getId() != null && opt.getId().equals(ans.getSelectedOptionId()) && opt.isCorrect()) {
                            isCorrect = true;
                            break;
                        }
                    }
                }
                double pts = isCorrect ? q.getPoints() : 0.0;
                totalAwarded += pts;
                attemptRepository.updateAnswerGrade(ans.getId(), isCorrect, pts, isCorrect ? 100 : 0, null, null);
            }
        }

        if (!hasShortAnswer) {
            Result res = returnGrade(session.getAttempt().getId());
            if (autoSubmitted) {
                attemptRepository.finalizeAttempt(session.getAttempt().getId(), AttemptStatus.AUTO_SUBMITTED);
            }
            return res;
        }

        return Result.builder()
                .attemptId(session.getAttempt().getId())
                .studentId(session.getAttempt().getStudentId())
                .quizId(session.getQuiz().getId())
                .quizTitle(session.getQuiz().getTitle())
                .assessmentType(session.getQuiz().getAssessmentType())
                .totalPoints(0.0)
                .maxPoints(Math.round(maxPoints * 10.0) / 10.0)
                .percentage(0.0)
                .passed(false)
                .pendingReview(true)
                .gradedAt(null)
                .build();
    }

    public boolean gradeWithAI(int attemptId) {
        Optional<Attempt> attemptOpt = attemptRepository.getAttempt(attemptId);
        if (attemptOpt.isEmpty()) return false;

        Attempt attempt = attemptOpt.get();
        if (attempt.getStatus() == AttemptStatus.GRADED) {
            throw new ValidationException("This submission is already graded. AI grading cannot be re-run.");
        }
        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            throw new ValidationException("Cannot grade an assessment that is still in progress by the student.");
        }

        Optional<Quiz> quizOpt = quizRepository.findById(attempt.getQuizId());
        if (quizOpt.isEmpty()) return false;

        Quiz quiz = quizOpt.get();
        Map<Integer, Question> qMap = new HashMap<>();
        for (Question q : quiz.getQuestions()) {
            qMap.put(q.getId(), q);
        }

        List<AttemptAnswer> answers = attemptRepository.getAttemptAnswers(attemptId);
        for (AttemptAnswer ans : answers) {
            Question q = qMap.get(ans.getQuestionId());
            if (q != null && q.getQuestionType() == QuestionType.SHORT_ANSWER) {
                if (ans.getTextAnswer() != null && !ans.getTextAnswer().isBlank()) {
                    try {
                        AIGradeResult gradeResult = aiService.gradeShortAnswer(q.getQuestionText(), q.getExplanation(), ans.getTextAnswer());
                        double fraction = gradeResult.getScore() / 100.0;
                        double pts = Math.round(fraction * q.getPoints() * 10.0) / 10.0;
                        boolean isCorrect = gradeResult.getScore() >= 50;
                        attemptRepository.updateAnswerGrade(ans.getId(), isCorrect, pts, gradeResult.getScore(), gradeResult.getFeedback(), ans.getTeacherFeedback());
                    } catch (Exception e) {
                        double pts = q.getPoints();
                        attemptRepository.updateAnswerGrade(ans.getId(), true, pts, 100, "Evaluated (Offline mode)", ans.getTeacherFeedback());
                    }
                } else {
                    attemptRepository.updateAnswerGrade(ans.getId(), false, 0.0, 0, "No answer provided", ans.getTeacherFeedback());
                }
            }
        }
        return true;
    }

    public Result returnGrade(int attemptId) {
        Optional<Attempt> attemptOpt = attemptRepository.getAttempt(attemptId);
        if (attemptOpt.isEmpty()) {
            throw new ValidationException("Attempt not found.");
        }

        Attempt attempt = attemptOpt.get();
        if (attempt.getStatus() == AttemptStatus.GRADED) {
            throw new ValidationException("Grade has already been returned for this submission.");
        }
        if (attempt.getStatus() == AttemptStatus.IN_PROGRESS) {
            throw new ValidationException("Cannot return grade for an assessment that is still in progress.");
        }

        Optional<Quiz> quizOpt = quizRepository.findById(attempt.getQuizId());
        if (quizOpt.isEmpty()) {
            throw new ValidationException("Quiz not found.");
        }

        Quiz quiz = quizOpt.get();
        List<AttemptAnswer> answers = attemptRepository.getAttemptAnswers(attemptId);

        double totalAwarded = 0.0;
        double maxPoints = 0.0;

        Map<Integer, Question> qMap = new HashMap<>();
        for (Question q : quiz.getQuestions()) {
            qMap.put(q.getId(), q);
            maxPoints += q.getPoints();
        }

        for (AttemptAnswer ans : answers) {
            totalAwarded += ans.getPointsAwarded();
        }

        double percentage = maxPoints > 0 ? (totalAwarded / maxPoints) * 100.0 : 0.0;
        boolean passed = percentage >= quiz.getPassScore();

        Result result = Result.builder()
                .attemptId(attempt.getId())
                .studentId(attempt.getStudentId())
                .quizId(quiz.getId())
                .quizTitle(quiz.getTitle())
                .assessmentType(quiz.getAssessmentType())
                .totalPoints(Math.round(totalAwarded * 10.0) / 10.0)
                .maxPoints(Math.round(maxPoints * 10.0) / 10.0)
                .percentage(Math.round(percentage * 10.0) / 10.0)
                .passed(passed)
                .build();

        resultRepository.saveResult(result);
        attemptRepository.finalizeAttempt(attemptId, AttemptStatus.GRADED);
        return result;
    }
}