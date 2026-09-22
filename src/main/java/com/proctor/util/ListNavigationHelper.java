package com.proctor.util;

import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

public final class ListNavigationHelper {

    private ListNavigationHelper() {
    }

    public static int clampIndex(int index, int size) {
        if (size <= 0) {
            return 0;
        }
        if (index < 0) {
            return 0;
        }
        if (index >= size) {
            return size - 1;
        }
        return index;
    }

    public static int adjustIndex(int currentIndex, int delta, int listSize) {
        if (listSize <= 0) {
            return 0;
        }
        return (currentIndex + delta + listSize) % listSize;
    }

    public static int prevPage(int currentIndex, int pageSize) {
        int currentPage = currentIndex / pageSize;
        return Math.max(0, (currentPage - 1) * pageSize);
    }

    public static int nextPage(int currentIndex, int listSize, int pageSize) {
        if (listSize <= 0) {
            return 0;
        }
        int totalPages = Math.max(1, (int) Math.ceil((double) listSize / pageSize));
        int currentPage = currentIndex / pageSize;
        if (currentPage < totalPages - 1) {
            return Math.min(listSize - 1, (currentPage + 1) * pageSize);
        }
        return currentIndex;
    }

    public static int handleWheel(Message msg, int selectedIndex, int listSize) {
        if (MouseUtil.isWheelUp(msg)) {
            return Math.max(0, selectedIndex - 1);
        }
        if (MouseUtil.isWheelDown(msg)) {
            return listSize > 0 ? Math.min(listSize - 1, selectedIndex + 1) : 0;
        }
        return selectedIndex;
    }

    public static boolean handleSearchKey(KeyPressMessage k, StringBuilder buffer, Runnable onChange) {
        if (KeyUtil.isEnter(k) || KeyUtil.isEsc(k)) {
            onChange.run();
            return false;
        }
        if (KeyUtil.isBackspace(k)) {
            if (!buffer.isEmpty()) {
                buffer.deleteCharAt(buffer.length() - 1);
                onChange.run();
            }
            return true;
        }
        if (k.type() == KeyType.KeyRunes && k.runes() != null) {
            for (char c : k.runes()) {
                if (!Character.isISOControl(c)) {
                    buffer.append(c);
                }
            }
            onChange.run();
            return true;
        }
        if (k.key() != null && k.key().length() == 1 && !Character.isISOControl(k.key().charAt(0))) {
            buffer.append(k.key());
            onChange.run();
            return true;
        }
        return true;
    }

    public static int getClickedItemIndex(int line, int itemsStartLine, int listSize, int currentIndex, int pageSize) {
        if (itemsStartLine == -1 || listSize <= 0) {
            return -1;
        }
        int currentPage = currentIndex / pageSize;
        int startRow = currentPage * pageSize;
        int endRow = Math.min(listSize, startRow + pageSize);
        int displayedRows = endRow - startRow;
        if (line >= itemsStartLine && line < itemsStartLine + displayedRows * 2) {
            int clickedOffset = (line - itemsStartLine) / 2;
            int targetIdx = startRow + clickedOffset;
            if (targetIdx < listSize) {
                return targetIdx;
            }
        }
        return -1;
    }

    public static int handlePaginationClick(int col, int currentIndex, int listSize, int pageSize) {
        if (listSize <= 0) {
            return 0;
        }
        int totalPages = Math.max(1, (int) Math.ceil((double) listSize / pageSize));
        int currentPage = currentIndex / pageSize;
        int action = MouseUtil.getClickedPaginationAction(col, currentPage, totalPages);
        if (action < 0) {
            return Math.max(0, (currentPage - 1) * pageSize);
        }
        if (action > 0) {
            return Math.min(listSize - 1, (currentPage + 1) * pageSize);
        }
        return currentIndex;
    }

    public static int handleConfirmationClick(Message msg, String renderedView, String confirmLabel, String cancelLabel) {
        int line = MouseUtil.getLineIndex(msg);
        int col = MouseUtil.getColInLine(msg);
        int btnLine = MouseUtil.findButtonRowLine(renderedView);
        if (btnLine != -1 && line >= btnLine && line <= btnLine + 2) {
            return MouseUtil.getClickedButtonIndex(col, confirmLabel, cancelLabel);
        }
        if (btnLine != -1 && (line < btnLine - 4 || line > btnLine + 4)) {
            return 1;
        }
        return -1;
    }
}
