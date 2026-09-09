package com.proctor.view;

import com.proctor.model.entity.LeaderboardEntry;
import com.proctor.util.TuiHelper;

import java.util.List;

public class PortalViews {

    public static String renderGlobalLeaderboard(List<LeaderboardEntry> leaderboard, int selectedIndex) {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("GLOBAL LEADERBOARD", String.format("Top Performers (%d ranked)  •  Scroll with [↑/↓]", leaderboard.size())));
        sb.append("\n");

        sb.append(String.format("  %-6s  %-30s  %-20s  %-10s  %-12s  %-8s%n",
                "RANK", "STUDENT NAME", "USERNAME", "QUIZZES", "TOTAL PTS", "AVG %")).append("\n");
        sb.append("  " + "─".repeat(95) + "\n\n");

        if (leaderboard.isEmpty()) {
            sb.append("  ").append(TuiHelper.dim("No quizzes completed yet. Be the first on the leaderboard!")).append("\n");
        } else {
            int windowSize = 5;
            int startRow = Math.max(0, Math.min(selectedIndex - 2, leaderboard.size() - windowSize));
            int endRow = Math.min(leaderboard.size(), startRow + windowSize);

            if (startRow > 0) {
                sb.append(TuiHelper.dim(String.format("  ▲ %d more students above (Press ↑ to scroll)", startRow))).append("\n\n");
            }

            for (int i = startRow; i < endRow; i++) {
                LeaderboardEntry entry = leaderboard.get(i);
                String cursor = (i == selectedIndex) ? TuiHelper.cyan("▶ ") : "  ";

                String rankStr;
                if (entry.getRank() == 1) rankStr = "#1";
                else if (entry.getRank() == 2) rankStr = "#2";
                else if (entry.getRank() == 3) rankStr = "#3";
                else rankStr = String.format("%d", entry.getRank());

                String line = String.format("%-6s  %-30s  %-20s  %-10d  %-12.1f  %-8s",
                        rankStr,
                        truncate(entry.getStudentName(), 30),
                        truncate("@" + entry.getUsername(), 20),
                        entry.getTotalQuizzes(),
                        entry.getTotalPoints(),
                        String.format("%.1f%%", entry.getAvgPercentage()));

                if (i == selectedIndex) {
                    sb.append(cursor).append(TuiHelper.bold(line)).append("\n");
                } else {
                    sb.append(cursor).append(line).append("\n");
                }
                if (i < endRow - 1) {
                    sb.append("\n");
                }
            }

            if (endRow < leaderboard.size()) {
                sb.append("\n").append(TuiHelper.dim(String.format("  ▼ %d more students below (Press ↓ to scroll)", leaderboard.size() - endRow))).append("\n");
            }
        }

        sb.append("\n  " + "─".repeat(95) + "\n\n");
        sb.append(TuiHelper.dim("  [↑/↓] Move  •  [r] Refresh  •  [Esc] Back\n"));
        return sb.toString();
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}