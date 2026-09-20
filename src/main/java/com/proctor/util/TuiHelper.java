package com.proctor.util;

import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;

import java.util.List;

public class TuiHelper {
    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String DIM = "\u001B[38;2;100;116;139m";
    public static final String RED = "\u001B[38;2;185;28;28m";
    public static final String GREEN = "\u001B[38;2;21;128;61m";
    public static final String YELLOW = "\u001B[38;2;180;83;9m";
    public static final String NAVY_BLUE = "\u001B[38;2;30;58;138m";
    public static final String CYAN = NAVY_BLUE;
    public static final String CLEAR_EOL = "\u001B[K";
    public static final String HEADER_START = "\u001B[?9901h";
    public static final String HEADER_END = "\u001B[?9901l";
    public static final int PAGE_SIZE = 10;
    public static final int TABLE_WIDTH = 132;
    public static final String DIALOG_MARKER = "\u001B[?9904h";
    public static final int DIALOG_INNER_WIDTH = 64;
    public static final int DIALOG_MIN_BODY_ROWS = 8;
    public static final int MIN_BODY_ROWS = 16;
    public static final int MIN_INNER_WIDTH = 76;
    public static final int MAX_INNER_WIDTH = TABLE_WIDTH + 2;

    public static String tableSeparator() {
        return "  " + "─".repeat(TABLE_WIDTH) + "\n\n";
    }

    private static Integer cachedTermWidth = null;
    private static Integer cachedTermHeight = null;

    public static void setTerminalDimensions(int width, int height) {
        if (width > 30) cachedTermWidth = width;
        if (height > 10) cachedTermHeight = height;
    }

    public static String bold(String text) {
        return BOLD + text + RESET;
    }

    public static String navyBlue(String text) {
        return NAVY_BLUE + text + RESET;
    }

    public static String cyan(String text) {
        return navyBlue(text);
    }

    public static String green(String text) {
        return GREEN + text + RESET;
    }

    public static String red(String text) {
        return RED + text + RESET;
    }

    public static String yellow(String text) {
        return YELLOW + text + RESET;
    }

    public static String dim(String text) {
        return DIM + text + RESET;
    }

    private static final String[] ASCII_PROCTOR = new String[]{
        "██████╗ ██████╗  ██████╗  ██████╗████████╗ ██████╗ ██████╗ ",
        "██╔══██╗██╔══██╗██╔═══██╗██╔════╝╚══██╔══╝██╔═══██╗██╔══██╗",
        "██████╔╝██████╔╝██║   ██║██║        ██║   ██║   ██║██████╔╝",
        "██╔═══╝ ██╔══██╗██║   ██║██║        ██║   ██║   ██║██╔══██╗",
        "██║     ██║  ██║╚██████╔╝╚██████╗   ██║   ╚██████╔╝██║  ██║",
        "╚═╝     ╚═╝  ╚═╝ ╚═════╝  ╚═════╝   ╚═╝    ╚═════╝ ╚═╝  ╚═╝"
    };

    private static final String[] ASCII_QUIZZES = new String[]{
        " ██████╗ ██╗   ██╗██╗███████╗███████╗███████╗███████╗",
        "██╔═══██╗██║   ██║██║╚══███╔╝╚══███╔╝██╔════╝██╔════╝",
        "██║   ██║██║   ██║██║  ███╔╝   ███╔╝ █████╗  ███████╗",
        "██║▄▄ ██║██║   ██║██║ ███╔╝   ███╔╝  ██╔══╝  ╚════██║",
        "╚██████╔╝╚██████╔╝██║███████╗███████╗███████╗███████║",
        " ╚══▀▀═╝  ╚═════╝ ╚═╝╚══════╝╚══════╝╚══════╝╚══════╝"
    };

    private static final String[] ASCII_EXAMS = new String[]{
        "███████╗██╗  ██╗ █████╗ ███╗   ███╗███████╗",
        "██╔════╝╚██╗██╔╝██╔══██╗████╗ ████║██╔════╝",
        "█████╗   ╚███╔╝ ███████║██╔████╔██║███████╗",
        "██╔══╝   ██╔██╗ ██╔══██║██║╚██╔╝██║╚════██║",
        "███████╗██╔╝ ██╗██║  ██║██║ ╚═╝ ██║███████║",
        "╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝╚═╝     ╚═╝╚══════╝"
    };

    private static final String[] ASCII_INBOX = new String[]{
        "██╗███╗   ██╗██████╗  ██████╗ ██╗  ██╗",
        "██║████╗  ██║██╔══██╗██╔═══██╗╚██╗██╔╝",
        "██║██╔██╗ ██║██████╔╝██║   ██║ ╚███╔╝ ",
        "██║██║╚██╗██║██╔══██╗██║   ██║ ██╔██╗ ",
        "██║██║ ╚████║██████╔╝╚██████╔╝██╔╝ ██╗",
        "╚═╝╚═╝  ╚═══╝╚═════╝  ╚═════╝ ╚═╝  ╚═╝"
    };

    private static final String[] ASCII_DASHBOARD = new String[]{
        "██████╗  █████╗ ███████╗██╗  ██╗██████╗  ██████╗  █████╗ ██████╗ ██████╗ ",
        "██╔══██╗██╔══██╗██╔════╝██║  ██║██╔══██╗██╔═══██╗██╔══██╗██╔══██╗██╔══██╗",
        "██║  ██║███████║███████╗███████║██████╔╝██║   ██║███████║██████╔╝██║  ██║",
        "██║  ██║██╔══██║╚════██║██╔══██║██╔══██╗██║   ██║██╔══██║██╔══██╗██║  ██║",
        "██████╔╝██║  ██║███████║██║  ██║██████╔╝╚██████╔╝██║  ██║██║  ██║██████╔╝",
        "╚═════╝ ╚═╝  ╚═╝╚══════╝╚═╝  ╚═╝╚═════╝  ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═════╝ "
    };

    private static final String[] ASCII_USERS = new String[]{
        "██╗   ██╗███████╗███████╗██████╗ ███████╗",
        "██║   ██║██╔════╝██╔════╝██╔══██╗██╔════╝",
        "██║   ██║███████╗█████╗  ██████╔╝███████╗",
        "██║   ██║╚════██║██╔══╝  ██╔══██╗╚════██║",
        "╚██████╔╝███████║███████╗██║  ██║███████║",
        " ╚═════╝ ╚══════╝╚══════╝╚═╝  ╚═╝╚══════╝"
    };

    private static final String[] ASCII_REPORTS = new String[]{
        "██████╗ ███████╗██████╗  ██████╗ ██████╗ ████████╗███████╗",
        "██╔══██╗██╔════╝██╔══██╗██╔═══██╗██╔══██╗╚══██╔══╝██╔════╝",
        "██████╔╝█████╗  ██████╔╝██║   ██║██████╔╝   ██║   ███████╗",
        "██╔══██╗██╔══╝  ██╔═══╝ ██║   ██║██╔══██╗   ██║   ╚════██║",
        "██║  ██║███████╗██║     ╚██████╔╝██║  ██║   ██║   ███████║",
        "╚═╝  ╚═╝╚══════╝╚═╝      ╚═════╝ ╚═╝  ╚═╝   ╚═╝   ╚══════╝"
    };

    private static final String[] ASCII_LEADERBOARD = new String[]{
        "██╗     ███████╗ █████╗ ██████╗ ███████╗██████╗ ██████╗  ██████╗  █████╗ ██████╗ ██████╗ ",
        "██║     ██╔════╝██╔══██╗██╔══██╗██╔════╝██╔══██╗██╔══██╗██╔═══██╗██╔══██╗██╔══██╗██╔══██╗",
        "██║     █████╗  ███████║██║  ██║█████╗  ██████╔╝██████╔╝██║   ██║███████║██████╔╝██║  ██║",
        "██║     ██╔══╝  ██╔══██║██║  ██║██╔══╝  ██╔══██╗██╔══██╗██║   ██║██╔══██║██╔══██╗██║  ██║",
        "███████╗███████╗██║  ██║██████╔╝███████╗██║  ██║██████╔╝╚██████╔╝██║  ██║██║  ██║██████╔╝",
        "╚══════╝╚══════╝╚═╝  ╚═╝╚═════╝ ╚══════╝╚═╝  ╚═╝╚═════╝  ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═════╝ "
    };

    private static final String[] ASCII_SUBMISSIONS = new String[]{
        "███████╗██╗   ██╗██████╗ ███╗   ███╗██╗███████╗███████╗██╗ ██████╗ ███╗   ██╗███████╗",
        "██╔════╝██║   ██║██╔══██╗████╗ ████║██║██╔════╝██╔════╝██║██╔═══██╗████╗  ██║██╔════╝",
        "███████╗██║   ██║██████╔╝██╔████╔██║██║███████╗███████╗██║██║   ██║██╔██╗ ██║███████╗",
        "╚════██║██║   ██║██╔══██╗██║╚██╔╝██║██║╚════██║╚════██║██║██║   ██║██║╚██╗██║╚════██║",
        "███████║╚██████╔╝██████╔╝██║ ╚═╝ ██║██║███████║███████║██║╚██████╔╝██║ ╚████║███████║",
        "╚══════╝ ╚═════╝ ╚═════╝ ╚═╝     ╚═╝╚═╝╚══════╝╚══════╝╚═╝ ╚═════╝ ╚═╝  ╚═══╝╚══════╝"
    };

    private static final String[] ASCII_QUESTIONS = new String[]{
        " ██████╗ ██╗   ██╗███████╗███████╗████████╗██╗ ██████╗ ███╗   ██╗███████╗",
        "██╔═══██╗██║   ██║██╔════╝██╔════╝╚══██╔══╝██║██╔═══██╗████╗  ██║██╔════╝",
        "██║   ██║██║   ██║█████╗  ███████╗   ██║   ██║██║   ██║██╔██╗ ██║███████╗",
        "██║▄▄ ██║██║   ██║██╔══╝  ╚════██║   ██║   ██║██║   ██║██║╚██╗██║╚════██║",
        "╚██████╔╝╚██████╔╝███████╗███████║   ██║   ██║╚██████╔╝██║ ╚████║███████║",
        " ╚══▀▀═╝  ╚═════╝ ╚══════╝╚══════╝   ╚═╝   ╚═╝ ╚═════╝ ╚═╝  ╚═══╝╚══════╝"
    };

    private static final String[] ASCII_SUBJECTS = new String[]{
        "███████╗██╗   ██╗██████╗      ██╗███████╗ ██████╗████████╗███████╗",
        "██╔════╝██║   ██║██╔══██╗     ██║██╔════╝██╔════╝╚══██╔══╝██╔════╝",
        "███████╗██║   ██║██████╔╝     ██║█████╗  ██║        ██║   ███████╗",
        "╚════██║██║   ██║██╔══██╗██   ██║██╔══╝  ██║        ██║   ╚════██║",
        "███████║╚██████╔╝██████╔╝╚█████╔╝███████╗╚██████╗   ██║   ███████║",
        "╚══════╝ ╚═════╝ ╚═════╝  ╚════╝ ╚══════╝ ╚═════╝   ╚═╝   ╚══════╝"
    };

    private static final String[] ASCII_HISTORY = new String[]{
        "██╗  ██╗██╗███████╗████████╗ ██████╗ ██████╗ ██╗   ██╗",
        "██║  ██║██║██╔════╝╚══██╔══╝██╔═══██╗██╔══██╗╚██╗ ██╔╝",
        "███████║██║███████╗   ██║   ██║   ██║██████╔╝ ╚████╔╝ ",
        "██╔══██║██║╚════██║   ██║   ██║   ██║██╔══██╗  ╚██╔╝  ",
        "██║  ██║██║███████║   ██║   ╚██████╔╝██║  ██║   ██║   ",
        "╚═╝  ╚═╝╚═╝╚══════╝   ╚═╝    ╚═════╝ ╚═╝  ╚═╝   ╚═╝   "
    };

    private static final String[] ASCII_LOGIN = new String[]{
        "██╗      ██████╗  ██████╗     ██╗███╗   ██╗",
        "██║     ██╔═══██╗██╔════╝     ██║████╗  ██║",
        "██║     ██║   ██║██║  ███╗    ██║██╔██╗ ██║",
        "██║     ██║   ██║██║   ██║    ██║██║╚██╗██║",
        "███████╗╚██████╔╝╚██████╔╝    ██║██║ ╚████║",
        "╚══════╝ ╚═════╝  ╚═════╝     ╚═╝╚═╝  ╚═══╝"
    };

    private static final String[] ASCII_REGISTER = new String[]{
        "██████╗ ███████╗ ██████╗ ██╗███████╗████████╗███████╗██████╗ ",
        "██╔══██╗██╔════╝██╔════╝ ██║██╔════╝╚══██╔══╝██╔════╝██╔══██╗",
        "██████╔╝█████╗  ██║  ███╗██║███████╗   ██║   █████╗  ██████╔╝",
        "██╔══██╗██╔══╝  ██║   ██║██║╚════██║   ██║   ██╔══╝  ██╔══██╗",
        "██║  ██║███████╗╚██████╔╝██║███████║   ██║   ███████╗██║  ██║",
        "╚═╝  ╚═╝╚══════╝ ╚═════╝ ╚═╝╚══════╝   ╚═╝   ╚══════╝╚═╝  ╚═╝"
    };

    private static final String[] ASCII_SPEED = new String[]{
        "███████╗██████╗ ███████╗███████╗██████╗ ",
        "██╔════╝██╔══██╗██╔════╝██╔════╝██╔══██╗",
        "███████╗██████╔╝█████╗  █████╗  ██║  ██║",
        "╚════██║██╔═══╝ ██╔══╝  ██╔══╝  ██║  ██║",
        "███████║██║     ███████╗███████╗██████╔╝",
        "╚══════╝╚═╝     ╚══════╝╚══════╝╚═════╝ "
    };

    private static final String[] ASCII_SECURITY = new String[]{
        "███████╗███████╗ ██████╗██╗   ██╗██████╗ ██╗████████╗██╗   ██╗",
        "██╔════╝██╔════╝██╔════╝██║   ██║██╔══██╗██║╚══██╔══╝╚██╗ ██╔╝",
        "███████╗█████╗  ██║     ██║   ██║██████╔╝██║   ██║    ╚████╔╝ ",
        "╚════██║██╔══╝  ██║     ██║   ██║██╔══██╗██║   ██║     ╚██╔╝  ",
        "███████╗███████╗╚██████╗╚██████╔╝██║  ██║██║   ██║      ██║   ",
        "╚══════╝╚══════╝ ╚═════╝ ╚═════╝ ╚═╝  ╚═╝╚═╝   ╚═╝      ╚═╝   "
    };

    private static final String[] ASCII_GOODBYE = new String[]{
        " ██████╗  ██████╗  ██████╗ ██████╗ ██████╗ ██╗   ██╗███████╗",
        "██╔════╝ ██╔═══██╗██╔═══██╗██╔══██╗██╔══██╗╚██╗ ██╔╝██╔════╝",
        "██║  ███╗██║   ██║██║   ██║██║  ██║██████╔╝ ╚████╔╝ █████╗  ",
        "██║   ██║██║   ██║██║   ██║██║  ██║██╔══██╗  ╚██╔╝  ██╔══╝  ",
        "╚██████╔╝╚██████╔╝╚██████╔╝██████╔╝██████╔╝   ██║   ███████╗",
        " ╚═════╝  ╚═════╝  ╚═════╝ ╚═════╝ ╚═════╝    ╚═╝   ╚══════╝"
    };

    public static final String BOX_TITLE_MARKER = "\u001B[8888m";
    public static final String CENTER_MARKER = "\u001B[8889m";

    public static String centerText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return CENTER_MARKER + text;
    }

    public static String boxTitle(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }
        String t = title.trim();
        if (t.length() > 126) {
            t = t.substring(0, 123) + "...";
        }
        return BOX_TITLE_MARKER + bold(t) + RESET;
    }

    public static String boxTitle(String title, String subtitle) {
        if (title == null || title.isBlank()) {
            return boxTitle(subtitle);
        }
        if (subtitle == null || subtitle.isBlank()) {
            return boxTitle(title);
        }
        String t = title.trim();
        String s = subtitle.trim();
        int maxTotal = 126;
        int sepLen = 5;
        int tLen = stripAnsi(t).length();
        int sLen = stripAnsi(s).length();
        if (tLen > 60) {
            t = t.substring(0, 57) + "...";
            tLen = 60;
        }
        if (tLen + sepLen + sLen > maxTotal) {
            int maxSub = Math.max(10, maxTotal - sepLen - tLen);
            if (sLen > maxSub) {
                s = s.substring(0, Math.max(0, maxSub - 3)) + "...";
            }
        }
        return BOX_TITLE_MARKER + bold(t) + dim("  •  " + s) + RESET;
    }

    public static String asciiBannerBox(String[] asciiLines) {
        StringBuilder sb = new StringBuilder();
        sb.append(HEADER_START).append(CLEAR_EOL).append("\n");
        for (String line : asciiLines) {
            sb.append(NAVY_BLUE).append(line).append(RESET).append(CLEAR_EOL).append("\n");
        }
        sb.append(HEADER_END).append(CLEAR_EOL).append("\n");
        return sb.toString();
    }

    public static String header(String title) {
        return header(title, null);
    }

    public static String header(String title, String subtitle) {
        String upper = (title != null) ? title.toUpperCase() : "";

        if (upper.equals("PROCTOR")) {
            return asciiBannerBox(ASCII_PROCTOR);
        }
        if (upper.equals("LOG IN") || upper.equals("LOGIN") || upper.equals("SIGN IN")) {
            return asciiBannerBox(ASCII_LOGIN);
        }
        if (upper.equals("REGISTER") || upper.contains("SIGN UP") || upper.contains("CREATE ACCOUNT")) {
            return asciiBannerBox(ASCII_REGISTER);
        }
        if (upper.contains("DASHBOARD") || upper.contains("PORTAL")) {
            return asciiBannerBox(ASCII_DASHBOARD);
        }
        if (upper.contains("SECURITY") || upper.contains("PASSWORD")) {
            return asciiBannerBox(ASCII_SECURITY);
        }
        if (upper.contains("HISTORY")) {
            return asciiBannerBox(ASCII_HISTORY);
        }
        if (upper.contains("SPEED")) {
            return asciiBannerBox(ASCII_SPEED);
        }
        if (upper.contains("EXAM")) {
            return asciiBannerBox(ASCII_EXAMS);
        }
        if (upper.contains("QUIZ")) {
            return asciiBannerBox(ASCII_QUIZZES);
        }
        if (upper.contains("INBOX") || upper.contains("NOTIFICATION") || upper.contains("MESSAGE")) {
            return asciiBannerBox(ASCII_INBOX);
        }
        if (upper.contains("USER")) {
            return asciiBannerBox(ASCII_USERS);
        }
        if (upper.contains("REPORT")) {
            return asciiBannerBox(ASCII_REPORTS);
        }
        if (upper.contains("LEADERBOARD")) {
            return asciiBannerBox(ASCII_LEADERBOARD);
        }
        if (upper.contains("SUBMISSION") || upper.contains("ANSWER SHEET") || upper.contains("ATTEMPT")) {
            return asciiBannerBox(ASCII_SUBMISSIONS);
        }
        if (upper.contains("QUESTION")) {
            return asciiBannerBox(ASCII_QUESTIONS);
        }
        if (upper.contains("SUBJECT") || upper.contains("TEACHER ASSIGNMENT")) {
            return asciiBannerBox(ASCII_SUBJECTS);
        }
        if (upper.contains("QUIT") || upper.contains("GOODBYE") || upper.contains("EXIT")) {
            return asciiBannerBox(ASCII_GOODBYE);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(HEADER_START).append(CLEAR_EOL).append("\n");
        sb.append(bold(NAVY_BLUE + title.trim())).append(RESET).append(CLEAR_EOL).append("\n");
        sb.append(HEADER_END).append(CLEAR_EOL).append("\n");
        return sb.toString();
    }

    public static String banner(String text) {
        return bold(navyBlue(padCenter(text, 54))) + CLEAR_EOL + "\n";
    }

    public static String paginationBar(int currentPage, int totalPages, int totalItems) {
        if (totalItems <= 0) {
            return "";
        }
        if (totalPages <= 1) {
            return dim(String.format("  Page 1 of 1  •  %d %s", totalItems, totalItems == 1 ? "item" : "items")) + "\n\n";
        }
        String prevLabel = (currentPage > 0) ? cyan("◀ [←] Prev") : dim("  [←] Prev");
        String nextLabel = (currentPage < totalPages - 1) ? cyan("[→] Next ▶") : dim("[→] Next  ");
        String pageInfo = bold(String.format("Page %d of %d", currentPage + 1, totalPages));
        String countInfo = dim(String.format("(%d %s)", totalItems, totalItems == 1 ? "item" : "items"));

        return String.format("  %s   %s  %s   %s%n%n", prevLabel, pageInfo, countInfo, nextLabel);
    }

    public static String tabBar(String[] tabs, int activeIndex) {
        if (tabs == null || tabs.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder("  Tabs:  ");
        for (int i = 0; i < tabs.length; i++) {
            if (i == activeIndex) {
                sb.append(cyan(bold("[ ● " + tabs[i] + " ]")));
            } else {
                sb.append(dim("[   " + tabs[i] + " ]"));
            }
            if (i < tabs.length - 1) {
                sb.append("   ");
            }
        }
        return sb.toString();
    }

    public static String birthdayMask(String digits, boolean focused) {
        char[] tpl = "DD - MM - YYYY".toCharArray();
        int[] slots = {0, 1, 5, 6, 10, 11, 12, 13};
        int len = (digits == null) ? 0 : Math.min(digits.length(), 8);
        for (int i = 0; i < len; i++) {
            tpl[slots[i]] = digits.charAt(i);
        }
        if (focused && len < 8) {
            tpl[slots[len]] = '_';
        }
        return new String(tpl);
    }

    public static String inputBox(String label, String value, boolean focused, int width, boolean masked, String placeholder) {
        StringBuilder sb = new StringBuilder();
        String borderCol = focused ? NAVY_BLUE : DIM;
        String labelCol = focused ? bold(NAVY_BLUE + "  " + label) : dim("  " + label);

        int maxInner = Math.max(10, width - 4);
        String displayVal;
        if (value == null || value.isEmpty()) {
            if (focused) {
                displayVal = "_" + " ".repeat(maxInner - 1);
            } else {
                String ph = (placeholder != null) ? placeholder : "";
                if (ph.length() > maxInner) {
                    ph = ph.substring(0, Math.max(0, maxInner - 3)) + "...";
                }
                displayVal = dim(ph) + " ".repeat(Math.max(0, maxInner - ph.length()));
            }
        } else {
            String txt = masked ? "*".repeat(value.length()) : value;
            if (focused) {
                if (txt.length() < maxInner) {
                    displayVal = txt + "_" + " ".repeat(maxInner - txt.length() - 1);
                } else {
                    displayVal = "..." + txt.substring(txt.length() - Math.max(0, maxInner - 4)) + "_";
                }
            } else {
                if (txt.length() <= maxInner) {
                    displayVal = txt + " ".repeat(maxInner - txt.length());
                } else {
                    displayVal = txt.substring(0, Math.max(0, maxInner - 3)) + "...";
                }
            }
        }

        sb.append("  ").append(labelCol).append(CLEAR_EOL).append("\n");
        sb.append("  ").append(borderCol).append("┌").append("─".repeat(width - 2)).append("┐").append(RESET).append(CLEAR_EOL).append("\n");

        String lineContent = " " + displayVal + " ";

        sb.append("  ").append(borderCol).append("│").append(RESET).append(lineContent).append(borderCol).append("│").append(RESET).append(CLEAR_EOL).append("\n");
        sb.append("  ").append(borderCol).append("└").append("─".repeat(width - 2)).append("┘").append(RESET).append(CLEAR_EOL).append("\n");
        return sb.toString();
    }

    public static String selectBox(String label, String value, boolean focused, int width, String helpText) {
        StringBuilder sb = new StringBuilder();
        String borderCol = focused ? NAVY_BLUE : DIM;
        String labelCol = focused ? bold(NAVY_BLUE + "  " + label) : dim("  " + label);

        String valDisplay = focused ? navyBlue("< " + value + " >") + (helpText != null ? dim(" (" + helpText + ")") : "") : value;
        String rawLenText = focused ? ("< " + value + " >" + (helpText != null ? " (" + helpText + ")" : "")) : (value != null ? value : "");
        int padLen = Math.max(0, (width - 4) - rawLenText.length());

        sb.append("  ").append(labelCol).append(CLEAR_EOL).append("\n");
        sb.append("  ").append(borderCol).append("┌").append("─".repeat(width - 2)).append("┐").append(RESET).append(CLEAR_EOL).append("\n");
        sb.append("  ").append(borderCol).append("│").append(RESET).append(" ").append(valDisplay).append(" ".repeat(padLen)).append(" ").append(borderCol).append("│").append(RESET).append(CLEAR_EOL).append("\n");
        sb.append("  ").append(borderCol).append("└").append("─".repeat(width - 2)).append("┘").append(RESET).append(CLEAR_EOL).append("\n");
        return sb.toString();
    }

    public static String[] boxButtonLines(String label, boolean focused, String focusColor, int width) {
        String text = (label != null) ? label.trim() : "";
        int innerW = Math.max(text.length() + 4, width - 2);
        int pad = innerW - text.length();
        int padLeft = pad / 2;
        int padRight = pad - padLeft;

        String col = (focusColor != null) ? focusColor : NAVY_BLUE;
        String top;
        String mid;
        String bot;

        if (focused) {
            top = col + "┌" + "─".repeat(innerW) + "┐" + RESET;
            mid = col + "│" + RESET + " ".repeat(padLeft) + bold(col + text) + " ".repeat(padRight) + col + "│" + RESET;
            bot = col + "└" + "─".repeat(innerW) + "┘" + RESET;
        } else {
            top = dim("┌" + "─".repeat(innerW) + "┐");
            mid = dim("│") + " ".repeat(padLeft) + dim(text) + " ".repeat(padRight) + dim("│");
            bot = dim("└" + "─".repeat(innerW) + "┘");
        }
        return new String[]{top, mid, bot};
    }

    public static String boxButton(String label, boolean focused, String focusColor, int width) {
        String[] lines = boxButtonLines(label, focused, focusColor, width);
        return CENTER_MARKER + lines[0] + "\n" + CENTER_MARKER + lines[1] + "\n" + CENTER_MARKER + lines[2];
    }

    public static String buttonRow(String primaryLabel, boolean primaryFocused, String secondaryLabel, boolean secondaryFocused) {
        return buttonRow(primaryLabel, primaryFocused, secondaryLabel, secondaryFocused, 106);
    }

    public static String buttonRow(String primaryLabel, boolean primaryFocused, String secondaryLabel, boolean secondaryFocused, int width) {
        int btnWidth = Math.max(16, Math.max(primaryLabel.length(), secondaryLabel.length()) + 6);
        String[] b1 = boxButtonLines(primaryLabel, primaryFocused, NAVY_BLUE, btnWidth);
        String[] b2 = boxButtonLines(secondaryLabel, secondaryFocused, RED, btnWidth);

        int rowWidth = visibleLength(b1[0]) + 4 + visibleLength(b2[0]);
        int leftPad = Math.max(0, (width - rowWidth) / 2);
        String padStr = " ".repeat(leftPad);

        return CENTER_MARKER + padStr + b1[0] + "    " + b2[0] + CLEAR_EOL + "\n"
             + CENTER_MARKER + padStr + b1[1] + "    " + b2[1] + CLEAR_EOL + "\n"
             + CENTER_MARKER + padStr + b1[2] + "    " + b2[2] + CLEAR_EOL;
    }

    public static String buttonRow(String primaryLabel, boolean primaryFocused,
                                   String dangerLabel, boolean dangerFocused,
                                   String secondaryLabel, boolean secondaryFocused) {
        return buttonRow(primaryLabel, primaryFocused, dangerLabel, dangerFocused, secondaryLabel, secondaryFocused, 106);
    }

    public static String buttonRow(String primaryLabel, boolean primaryFocused,
                                   String dangerLabel, boolean dangerFocused,
                                   String secondaryLabel, boolean secondaryFocused, int width) {
        return buttonRow(primaryLabel, primaryFocused, NAVY_BLUE,
                         dangerLabel, dangerFocused, RED,
                         secondaryLabel, secondaryFocused, YELLOW, width);
    }

    public static String buttonRow(String label1, boolean focused1, String color1,
                                   String label2, boolean focused2, String color2,
                                   String label3, boolean focused3, String color3, int width) {
        int btnWidth = Math.max(16, Math.max(label1.length(), Math.max(label2.length(), label3.length())) + 6);
        String[] b1 = boxButtonLines(label1, focused1, color1, btnWidth);
        String[] b2 = boxButtonLines(label2, focused2, color2, btnWidth);
        String[] b3 = boxButtonLines(label3, focused3, color3, btnWidth);

        int rowWidth = visibleLength(b1[0]) + 4 + visibleLength(b2[0]) + 4 + visibleLength(b3[0]);
        int leftPad = Math.max(0, (width - rowWidth) / 2);
        String padStr = " ".repeat(leftPad);

        return CENTER_MARKER + padStr + b1[0] + "    " + b2[0] + "    " + b3[0] + CLEAR_EOL + "\n"
             + CENTER_MARKER + padStr + b1[1] + "    " + b2[1] + "    " + b3[1] + CLEAR_EOL + "\n"
             + CENTER_MARKER + padStr + b1[2] + "    " + b2[2] + "    " + b3[2] + CLEAR_EOL;
    }

    public static String confirmationModal(String title, String message, String warningDetail, String confirmLabel, String cancelLabel, boolean confirmFocused) {
        StringBuilder sb = new StringBuilder();
        sb.append(DIALOG_MARKER);
        sb.append(header("CONFIRM ACTION"));
        sb.append("\n");
        sb.append(boxTitle(title)).append("\n\n");
        sb.append(bold(centerText(message))).append(CLEAR_EOL).append("\n\n");
        if (warningDetail != null && !warningDetail.isBlank()) {
            sb.append(dim(centerText(warningDetail))).append(CLEAR_EOL).append("\n\n");
        }
        sb.append(dim(centerText("─".repeat(56)))).append(CLEAR_EOL).append("\n\n");

        int btnWidth = Math.max(16, Math.max(confirmLabel.length(), cancelLabel.length()) + 6);
        String[] b1 = boxButtonLines(confirmLabel, confirmFocused, NAVY_BLUE, btnWidth);
        String[] b2 = boxButtonLines(cancelLabel, !confirmFocused, RED, btnWidth);

        sb.append(CENTER_MARKER).append(b1[0]).append("    ").append(b2[0]).append(CLEAR_EOL).append("\n");
        sb.append(CENTER_MARKER).append(b1[1]).append("    ").append(b2[1]).append(CLEAR_EOL).append("\n");
        sb.append(CENTER_MARKER).append(b1[2]).append("    ").append(b2[2]).append(CLEAR_EOL).append("\n\n");
        sb.append(dim("[←/→] Select Option  •  [Enter] Confirm  •  [Esc] Cancel")).append(CLEAR_EOL).append("\n");
        return sb.toString();
    }

    public static String quitConfirmationModal(boolean quitConfirmFocused) {
        return quitConfirmationModal(Session.getCurrentUser().orElse(null), quitConfirmFocused);
    }

    public static String quitConfirmationModal(User currentUser, boolean quitConfirmFocused) {
        StringBuilder sb = new StringBuilder();
        sb.append(DIALOG_MARKER);
        sb.append(header("GOODBYE"));
        sb.append("\n");

        String subtitle = (currentUser != null)
                ? "Logged in as " + currentUser.getDisplayNameWithHonorific() + " (" + currentUser.getRole() + ")"
                : "Main Menu";
        sb.append(boxTitle("Quit Proctor", subtitle)).append("\n\n");

        sb.append(bold(centerText("Are you sure you want to exit Proctor?"))).append(CLEAR_EOL).append("\n\n");
        sb.append(dim(centerText("All session progress and data have been safely saved."))).append(CLEAR_EOL).append("\n\n");
        sb.append(dim(centerText("─".repeat(56)))).append(CLEAR_EOL).append("\n\n");

        int btnWidth = 22;
        String[] b1 = boxButtonLines("Quit Application", quitConfirmFocused, RED, btnWidth);
        String[] b2 = boxButtonLines("Return to App", !quitConfirmFocused, NAVY_BLUE, btnWidth);

        sb.append(CENTER_MARKER).append(b1[0]).append("    ").append(b2[0]).append(CLEAR_EOL).append("\n");
        sb.append(CENTER_MARKER).append(b1[1]).append("    ").append(b2[1]).append(CLEAR_EOL).append("\n");
        sb.append(CENTER_MARKER).append(b1[2]).append("    ").append(b2[2]).append(CLEAR_EOL).append("\n\n");
        sb.append(dim("[←/→] Select Option  •  [Enter] Confirm  •  [Esc] Cancel")).append(CLEAR_EOL).append("\n");
        return sb.toString();
    }

    public static String padCenter(String text, int width) {
        if (text == null) text = "";
        int vis = visibleLength(text);
        if (vis >= width) return text;
        int totalPad = width - vis;
        int padLeft = totalPad / 2;
        int padRight = totalPad - padLeft;
        return " ".repeat(padLeft) + text + " ".repeat(padRight);
    }

    public static int getTerminalWidth() {
        if (cachedTermWidth != null) {
            return cachedTermWidth;
        }

        try {
            String cols = System.getenv("COLUMNS");
            if (cols != null && !cols.isBlank()) {
                int w = Integer.parseInt(cols.trim());
                if (w > 30) {
                    cachedTermWidth = w;
                    return w;
                }
            }
        } catch (Exception ignored) {}

        cachedTermWidth = 144;
        return cachedTermWidth;
    }

    public static int getTerminalHeight() {
        if (cachedTermHeight != null) {
            return cachedTermHeight;
        }
        try {
            String lines = System.getenv("LINES");
            if (lines != null && !lines.isBlank()) {
                int h = Integer.parseInt(lines.trim());
                if (h > 10) {
                    cachedTermHeight = h;
                    return h;
                }
            }
        } catch (Exception ignored) {}
        return 30;
    }

    public static int visibleLength(String str) {
        if (str == null || str.isEmpty()) return 0;
        String stripped = str.replaceAll("\\[[;?0-9]*[a-zA-Z]", "");
        return stripped.length();
    }

    public static String truncate(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        if (max <= 3) return text.substring(0, Math.max(0, max));
        return text.substring(0, max - 3) + "...";
    }

    public static String stripSpaces(String s) {
        if (s == null || s.isEmpty()) return "";
        String res = s.strip();
        res = res.replaceFirst("^((?:\\[[;?0-9]*[a-zA-Z])+)\\s+", "$1");
        res = res.replaceFirst("\\s+((?:\\[[;?0-9]*[a-zA-Z])+)$", "$1");
        return res;
    }

    public static boolean isHintRow(String stripped) {
        if (stripped == null || stripped.isEmpty()) return false;
        String s = stripped.trim();
        if (!s.startsWith("[")) {
            return false;
        }
        if (s.contains("[ ▶ ") || s.contains("[   ")) {
            return false;
        }
        if (s.startsWith("[Pinned") || s.startsWith("[NEW]") || s.startsWith("[READ]")) {
            return false;
        }
        if (s.contains(" • ")) {
            return true;
        }
        return s.startsWith("[Esc]") || s.startsWith("[Enter]") || s.startsWith("[Enter/Esc]") || s.startsWith("[Enter / Esc]");
    }

    public static String wrapHints(List<String> hints, int maxWidth) {
        if (hints == null || hints.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        StringBuilder currentLine = new StringBuilder("  ");
        List<String> lines = new java.util.ArrayList<>();
        for (String hint : hints) {
            if (hint == null || hint.isBlank()) continue;
            String separator = currentLine.length() > 2 ? "  •  " : "";
            if (visibleLength(currentLine.toString()) + visibleLength(separator) + visibleLength(hint) > maxWidth && currentLine.length() > 2) {
                lines.add(currentLine.toString());
                currentLine.setLength(0);
                currentLine.append("  ").append(hint);
            } else {
                currentLine.append(separator).append(hint);
            }
        }
        if (currentLine.length() > 2) {
            lines.add(currentLine.toString());
        }
        for (int i = 0; i < lines.size(); i++) {
            sb.append(dim(lines.get(i))).append("\n");
            if (i < lines.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    public static String wrapHints(List<String> hints) {
        return wrapHints(hints, TABLE_WIDTH);
    }

private static String stripAnsi(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.replace(CLEAR_EOL, "").replaceAll("\u001B\\[[;?0-9]*[a-zA-Z]", "").trim();
    }

    public static String centerLayout(String content) {
        if (content == null || content.isEmpty()) return "";

        boolean isDialog = content.contains(DIALOG_MARKER);

        int termWidth = getTerminalWidth();
        int termHeight = getTerminalHeight();

        String[] rawLines = content.split("\n", -1);
        for (int i = 0; i < rawLines.length; i++) {
            if (rawLines[i].contains(DIALOG_MARKER)) {
                rawLines[i] = rawLines[i].replace(DIALOG_MARKER, "");
            }
        }
        int lastNonBlank = rawLines.length - 1;
        while (lastNonBlank >= 0 && stripAnsi(rawLines[lastNonBlank]).isEmpty()) {
            lastNonBlank--;
        }
        if (lastNonBlank < 0) return "";

        int topHeaderStart = -1;
        int topHeaderEnd = -1;
        for (int i = 0; i <= lastNonBlank; i++) {
            if (rawLines[i].contains(HEADER_START)) {
                topHeaderStart = i;
            }
            if (rawLines[i].contains(HEADER_END)) {
                topHeaderEnd = i;
                break;
            }
        }
        boolean isMarkedHeader = (topHeaderStart != -1 && topHeaderEnd != -1);
        if (!isMarkedHeader) {
            int topBoxStart = -1;
            int topBoxEnd = -1;
            for (int i = 0; i <= lastNonBlank; i++) {
                String stripped = stripAnsi(rawLines[i]);
                if (stripped.startsWith("┏━") || stripped.startsWith("╔═")) {
                    topBoxStart = i;
                    break;
                }
            }
            if (topBoxStart != -1) {
                for (int i = topBoxStart; i <= lastNonBlank; i++) {
                    String stripped = stripAnsi(rawLines[i]);
                    if (stripped.startsWith("┗━") || stripped.startsWith("╚═")) {
                        topBoxEnd = i;
                        break;
                    }
                }
            }
            if (topBoxStart != -1 && topBoxEnd != -1 && topBoxEnd >= topBoxStart) {
                topHeaderStart = topBoxStart;
                topHeaderEnd = topBoxEnd;
            }
        }

        boolean hasHeader = (topHeaderStart != -1 && topHeaderEnd != -1 && topHeaderEnd >= topHeaderStart);
        java.util.List<String> headerLines = new java.util.ArrayList<>();
        if (hasHeader) {
            for (int i = topHeaderStart; i <= topHeaderEnd; i++) {
                String raw = rawLines[i].replace(CLEAR_EOL, "");
                if (raw.contains(HEADER_START) || raw.contains(HEADER_END)) {
                    continue;
                }
                String stripped = stripAnsi(raw);
                if (!isMarkedHeader) {
                    if (stripped.startsWith("┏━") || stripped.startsWith("╔═")
                            || stripped.startsWith("┗━") || stripped.startsWith("╚═")) {
                        continue;
                    }
                    if (stripped.startsWith("┃") && stripped.endsWith("┃") && stripped.length() > 2) {
                        int firstPipe = raw.indexOf('┃');
                        int lastPipe = raw.lastIndexOf('┃');
                        if (firstPipe != -1 && lastPipe > firstPipe) {
                            raw = raw.substring(firstPipe + 1, lastPipe).stripTrailing();
                        }
                    }
                }
                headerLines.add(raw);
            }
            while (!headerLines.isEmpty() && stripAnsi(headerLines.get(0)).isEmpty()) {
                headerLines.remove(0);
            }
            while (!headerLines.isEmpty() && stripAnsi(headerLines.get(headerLines.size() - 1)).isEmpty()) {
                headerLines.remove(headerLines.size() - 1);
            }
        }

        int startBody = hasHeader ? (topHeaderEnd + 1) : 0;
        while (startBody <= lastNonBlank && stripAnsi(rawLines[startBody]).isEmpty()) {
            startBody++;
        }

        java.util.List<Integer> hintIndices = new java.util.ArrayList<>();
        int curr = lastNonBlank;
        while (curr >= startBody) {
            String stripped = stripAnsi(rawLines[curr]);
            if (stripped.isEmpty()) {
                int prev = curr - 1;
                while (prev >= startBody && stripAnsi(rawLines[prev]).isEmpty()) {
                    prev--;
                }
                if (prev >= startBody && isHintRow(stripAnsi(rawLines[prev]))) {
                    curr = prev;
                    continue;
                }
                break;
            }
            if (isHintRow(stripped)) {
                hintIndices.add(0, curr);
                curr--;
            } else {
                break;
            }
        }

        int endBody = hintIndices.isEmpty() ? lastNonBlank : (hintIndices.get(0) - 1);
        while (endBody >= startBody && stripAnsi(rawLines[endBody]).isEmpty()) {
            endBody--;
        }

        int maxLineLen = 0;
        for (int i = startBody; i <= endBody; i++) {
            String clean = rawLines[i].replace(CLEAR_EOL, "");
            int len = visibleLength(clean);
            if (len > maxLineLen) {
                maxLineLen = len;
            }
        }
        for (int idx : hintIndices) {
            String clean = rawLines[idx].replace(CLEAR_EOL, "");
            int len = visibleLength(stripSpaces(clean));
            if (len > maxLineLen) {
                maxLineLen = len;
            }
        }

        int minInnerWidth = isDialog ? DIALOG_INNER_WIDTH : MIN_INNER_WIDTH;
        int minBodyRows = isDialog ? DIALOG_MIN_BODY_ROWS : MIN_BODY_ROWS;

        int innerWidth = Math.max(minInnerWidth, Math.min(MAX_INNER_WIDTH, maxLineLen));
        if (termWidth > 30) {
            innerWidth = Math.min(innerWidth, termWidth - 8);
        }

        int contentBlockOffset = Math.max(0, (innerWidth - maxLineLen) / 2);
        int targetInnerWidth = innerWidth + 2;
        int boxWidth = innerWidth + 4;

        int outerMarginX = (termWidth > 146) ? 2 : 0;
        int outerWidth = termWidth - (outerMarginX * 2);
        if (outerWidth < 144) {
            outerWidth = Math.min(144, termWidth);
        }
        int outerInnerWidth = outerWidth - 2;
        if (outerInnerWidth % 2 != 0) {
            outerWidth -= 1;
            outerInnerWidth = outerWidth - 2;
        }
        int leftIndent = Math.max(0, (termWidth - outerWidth) / 2);
        int rightMarginLen = Math.max(0, termWidth - (leftIndent + outerWidth));
        String indent = (leftIndent > 0) ? " ".repeat(leftIndent) : "";
        String rightMargin = (rightMarginLen > 0) ? " ".repeat(rightMarginLen) : "";

        int contentLeftPad = Math.max(0, (outerInnerWidth - boxWidth) / 2);
        int contentRightPad = Math.max(0, outerInnerWidth - (contentLeftPad + boxWidth));

        int headerRows = headerLines.size();
        int bodyRows = (endBody >= startBody) ? (endBody - startBody + 1) : 0;
        int hintRows = hintIndices.size();

        int contentHeight = (headerRows > 0 ? (headerRows + 1) : 0)
                + (bodyRows > 0 ? (Math.max(bodyRows, minBodyRows) + 4 + (hintRows > 0 ? 1 : 0)) : 0)
                + (hintRows > 0 ? (hintRows + 2) : 0);

        int extraPadding = (bodyRows > 0 && bodyRows < minBodyRows) ? (minBodyRows - bodyRows) : 0;
        int extraTopPad = extraPadding / 2;
        int extraBotPad = extraPadding - extraTopPad;

        int targetFrameHeight;
        int topMarginOutside;
        int botMarginOutside;

        int usableTermHeight = (termHeight > 10) ? termHeight : 30;
        if (usableTermHeight >= contentHeight + 4) {
            targetFrameHeight = usableTermHeight - 2;
            topMarginOutside = 1;
            botMarginOutside = 1;
        } else {
            targetFrameHeight = Math.max(usableTermHeight - 2, contentHeight + 2);
            topMarginOutside = Math.max(0, (usableTermHeight - targetFrameHeight) / 2);
            botMarginOutside = Math.max(0, usableTermHeight - targetFrameHeight - topMarginOutside);
        }

        int interiorHeight = targetFrameHeight - 2;
        int remainingY = Math.max(0, interiorHeight - contentHeight);
        int topPadInside = remainingY / 2;
        int botPadInside = remainingY - topPadInside;

        String borderCol = NAVY_BLUE;

        StringBuilder sb = new StringBuilder();

        for (int r = 0; r < topMarginOutside; r++) {
            sb.append(" ".repeat(termWidth)).append(CLEAR_EOL).append("\n");
        }

        sb.append(indent)
          .append(borderCol).append("┏").append("━".repeat(outerInnerWidth)).append("┓").append(RESET)
          .append(rightMargin)
          .append(CLEAR_EOL).append("\n");

        int asciiLift = (!headerLines.isEmpty() && topPadInside >= 4) ? 2 : 0;

        for (int p = 0; p < topPadInside - asciiLift; p++) {
            sb.append(indent)
              .append(borderCol).append("┃").append(RESET)
              .append(" ".repeat(outerInnerWidth))
              .append(borderCol).append("┃").append(RESET)
              .append(rightMargin)
              .append(CLEAR_EOL).append("\n");
        }

        if (!headerLines.isEmpty()) {
            for (String hLine : headerLines) {
                String cleanHLine = hLine.replace(CLEAR_EOL, "");
                int visLen = visibleLength(cleanHLine);
                int pad = Math.max(0, (outerInnerWidth - visLen) / 2);
                int rightPad = Math.max(0, outerInnerWidth - (pad + visLen));
                sb.append(indent)
                  .append(borderCol).append("┃").append(RESET)
                  .append(" ".repeat(pad))
                  .append(cleanHLine)
                  .append(" ".repeat(rightPad))
                  .append(borderCol).append("┃").append(RESET)
                  .append(rightMargin)
                  .append(CLEAR_EOL).append("\n");
            }
            if (bodyRows > 0 || hintRows > 0) {
                for (int g = 0; g < 1 + asciiLift; g++) {
                    sb.append(indent)
                      .append(borderCol).append("┃").append(RESET)
                      .append(" ".repeat(outerInnerWidth))
                      .append(borderCol).append("┃").append(RESET)
                      .append(rightMargin)
                      .append(CLEAR_EOL).append("\n");
                }
            }
        }

        if (bodyRows > 0) {
            sb.append(indent)
              .append(borderCol).append("┃").append(RESET)
              .append(" ".repeat(contentLeftPad))
              .append(borderCol).append("┏").append("━".repeat(targetInnerWidth)).append("┓").append(RESET)
              .append(" ".repeat(contentRightPad))
              .append(borderCol).append("┃").append(RESET)
              .append(rightMargin)
              .append(CLEAR_EOL).append("\n");

            boolean hasBoxTitleAtTop = rawLines[startBody].contains(BOX_TITLE_MARKER);
            if (hasBoxTitleAtTop) {
                appendCardEmptyRow(sb, indent, borderCol, contentLeftPad, targetInnerWidth, contentRightPad, rightMargin);

                String titleClean = rawLines[startBody].replace(CLEAR_EOL, "");
                String trimmedTitle = stripSpaces(titleClean.replace(BOX_TITLE_MARKER, "").replace(CENTER_MARKER, ""));
                int titleVisLen = visibleLength(trimmedTitle);
                int titleLeftPad = Math.max(0, (innerWidth - titleVisLen) / 2);
                int titleRightPad = Math.max(0, innerWidth - (titleLeftPad + titleVisLen));
                appendCardContentRow(sb, indent, borderCol, contentLeftPad, titleLeftPad, trimmedTitle, titleRightPad, contentRightPad, rightMargin);

                for (int p = 0; p < extraTopPad; p++) {
                    appendCardEmptyRow(sb, indent, borderCol, contentLeftPad, targetInnerWidth, contentRightPad, rightMargin);
                }

                for (int i = startBody + 1; i <= endBody; i++) {
                    renderCardBodyLine(sb, rawLines[i], innerWidth, contentBlockOffset, indent, borderCol, contentLeftPad, contentRightPad, rightMargin);
                }
            } else {
                for (int p = 0; p < 1 + extraTopPad; p++) {
                    appendCardEmptyRow(sb, indent, borderCol, contentLeftPad, targetInnerWidth, contentRightPad, rightMargin);
                }

                for (int i = startBody; i <= endBody; i++) {
                    renderCardBodyLine(sb, rawLines[i], innerWidth, contentBlockOffset, indent, borderCol, contentLeftPad, contentRightPad, rightMargin);
                }
            }

            for (int p = 0; p < 1 + extraBotPad; p++) {
                appendCardEmptyRow(sb, indent, borderCol, contentLeftPad, targetInnerWidth, contentRightPad, rightMargin);
            }

            sb.append(indent)
              .append(borderCol).append("┃").append(RESET)
              .append(" ".repeat(contentLeftPad))
              .append(borderCol).append("┗").append("━".repeat(targetInnerWidth)).append("┛").append(RESET)
              .append(" ".repeat(contentRightPad))
              .append(borderCol).append("┃").append(RESET)
              .append(rightMargin)
              .append(CLEAR_EOL);

            if (hintRows > 0) {
                sb.append("\n")
                  .append(indent)
                  .append(borderCol).append("┃").append(RESET)
                  .append(" ".repeat(outerInnerWidth))
                  .append(borderCol).append("┃").append(RESET)
                  .append(rightMargin)
                  .append(CLEAR_EOL).append("\n");
            }
        }

        if (hintRows > 0) {
            sb.append(indent)
              .append(borderCol).append("┃").append(RESET)
              .append(" ".repeat(contentLeftPad))
              .append(borderCol).append("┏").append("━".repeat(targetInnerWidth)).append("┓").append(RESET)
              .append(" ".repeat(contentRightPad))
              .append(borderCol).append("┃").append(RESET)
              .append(rightMargin)
              .append(CLEAR_EOL).append("\n");

            for (int idx : hintIndices) {
                String cleanLine = rawLines[idx].replace(CLEAR_EOL, "");
                String trimmedClean = stripSpaces(cleanLine.replace(BOX_TITLE_MARKER, "").replace(CENTER_MARKER, ""));
                int trimmedVisLen = visibleLength(trimmedClean);
                int leftPad = Math.max(0, (innerWidth - trimmedVisLen) / 2);
                int rightPad = Math.max(0, innerWidth - (leftPad + trimmedVisLen));

                appendCardContentRow(sb, indent, borderCol, contentLeftPad, leftPad, trimmedClean, rightPad, contentRightPad, rightMargin);
            }

            sb.append(indent)
              .append(borderCol).append("┃").append(RESET)
              .append(" ".repeat(contentLeftPad))
              .append(borderCol).append("┗").append("━".repeat(targetInnerWidth)).append("┛").append(RESET)
              .append(" ".repeat(contentRightPad))
              .append(borderCol).append("┃").append(RESET)
              .append(rightMargin)
              .append(CLEAR_EOL);
        }

        for (int p = 0; p < botPadInside; p++) {
            sb.append("\n")
              .append(indent)
              .append(borderCol).append("┃").append(RESET)
              .append(" ".repeat(outerInnerWidth))
              .append(borderCol).append("┃").append(RESET)
              .append(rightMargin)
              .append(CLEAR_EOL);
        }

        sb.append("\n")
          .append(indent)
          .append(borderCol).append("┗").append("━".repeat(outerInnerWidth)).append("┛").append(RESET)
          .append(rightMargin)
          .append(CLEAR_EOL);

        for (int r = 0; r < botMarginOutside; r++) {
            sb.append("\n").append(" ".repeat(termWidth)).append(CLEAR_EOL);
        }

        return sb.toString();
    }

    private static void appendCardEmptyRow(StringBuilder sb, String indent, String borderCol,
                                           int contentLeftPad, int targetInnerWidth,
                                           int contentRightPad, String rightMargin) {
        sb.append(indent)
          .append(borderCol).append("┃").append(RESET)
          .append(" ".repeat(contentLeftPad))
          .append(borderCol).append("┃").append(RESET)
          .append(" ".repeat(targetInnerWidth))
          .append(borderCol).append("┃").append(RESET)
          .append(" ".repeat(contentRightPad))
          .append(borderCol).append("┃").append(RESET)
          .append(rightMargin)
          .append(CLEAR_EOL).append("\n");
    }

    private static void appendCardContentRow(StringBuilder sb, String indent, String borderCol,
                                             int contentLeftPad, int leftPad, String content,
                                             int rightPad, int contentRightPad, String rightMargin) {
        sb.append(indent)
          .append(borderCol).append("┃").append(RESET)
          .append(" ".repeat(contentLeftPad))
          .append(borderCol).append("┃").append(RESET)
          .append(" ")
          .append(" ".repeat(leftPad))
          .append(content)
          .append(" ".repeat(rightPad))
          .append(" ")
          .append(borderCol).append("┃").append(RESET)
          .append(" ".repeat(contentRightPad))
          .append(borderCol).append("┃").append(RESET)
          .append(rightMargin)
          .append(CLEAR_EOL).append("\n");
    }

    private static void renderCardBodyLine(StringBuilder sb, String rawLine, int innerWidth,
                                           int contentBlockOffset, String indent, String borderCol,
                                           int contentLeftPad, int contentRightPad, String rightMargin) {
        String cleanLine = rawLine.replace(CLEAR_EOL, "");
        int visLen = visibleLength(cleanLine);
        String stripped = cleanLine.replaceAll("\u001B\\[[;?0-9]*[a-zA-Z]", "");

        boolean isCentered = cleanLine.contains(BOX_TITLE_MARKER) || cleanLine.contains(CENTER_MARKER);
        boolean isTabsRow = stripped.trim().startsWith("Tabs:") || cleanLine.contains("Tabs:");
        boolean isButtonRow = !isTabsRow && (
                ((stripped.contains("[ ▶ ") || stripped.contains("[   ")) && stripped.trim().endsWith("]"))
                || ((stripped.contains("┌") && stripped.contains("┐"))
                    || (stripped.contains("│") && !stripped.contains("┃") && !stripped.contains("Email") && !stripped.contains("Password") && !stripped.contains("Search:") && !stripped.contains("Page "))
                    || (stripped.contains("└") && stripped.contains("┘")))
        );

        int leftPad;
        int rightPad;
        if (isCentered) {
            String trimmedClean = stripSpaces(cleanLine.replace(BOX_TITLE_MARKER, "").replace(CENTER_MARKER, ""));
            int trimmedVisLen = visibleLength(trimmedClean);
            leftPad = Math.max(0, (innerWidth - trimmedVisLen) / 2);
            rightPad = Math.max(0, innerWidth - (leftPad + trimmedVisLen));
            cleanLine = trimmedClean;
        } else if (isButtonRow) {
            String trimmedClean = stripSpaces(cleanLine);
            int trimmedVisLen = visibleLength(trimmedClean);
            leftPad = Math.max(0, (innerWidth - trimmedVisLen) / 2);
            rightPad = Math.max(0, innerWidth - (leftPad + trimmedVisLen));
            cleanLine = trimmedClean;
        } else {
            leftPad = contentBlockOffset;
            rightPad = Math.max(0, innerWidth - (leftPad + visLen));
        }

        appendCardContentRow(sb, indent, borderCol, contentLeftPad, leftPad, cleanLine, rightPad, contentRightPad, rightMargin);
    }
}
