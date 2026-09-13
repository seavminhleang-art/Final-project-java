package com.proctor.view;

import com.proctor.model.entity.User;
import com.proctor.model.enums.Role;
import com.proctor.util.TuiHelper;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class UserViews {

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static String renderUserList(List<User> users, int selectedIndex, Role filterRole,
                                       String searchBuffer, boolean searchMode, String bannerMessage) {
        StringBuilder sb = new StringBuilder();
        int activeTab = 0;
        if (filterRole == Role.STUDENT) activeTab = 1;
        else if (filterRole == Role.TEACHER) activeTab = 2;
        else if (filterRole == Role.ADMIN) activeTab = 3;

        sb.append(TuiHelper.header("USERS"));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle("User Management", String.format("Total Users: %d", users.size()))).append("\n\n");
        sb.append(TuiHelper.tabBar(new String[]{"All Roles", "Students", "Teachers", "Admins"}, activeTab)).append("\n\n");

        if (searchMode) {
            sb.append("  Search: [ ").append(TuiHelper.cyan(searchBuffer + "_")).append(" ] (Press Enter to finish)\n\n");
        } else if (!searchBuffer.isEmpty()) {
            sb.append("  Search: [ ").append(searchBuffer).append(" ] (Press '/' to edit)\n\n");
        }
        sb.append(String.format("  %-4s  %-6s  %-18s  %-28s  %-20s  %-8s  %-12s  %-10s  %-9s%n",
                "#", "ID", "USERNAME", "EMAIL", "FULL NAME", "GENDER", "BIRTHDAY", "ROLE", "STATUS")).append("\n");
        sb.append("  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

        if (users.isEmpty()) {
            sb.append("  ").append(TuiHelper.dim("No users found matching search/filter criteria.")).append("\n");
        } else {
            int pageSize = TuiHelper.PAGE_SIZE;
            int startRow = (selectedIndex / pageSize) * pageSize;
            int endRow = Math.min(users.size(), startRow + pageSize);

            for (int i = startRow; i < endRow; i++) {
                User u = users.get(i);
                String cursor = (i == selectedIndex) ? TuiHelper.cyan("▶ ") : "  ";
                String status = u.isEnabled() ? TuiHelper.green("Enabled") : TuiHelper.red("Disabled");
                String dobStr = (u.getDateOfBirth() != null) ? u.getDateOfBirth().format(DISPLAY_FMT) : "-";
                String genderStr = (u.getGender() != null && !u.getGender().isBlank()) ? u.getGender() : "-";
                String displayName = (u.getRole() == Role.TEACHER)
                        ? u.getDisplayNameWithHonorific()
                        : (u.getFullName() != null ? u.getFullName() : "-");

                String line = String.format("%-4d  %-6d  %-18s  %-28s  %-20s  %-8s  %-12s  %-10s  %-9s",
                        (i + 1),
                        u.getId(),
                        truncate("@" + u.getUsername(), 18),
                        truncate(u.getEmail() != null ? u.getEmail() : "-", 28),
                        truncate(displayName, 20),
                        truncate(genderStr, 8),
                        dobStr,
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

        sb.append("\n  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

        if (!users.isEmpty()) {
            int pageSize = TuiHelper.PAGE_SIZE;
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
                "[f] Tab",
                "[/] Search",
                "[Esc] Back"
        );
        sb.append(TuiHelper.wrapHints(hints));
        return sb.toString();
    }

    public static String renderUserForm(boolean isEditMode, User userToEdit, String email, String username,
                                        String password, String fullName, String birthday, String gender,
                                        Role selectedRole, boolean enabledStatus, int focusedField,
                                        int saveBtnIndex, int cancelBtnIndex, String errorMessage) {
        StringBuilder sb = new StringBuilder();
        String boxSub = isEditMode ? "Update user account" : "Create user account";
        String formTitle = isEditMode ? "Edit User: @" + userToEdit.getUsername() : "Create New User";
        sb.append(TuiHelper.header("USERS"));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle(formTitle, boxSub)).append("\n\n");

        String genderVal = (gender != null && !gender.isBlank()) ? gender : "Male";

        if (isEditMode) {
            // Edit: 0=fullName, 1=gender, 2=birthday, 3=role, 4=status
            sb.append(TuiHelper.inputBox("Full Name", fullName, focusedField == 0, 102, false, "enter full name"));
            sb.append("\n");
            sb.append(TuiHelper.selectBox("Gender", genderVal, focusedField == 1, 102, "Space to cycle"));
            sb.append("\n");
            String bdDisplay = TuiHelper.birthdayMask(birthday, focusedField == 2);
            sb.append(TuiHelper.inputBox("Date of Birth", bdDisplay, focusedField == 2, 102, false, "DD - MM - YYYY"));
            sb.append("\n");
            sb.append(TuiHelper.selectBox("Role", selectedRole.name(), focusedField == 3, 102, "Space to cycle"));
            sb.append("\n");
            String statusText = enabledStatus ? "Enabled" : "Disabled";
            sb.append(TuiHelper.selectBox("Account Status", statusText, focusedField == 4, 102, "Space to toggle (Enabled/Disabled)"));
            sb.append("\n");
        } else {
            // Create: 0=fullName, 1=gender, 2=birthday, 3=email, 4=username, 5=password, 6=role
            sb.append(TuiHelper.inputBox("Full Name", fullName, focusedField == 0, 102, false, "enter full name"));
            sb.append("\n");
            sb.append(TuiHelper.selectBox("Gender", genderVal, focusedField == 1, 102, "Space to cycle"));
            sb.append("\n");
            String bdDisplay = TuiHelper.birthdayMask(birthday, focusedField == 2);
            sb.append(TuiHelper.inputBox("Date of Birth", bdDisplay, focusedField == 2, 102, false, "DD - MM - YYYY"));
            sb.append("\n");
            sb.append(TuiHelper.inputBox("Email Address", email, focusedField == 3, 102, false, "e.g. user@proctor.edu"));
            sb.append("\n");
            sb.append(TuiHelper.inputBox("Username", username, focusedField == 4, 102, false, "e.g. jdoe"));
            sb.append("\n");
            sb.append(TuiHelper.inputBox("Password", password, focusedField == 5, 102, true, "enter password"));
            sb.append("\n");
            sb.append(TuiHelper.selectBox("Role", selectedRole.name(), focusedField == 6, 102, "Space to cycle"));
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
