package com.proctor.util;

import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

public class KeyUtil {
    public static boolean isEnter(KeyPressMessage k) {
        return "enter".equalsIgnoreCase(k.key()) || k.type() == KeyType.keyCR || k.type() == KeyType.keyLF;
    }

    public static boolean isTab(KeyPressMessage k) {
        return "tab".equalsIgnoreCase(k.key()) || k.type() == KeyType.keyHT;
    }

    public static boolean isBackspace(KeyPressMessage k) {
        return "backspace".equalsIgnoreCase(k.key()) || k.type() == KeyType.keyBS || k.type() == KeyType.keyDEL;
    }

    public static boolean isEsc(KeyPressMessage k) {
        return "esc".equalsIgnoreCase(k.key()) || k.type() == KeyType.keyESC;
    }

    public static boolean isUp(KeyPressMessage k) {
        return "up".equalsIgnoreCase(k.key()) || k.type() == KeyType.KeyUp;
    }

    public static boolean isDown(KeyPressMessage k) {
        return "down".equalsIgnoreCase(k.key()) || k.type() == KeyType.KeyDown;
    }

    public static boolean isLeft(KeyPressMessage k) {
        return "left".equalsIgnoreCase(k.key()) || k.type() == KeyType.KeyLeft;
    }

    public static boolean isRight(KeyPressMessage k) {
        return "right".equalsIgnoreCase(k.key()) || k.type() == KeyType.KeyRight;
    }

    public static boolean isSpace(KeyPressMessage k) {
        return "space".equalsIgnoreCase(k.key()) || " ".equals(k.key()) || k.type() == KeyType.KeySpace
                || (k.runes() != null && k.runes().length == 1 && k.runes()[0] == ' ');
    }

    public static boolean isDelete(KeyPressMessage k) {
        return "delete".equalsIgnoreCase(k.key()) || k.type() == KeyType.KeyDelete;
    }

    public static boolean handleBackspace(StringBuilder buffer, KeyPressMessage k) {
        if (isBackspace(k) && !buffer.isEmpty()) {
            buffer.deleteCharAt(buffer.length() - 1);
            return true;
        }
        return false;
    }

    public static boolean appendInput(StringBuilder buffer, KeyPressMessage k) {
        return appendInput(buffer, k, Integer.MAX_VALUE);
    }

    public static boolean appendInput(StringBuilder buffer, KeyPressMessage k, int maxLen) {
        if (isSpace(k)) {
            if (buffer.length() < maxLen) {
                buffer.append(' ');
                return true;
            }
            return false;
        }
        if (k.type() == KeyType.KeyRunes && k.runes() != null) {
            boolean added = false;
            for (char c : k.runes()) {
                if (!Character.isISOControl(c) && buffer.length() < maxLen) {
                    buffer.append(c);
                    added = true;
                }
            }
            return added;
        }
        if (k.key() != null && k.key().length() == 1 && !Character.isISOControl(k.key().charAt(0))) {
            if (buffer.length() < maxLen) {
                buffer.append(k.key());
                return true;
            }
        }
        return false;
    }

    public static int getDigit(KeyPressMessage k) {
        if (k == null) return -1;
        if (k.type() == KeyType.KeyRunes && k.runes() != null && k.runes().length > 0) {
            char c = k.runes()[0];
            if (c >= '0' && c <= '9') {
                return c - '0';
            }
        }
        if (k.key() != null && k.key().length() == 1) {
            char c = k.key().charAt(0);
            if (c >= '0' && c <= '9') {
                return c - '0';
            }
        }
        return -1;
    }
    /**
     * Pastes text into a buffer, stripping control characters (keeping spaces).
     * No length limit.
     *
     * @param buffer the target StringBuilder
     * @param text   the pasted text (may be null - treated as empty)
     */
    public static void pasteToBuffer(StringBuilder buffer, String text) {
        pasteToBuffer(buffer, text, Integer.MAX_VALUE);
    }

    /**
     * Pastes text into a buffer, stripping control characters (keeping spaces),
     * up to {@code maxLen} total characters.
     *
     * @param buffer the target StringBuilder
     * @param text   the pasted text (may be null - treated as empty)
     * @param maxLen maximum allowed buffer length after paste
     */
    public static void pasteToBuffer(StringBuilder buffer, String text, int maxLen) {
        if (text == null || text.isEmpty()) return;
        for (int i = 0; i < text.length() && buffer.length() < maxLen; i++) {
            char c = text.charAt(i);
            if (c == ' ' || !Character.isISOControl(c)) {
                buffer.append(c);
            }
        }
    }
}
