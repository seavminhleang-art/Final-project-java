package controller;

import db.Session;
import model.entity.Attempt;
import model.entity.AttemptAnswer;
import model.entity.Question;
import model.entity.Quiz;
import model.service.CertificateService;
import model.service.ReportService;
import model.service.impl.ExamServiceImpl;
import model.service.ExamService;
import service.QuizService;
import view.ConsoleUI;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StudentPortalController {

    private final ExamService examService;
    private final QuizService quizService;
    private final ReportService reportService;
    private CertificateService certificateService ;

    public StudentPortalController(ExamServiceImpl examService, QuizService quizService, ReportService reportService) {
        this.examService = examService;
        this.quizService = quizService;
        this.reportService = reportService;
        this.certificateService = certificateService;
    }

    public void menu() {
        boolean back = false;
        while (!back) {
            ConsoleUI.banner("Student Portal");
            ConsoleUI.println("1. My attempt history");
            ConsoleUI.println("2. Leaderboard for a quiz");
            ConsoleUI.println("3. Review a graded attempt");
            ConsoleUI.println("4. Download certificate for a passed attempt");
            ConsoleUI.println("0. Back");
            switch (ConsoleUI.prompt("Choose")) {
                case "1" -> history();
                case "2" -> leaderboard();
                case "3" -> reviewAttempt();
                case "4" -> downloadCertificate();
                case "0" -> back = true;
                default -> ConsoleUI.error("Invalid choice.");
            }
        }
    }

    private void history() {
        List<Attempt> attempts = examService.historyFor(Session.current().getId());
        if (attempts.isEmpty()) {
            ConsoleUI.println("You haven't attempted any quizzes yet.");
            return;
        }
        ConsoleUI.banner("Your Attempt History");
        for (Attempt a : attempts) {
            Quiz quiz = quizService.get(a.getQuizId());
            String scoreLine = a.getStatus().name().equals("IN_PROGRESS")
                    ? "in progress"
                    : a.getScore() + "/" + a.getTotalMarks() + " (" + a.getPercentage() + "%) - "
                    + (Boolean.TRUE.equals(a.getPassed()) ? "PASS" : "FAIL");
            ConsoleUI.println("Attempt #" + a.getId() + " | " + quiz.getTitle() + " | " + a.getStatus() + " | " + scoreLine);
        }
        ConsoleUI.pause();
    }

    private void leaderboard() {
        Long quizId = ConsoleUI.promptLong("Quiz ID");
        int limit = ConsoleUI.promptInt("How many top scores to show");
        List<Attempt> top = reportService.leaderboard(quizId, limit);
        if (top.isEmpty()) {
            ConsoleUI.println("No finished attempts for this quiz yet.");
            return;
        }
        ConsoleUI.banner("Leaderboard");
        int rank = 1;
        for (Attempt a : top) {
            ConsoleUI.println(rank++ + ". Student #" + a.getStudentId() + " - " + a.getPercentage() + "%  ("
                    + (Boolean.TRUE.equals(a.getPassed()) ? "PASS" : "FAIL") + ")");
        }
        ConsoleUI.pause();
    }

    private void reviewAttempt() {
        Long attemptId = ConsoleUI.promptLong("Attempt ID to review (see attempt history for the ID)");
        Long studentId = Session.current().getId();
        try {
            Attempt attempt = examService.get(attemptId);
            if (!attempt.getStudentId().equals(studentId)) {
                ConsoleUI.error("That attempt doesn't belong to you.");
                return;
            }
            if (!examService.canReview(attemptId, studentId)) {
                ConsoleUI.error("This attempt isn't reviewable yet — either it's still in progress, "
                        + "or the teacher has turned off answer review for this quiz.");
                return;
            }
            Quiz quiz = quizService.get(attempt.getQuizId());
            List<Question> questions = quizService.questionsFor(quiz.getId());
            Map<Long, AttemptAnswer> byQuestion = examService.answersFor(attemptId).stream()
                    .collect(Collectors.toMap(AttemptAnswer::getQuestionId, a -> a));

            ConsoleUI.banner("Review: " + quiz.getTitle() + " (Attempt #" + attemptId + ")");
            int i = 1;
            for (Question q : questions) {
                AttemptAnswer ans = byQuestion.get(q.getId());
                Character selected = ans == null ? null : ans.getSelectedOption();
                boolean correct = ans != null && Boolean.TRUE.equals(ans.getIsCorrect());
                ConsoleUI.println(i++ + ". " + q.getQuestionText());
                ConsoleUI.println("   A) " + q.getOptionA());
                ConsoleUI.println("   B) " + q.getOptionB());
                ConsoleUI.println("   C) " + q.getOptionC());
                ConsoleUI.println("   D) " + q.getOptionD());
                ConsoleUI.println("   Your answer: " + (selected == null ? "(unanswered)" : selected)
                        + (correct ? "  [correct]" : "  [incorrect — correct answer: " + q.getCorrectOption() + "]"));
            }
            ConsoleUI.pause();
        } catch (RuntimeException e) {
            ConsoleUI.error(e.getMessage());
        }
    }

    private void downloadCertificate() {
        Long attemptId = ConsoleUI.promptLong("Attempt ID (see attempt history for the ID)");
        try {
            Attempt attempt = examService.get(attemptId);
            if (!attempt.getStudentId().equals(Session.current().getId())) {
                ConsoleUI.error("That attempt doesn't belong to you.");
                return;
            }
            if (!Boolean.TRUE.equals(attempt.getPassed())) {
                ConsoleUI.error("Certificates are only available for passed attempts.");
                return;
            }
            String path = certificateService.generate(attemptId);
            ConsoleUI.success("Certificate saved to: " + path);
        } catch (RuntimeException e) {
            ConsoleUI.error(e.getMessage());
        }
    }
}
