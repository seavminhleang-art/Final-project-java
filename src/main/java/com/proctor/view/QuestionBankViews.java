package com.proctor.view;

import com.proctor.model.enums.Difficulty;
import com.proctor.model.enums.QuestionType;
import com.proctor.model.entity.Question;
import com.proctor.model.entity.Quiz;
import com.proctor.util.TuiHelper;

import java.util.List;
import java.util.Set;

public class QuestionBankViews {

    public static String renderBankList(List<Question> questions, int selectedIndex,
                                        String subjectFilterDisplay, QuestionType typeFilter,
                                        Difficulty diffFilter, String searchBuffer,
                                        boolean searchMode, String bannerMessage) {
        StringBuilder sb = new StringBuilder();
        String tf = (typeFilter == null) ? "ALL" : typeFilter.name();
        String df = (diffFilter == null) ? "ALL" : diffFilter.name();
        String subjLabel = (subjectFilterDisplay == null || subjectFilterDisplay.isBlank()) ? "[ALL]" : subjectFilterDisplay;
        if (!subjLabel.startsWith("[")) {
            subjLabel = "[" + subjLabel + "]";
        }
        sb.append(TuiHelper.header("QUESTION BANK"));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle("My Question Bank",
                String.format("Subj: %s  Type: [%s]  Diff: [%s]  Total: %d",
                        subjLabel, tf, df, questions.size()))).append("\n\n");

        if (searchMode) {
            sb.append("  Search: [ ").append(TuiHelper.cyan(TuiHelper.truncate(searchBuffer, 50) + "_")).append(" ] (Press Enter to finish)\n\n");
        } else if (!searchBuffer.isEmpty()) {
            sb.append("  Search: [ ").append(TuiHelper.truncate(searchBuffer, 50)).append(" ] (Press '/' to edit)\n\n");
        }

        sb.append(String.format("  %-4s  %-10s  %-12s  %-8s  %-5s  %-4s  %-75s%n",
                "#", "SUBJ", "TYPE", "DIFF", "PTS", "AI?", "QUESTION TEXT")).append("\n");
        sb.append("  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

        if (questions.isEmpty()) {
            sb.append("  ").append(TuiHelper.dim("No bank questions found. Press [n] to create one or [g] to generate with AI.")).append("\n");
        } else {
            int pageSize = TuiHelper.PAGE_SIZE;
            int startRow = (selectedIndex / pageSize) * pageSize;
            int endRow = Math.min(questions.size(), startRow + pageSize);

            for (int i = startRow; i < endRow; i++) {
                Question q = questions.get(i);
                String cursor = (i == selectedIndex) ? TuiHelper.cyan("▶ ") : "  ";
                String subj = q.getSubjectCode() != null ? q.getSubjectCode() : "-";
                String aiMark = q.isAiGenerated() ? TuiHelper.cyan("AI") : TuiHelper.dim("--");

                String line = String.format("%-4d  %-10s  %-12s  %-8s  %-5.1f  %-4s  %-75s",
                        (i + 1),
                        truncate(subj, 10),
                        q.getQuestionType().name(),
                        q.getDifficulty().name(),
                        q.getPoints(),
                        aiMark,
                        truncate(q.getQuestionText(), 75));

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

        if (!questions.isEmpty()) {
            int pageSize = TuiHelper.PAGE_SIZE;
            int totalPages = Math.max(1, (int) Math.ceil((double) questions.size() / pageSize));
            int currentPage = selectedIndex / pageSize;
            sb.append(TuiHelper.paginationBar(currentPage, totalPages, questions.size()));
        }

        if (!bannerMessage.isBlank()) {
            sb.append("  ").append(bannerMessage).append("\n\n");
        }

        if (subjectFilterDisplay != null && subjectFilterDisplay.contains("→")) {
            sb.append(TuiHelper.dim("  [Type] Search  •  [Tab/←/→] Cycle Matches  •  [Enter] Confirm  •  [Esc] Cancel\n"));
            return sb.toString();
        }

        List<String> hints = List.of(
                "[↑/↓] Move",
                "[←/→] Page",
                "[n] New",
                "[g] AI Generate",
                "[e/Enter] Edit",
                "[d] Delete",
                "[Tab] Type Filter",
                "[s] Subj Filter",
                "[x] Diff Filter",
                "[/] Search",
                "[Esc] Back"
        );
        sb.append(TuiHelper.wrapHints(hints));
        return sb.toString();
    }

    public static String renderBankPicker(Quiz quiz, List<Question> bankQuestions,
                                          Set<Integer> selectedIds, int selectedIndex,
                                          String filterSummary, String bannerMessage) {
        return renderBankPicker(quiz, bankQuestions, selectedIds, java.util.Collections.emptySet(), selectedIndex, filterSummary, bannerMessage);
    }

    public static String renderBankPicker(Quiz quiz, List<Question> bankQuestions,
                                          Set<Integer> selectedIds, Set<Integer> alreadyAddedIds, int selectedIndex,
                                          String filterSummary, String bannerMessage) {
        StringBuilder sb = new StringBuilder();
        double selectedPts = 0;
        for (Question q : bankQuestions) {
            if (selectedIds.contains(q.getId())) {
                selectedPts += q.getPoints();
            }
        }

        String builderHeader = (quiz.getAssessmentType() == com.proctor.model.enums.AssessmentType.EXAM ? "EXAMS"
                : (quiz.getAssessmentType() == com.proctor.model.enums.AssessmentType.SPEED ? "SPEED QUIZZES" : "QUIZZES"));
        String subtitle = String.format("Selected: %d Question(s) (%.1f pts)  •  Bank: %d available",
                selectedIds.size(), selectedPts, bankQuestions.size());
        sb.append(TuiHelper.header(builderHeader));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle("Import from Bank \u2192 " + quiz.getTitle(), subtitle)).append("\n\n");

        if (filterSummary != null && !filterSummary.isBlank()) {
            sb.append("  ").append(TuiHelper.dim(filterSummary)).append("\n\n");
        }

        sb.append(String.format("    %-14s  %-4s  %-12s  %-8s  %-5s  %-75s%n",
                "SELECT", "#", "TYPE", "DIFF", "PTS", "QUESTION TEXT")).append("\n");
        sb.append("  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

        if (bankQuestions.isEmpty()) {
            sb.append("  ").append(TuiHelper.dim("No bank questions match the current filter. Create questions in the Question Bank first.")).append("\n");
        } else {
            int pageSize = TuiHelper.PAGE_SIZE;
            int startRow = (selectedIndex / pageSize) * pageSize;
            int endRow = Math.min(bankQuestions.size(), startRow + pageSize);

            for (int i = startRow; i < endRow; i++) {
                Question q = bankQuestions.get(i);
                boolean isAlreadyAdded = alreadyAddedIds != null && alreadyAddedIds.contains(q.getId());
                boolean isSelected = selectedIds.contains(q.getId());
                String checkbox;
                if (isAlreadyAdded) {
                    checkbox = TuiHelper.yellow("[✔ Added]     ");
                } else if (isSelected) {
                    checkbox = TuiHelper.green("[✔] Selected  ");
                } else {
                    checkbox = TuiHelper.dim("[ ] Unselected");
                }
                String cursor = (i == selectedIndex) ? TuiHelper.cyan("▶ ") : "  ";

                String line = String.format("%s  %-4d  %-12s  %-8s  %-5.1f  %-75s",
                        checkbox,
                        (i + 1),
                        truncate(q.getQuestionType().name(), 12),
                        truncate(q.getDifficulty().name(), 8),
                        q.getPoints(),
                        truncate(q.getQuestionText(), 75));

                if (i == selectedIndex) {
                    sb.append(TuiHelper.cyan(cursor + line)).append("\n");
                } else {
                    sb.append(cursor).append(line).append("\n");
                }
                if (i < endRow - 1) {
                    sb.append("\n");
                }
            }
        }

        sb.append("\n  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

        if (!bankQuestions.isEmpty()) {
            int pageSize = TuiHelper.PAGE_SIZE;
            int totalPages = Math.max(1, (int) Math.ceil((double) bankQuestions.size() / pageSize));
            int currentPage = selectedIndex / pageSize;
            sb.append(TuiHelper.paginationBar(currentPage, totalPages, bankQuestions.size()));
        }

        if (!bannerMessage.isBlank()) {
            sb.append("  ").append(bannerMessage).append("\n\n");
        }

        List<String> hints = List.of(
                "[↑/↓] Navigate",
                "[←/→] Page",
                "[Tab] Filter",
                "[/] Search",
                "[Space/Enter] Toggle",
                "[c] Confirm Import",
                "[Esc] Cancel"
        );
        sb.append(TuiHelper.wrapHints(hints));
        return sb.toString();
    }

    private static String truncate(String text, int max) {
        return TuiHelper.truncate(text, max);
    }
}
