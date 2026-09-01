package controller;

import model.service.ReportService;
import view.ConsoleUI;

import java.util.List;

public class ReportController {
    private final ReportService reportService;
    private final CertificateService certificateService;

    public ReportController(ReportService reportService, CertificateService certificateService) {
        this.reportService = reportService;
        this.certificateService = certificateService;
    }

    public void menu() {
        boolean back = false;
        while (!back) {
            ConsoleUI.banner("Reporting");
            ConsoleUI.println("1. Quiz statistics (teacher)");
            ConsoleUI.println("2. Platform statistics (admin)");
            ConsoleUI.println("3. Quiz leaderboard");
            ConsoleUI.println("4. Generate certificates for all passing students in a quiz");
            ConsoleUI.println("0. Back");
            switch (ConsoleUI.prompt("Choose")) {
                case "1" -> quizStats();
                case "2" -> platformStats();
                case "3" -> leaderboard();
                case "4" -> generateCertificates();
                case "0" -> back = true;
                default -> ConsoleUI.error("Invalid choice.");
            }
        }
    }

    private void quizStats() {
        Long quizId = ConsoleUI.promptLong("Quiz ID");
        ReportService.QuizStats stats = reportService.statsForQuiz(quizId);
        ConsoleUI.banner("Quiz Statistics");
        ConsoleUI.println("Attempts finished : " + stats.attemptCount());
        ConsoleUI.println("Passed            : " + stats.passCount());
        ConsoleUI.println("Failed            : " + stats.failCount());
        ConsoleUI.println("Average %         : " + stats.averagePercentage());
        ConsoleUI.println("Highest %         : " + stats.highestPercentage());
        ConsoleUI.println("Lowest %          : " + stats.lowestPercentage());
        ConsoleUI.pause();
    }

    private void platformStats() {
        ReportService.PlatformStats stats = reportService.platformStats();
        ConsoleUI.banner("Platform Statistics");
        ConsoleUI.println("Total users    : " + stats.totalUsers());
        for (Map.Entry<String, Long> e : stats.usersByRole().entrySet()) {
            ConsoleUI.println("  " + e.getKey() + ": " + e.getValue());
        }
        ConsoleUI.println("Total subjects : " + stats.totalSubjects());
        ConsoleUI.println("Total questions: " + stats.totalQuestions());
        ConsoleUI.println("Total quizzes  : " + stats.totalQuizzes());
        ConsoleUI.println("Total attempts : " + stats.totalAttempts());
        ConsoleUI.pause();
    }

    private void leaderboard() {
        Long quizId = ConsoleUI.promptLong("Quiz ID");
        int limit = ConsoleUI.promptInt("Top N");
        List<Attempt> top = reportService.leaderboard(quizId, limit);
        if (top.isEmpty()) {
            ConsoleUI.println("No finished attempts yet.");
            return;
        }
        int rank = 1;
        for (Attempt a : top) {
            ConsoleUI.println(rank++ + ". Student #" + a.getStudentId() + " - " + a.getPercentage() + "%");
        }
        ConsoleUI.pause();
    }

    private void generateCertificates() {
        Long quizId = ConsoleUI.promptLong("Quiz ID");
        try {
            List<String> paths = certificateService.generateAllForQuiz(quizId);
            if (paths.isEmpty()) {
                ConsoleUI.println("No passed attempts found for this quiz yet.");
                return;
            }
            ConsoleUI.success(paths.size() + " certificate(s) generated:");
            paths.forEach(ConsoleUI::println);
        } catch (RuntimeException e) {
            ConsoleUI.error(e.getMessage());
        }
    }
}
