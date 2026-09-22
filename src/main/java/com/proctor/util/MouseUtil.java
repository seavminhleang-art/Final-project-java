package com.proctor.util;

import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseAction;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseButton;
import com.williamcallahan.tui4j.compat.bubbletea.input.MouseMessage;

import java.util.List;

public class MouseUtil {

    public static boolean isLeftClick(Message msg) {
        if (msg instanceof MouseMessage m) {
            return m.getAction() == MouseAction.MouseActionPress && m.getButton() == MouseButton.MouseButtonLeft;
        }
        return false;
    }

    public static boolean isWheelUp(Message msg) {
        if (msg instanceof MouseMessage m) {
            return m.getButton() == MouseButton.MouseButtonWheelUp;
        }
        return false;
    }

    public static boolean isWheelDown(Message msg) {
        if (msg instanceof MouseMessage m) {
            return m.getButton() == MouseButton.MouseButtonWheelDown;
        }
        return false;
    }

    public static int getColumn(Message msg) {
        if (msg instanceof MouseMessage m) {
            return m.column();
        }
        return -1;
    }

    public static int getRow(Message msg) {
        if (msg instanceof MouseMessage m) {
            return m.row();
        }
        return -1;
    }

    public static int getLineIndex(Message msg) {
        int row = getRow(msg);
        return TuiHelper.getHitMap().getLineIndex(row);
    }

    public static int getColInLine(Message msg) {
        int col = getColumn(msg);
        int row = getRow(msg);
        return TuiHelper.getHitMap().getColInLine(col, row);
    }

    public static int getClickedButtonIndex(int colInLine, List<String> labels, int width) {
        if (labels == null || labels.isEmpty() || colInLine < 0) {
            return -1;
        }
        int maxLen = 0;
        for (String label : labels) {
            if (label != null && label.length() > maxLen) {
                maxLen = label.length();
            }
        }
        int btnWidth = Math.max(16, maxLen + 6);
        int curX = 0;
        for (int i = 0; i < labels.size(); i++) {
            if (colInLine >= curX && colInLine < curX + btnWidth) {
                return i;
            }
            curX += btnWidth + 4;
        }
        return -1;
    }

    public static int getClickedButtonIndex(int colInLine, int width, String... labels) {
        return getClickedButtonIndex(colInLine, List.of(labels), width);
    }

    public static int getClickedButtonIndex(int colInLine, List<String> labels) {
        return getClickedButtonIndex(colInLine, labels, 0);
    }

    public static int getClickedButtonIndex(int colInLine, String... labels) {
        return getClickedButtonIndex(colInLine, List.of(labels), 0);
    }

    public static boolean isInside(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }

    public static int findButtonRowLine(String view) {
        if (view == null) return -1;
        String[] lines = view.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains(TuiHelper.CENTER_MARKER) && lines[i].contains("┌")) {
                return i;
            }
        }
        return -1;
    }

    public static int findTabBarLine(String view) {
        if (view == null) return -1;
        String[] lines = view.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("Tabs:") || lines[i].contains("TABS:")) {
                return i;
            }
        }
        return -1;
    }

    public static int getClickedTabIndex(int colInLine, String... tabs) {
        if (tabs == null || tabs.length == 0 || colInLine < 0) return -1;
        int curX = 9;
        for (int i = 0; i < tabs.length; i++) {
            int tabW = 6 + tabs[i].length();
            if (colInLine >= curX && colInLine < curX + tabW) {
                return i;
            }
            curX += tabW + 3;
        }
        return -1;
    }

    public static int findTableStartLine(String view) {
        if (view == null) return -1;
        String[] lines = view.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("────") && !lines[i].contains("┌") && !lines[i].contains("└")) {
                return i + 2;
            }
        }
        return -1;
    }

    public static int findPaginationLine(String view) {
        if (view == null) return -1;
        String[] lines = view.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("Page ") && (lines[i].contains("Prev") || lines[i].contains("Next"))) {
                return i;
            }
        }
        return -1;
    }

    public static int getClickedPaginationAction(int colInLine, int currentPage, int totalPages) {
        if (colInLine < 0) return 0;
        if (colInLine < 20 && currentPage > 0) {
            return -1;
        }
        if (colInLine >= 35 && currentPage < totalPages - 1) {
            return 1;
        }
        return 0;
    }

    public static int findMenuItemIndex(String view, int lineIndex) {
        if (view == null || lineIndex < 0) return -1;
        String[] lines = view.split("\n", -1);
        int itemIdx = 0;
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i];
            if (l.contains(TuiHelper.BUTTON_MARKER) && !l.contains("Tabs:") && !l.contains("Page ")) {
                if (i == lineIndex || i + 1 == lineIndex) {
                    return itemIdx;
                }
                itemIdx++;
            }
        }
        return -1;
    }

    public static int findOptionIndex(String view, int lineIndex) {
        if (view == null || lineIndex < 0) return -1;
        String[] lines = view.split("\n", -1);
        int optIdx = 0;
        for (int i = 0; i < lines.length; i++) {
            String l = lines[i];
            if (l.contains("(•) ") || l.contains("( ) ") || l.contains("[1] ") || l.contains("[2] ") || l.contains("[3] ") || l.contains("[4] ")) {
                if (i == lineIndex || i + 1 == lineIndex) {
                    return optIdx;
                }
                optIdx++;
            }
        }
        return -1;
    }

    public static String getClickedHintAction(String view, int lineIndex, int colInLine) {
        if (view == null || lineIndex < 0 || colInLine < 0) return null;
        String[] lines = view.split("\n", -1);
        if (lineIndex >= lines.length) return null;
        String rawLine = lines[lineIndex];
        String stripped = rawLine.replace(TuiHelper.CLEAR_EOL, "")
                                 .replace(TuiHelper.BUTTON_MARKER, "")
                                 .replaceAll("\u001B\\[[;?0-9]*[a-zA-Z]", "");
        String trimmedClean = stripped.trim();
        if (!TuiHelper.isHintRow(stripped) && !trimmedClean.startsWith("[") && !trimmedClean.contains("to continue") && !trimmedClean.contains("to view scorecard")) {
            return null;
        }

        if ((trimmedClean.startsWith("[Enter / Esc]") || trimmedClean.startsWith("[Enter/Esc]")) && !stripped.contains("•")) {
            return "Esc";
        }
        if ((trimmedClean.startsWith("[Esc] Back") || trimmedClean.startsWith("[Esc] Cancel") || trimmedClean.startsWith("[Esc] Quit") || trimmedClean.startsWith("[Esc] Forfeit")) && !stripped.contains("•")) {
            return "Esc";
        }

        String[] parts = stripped.split("  •  | • ");
        int searchStart = 0;
        for (String part : parts) {
            String pTrimmed = part.trim();
            if (pTrimmed.isEmpty()) continue;
            int idx = stripped.indexOf(part, searchStart);
            if (idx == -1) idx = stripped.indexOf(pTrimmed, searchStart);
            int startCol = idx != -1 ? idx : 0;
            int endCol = startCol + (idx != -1 ? part.length() : pTrimmed.length());
            searchStart = endCol;

            if (colInLine >= startCol - 1 && colInLine <= endCol + 2) {
                if (pTrimmed.contains("[Esc]") || pTrimmed.contains("[Enter / Esc]") || pTrimmed.contains("[Enter/Esc]") || pTrimmed.contains("Back") || pTrimmed.contains("Cancel") || pTrimmed.contains("Forfeit") || pTrimmed.contains("Quit")) {
                    return "Esc";
                }
                if (pTrimmed.contains("[n]")) return "n";
                if (pTrimmed.contains("[b]")) return "b";
                if (pTrimmed.contains("[g]")) return "g";
                if (pTrimmed.contains("[c]")) return "c";
                if (pTrimmed.contains("[d]")) return "d";
                if (pTrimmed.contains("[e]") || pTrimmed.contains("[e/Enter]") || pTrimmed.contains("[Enter/e]")) return "e";
                if (pTrimmed.contains("[s]")) return "s";
                if (pTrimmed.contains("[r]")) return "r";
                if (pTrimmed.contains("[m]")) return "m";
                if (pTrimmed.contains("[v]")) return "v";
                if (pTrimmed.contains("[Space]")) return "Space";
                if (pTrimmed.contains("[Enter]")) return "Enter";
                if (pTrimmed.contains("[Tab]")) return "Tab";
                if (pTrimmed.contains("[/]")) return "/";
            }
        }
        return null;
    }
}
