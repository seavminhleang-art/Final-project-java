package view;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class ConsoleUI {
    private static final Scanner SCANNER = new Scanner(System.in);

    private ConsoleUI() {
    }

    public static void println(String s) {
        System.out.println(s);
    }

    public static void print(String s) {
        System.out.print(s);
    }

    public static void banner(String title) {
        String line = "=".repeat(Math.max(40, title.length() + 8));
        System.out.println("\n" + line);
        System.out.println("  " + title);
        System.out.println(line);
    }

    public static void error(String s) {
        System.out.println("[!] " + s);
    }

    public static void success(String s) {
        System.out.println("[OK] " + s);
    }

    public static String prompt(String label) {
        System.out.print(label + ": ");
        return SCANNER.nextLine().trim();
    }

    public static String promptPassword(String label) {
        // Console masking requires java.io.Console (unavailable in some IDE/redirected environments),
        // so we fall back to a plain (unmasked) read for portability.
        java.io.Console console = System.console();
        if (console != null) {
            char[] pwd = console.readPassword(label + ": ");
            return new String(pwd);
        }
        System.out.print(label + ": ");
        return SCANNER.nextLine().trim();
    }

    public static int promptInt(String label) {
        while (true) {
            String raw = prompt(label);
            try {
                return Integer.parseInt(raw.trim());
            } catch (NumberFormatException e) {
                error("Please enter a whole number.");
            }
        }
    }

    public static Long promptLong(String label) {
        while (true) {
            String raw = prompt(label);
            try {
                return Long.parseLong(raw.trim());
            } catch (NumberFormatException e) {
                error("Please enter a valid ID number.");
            }
        }
    }

    public static BigDecimal promptDecimal(String label) {
        while (true) {
            String raw = prompt(label);
            try {
                return new BigDecimal(raw.trim());
            } catch (NumberFormatException e) {
                error("Please enter a valid number.");
            }
        }
    }

    public static char promptChar(String label, String allowed) {
        while (true) {
            String raw = prompt(label).toUpperCase();
            if (raw.length() == 1 && allowed.indexOf(raw.charAt(0)) >= 0) {
                return raw.charAt(0);
            }
            error("Please enter one of: " + allowed);
        }
    }

    public static boolean promptYesNo(String label) {
        while (true) {
            String raw = prompt(label + " (y/n)").toLowerCase();
            if (raw.equals("y") || raw.equals("yes")) return true;
            if (raw.equals("n") || raw.equals("no")) return false;
            error("Please answer y or n.");
        }
    }

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** Prompts for an optional date/time ("yyyy-MM-dd HH:mm"). Blank input returns null. */
    public static LocalDateTime promptDateTimeOptional(String label) {
        while (true) {
            String raw = prompt(label + " (yyyy-MM-dd HH:mm, blank = none)");
            if (raw.isBlank()) return null;
            try {
                return LocalDateTime.parse(raw.trim(), DATE_TIME_FORMAT);
            } catch (DateTimeParseException e) {
                error("Please use the format yyyy-MM-dd HH:mm (e.g. 2026-09-01 09:00), or leave blank.");
            }
        }
    }

    public static String formatDateTime(LocalDateTime dt) {
        return dt == null ? "any time" : dt.format(DATE_TIME_FORMAT);
    }

    public static void pause() {
        System.out.print("\nPress Enter to continue...");
        SCANNER.nextLine();
    }
}