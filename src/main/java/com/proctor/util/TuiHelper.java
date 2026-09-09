package com.proctor.util;

import java.util.List;

public class TuiHelper {
    public static final String RESET = "[0m";
    public static final String BOLD = "[1m";
    public static final String DIM = "[90m";
    public static final String RED = "[31m";
    public static final String GREEN = "[32m";
    public static final String YELLOW = "[33m";
    public static final String BLUE = "[34m";
    public static final String MAGENTA = "[35m";
    public static final String NAVY_BLUE = "[38;2;30;58;138m";
    public static final String PURPLE = NAVY_BLUE;
    public static final String CYAN = NAVY_BLUE;
    public static final String WHITE = "[37m";
    public static final String CLEAR_EOL = "[K";

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

    public static String purple(String text) {
        return navyBlue(text);
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

    public static String asciiBannerBox(String[] asciiLines, String subtitle) {
        StringBuilder sb = new StringBuilder();
        int boxInnerWidth = 98;
        sb.append(NAVY_BLUE).append("┏").append("━".repeat(boxInnerWidth)).append("┓").append(RESET).append(CLEAR_EOL).append("\n");
        sb.append(NAVY_BLUE).append("┃").append(" ".repeat(boxInnerWidth)).append("┃").append(RESET).append(CLEAR_EOL).append("\n");
        for (String line : asciiLines) {
            sb.append(NAVY_BLUE).append("┃").append(RESET)
              .append(NAVY_BLUE).append(padCenter(line, boxInnerWidth)).append(RESET)
              .append(NAVY_BLUE).append("┃").append(RESET)
              .append(CLEAR_EOL).append("\n");
        }
        sb.append(NAVY_BLUE).append("┃").append(" ".repeat(boxInnerWidth)).append("┃").append(RESET).append(CLEAR_EOL).append("\n");
        if (subtitle != null && !subtitle.isBlank()) {
            String cleanSub = subtitle;
            if (visibleLength(cleanSub) > boxInnerWidth - 4) {
                cleanSub = cleanSub.substring(0, boxInnerWidth - 5) + "…";
            }
            sb.append(NAVY_BLUE).append("┃").append(RESET)
              .append(DIM).append(padCenter(cleanSub, boxInnerWidth)).append(RESET)
              .append(NAVY_BLUE).append("┃").append(RESET)
              .append(CLEAR_EOL).append("\n");
            sb.append(NAVY_BLUE).append("┃").append(" ".repeat(boxInnerWidth)).append("┃").append(RESET).append(CLEAR_EOL).append("\n");
        }
        sb.append(NAVY_BLUE).append("┗").append("━".repeat(boxInnerWidth)).append("┛").append(RESET).append(CLEAR_EOL).append("\n");
        return sb.toString();
    }

    private static String formatSubHeader(String title, String subtitle, String defaultKeyword) {
        if (title == null || title.isBlank() || title.equalsIgnoreCase(defaultKeyword)) {
            return subtitle;
        }
        String cleanTitle = title.trim();
        if (subtitle == null || subtitle.isBlank()) {
            return cleanTitle;
        }
        return cleanTitle + "  •  " + subtitle;
    }

    public static String proctorAsciiBanner(String subtitle) {
        return asciiBannerBox(ASCII_PROCTOR, subtitle);
    }

    public static String header(String title, String subtitle) {
        String upper = (title != null) ? title.toUpperCase() : "";

        if (upper.equals("PROCTOR")) {
            return asciiBannerBox(ASCII_PROCTOR, subtitle);
        }
        if (upper.contains("DASHBOARD") || upper.contains("PORTAL")) {
            String sub = subtitle;
            if (upper.contains("ADMIN")) {
                sub = (sub != null && !sub.isBlank()) ? "Admin Portal  •  " + sub : "Admin Portal";
            } else if (upper.contains("TEACHER")) {
                sub = (sub != null && !sub.isBlank()) ? "Teacher Portal  •  " + sub : "Teacher Portal";
            } else if (upper.contains("STUDENT")) {
                sub = (sub != null && !sub.isBlank()) ? "Student Portal  •  " + sub : "Student Portal";
            }
            return asciiBannerBox(ASCII_DASHBOARD, sub);
        }
        if (upper.contains("EXAM")) {
            String sub = formatSubHeader(title, subtitle, "EXAMS");
            return asciiBannerBox(ASCII_EXAMS, sub);
        }
        if (upper.contains("QUIZ")) {
            String sub = formatSubHeader(title, subtitle, "QUIZZES");
            return asciiBannerBox(ASCII_QUIZZES, sub);
        }
        if (upper.contains("INBOX") || upper.contains("NOTIFICATION") || upper.contains("MESSAGE")) {
            String sub = formatSubHeader(title, subtitle, "INBOX");
            return asciiBannerBox(ASCII_INBOX, sub);
        }
        if (upper.contains("USER")) {
            String sub = formatSubHeader(title, subtitle, "USERS");
            return asciiBannerBox(ASCII_USERS, sub);
        }
        if (upper.contains("REPORT")) {
            String sub = formatSubHeader(title, subtitle, "REPORTS");
            return asciiBannerBox(ASCII_REPORTS, sub);
        }
        if (upper.contains("LEADERBOARD")) {
            String sub = formatSubHeader(title, subtitle, "LEADERBOARD");
            return asciiBannerBox(ASCII_LEADERBOARD, sub);
        }
        if (upper.contains("SUBMISSION") || upper.contains("ANSWER SHEET") || upper.contains("ATTEMPT")) {
            String sub = formatSubHeader(title, subtitle, "SUBMISSIONS");
            return asciiBannerBox(ASCII_SUBMISSIONS, sub);
        }
        if (upper.contains("QUESTION")) {
            String sub = formatSubHeader(title, subtitle, "QUESTIONS");
            return asciiBannerBox(ASCII_QUESTIONS, sub);
        }
        if (upper.contains("SUBJECT") || upper.contains("TEACHER ASSIGNMENT")) {
            String sub = formatSubHeader(title, subtitle, "SUBJECTS");
            return asciiBannerBox(ASCII_SUBJECTS, sub);
        }
        if (upper.contains("ASSESSMENT")) {
            String sub = formatSubHeader(title, subtitle, "ASSESSMENT");
            return asciiBannerBox(ASCII_EXAMS, sub);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(NAVY_BLUE).append("┏").append("━".repeat(98)).append("┓").append(RESET).append(CLEAR_EOL).append("\n");
        sb.append(NAVY_BLUE).append("┃").append(" ".repeat(98)).append("┃").append(RESET).append(CLEAR_EOL).append("\n");
        sb.append(NAVY_BLUE).append("┃").append(RESET).append("  ").append(bold(NAVY_BLUE + padCenter(title, 94))).append("  ").append(NAVY_BLUE).append("┃").append(RESET).append(CLEAR_EOL).append("\n");
        if (subtitle != null && !subtitle.isBlank()) {
            sb.append(NAVY_BLUE).append("┃").append(" ".repeat(98)).append("┃").append(RESET).append(CLEAR_EOL).append("\n");
            sb.append(NAVY_BLUE).append("┃").append(RESET).append("  ").append(DIM).append(padCenter(subtitle, 94)).append(RESET).append("  ").append(NAVY_BLUE).append("┃").append(RESET).append(CLEAR_EOL).append("\n");
        }
        sb.append(NAVY_BLUE).append("┃").append(" ".repeat(98)).append("┃").append(RESET).append(CLEAR_EOL).append("\n");
        sb.append(NAVY_BLUE).append("┗").append("━".repeat(98)).append("┛").append(RESET).append(CLEAR_EOL).append("\n");
        return sb.toString();
    }

    public static String banner(String text) {
        return navyBlue("┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓") + CLEAR_EOL + "\n"
             + navyBlue("┃ ") + bold(navyBlue(padCenter(text, 54))) + navyBlue(" ┃") + CLEAR_EOL + "\n"
             + navyBlue("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛") + CLEAR_EOL + "\n";
    }

    public static String inputBox(String label, String value, boolean focused, int width, boolean masked, String placeholder) {
        StringBuilder sb = new StringBuilder();
        String borderCol = focused ? NAVY_BLUE : DIM;
        String labelCol = focused ? bold(NAVY_BLUE + "▶ " + label) : dim("  " + label);

        int maxInner = Math.max(10, width - 4);
        String displayVal;
        if (value == null || value.isEmpty()) {
            if (focused) {
                displayVal = "_" + " ".repeat(maxInner - 1);
            } else {
                String ph = (placeholder != null) ? placeholder : "";
                if (ph.length() > maxInner) {
                    ph = ph.substring(0, maxInner - 1) + "…";
                }
                displayVal = dim(ph) + " ".repeat(Math.max(0, maxInner - ph.length()));
            }
        } else {
            String txt = masked ? "*".repeat(value.length()) : value;
            if (focused) {
                if (txt.length() < maxInner) {
                    displayVal = txt + "_" + " ".repeat(maxInner - txt.length() - 1);
                } else {
                    displayVal = "…" + txt.substring(txt.length() - (maxInner - 2)) + "_";
                }
            } else {
                if (txt.length() <= maxInner) {
                    displayVal = txt + " ".repeat(maxInner - txt.length());
                } else {
                    displayVal = txt.substring(0, maxInner - 1) + "…";
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
        String labelCol = focused ? bold(NAVY_BLUE + "▶ " + label) : dim("  " + label);

        String valDisplay = focused ? navyBlue("< " + value + " >") + (helpText != null ? dim(" (" + helpText + ")") : "") : value;
        String rawLenText = "< " + value + " >" + (helpText != null ? " (" + helpText + ")" : "");
        int padLen = Math.max(0, (width - 4) - rawLenText.length());

        sb.append("  ").append(labelCol).append(CLEAR_EOL).append("\n");
        sb.append("  ").append(borderCol).append("┌").append("─".repeat(width - 2)).append("┐").append(RESET).append(CLEAR_EOL).append("\n");
        sb.append("  ").append(borderCol).append("│").append(RESET).append(" ").append(valDisplay).append(" ".repeat(padLen)).append(" ").append(borderCol).append("│").append(RESET).append(CLEAR_EOL).append("\n");
        sb.append("  ").append(borderCol).append("└").append("─".repeat(width - 2)).append("┘").append(RESET).append(CLEAR_EOL).append("\n");
        return sb.toString();
    }

    public static String buttonRow(String primaryLabel, boolean primaryFocused, String secondaryLabel, boolean secondaryFocused) {
        return buttonRow(primaryLabel, primaryFocused, secondaryLabel, secondaryFocused, 100);
    }

    public static String buttonRow(String primaryLabel, boolean primaryFocused, String secondaryLabel, boolean secondaryFocused, int width) {
        String btn1 = primaryFocused ? bold(NAVY_BLUE + "[ ▶ " + primaryLabel + " ]") : dim("[   " + primaryLabel + "   ]");
        String btn2 = secondaryFocused ? bold(RED + "[ ▶ " + secondaryLabel + " ]") : dim("[   " + secondaryLabel + "   ]");
        int totalBtnWidth = visibleLength(btn1) + 4 + visibleLength(btn2);
        int leftPad = Math.max(0, (width - totalBtnWidth) / 2);
        return " ".repeat(leftPad) + btn1 + "    " + btn2 + CLEAR_EOL;
    }

    public static String confirmationModal(String title, String message, String warningDetail, String confirmLabel, String cancelLabel, boolean confirmFocused) {
        StringBuilder sb = new StringBuilder();
        sb.append(header("CONFIRM ACTION", title));
        sb.append("\n");
        sb.append(bold(padCenter(message, 100))).append(CLEAR_EOL).append("\n\n");
        if (warningDetail != null && !warningDetail.isBlank()) {
            sb.append(dim(padCenter(warningDetail, 100))).append(CLEAR_EOL).append("\n\n");
        }
        sb.append(dim(padCenter("─".repeat(96), 100))).append(CLEAR_EOL).append("\n\n");

        String btn1 = confirmFocused ? bold(NAVY_BLUE + "[ ▶ " + confirmLabel + " ]") : dim("[   " + confirmLabel + "   ]");
        String btn2 = !confirmFocused ? bold(RED + "[ ▶ " + cancelLabel + " ]") : dim("[   " + cancelLabel + "   ]");
        int textWidth = confirmLabel.length() + cancelLabel.length() + 18;
        int leftPad = Math.max(0, (100 - textWidth) / 2);

        sb.append(" ".repeat(leftPad)).append(btn1).append("    ").append(btn2).append(CLEAR_EOL).append("\n\n");
        sb.append(dim(padCenter("[←/→] Select Option  •  [Enter] Confirm  •  [Esc] Cancel", 100))).append(CLEAR_EOL).append("\n");
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

        cachedTermWidth = 100;
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
        return wrapHints(hints, 90);
    }

private static String stripAnsi(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.replace(CLEAR_EOL, "").replaceAll("\u001B\\[[;?0-9]*[a-zA-Z]", "").trim();
    }

    public static String centerLayout(String content) {
        if (content == null || content.isEmpty()) return "";

        int termWidth = getTerminalWidth();
        int termHeight = getTerminalHeight();

        String[] rawLines = content.split("\n", -1);
        int lastNonBlank = rawLines.length - 1;
        while (lastNonBlank >= 0 && stripAnsi(rawLines[lastNonBlank]).isEmpty()) {
            lastNonBlank--;
        }
        if (lastNonBlank < 0) return "";

        int topBoxStart = -1;
        int topBoxEnd = -1;
        for (int i = 0; i <= lastNonBlank; i++) {
            String stripped = stripAnsi(rawLines[i]);
            if (stripped.startsWith("┏") || stripped.startsWith("╔")) {
                topBoxStart = i;
                break;
            }
        }
        if (topBoxStart != -1) {
            for (int i = topBoxStart; i <= lastNonBlank; i++) {
                String stripped = stripAnsi(rawLines[i]);
                if (stripped.startsWith("┗") || stripped.startsWith("╚")) {
                    topBoxEnd = i;
                    break;
                }
            }
        }

        boolean hasHeader = (topBoxStart != -1 && topBoxEnd != -1 && topBoxEnd >= topBoxStart);
        int startBody = hasHeader ? (topBoxEnd + 1) : 0;
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
            int len = visibleLength(clean);
            if (len > maxLineLen) {
                maxLineLen = len;
            }
        }

        int innerWidth = Math.max(100, maxLineLen);
        if (termWidth > 30) {
            innerWidth = Math.min(innerWidth, termWidth - 4);
        }

        int contentBlockOffset = Math.max(0, (innerWidth - maxLineLen) / 2);

        int targetInnerWidth = innerWidth + 2;
        int boxWidth = innerWidth + 4;
        int leftMargin = Math.max(0, (termWidth - boxWidth) / 2);
        String indent = (leftMargin > 0) ? " ".repeat(leftMargin) : "";

        int headerRows = hasHeader ? (topBoxEnd - topBoxStart + 1) : 0;
        int bodyRows = (endBody >= startBody) ? (endBody - startBody + 1) : 0;
        int hintRows = hintIndices.size();

        int totalHeight = (hasHeader ? (headerRows + 1) : 0)
                + (bodyRows > 0 ? (bodyRows + 4 + (hintRows > 0 ? 1 : 0)) : 0)
                + (hintRows > 0 ? (hintRows + 2) : 0);
        int topMargin = Math.max(0, (termHeight - totalHeight) / 2);

        String borderCol = NAVY_BLUE;

        StringBuilder sb = new StringBuilder();

        for (int r = 0; r < topMargin; r++) {
            sb.append(CLEAR_EOL).append("\n");
        }

        if (hasHeader) {
            for (int i = topBoxStart; i <= topBoxEnd; i++) {
                String cleanLine = rawLines[i].replace(CLEAR_EOL, "");
                String stripped = stripAnsi(cleanLine);
                if (stripped.startsWith("┏") || stripped.startsWith("╔")) {
                    sb.append(indent).append(borderCol).append("┏").append("━".repeat(targetInnerWidth)).append("┓").append(RESET).append(CLEAR_EOL).append("\n");
                } else if (stripped.startsWith("┗") || stripped.startsWith("╚")) {
                    sb.append(indent).append(borderCol).append("┗").append("━".repeat(targetInnerWidth)).append("┛").append(RESET).append(CLEAR_EOL).append("\n");
                } else {
                    int firstPipe = cleanLine.indexOf('┃');
                    if (firstPipe == -1) firstPipe = cleanLine.indexOf('║');
                    int lastPipe = cleanLine.lastIndexOf('┃');
                    if (lastPipe == -1) lastPipe = cleanLine.lastIndexOf('║');
                    if (firstPipe != -1 && lastPipe > firstPipe) {
                        String inner = cleanLine.substring(firstPipe + 1, lastPipe);
                        int visInner = visibleLength(inner);
                        int leftPad = Math.max(0, (targetInnerWidth - visInner) / 2);
                        int rightPad = Math.max(0, targetInnerWidth - (leftPad + visInner));
                        sb.append(indent)
                          .append(borderCol).append("┃").append(RESET)
                          .append(" ".repeat(leftPad))
                          .append(inner)
                          .append(" ".repeat(rightPad))
                          .append(borderCol).append("┃").append(RESET)
                          .append(CLEAR_EOL).append("\n");
                    } else {
                        sb.append(indent).append(cleanLine).append(CLEAR_EOL).append("\n");
                    }
                }
            }
            if (bodyRows > 0 || hintRows > 0) {
                sb.append(CLEAR_EOL).append("\n");
            }
        }

        if (bodyRows > 0) {
            sb.append(indent)
              .append(borderCol).append("┏").append("━".repeat(targetInnerWidth)).append("┓").append(RESET)
              .append(CLEAR_EOL).append("\n");

            sb.append(indent)
              .append(borderCol).append("┃").append(RESET)
              .append(" ".repeat(targetInnerWidth))
              .append(borderCol).append("┃").append(RESET)
              .append(CLEAR_EOL).append("\n");

            for (int i = startBody; i <= endBody; i++) {
                String cleanLine = rawLines[i].replace(CLEAR_EOL, "");
                int visLen = visibleLength(cleanLine);
                String stripped = cleanLine.replaceAll("\u001B\\[[;?0-9]*[a-zA-Z]", "");

                boolean isButtonRow = (stripped.contains("[ ▶ ") || stripped.contains("[   "))
                        && (stripped.contains("Sign In") || stripped.contains("Sign Up") || stripped.contains("Submit")
                        || stripped.contains("Cancel") || stripped.contains("Register") || stripped.contains("Approve")
                        || stripped.contains("Reject") || stripped.contains("Generate") || stripped.contains("Exit")
                        || stripped.contains("Back") || stripped.contains("Forgot Password"));

                int leftPad;
                int rightPad;
                if (isButtonRow) {
                    String trimmedClean = stripSpaces(cleanLine);
                    int trimmedVisLen = visibleLength(trimmedClean);
                    leftPad = Math.max(0, (innerWidth - trimmedVisLen) / 2);
                    rightPad = Math.max(0, innerWidth - (leftPad + trimmedVisLen));
                    cleanLine = trimmedClean;
                } else {
                    leftPad = contentBlockOffset;
                    rightPad = Math.max(0, innerWidth - (leftPad + visLen));
                }

                sb.append(indent)
                  .append(borderCol).append("┃").append(RESET)
                  .append(" ")
                  .append(" ".repeat(leftPad))
                  .append(cleanLine)
                  .append(" ".repeat(rightPad))
                  .append(" ")
                  .append(borderCol).append("┃").append(RESET)
                  .append(CLEAR_EOL).append("\n");
            }

            sb.append(indent)
              .append(borderCol).append("┃").append(RESET)
              .append(" ".repeat(targetInnerWidth))
              .append(borderCol).append("┃").append(RESET)
              .append(CLEAR_EOL).append("\n");

            sb.append(indent)
              .append(borderCol).append("┗").append("━".repeat(targetInnerWidth)).append("┛").append(RESET)
              .append(CLEAR_EOL);

            if (hintRows > 0) {
                sb.append("\n").append(CLEAR_EOL).append("\n");
            }
        }

        if (hintRows > 0) {
            sb.append(indent)
              .append(borderCol).append("┏").append("━".repeat(targetInnerWidth)).append("┓").append(RESET)
              .append(CLEAR_EOL).append("\n");

            for (int idx : hintIndices) {
                String cleanLine = rawLines[idx].replace(CLEAR_EOL, "");
                String trimmedClean = stripSpaces(cleanLine);
                int trimmedVisLen = visibleLength(trimmedClean);
                int leftPad = Math.max(0, (innerWidth - trimmedVisLen) / 2);
                int rightPad = Math.max(0, innerWidth - (leftPad + trimmedVisLen));

                sb.append(indent)
                  .append(borderCol).append("┃").append(RESET)
                  .append(" ")
                  .append(" ".repeat(leftPad))
                  .append(trimmedClean)
                  .append(" ".repeat(rightPad))
                  .append(" ")
                  .append(borderCol).append("┃").append(RESET)
                  .append(CLEAR_EOL).append("\n");
            }

            sb.append(indent)
              .append(borderCol).append("┗").append("━".repeat(targetInnerWidth)).append("┛").append(RESET)
              .append(CLEAR_EOL);
        }

        int bottomMargin = Math.max(0, termHeight - totalHeight - topMargin);
        for (int r = 0; r < bottomMargin; r++) {
            sb.append("\n").append(CLEAR_EOL);
        }

        return sb.toString();
    }
}
