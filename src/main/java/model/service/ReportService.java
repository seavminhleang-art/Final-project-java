package model.service;

import java.math.BigDecimal;
import java.util.Map;

public interface ReportService {
    /** Score, pass/fail breakdown, average, highest/lowest for one quiz (teacher view). */
    QuizStats statsForQuiz(Long quizId);

    /** Aggregate counts across the whole platform (admin view). */
    PlatformStats platformStats();

    /** Top attempts for a quiz, best score first (student leaderboard). */
    List<Attempt> leaderboard(Long quizId, int limit);

    record QuizStats(long attemptCount, long passCount, long failCount,
                     BigDecimal averagePercentage, BigDecimal highestPercentage, BigDecimal lowestPercentage) {
    }

    record PlatformStats(long totalUsers, Map<String, Long> usersByRole,
                         long totalSubjects, long totalQuestions, long totalQuizzes,
                         long totalAttempts) {
    }
}
