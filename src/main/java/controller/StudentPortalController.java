package controller;

import model.entity.Attempt;
import model.entity.Quiz;
import service.ExamService;
import service.QuizService;
import service.ReportService;
import view.ConsoleUI;
import db.Session;

import java.util.List;

public class StudentPortalController {

    private final ExamService examService;
    private final QuizService quizService;
    private final ReportService reportService;

    public StudentPortalController(ExamService examService, QuizService quizService, ReportService reportService) {
        this.examService = examService;
        this.quizService = quizService;
        this.reportService = reportService;
    }

    public void menu() {
        boolean back = false;
        while (!back) {
            ConsoleUI.banner("Student Portal");
            ConsoleUI.println("1. My attempt history");
            ConsoleUI.println("2. Leaderboard for a quiz");
            ConsoleUI.println("0. Back");
            switch (ConsoleUI.prompt("Choose")) {
                case "1" -> history();
                case "2" -> leaderboard();
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
            ConsoleUI.println(quiz.getTitle() + " | " + a.getStatus() + " | " + scoreLine);
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
}