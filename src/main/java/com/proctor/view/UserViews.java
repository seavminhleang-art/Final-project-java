package com.proctor.view;

import com.proctor.model.entity.User;
import com.proctor.model.enums.Role;
import com.proctor.util.TuiHelper;

import java.util.List;

public class UserViews {

    public static String renderUserList(List<User> users, int selectedIndex, Role filterRole,
                                       String searchBuffer, boolean searchMode, String bannerMessage) {
        StringBuilder sb = new StringBuilder();
        String roleLabel = (filterRole == null) ? "ALL ROLES" : filterRole.name();
        sb.append(TuiHelper.header("USER MANAGEMENT", String.format("Filter: [ %s ]  •  Total Users: %d", roleLabel, users.size())));
        sb.append("\n");

        if (searchMode) {
            sb.append("  Search: [ ").append(TuiHelper.cyan(searchBuffer + "_")).append(" ] (Press Enter to finish)\n\n");
        } else if (!searchBuffer.isEmpty()) {
            sb.append("  Search: [ ").append(searchBuffer).append(" ] (Press '/' to edit)\n\n");
        }
        sb.append(String.format("  %-4s  %-16s  %-26s  %-22s  %-9s  %-9s%n", "ID", "USERNAME", "EMAIL", "FULL NAME", "ROLE", "STATUS")).append("\n");
        sb.append("  " + "─".repeat(96) + "\n\n");

        if (users.isEmpty()) {
            sb.append("  ").append(TuiHelper.dim("No users found matching search/filter criteria.")).append("\n");
        } else {
            int pageSize = 5;
            int startRow = (selectedIndex / pageSize) * pageSize;
            int endRow = Math.min(users.size(), startRow + pageSize);

            for (int i = startRow; i < endRow; i++) {
                User u = users.get(i);
                String cursor = (i == selectedIndex) ? TuiHelper.cyan("▶ ") : "  ";
                String status = u.isEnabled() ? TuiHelper.green("Enabled") : TuiHelper.red("Disabled");

                String line = String.format("%-4d  %-16s  %-26s  %-22s  %-9s  %-9s",
                        u.getId(),
                        truncate("@" + u.getUsername(), 16),
                        truncate(u.getEmail() != null ? u.getEmail() : "-", 26),
                        truncate(u.getFullName(), 22),
                        u.getRole().name(),
                        status);

                if (i == selectedIndex) {
                    sb.append(cursor).append(TuiHelper.bold(line)).append("\n");
                } else {
                    sb.append(cursor).append(line).append("\n");
                }
                if (i < endRow - 1) {
                    sb.append("\n");
                }
            }
        }

        sb.append("\n  " + "─".repeat(96) + "\n\n");

        if (!users.isEmpty()) {
            int pageSize = 5;
            int totalPages = Math.max(1, (int) Math.ceil((double) users.size() / pageSize));
            int currentPage = selectedIndex / pageSize;
            sb.append(TuiHelper.paginationBar(currentPage, totalPages, users.size()));
        }

        if (!bannerMessage.isBlank()) {
            sb.append("  ").append(bannerMessage).append("\n\n");
        }

        List<String> hints = List.of(
                "[↑/↓] Move",
                "[←/→] Page",
                "[Enter] Edit",
                "[Space] Toggle Enabled",
                "[n] New",
                "[r] Reset Pass",
                "[f] Filter",
                "[/] Search",
                "[Esc] Back"
        );
        sb.append(TuiHelper.wrapHints(hints, 90));
        return sb.toString();
    }

    public static String renderUserForm(boolean isEditMode, User userToEdit, String email, String username,
                                        String password, String fullName, Role selectedRole, boolean enabledStatus,
                                        int focusedField, int saveBtnIndex, int cancelBtnIndex, String errorMessage) {
        StringBuilder sb = new StringBuilder();
        String title = isEditMode ? "EDIT USER: @" + userToEdit.getUsername() : "CREATE NEW USER";
        sb.append(TuiHelper.header(title, "Fill in user credentials and role"));
        sb.append("\n");

        if (isEditMode) {
            sb.append(TuiHelper.inputBox("Full Name", fullName, focusedField == 0, 86, false, "enter full name"));
            sb.append("\n");
            sb.append(TuiHelper.selectBox("Role", selectedRole.name(), focusedField == 1, 86, "Space to cycle"));
            sb.append("\n");
            String statusText = enabledStatus ? "Enabled" : "Disabled";
            sb.append(TuiHelper.selectBox("Account Status", statusText, focusedField == 2, 86, "Space to toggle (Enabled/Disabled)"));
            sb.append("\n");
        } else {
            sb.append(TuiHelper.inputBox("Email Address", email, focusedField == 0, 86, false, "e.g. user@proctor.edu"));
            sb.append("\n");
            sb.append(TuiHelper.inputBox("Username", username, focusedField == 1, 86, false, "e.g. jdoe"));
            sb.append("\n");
            sb.append(TuiHelper.inputBox("Password", password, focusedField == 2, 86, true, "enter password"));
            sb.append("\n");
            sb.append(TuiHelper.inputBox("Full Name", fullName, focusedField == 3, 86, false, "enter full name"));
            sb.append("\n");
            sb.append(TuiHelper.selectBox("Role", selectedRole.name(), focusedField == 4, 86, "Space to cycle"));
            sb.append("\n");
        }

        sb.append(TuiHelper.buttonRow("Submit", focusedField == saveBtnIndex, "Cancel", focusedField == cancelBtnIndex)).append("\n\n");

        if (!errorMessage.isBlank()) {
            sb.append("  ").append(TuiHelper.red("✖ " + errorMessage)).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [Tab/↑/↓] Switch Field  •  [Enter] Confirm / Submit  •  [Esc] Cancel\n"));
        return sb.toString();
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}