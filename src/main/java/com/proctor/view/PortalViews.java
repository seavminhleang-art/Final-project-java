package com.proctor.view;

import com.proctor.model.entity.LeaderboardEntry;
import com.proctor.util.TuiHelper;

import java.util.List;

public class PortalViews {

    public static String renderGlobalLeaderboard(List<LeaderboardEntry> leaderboard, int selectedIndex) {
        return renderGlobalLeaderboard(leaderboard, selectedIndex, com.proctor.controller.GlobalLeaderboardScreen.Mode.QUIZ);
    }

    public static String renderGlobalLeaderboard(List<LeaderboardEntry> leaderboard, int selectedIndex, boolean isSpeedMode) {
        return renderGlobalLeaderboard(leaderboard, selectedIndex,
                isSpeedMode ? com.proctor.controller.GlobalLeaderboardScreen.Mode.SPEED
                            : com.proctor.controller.GlobalLeaderboardScreen.Mode.QUIZ);
    }

    public static String renderGlobalLeaderboard(List<LeaderboardEntry> leaderboard, int selectedIndex,
                                                com.proctor.controller.GlobalLeaderboardScreen.Mode mode) {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("LEADERBOARD"));
        sb.append("\n");

        String title;
        String countColHeader;
        int activeTab;
        if (mode == com.proctor.controller.GlobalLeaderboardScreen.Mode.SPEED) {
            title = "Speed Quiz Champions";
            countColHeader = "RUNS";
            activeTab = 2;
        } else if (mode == com.proctor.controller.GlobalLeaderboardScreen.Mode.EXAM) {
            title = "Exam Honor Roll";
            countColHeader = "EXAMS";
            activeTab = 1;
        } else {
            title = "Quiz Champions";
            countColHeader = "QUIZZES";
            activeTab = 0;
        }

        sb.append(TuiHelper.boxTitle(title, String.format("Top Performers (%d ranked)", leaderboard.size()))).append("\n\n");
        sb.append(TuiHelper.tabBar(new String[]{"Quizzes", "Exams", "Speed Quizzes"}, activeTab)).append("\n\n");

        if (mode == com.proctor.controller.GlobalLeaderboardScreen.Mode.SPEED) {
            sb.append(String.format("  %-6s  %-54s  %-32s  %-10s  %-14s%n",
                    "RANK", "STUDENT NAME", "USERNAME", countColHeader, "HIGH SCORE")).append("\n");
        } else {
            sb.append(String.format("  %-6s  %-54s  %-32s  %-10s  %-12s  %-8s%n",
                    "RANK", "STUDENT NAME", "USERNAME", countColHeader, "TOTAL PTS", "AVG %")).append("\n");
        }
        sb.append("  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

        if (leaderboard.isEmpty()) {
            String emptyMsg;
            if (mode == com.proctor.controller.GlobalLeaderboardScreen.Mode.SPEED) {
                emptyMsg = "No speed quizzes completed yet. Take a speed quiz to claim the #1 spot!";
            } else if (mode == com.proctor.controller.GlobalLeaderboardScreen.Mode.EXAM) {
                emptyMsg = "No exams completed yet. Complete an exam to earn a spot on the honor roll!";
            } else {
                emptyMsg = "No quizzes completed yet. Be the first on the quiz leaderboard!";
            }
            sb.append("  ").append(TuiHelper.dim(emptyMsg)).append("\n");
        } else {
            int pageSize = TuiHelper.PAGE_SIZE;
            int startRow = (selectedIndex / pageSize) * pageSize;
            int endRow = Math.min(leaderboard.size(), startRow + pageSize);

            for (int i = startRow; i < endRow; i++) {
                LeaderboardEntry entry = leaderboard.get(i);
                String cursor = (i == selectedIndex) ? TuiHelper.cyan("▶ ") : "  ";

                String rankStr;
                if (entry.getRank() == 1) rankStr = "#1";
                else if (entry.getRank() == 2) rankStr = "#2";
                else if (entry.getRank() == 3) rankStr = "#3";
                else rankStr = String.format("%d", entry.getRank());

                String line;
                if (mode == com.proctor.controller.GlobalLeaderboardScreen.Mode.SPEED) {
                    line = String.format("%-6s  %-54s  %-32s  %-10d  %-14s",
                            rankStr,
                            truncate(entry.getStudentName(), 54),
                            truncate("@" + entry.getUsername(), 32),
                            entry.getTotalQuizzes(),
                            String.format("%.1f pts", entry.getHighScore()));
                } else {
                    line = String.format("%-6s  %-54s  %-32s  %-10d  %-12.1f  %-8s",
                            rankStr,
                            truncate(entry.getStudentName(), 54),
                            truncate("@" + entry.getUsername(), 32),
                            entry.getTotalQuizzes(),
                            entry.getTotalPoints(),
                            String.format("%.1f%%", entry.getAvgPercentage()));
                }

                if (i == selectedIndex) {
                    sb.append(cursor).append(TuiHelper.bold(line)).append("\n");
                } else {
                    sb.append(cursor).append(line).append("\n");
                }
                if (i < endRow - 1) {
                    sb.append("\n");
                }
            }
        }

        sb.append("\n  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

        if (!leaderboard.isEmpty()) {
            int pageSize = TuiHelper.PAGE_SIZE;
            int totalPages = Math.max(1, (int) Math.ceil((double) leaderboard.size() / pageSize));
            int currentPage = selectedIndex / pageSize;
            sb.append(TuiHelper.paginationBar(currentPage, totalPages, leaderboard.size()));
        }

        sb.append(TuiHelper.dim("  [↑/↓] Move  •  [←/→] Page  •  [Tab] Switch Tab  •  [r] Refresh  •  [Esc] Back\n"));
        return sb.toString();
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, Math.max(0, max - 3)) + "...";
    }
}