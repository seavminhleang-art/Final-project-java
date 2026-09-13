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
}