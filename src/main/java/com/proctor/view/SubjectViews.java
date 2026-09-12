package com.proctor.view;

import com.proctor.model.entity.Subject;
import com.proctor.util.TuiHelper;

import java.util.List;

public class SubjectViews {

    public static String renderSubjectList(List<Subject> subjects, int selectedIndex,
                                          String searchBuffer, boolean searchMode, String bannerMessage) {
        return renderSubjectList(subjects, selectedIndex, "ALL", searchBuffer, searchMode, bannerMessage);
    }

    public static String renderSubjectList(List<Subject> subjects, int selectedIndex,
                                          String statusFilterDisplay,
                                          String searchBuffer, boolean searchMode, String bannerMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("SUBJECTS"));
        sb.append("\n");
        String statusLabel = (statusFilterDisplay == null || statusFilterDisplay.isBlank()) ? "ALL" : statusFilterDisplay;
        sb.append(TuiHelper.boxTitle("Subject Management",
                String.format("Status: [ %s ]  •  Total: %d", statusLabel, subjects.size()))).append("\n\n");

        if (searchMode) {
            sb.append("  Search: [ ").append(TuiHelper.cyan(searchBuffer + "_")).append(" ] (Press Enter to finish)\n\n");
        } else if (!searchBuffer.isEmpty()) {
            sb.append("  Search: [ ").append(searchBuffer).append(" ] (Press '/' to edit)\n\n");
        }
        sb.append(String.format("  %-4s  %-6s  %-16s  %-88s  %-9s%n", "#", "ID", "CODE", "SUBJECT NAME", "STATUS")).append("\n");
        sb.append("  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

        if (subjects.isEmpty()) {
            sb.append("  ").append(TuiHelper.dim("No subjects found.")).append("\n");
        } else {
            int pageSize = TuiHelper.PAGE_SIZE;
            int startRow = (selectedIndex / pageSize) * pageSize;
            int endRow = Math.min(subjects.size(), startRow + pageSize);

            for (int i = startRow; i < endRow; i++) {
                Subject s = subjects.get(i);
                String cursor = (i == selectedIndex) ? TuiHelper.cyan("▶ ") : "  ";
                String status = s.isEnabled() ? TuiHelper.green("Enabled") : TuiHelper.red("Disabled");

                String line = String.format("%-4d  %-6d  %-16s  %-88s  %-9s",
                        (i + 1),
                        s.getId(),
                        truncate(s.getCode(), 16),
                        truncate(s.getName(), 88),
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

        if (!subjects.isEmpty()) {
            int pageSize = TuiHelper.PAGE_SIZE;
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
                "[n] New",
                "[d] Delete",
                "[/] Search",
                "[f] Filter",
                "[Esc] Back"
        );
        sb.append(TuiHelper.wrapHints(hints));
        return sb.toString();
    }

    public static String renderSubjectForm(boolean isEditMode, String subjectCode, String name, String description,
                                           boolean enabledStatus, int focusedField, int saveBtnIndex,
                                           int deleteBtnIndex, int cancelBtnIndex,
                                           String errorMessage) {
        StringBuilder sb = new StringBuilder();
        String formTitle = isEditMode ? "Edit Subject: " + subjectCode : "Create New Subject";
        sb.append(TuiHelper.header("SUBJECTS"));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle(formTitle, "Configure subject details")).append("\n\n");

        if (isEditMode) {
            sb.append(TuiHelper.inputBox("Subject Code", subjectCode, focusedField == 0, 102, false, "e.g. JAVA, MATH, ENG"));
            sb.append("\n");
            sb.append(TuiHelper.inputBox("Subject Name", name, focusedField == 1, 102, false, "enter subject name"));
            sb.append("\n");
            sb.append(TuiHelper.inputBox("Description", description, focusedField == 2, 102, false, "enter description"));
            sb.append("\n");
            String statusText = enabledStatus ? "Enabled" : "Disabled";
            sb.append(TuiHelper.selectBox("Subject Status", statusText, focusedField == 3, 102, "Space to toggle (Enabled/Disabled)"));
            sb.append("\n");
            sb.append(TuiHelper.buttonRow("Submit", focusedField == saveBtnIndex, "Delete", focusedField == deleteBtnIndex, "Cancel", focusedField == cancelBtnIndex)).append("\n\n");
        } else {
            sb.append(TuiHelper.inputBox("Subject Code", subjectCode, focusedField == 0, 102, false, "e.g. JAVA, MATH, ENG"));
            sb.append("\n");
            sb.append(TuiHelper.inputBox("Subject Name", name, focusedField == 1, 102, false, "e.g. Java, Mathematics, English"));
            sb.append("\n");
            sb.append(TuiHelper.inputBox("Description", description, focusedField == 2, 102, false, "enter optional description"));
            sb.append("\n");
            sb.append(TuiHelper.buttonRow("Submit", focusedField == saveBtnIndex, "Cancel", focusedField == cancelBtnIndex)).append("\n\n");
        }

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