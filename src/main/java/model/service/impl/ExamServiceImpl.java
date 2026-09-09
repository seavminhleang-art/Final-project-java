package service.impl;

import repository.AttemptDao;
import repository.QuizDao;
import exception.NotFoundException;
import service.AnnouncementService;
import service.ExamService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExamServiceImpl implements ExamService {

    private final AttemptDao attemptDao;
    private final QuizDao quizDao;
    private final AnnouncementService announcementService;

    public ExamServiceImpl(AttemptDao attemptDao, QuizDao quizDao, AnnouncementService announcementService) {
        this.attemptDao = attemptDao;
        this.quizDao = quizDao;
        this.announcementService = announcementService;
    }

    @Override
    public Attempt startAttempt(Long quizId, Long studentId) {
        Quiz quiz = quizDao.findById(quizId).orElseThrow(() -> new NotFoundException("Quiz not found: " + quizId));
        if (!quiz.isPublished()) {
            throw new IllegalStateException("This quiz is not currently open for attempts.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (quiz.getExamOpenAt() != null && now.isBefore(quiz.getExamOpenAt())) {
            throw new IllegalStateException("This quiz opens at "
                    + quiz.getExamOpenAt() + " and is not available yet.");
        }
        if (quiz.getExamCloseAt() != null && now.isAfter(quiz.getExamCloseAt())) {
            throw new IllegalStateException("This quiz's exam window closed at "
                    + quiz.getExamCloseAt() + ".");
        }

        int used = attemptsUsed(quizId, studentId);
        if (quiz.getMaxAttempts() > 0 && used >= quiz.getMaxAttempts()) {
            throw new IllegalStateException("You have used all " + quiz.getMaxAttempts()
                    + " attempt(s) allowed for this quiz.");
        }

        Attempt attempt = Attempt.builder()
                .quizId(quizId)
                .studentId(studentId)
                .status(AttemptStatus.IN_PROGRESS)
                .build();
        return attemptDao.create(attempt);
    }

    @Override
    public int attemptsUsed(Long quizId, Long studentId) {
        return (int) attemptDao.findByQuizAndStudent(quizId, studentId).stream()
                .filter(a -> a.getStatus() != AttemptStatus.IN_PROGRESS)
                .count();
    }

    @Override
    public List<Question> questionsFor(Long attemptId) {
        Attempt attempt = get(attemptId);
        return quizDao.getQuestions(attempt.getQuizId());
    }

    @Override
    public void saveAnswer(Long attemptId, Long questionId, Character selectedOption) {
        Attempt attempt = get(attemptId);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new IllegalStateException("This attempt has already been submitted.");
        }
        AttemptAnswer answer = AttemptAnswer.builder()
                .attemptId(attemptId)
                .questionId(questionId)
                .selectedOption(selectedOption)
                .isCorrect(null) // computed at grading time
                .build();
        attemptDao.upsertAnswer(answer);
    }

    @Override
    public List<AttemptAnswer> answersFor(Long attemptId) {
        return attemptDao.getAnswers(attemptId);
    }

    @Override
    public Attempt submit(Long attemptId) {
        return grade(attemptId, AttemptStatus.SUBMITTED);
    }

    @Override
    public synchronized Attempt autoSubmit(Long attemptId) {
        Attempt attempt = get(attemptId);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            return attempt; // already finalized by the student
        }
        return grade(attemptId, AttemptStatus.AUTO_SUBMITTED);
    }

    private Attempt grade(Long attemptId, AttemptStatus finalStatus) {
        Attempt attempt = get(attemptId);
        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            return attempt;
        }
        Quiz quiz = quizDao.findById(attempt.getQuizId())
                .orElseThrow(() -> new NotFoundException("Quiz not found: " + attempt.getQuizId()));
        List<Question> questions = quizDao.getQuestions(quiz.getId());
        Map<Long, BigDecimal> marksByQuestion = quizDao.getQuizQuestionLinks(quiz.getId()).stream()
                .collect(Collectors.toMap(QuizQuestion::getQuestionId, QuizQuestion::getMarks));
        List<AttemptAnswer> answers = attemptDao.getAnswers(attemptId);
        Map<Long, Character> selectedByQuestion = answers.stream()
                .collect(Collectors.toMap(AttemptAnswer::getQuestionId, AttemptAnswer::getSelectedOption));

        BigDecimal score = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (Question q : questions) {
            BigDecimal marks = marksByQuestion.getOrDefault(q.getId(), BigDecimal.ONE);
            total = total.add(marks);
            Character selected = selectedByQuestion.get(q.getId());
            boolean correct = selected != null && Character.toUpperCase(selected) == q.getCorrectOption();
            if (correct) {
                score = score.add(marks);
            }
            attemptDao.upsertAnswer(AttemptAnswer.builder()
                    .attemptId(attemptId)
                    .questionId(q.getId())
                    .selectedOption(selected)
                    .isCorrect(correct)
                    .build());
        }

        BigDecimal percentage = total.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : score.multiply(BigDecimal.valueOf(100)).divide(total, 2, RoundingMode.HALF_UP);
        boolean passed = percentage.compareTo(quiz.getPassPercentage()) >= 0;

        attempt.setEndTime(LocalDateTime.now());
        attempt.setStatus(finalStatus);
        attempt.setScore(score);
        attempt.setTotalMarks(total);
        attempt.setPercentage(percentage);
        attempt.setPassed(passed);
        Attempt saved = attemptDao.update(attempt);

        if (!passed) {
            int used = attemptsUsed(quiz.getId(), attempt.getStudentId());
            announcementService.notifyFailure(attempt.getStudentId(), quiz.getId(), saved.getId(),
                    quiz.getTitle(), used, quiz.getMaxAttempts());
        }
        return saved;
    }

    @Override
    public Attempt get(Long attemptId) {
        return attemptDao.findById(attemptId).orElseThrow(() -> new NotFoundException("Attempt not found: " + attemptId));
    }

    @Override
    public List<Attempt> historyFor(Long studentId) {
        return attemptDao.findByStudent(studentId);
    }

    @Override
    public List<Attempt> attemptsForQuiz(Long quizId) {
        return attemptDao.findByQuiz(quizId);
    }
}