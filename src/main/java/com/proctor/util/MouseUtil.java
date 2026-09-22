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
}
