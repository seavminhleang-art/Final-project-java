package com.proctor.view;

import com.proctor.model.entity.InboxMessage;
import com.proctor.model.enums.InboxMessageType;
import com.proctor.model.enums.InboxStatus;
import com.proctor.util.TuiHelper;

import java.text.SimpleDateFormat;
import java.util.List;

public class InboxViews {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public static String renderInboxList(List<InboxMessage> messages, int selectedIndex, int unreadCount,
                                         String bannerMessage, boolean showDeleteModal, boolean deleteConfirmFocused) {
        return renderInboxList(messages, selectedIndex, unreadCount, "ALL", "", false, bannerMessage, showDeleteModal, deleteConfirmFocused);
    }

    public static String renderInboxList(List<InboxMessage> messages, int selectedIndex, int unreadCount,
                                         String filterStatusDisplay, String searchBuffer, boolean searchMode,
                                         String bannerMessage, boolean showDeleteModal, boolean deleteConfirmFocused) {
        StringBuilder sb = new StringBuilder();
        String filterLabel = (filterStatusDisplay == null || filterStatusDisplay.isBlank()) ? "ALL" : filterStatusDisplay;
        int activeTab = switch (filterLabel.toUpperCase()) {
            case "UNREAD" -> 1;
            case "ACTIONABLE" -> 2;
            default -> 0;
        };
        String subtitle = String.format("Total: %d  •  Unread: %d", messages.size(), unreadCount);
        sb.append(TuiHelper.header("INBOX"));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle("Inbox & Notifications", subtitle)).append("\n\n");
        sb.append(TuiHelper.tabBar(new String[]{"All", "Unread", "Actionable"}, activeTab)).append("\n\n");

        if (searchMode) {
            sb.append("  Search: [ ").append(TuiHelper.cyan(searchBuffer + "_")).append(" ] (Press Enter to finish)\n\n");
        } else if (searchBuffer != null && !searchBuffer.isEmpty()) {
            sb.append("  Search: [ ").append(searchBuffer).append(" ] (Press '/' to edit)\n\n");
        }

        sb.append(String.format("    %-4s  %-14s  %-16s  %-22s  %-46s  %-16s%n",
                "#", "STATUS", "TYPE", "FROM", "SUBJECT", "RECEIVED")).append("\n");
        sb.append("  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

        if (messages.isEmpty()) {
            sb.append("  ").append(TuiHelper.dim("Your inbox is empty.")).append("\n");
        } else {
            int pageSize = TuiHelper.PAGE_SIZE;
            int startRow = (selectedIndex / pageSize) * pageSize;
            int endRow = Math.min(messages.size(), startRow + pageSize);

            for (int i = startRow; i < endRow; i++) {
                InboxMessage msg = messages.get(i);
                String cursor = (i == selectedIndex) ? TuiHelper.cyan("▶ ") : "  ";
                String unreadDot = !msg.isRead() ? TuiHelper.cyan("● ") : "  ";

                String statusBadge = formatStatusBadge(msg.getStatus(), !msg.isRead());
                String typeBadge = formatTypeBadge(msg.getType());
                String sender = msg.getSenderName() != null ? truncate(msg.getSenderName(), 22) : "System";
                String title = truncate(msg.getTitle(), 46);
                String dateStr = msg.getCreatedAt() != null ? DATE_FMT.format(msg.getCreatedAt()) : "-";

                String line = String.format("%-4d  %s  %-16s  %-22s  %-46s  %-16s",
                        (i + 1),
                        statusBadge,
                        typeBadge,
                        sender,
                        title,
                        dateStr);

                if (i == selectedIndex) {
                    sb.append(cursor).append(unreadDot).append(TuiHelper.bold(line)).append("\n");
                } else {
                    sb.append(cursor).append(unreadDot).append(line).append("\n");
                }
                if (i < endRow - 1) {
                    sb.append("\n");
                }
            }
        }

        sb.append("\n  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

        if (!messages.isEmpty()) {
            int pageSize = TuiHelper.PAGE_SIZE;
            int totalPages = Math.max(1, (int) Math.ceil((double) messages.size() / pageSize));
            int currentPage = selectedIndex / pageSize;
            sb.append(TuiHelper.paginationBar(currentPage, totalPages, messages.size()));
        }

        if (!bannerMessage.isBlank()) {
            sb.append("  ").append(bannerMessage).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [↑/↓] Move  •  [←/→] Page  •  [/] Search  •  [Tab] Tab  •  [Enter] Open  •  [d] Delete  •  [m] Mark All Read  •  [Esc] Back\n"));

        if (showDeleteModal) {
            sb.append("\n");
            sb.append(TuiHelper.confirmationModal(
                    "DELETE MESSAGE",
                    "Are you sure you want to delete this message?",
                    "This message will be permanently removed from your inbox.",
                    "Delete",
                    "Cancel",
                    deleteConfirmFocused
            ));
        }

        return sb.toString();
    }

    public static String renderInboxDetail(InboxMessage msg, boolean showDeleteModal, boolean deleteConfirmFocused,
                                           int focusedActionBtn, String errorMessage, String bannerMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("INBOX"));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle("Message Details", "Type: " + msg.getType().name())).append("\n\n");

        String sender = msg.getSenderName() != null ? msg.getSenderName() : "System";
        String dateStr = msg.getCreatedAt() != null ? DATE_FMT.format(msg.getCreatedAt()) : "-";

        sb.append("  ").append(TuiHelper.bold("Subject:  ")).append(msg.getTitle()).append("\n\n");
        sb.append("  ").append(TuiHelper.bold("From:     ")).append(sender);
        sb.append("   ").append(TuiHelper.bold("Received: ")).append(dateStr);
        sb.append("   ").append(TuiHelper.bold("Status:   ")).append(formatStatusBadge(msg.getStatus(), false)).append("\n\n");
        sb.append("  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

        for (String line : msg.getBody().split("\n")) {
            if (line.startsWith("[HASH:")) continue;
            sb.append("  ").append(line).append("\n");
        }
        sb.append("\n  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

        if (msg.isActionable()) {
            sb.append(TuiHelper.buttonRow("Approve Request", focusedActionBtn == 0, "Reject Request", focusedActionBtn == 1));
            sb.append("\n\n");
        }

        if (!errorMessage.isBlank()) {
            sb.append("  ").append(TuiHelper.red("✖ " + errorMessage)).append("\n\n");
        }
        if (!bannerMessage.isBlank()) {
            sb.append("  ").append(bannerMessage).append("\n\n");
        }

        if (msg.isActionable()) {
            sb.append(TuiHelper.dim("  [←/→] Switch Action  •  [Enter] Confirm Action  •  [d] Delete  •  [Esc] Back\n"));
        } else {
            sb.append(TuiHelper.dim("  [d] Delete Message  •  [Esc] Back\n"));
        }

        if (showDeleteModal) {
            sb.append("\n");
            sb.append(TuiHelper.confirmationModal(
                    "DELETE MESSAGE",
                    "Are you sure you want to delete this message?",
                    "This message will be permanently removed from your inbox.",
                    "Delete",
                    "Cancel",
                    deleteConfirmFocused
            ));
        }

        return sb.toString();
    }

    private static String formatStatusBadge(InboxStatus status, boolean unread) {
        if (status == InboxStatus.PENDING) {
            return TuiHelper.yellow(String.format("%-14s", "[PENDING]"));
        } else if (status == InboxStatus.APPROVED) {
            return TuiHelper.green(String.format("%-14s", "[APPROVED]"));
        } else if (status == InboxStatus.REJECTED) {
            return TuiHelper.red(String.format("%-14s", "[REJECTED]"));
        } else if (status == InboxStatus.RESOLVED) {
            return TuiHelper.cyan(String.format("%-14s", "[RESOLVED]"));
        } else {
            return unread ? TuiHelper.cyan(String.format("%-14s", "[NEW]")) : TuiHelper.dim(String.format("%-14s", "[READ]"));
        }
    }

    private static String formatTypeBadge(InboxMessageType type) {
        return switch (type) {
            case PASSWORD_RESET -> "[PASSWORD]";
            case QUIZ_RETAKE -> "[QUIZ RETAKE]";
            case EXAM_RETAKE -> "[EXAM RETAKE]";
            case NOTIFICATION -> "[NOTICE]";
        };
    }

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() > maxLen ? text.substring(0, maxLen - 1) + "…" : text;
    }
}