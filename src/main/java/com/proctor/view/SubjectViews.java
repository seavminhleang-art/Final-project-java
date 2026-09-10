package com.proctor.view;

import com.proctor.model.entity.User;
import com.proctor.model.entity.Subject;
import com.proctor.util.TuiHelper;

import java.util.List;
import java.util.Set;

public class SubjectViews {

    public static String renderSubjectList(List<Subject> subjects, int selectedIndex,
                                          String searchBuffer, boolean searchMode, String bannerMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("SUBJECTS"));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle("Subject Management", String.format("Total: %d", subjects.size()))).append("\n\n");

        if (searchMode) {
            sb.append("  Search: [ ").append(TuiHelper.cyan(searchBuffer + "_")).append(" ] (Press Enter to finish)\n\n");
        } else if (!searchBuffer.isEmpty()) {
            sb.append("  Search: [ ").append(searchBuffer).append(" ] (Press '/' to edit)\n\n");
        }
        sb.append(String.format("  %-4s  %-16s  %-60s  %-9s%n", "ID", "CODE", "SUBJECT NAME", "STATUS")).append("\n");
        sb.append("  " + "─".repeat(95) + "\n\n");

        if (subjects.isEmpty()) {
            sb.append("  ").append(TuiHelper.dim("No subjects found.")).append("\n");
        } else {
            int pageSize = 5;
            int startRow = (selectedIndex / pageSize) * pageSize;
            int endRow = Math.min(subjects.size(), startRow + pageSize);

            for (int i = startRow; i < endRow; i++) {
                Subject s = subjects.get(i);
                String cursor = (i == selectedIndex) ? TuiHelper.cyan("▶ ") : "  ";
                String status = s.isEnabled() ? TuiHelper.green("Enabled") : TuiHelper.red("Disabled");

                String line = String.format("%-4d  %-16s  %-60s  %-9s",
                        s.getId(),
                        truncate(s.getCode(), 16),
                        truncate(s.getName(), 60),
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

        sb.append("\n  " + "─".repeat(95) + "\n\n");

        if (!subjects.isEmpty()) {
            int pageSize = 5;
            int totalPages = Math.max(1, (int) Math.ceil((double) subjects.size() / pageSize));
            int currentPage = selectedIndex / pageSize;
            sb.append(TuiHelper.paginationBar(currentPage, totalPages, subjects.size()));
        }

        if (!bannerMessage.isBlank()) {
            sb.append("  ").append(bannerMessage).append("\n\n");
        }

        List<String> hints = List.of(
                "[↑/↓] Move",
                "[←/→] Page",
                "[Enter] Edit",
                "[Space] Toggle Enabled",
                "[a] Assign Teachers",
                "[n] New",
                "[/] Search",
                "[Esc] Back"
        );
        sb.append(TuiHelper.wrapHints(hints, 90));
        return sb.toString();
    }

    public static String renderSubjectForm(boolean isEditMode, String subjectCode, String name, String description,
                                           boolean enabledStatus, int focusedField, int saveBtnIndex, int cancelBtnIndex,
                                           String errorMessage) {
        StringBuilder sb = new StringBuilder();
        String formTitle = isEditMode ? "Edit Subject: " + subjectCode : "Create New Subject";
        sb.append(TuiHelper.header("SUBJECTS"));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle(formTitle, "Configure subject details")).append("\n\n");

        if (isEditMode) {
            sb.append(TuiHelper.inputBox("Subject Name", name, focusedField == 0, 86, false, "enter subject name"));
            sb.append("\n");
            sb.append(TuiHelper.inputBox("Description", description, focusedField == 1, 86, false, "enter description"));
            sb.append("\n");
            String statusText = enabledStatus ? "Enabled" : "Disabled";
            sb.append(TuiHelper.selectBox("Subject Status", statusText, focusedField == 2, 86, "Space to toggle (Enabled/Disabled)"));
            sb.append("\n");
        } else {
            sb.append(TuiHelper.inputBox("Subject Code", subjectCode, focusedField == 0, 86, false, "e.g. JAVA, MATH, ENG"));
            sb.append("\n");
            sb.append(TuiHelper.inputBox("Subject Name", name, focusedField == 1, 86, false, "e.g. Java, Mathematics, English"));
            sb.append("\n");
            sb.append(TuiHelper.inputBox("Description", description, focusedField == 2, 86, false, "enter optional description"));
            sb.append("\n");
        }

        sb.append(TuiHelper.buttonRow("Submit", focusedField == saveBtnIndex, "Cancel", focusedField == cancelBtnIndex)).append("\n\n");

        if (!errorMessage.isBlank()) {
            sb.append("  ").append(TuiHelper.red("✖ " + errorMessage)).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [Tab/↑/↓] Switch Field  •  [Enter] Confirm / Submit  •  [Esc] Cancel\n"));
        return sb.toString();
    }

    public static String renderTeacherAssignment(Subject subject, List<User> allTeachers,
                                                 Set<Integer> assignedTeacherIds, int selectedIndex,
                                                 String bannerMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("SUBJECTS"));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle("Teacher Assignment", subject.getCode() + " - " + subject.getName())).append("\n\n");

        if (allTeachers.isEmpty()) {
            sb.append("  ").append(TuiHelper.dim("No teachers found in the system. Create teacher accounts first in User Management.")).append("\n\n");
        } else {
            sb.append("   " + TuiHelper.bold("Assigned Teachers for this Subject:") + "\n\n");
            sb.append("  " + "─".repeat(95) + "\n\n");

            int pageSize = 5;
            int startRow = (selectedIndex / pageSize) * pageSize;
            int endRow = Math.min(allTeachers.size(), startRow + pageSize);

            for (int i = startRow; i < endRow; i++) {
                User t = allTeachers.get(i);
                boolean isAssigned = assignedTeacherIds.contains(t.getId());
                String checkbox = isAssigned ? TuiHelper.green("[✔] Assigned  ") : TuiHelper.dim("[ ] Unassigned");
                String cursor = (i == selectedIndex) ? TuiHelper.cyan("▶ ") : "  ";

                String line = String.format("%s  %-30s  (%s)", checkbox, truncate(t.getFullName(), 30), t.getDisplayIdentifier());
                if (i == selectedIndex) {
                    sb.append(cursor).append(TuiHelper.bold(line)).append("\n");
                } else {
                    sb.append(cursor).append(line).append("\n");
                }
                if (i < endRow - 1) {
                    sb.append("\n");
                }
            }
            sb.append("\n  " + "─".repeat(95) + "\n\n");

            int totalPages = Math.max(1, (int) Math.ceil((double) allTeachers.size() / pageSize));
            int currentPage = selectedIndex / pageSize;
            sb.append(TuiHelper.paginationBar(currentPage, totalPages, allTeachers.size()));
        }

        if (!bannerMessage.isBlank()) {
            sb.append("  ").append(bannerMessage).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [↑/↓] Move  •  [←/→] Page  •  [Space/Enter] Toggle Assignment  •  [Esc] Back\n"));
        return sb.toString();
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}