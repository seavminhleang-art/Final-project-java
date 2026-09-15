package com.proctor.model.service;

import com.proctor.model.entity.AIGradeResult;
import com.proctor.model.entity.AssessmentOverviewDTO;
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
import com.proctor.model.entity.SpeedQuizSession;
import com.proctor.model.entity.SpeedQuizAnswerRecord;

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

    public Optional<Quiz> getQuiz(int quizId) {
        return quizRepository.findById(quizId);
    }

    public List<Quiz> getAvailableExams(int studentId) {
        return quizRepository.findAll(AssessmentType.EXAM, null, null, true, null, false);
    }

    public List<Quiz> getAvailableSpeedQuizzes(int studentId) {
        return quizRepository.findAll(AssessmentType.SPEED, null, null, true, null, false);
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

    public SpeedQuizSession startSpeedQuiz(int quizId, int studentId) {
        Optional<Quiz> quizOpt = quizRepository.findById(quizId);
        if (quizOpt.isEmpty()) {
            throw new ValidationException("Speed quiz not found.");
        }
        Quiz quiz = quizOpt.get();
        if (!quiz.isPublished()) {
            throw new ValidationException("This speed quiz is not currently published.");
        }
        if (quiz.isExpired()) {
            throw new ValidationException("This speed quiz has expired.");
        }
        if (quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
            throw new ValidationException("This speed quiz has no questions available.");
        }

        Attempt attempt = attemptRepository.createAttempt(quizId, studentId);
        if (attempt == null) {
            throw new ValidationException("Failed to initiate speed quiz attempt.");
        }

        List<Question> questions = new ArrayList<>(quiz.getQuestions());
        if (quiz.isRandomizeAnswers()) {
            for (Question q : questions) {
                if (q.getOptions() != null) {
                    List<QuestionOption> shuffled = new ArrayList<>(q.getOptions());
                    Collections.shuffle(shuffled);
                    q.setOptions(shuffled);
                }
            }
        }

        SpeedQuizSession session = new SpeedQuizSession();
        session.initSession(quiz, attempt, questions);
        return session;
    }

    public Result submitSpeedQuiz(SpeedQuizSession session) {
        if (session == null || session.getAttempt() == null) {
            throw new ValidationException("Invalid speed quiz session.");
        }

        int attemptId = session.getAttempt().getId();
        int studentId = session.getAttempt().getStudentId();
        int quizId = session.getQuiz().getId();

        for (SpeedQuizAnswerRecord record : session.getAnswerRecords()) {
            Question q = record.getQuestion();
            if (q == null) continue;
            attemptRepository.saveAnswer(attemptId, q.getId(), record.getSelectedOptionId(), null);
        }

        List<AttemptAnswer> savedAnswers = attemptRepository.getAttemptAnswers(attemptId);
        Map<Integer, AttemptAnswer> answerMap = new HashMap<>();
        for (AttemptAnswer a : savedAnswers) {
            answerMap.put(a.getQuestionId(), a);
        }

        for (SpeedQuizAnswerRecord record : session.getAnswerRecords()) {
            Question q = record.getQuestion();
            if (q == null) continue;
            AttemptAnswer ans = answerMap.get(q.getId());
            if (ans != null) {
                attemptRepository.updateAnswerGrade(
                        ans.getId(),
                        record.isCorrect(),
                        record.getTotalPointsAwarded(),
                        record.isCorrect() ? 100 : 0,
                        null,
                        null
                );
            }
        }

        attemptRepository.finalizeAttempt(attemptId, AttemptStatus.GRADED);

        double roundedScore = Math.round(session.getTotalScore() * 10.0) / 10.0;
        Result result = Result.builder()
                .attemptId(attemptId)
                .studentId(studentId)
                .quizId(quizId)
                .quizTitle(session.getQuiz().getTitle())
                .assessmentType(AssessmentType.SPEED)
                .totalPoints(roundedScore)
                .maxPoints(roundedScore)
                .percentage(100.0)
                .passed(true)
                .pendingReview(false)
                .build();

        resultRepository.saveResult(result);
        return result;
    }

    public Optional<AssessmentOverviewDTO> getAssessmentOverview(int quizId, int studentId) {
        Optional<Quiz> quizOpt = quizRepository.findById(quizId);
        if (quizOpt.isEmpty()) {
            return Optional.empty();
        }
        Quiz q = quizOpt.get();
        Map<String, Object> qStats = quizRepository.getQuizQuestionStats(quizId);
        Map<String, Object> cStats = quizRepository.getQuizCommunityStats(quizId);

        int qCount = (int) qStats.getOrDefault("totalQuestions", q.getQuestionCount());
        double totalPts = (double) qStats.getOrDefault("totalPoints", q.getTotalPoints());
        int easyCount = (int) qStats.getOrDefault("easyCount", 0);
        int medCount = (int) qStats.getOrDefault("mediumCount", 0);
        int hardCount = (int) qStats.getOrDefault("hardCount", 0);
        String qTypesRaw = (String) qStats.getOrDefault("questionTypes", null);

        String qTypesSummary = "Multiple Choice";
        if (qTypesRaw != null && !qTypesRaw.isBlank()) {
            List<String> types = new ArrayList<>();
            for (String t : qTypesRaw.split(",")) {
                String cleanT = t.trim();
                if ("MCQ".equalsIgnoreCase(cleanT)) types.add("Multiple Choice");
                else if ("TRUE_FALSE".equalsIgnoreCase(cleanT)) types.add("True / False");
                else if ("SHORT_ANSWER".equalsIgnoreCase(cleanT)) types.add("Written (Short Answer)");
                else if (!cleanT.isEmpty()) types.add(cleanT);
            }
            if (!types.isEmpty()) {
                qTypesSummary = String.join(", ", types);
            }
        }

        String overallDifficulty;
        if (q.getAssessmentType() == AssessmentType.SPEED) {
            overallDifficulty = "ADAPTIVE (Dynamic Scaling)";
        } else {
            if (hardCount > medCount && hardCount > easyCount) {
                overallDifficulty = "HARD";
            } else if (easyCount > medCount && easyCount > hardCount) {
                overallDifficulty = "EASY";
            } else {
                overallDifficulty = "MEDIUM";
            }
        }

        int totalTakers = (int) cStats.getOrDefault("totalTakers", 0);
        int passedCount = (int) cStats.getOrDefault("passedCount", 0);
        double passRate = (double) cStats.getOrDefault("passRate", 0.0);
        double avgScore = (double) cStats.getOrDefault("avgScore", 0.0);
        double topScore = (double) cStats.getOrDefault("topScore", 0.0);

        Optional<Attempt> attOpt = studentId > 0 ? attemptRepository.findLatestAttempt(quizId, studentId) : Optional.empty();
        Optional<Result> resOpt = attOpt.isPresent() ? resultRepository.findByAttemptId(attOpt.get().getId()) : Optional.empty();

        boolean canStart = true;
        boolean canRetake = false;
        boolean canViewResult = false;

        if (q.isExpired()) {
            canStart = false;
        }

        if (attOpt.isPresent()) {
            Attempt att = attOpt.get();
            if (att.getStatus() == AttemptStatus.GRADED) {
                canViewResult = true;
                if (q.getAssessmentType() != AssessmentType.SPEED) {
                    canStart = false;
                }
            } else if (att.getStatus() == AttemptStatus.TURNED_IN) {
                canStart = false;
            } else if (att.getStatus() == AttemptStatus.AUTO_SUBMITTED) {
                canStart = false;
                canRetake = true;
            } else if (att.getStatus() == AttemptStatus.IN_PROGRESS) {
                canStart = true;
            }
        }

        if (q.getAssessmentType() == AssessmentType.EXAM && resOpt.isPresent() && !resOpt.get().isPassed()) {
            canRetake = true;
        }
        if (q.getAssessmentType() == AssessmentType.EXAM && attOpt.isEmpty() && q.isExpired()) {
            canRetake = true;
        }

        AssessmentOverviewDTO dto = AssessmentOverviewDTO.builder()
                .quiz(q)
                .subjectCode(q.getSubjectCode())
                .subjectName(q.getSubjectName() != null ? q.getSubjectName() : q.getSubjectCode())
                .teacherName(q.getCreatorName() != null ? q.getCreatorName() : "Teacher")
                .questionCount(qCount)
                .totalPoints(totalPts)
                .timeLimitMins(q.getTimeLimitMins())
                .speedSecondsPerQuestion(q.getSpeedSecondsPerQuestion())
                .passScorePercent(q.getPassScore())
                .questionTypesSummary(qTypesSummary)
                .overallDifficulty(overallDifficulty)
                .easyQuestions(easyCount)
                .mediumQuestions(medCount)
                .hardQuestions(hardCount)
                .totalTakers(totalTakers)
                .passedCount(passedCount)
                .passRate(passRate)
                .avgScore(avgScore)
                .topScore(topScore)
                .studentAttempt(attOpt.orElse(null))
                .studentResult(resOpt.orElse(null))
                .canStart(canStart)
                .canRetake(canRetake)
                .canViewResult(canViewResult)
                .build();

        return Optional.of(dto);
    }
}