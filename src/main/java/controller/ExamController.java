package controller;

import model.entity.Question;
import model.entity.Quiz;
import service.ExamService;
import service.QuizService;
import view.ConsoleUI;
import db.Session;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ExamController {

    private final ExamService examService;
    private final QuizService quizService;

    public ExamController(ExamService examService, QuizService quizService) {
        this.examService = examService;
        this.quizService = quizService;
    }

    public void takeExamMenu() {
        List<Quiz> published = quizService.listPublished();
        if (published.isEmpty()) {
            ConsoleUI.println("No quizzes are open for attempts right now.");
            return;
        }
        ConsoleUI.banner("Available Quizzes");
        for (Quiz q : published) {
            int used = examService.attemptsUsed(q.getId(), Session.current().getId());
            String attemptsInfo = q.getMaxAttempts() == 0
                    ? "unlimited attempts"
                    : used + "/" + q.getMaxAttempts() + " attempts used";
            String windowInfo = (q.getExamOpenAt() != null || q.getExamCloseAt() != null)
                    ? " | window " + ConsoleUI.formatDateTime(q.getExamOpenAt()) + "→" + ConsoleUI.formatDateTime(q.getExamCloseAt())
                    : "";
            ConsoleUI.println(q.getId() + ". " + q.getTitle() + " (" + q.getDurationMinutes() + " min, pass "
                    + q.getPassPercentage() + "%, " + attemptsInfo + windowInfo + ")");
        }
        Long quizId = ConsoleUI.promptLong("Quiz ID to attempt (0 to cancel)");
        if (quizId == 0) return;

        if (!ConsoleUI.promptYesNo("Once started the timer cannot be paused. Begin now?")) {
            return;
        }
        runExam(quizId);
    }

    private void runExam(Long quizId) {
        Attempt attempt;
        try {
            attempt = examService.startAttempt(quizId, Session.current().getId());
        } catch (RuntimeException e) {
            ConsoleUI.error(e.getMessage());
            return;
        }

        Quiz quiz = quizService.get(quizId);
        List<Question> questions = examService.questionsFor(attempt.getId());
        LocalDateTime deadline = attempt.getStartTime().plusMinutes(quiz.getDurationMinutes());

        // Background timer: auto-submits in the database the moment time is up,
        // even if the student is mid-question in the console loop below.
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            try {
                examService.autoSubmit(attempt.getId());
            } catch (RuntimeException ignored) {
                // attempt may already have been submitted manually
            }
        }, quiz.getDurationMinutes(), TimeUnit.MINUTES);

        try {
            examLoop(attempt, questions, deadline);
        } finally {
            scheduler.shutdownNow();
        }
    }

    private void examLoop(Attempt attempt, List<Question> questions, LocalDateTime deadline) {
        int current = 0;
        boolean finished = false;

        while (!finished) {
            if (LocalDateTime.now().isAfter(deadline) || attemptAlreadyClosed(attempt.getId())) {
                ConsoleUI.error("Time is up! Your exam has been auto-submitted.");
                showResult(examService.autoSubmit(attempt.getId()));
                return;
            }

            Question q = questions.get(current);
            Map<Long, AttemptAnswer> saved = examService.answersFor(attempt.getId()).stream()
                    .collect(Collectors.toMap(AttemptAnswer::getQuestionId, a -> a));

            ConsoleUI.banner("Question " + (current + 1) + " of " + questions.size()
                    + "  |  Time left: " + formatRemaining(deadline));
            ConsoleUI.println(q.getQuestionText());
            ConsoleUI.println("  A) " + q.getOptionA());
            ConsoleUI.println("  B) " + q.getOptionB());
            ConsoleUI.println("  C) " + q.getOptionC());
            ConsoleUI.println("  D) " + q.getOptionD());
            AttemptAnswer existing = saved.get(q.getId());
            if (existing != null && existing.getSelectedOption() != null) {
                ConsoleUI.println("  (currently answered: " + existing.getSelectedOption() + ")");
            }
            ConsoleUI.println("\nEnter A/B/C/D to answer, N=next, P=previous, J=jump, R=review, F=finish & submit");
            String input = ConsoleUI.prompt("Your choice").toUpperCase();

            switch (input) {
                case "A", "B", "C", "D" -> {
                    examService.saveAnswer(attempt.getId(), q.getId(), input.charAt(0));
                    if (current < questions.size() - 1) current++;
                }
                case "N" -> current = Math.min(current + 1, questions.size() - 1);
                case "P" -> current = Math.max(current - 1, 0);
                case "J" -> {
                    int target = ConsoleUI.promptInt("Go to question #") - 1;
                    if (target >= 0 && target < questions.size()) current = target;
                    else ConsoleUI.error("Invalid question number.");
                }
                case "R" -> review(questions, examService.answersFor(attempt.getId()));
                case "F" -> {
                    if (ConsoleUI.promptYesNo("Submit your exam now? This cannot be undone")) {
                        Attempt result = examService.submit(attempt.getId());
                        showResult(result);
                        finished = true;
                    }
                }
                default -> ConsoleUI.error("Unrecognized input.");
            }
        }
    }

    private boolean attemptAlreadyClosed(Long attemptId) {
        return examService.get(attemptId).getStatus() != AttemptStatus.IN_PROGRESS;
    }

    private void review(List<Question> questions, List<AttemptAnswer> answers) {
        Map<Long, Character> byQuestion = answers.stream()
                .filter(a -> a.getSelectedOption() != null)
                .collect(Collectors.toMap(AttemptAnswer::getQuestionId, AttemptAnswer::getSelectedOption));
        ConsoleUI.banner("Review");
        for (int i = 0; i < questions.size(); i++) {
            Long qid = questions.get(i).getId();
            Character ans = byQuestion.get(qid);
            ConsoleUI.println((i + 1) + ". " + (ans == null ? "(unanswered)" : "Answered: " + ans));
        }
        ConsoleUI.pause();
    }

    private String formatRemaining(LocalDateTime deadline) {
        Duration remaining = Duration.between(LocalDateTime.now(), deadline);
        if (remaining.isNegative()) return "00:00";
        long mins = remaining.toMinutes();
        long secs = remaining.minusMinutes(mins).getSeconds();
        return String.format("%02d:%02d", mins, secs);
    }

    private void showResult(Attempt attempt) {
        ConsoleUI.banner("Exam Submitted");
        ConsoleUI.println("Score: " + attempt.getScore() + " / " + attempt.getTotalMarks());
        ConsoleUI.println("Percentage: " + attempt.getPercentage() + "%");
        ConsoleUI.println("Result: " + (Boolean.TRUE.equals(attempt.getPassed()) ? "PASS" : "FAIL"));
        if (!Boolean.TRUE.equals(attempt.getPassed())) {
            ConsoleUI.println("Check Announcements on your dashboard for re-exam details.");
        }
        ConsoleUI.pause();
    }
}