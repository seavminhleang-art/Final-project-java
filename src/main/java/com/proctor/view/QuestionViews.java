package com.proctor.view;

import com.proctor.model.entity.AIQuestionDraft;
import com.proctor.model.enums.Difficulty;
import com.proctor.model.enums.QuestionType;
import com.proctor.model.entity.Question;
import com.proctor.model.entity.QuestionOption;
import com.proctor.util.TuiHelper;

import java.util.ArrayList;
import java.util.List;

public class QuestionViews {

    public static String renderQuestionList(List<Question> questions, int selectedIndex,
                                           QuestionType typeFilter, Difficulty diffFilter,
                                           String searchBuffer, boolean searchMode, String bannerMessage) {
        StringBuilder sb = new StringBuilder();
        String tf = (typeFilter == null) ? "ALL" : typeFilter.name();
        String df = (diffFilter == null) ? "ALL" : diffFilter.name();
        sb.append(TuiHelper.header("QUESTIONS"));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle("Question Bank", String.format("Type: [%s]  •  Diff: [%s]  •  Total: %d", tf, df, questions.size()))).append("\n\n");

        if (searchMode) {
            sb.append("  Search: [ ").append(TuiHelper.cyan(searchBuffer + "_")).append(" ] (Press Enter to finish)\n\n");
        } else if (!searchBuffer.isEmpty()) {
            sb.append("  Search: [ ").append(searchBuffer).append(" ] (Press '/' to edit)\n\n");
        }

        sb.append(String.format("  %-4s  %-10s  %-11s  %-8s  %-5s  %-39s  %-8s%n",
                "ID", "SUBJ", "TYPE", "DIFF", "PTS", "QUESTION TEXT", "STATUS")).append("\n");
        sb.append("  " + "─".repeat(95) + "\n\n");

        if (questions.isEmpty()) {
            sb.append("  ").append(TuiHelper.dim("No questions found matching criteria.")).append("\n");
        } else {
            int pageSize = 5;
            int startRow = (selectedIndex / pageSize) * pageSize;
            int endRow = Math.min(questions.size(), startRow + pageSize);

            for (int i = startRow; i < endRow; i++) {
                Question q = questions.get(i);
                String cursor = (i == selectedIndex) ? TuiHelper.cyan("▶ ") : "  ";
                String status = q.isEnabled() ? TuiHelper.green("Enabled") : TuiHelper.red("Disabled");
                String subj = q.getSubjectCode() != null ? q.getSubjectCode() : "-";

                String line = String.format("%-4d  %-10s  %-11s  %-8s  %-5.1f  %-39s  %-8s",
                        q.getId(),
                        truncate(subj, 10),
                        q.getQuestionType().name(),
                        q.getDifficulty().name(),
                        q.getPoints(),
                        truncate(q.getQuestionText(), 39),
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

        if (!questions.isEmpty()) {
            int pageSize = 5;
            int totalPages = Math.max(1, (int) Math.ceil((double) questions.size() / pageSize));
            int currentPage = selectedIndex / pageSize;
            sb.append(TuiHelper.paginationBar(currentPage, totalPages, questions.size()));
        }

        if (!bannerMessage.isBlank()) {
            sb.append("  ").append(bannerMessage).append("\n\n");
        }

        List<String> hints = List.of(
                "[↑/↓] Move",
                "[←/→] Page",
                "[Enter] View",
                "[Space] Toggle Enabled",
                "[n] New",
                "[e] Edit",
                "[f] Type",
                "[d] Diff",
                "[/] Search",
                "[Esc] Back"
        );
        sb.append(TuiHelper.wrapHints(hints, 90));
        return sb.toString();
    }

    public static String renderQuestionForm(boolean isEditMode, boolean isPinnedQuiz, String pinnedQuizTitle,
                                            String subjectName, String questionText, QuestionType selectedType,
                                            Difficulty selectedDifficulty, String points,
                                            List<StringBuilder> options, int correctOptionIndex,
                                            String explanation, int focusedField, int totalFields,
                                            int saveBtnIndex, int cancelBtnIndex, String errorMessage) {
        StringBuilder sb = new StringBuilder();
        String title = isEditMode ? "Edit Question" : "Create New Question";
        String subtitle = isPinnedQuiz ? "Pinned to: " + pinnedQuizTitle : "Question Bank";
        sb.append(TuiHelper.header("QUESTIONS"));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle(title, subtitle)).append("\n\n");

        if (isPinnedQuiz) {
            sb.append("  ").append(TuiHelper.cyan("[Pinned to Quiz: ")).append(TuiHelper.bold(pinnedQuizTitle)).append(TuiHelper.cyan("]"))
              .append(" • Subject: ").append(subjectName.isEmpty() ? "General" : subjectName).append("\n\n");
        }

        int numInputs = totalFields - 2;
        int windowSize = 4;
        int startField = Math.max(0, Math.min(Math.min(focusedField, numInputs - 1) - 1, numInputs - windowSize));
        int endField = Math.min(numInputs, startField + windowSize);

        if (startField > 0) {
            sb.append(TuiHelper.dim(String.format("  ▲ %d more fields above (Press ↑ to scroll)", startField))).append("\n");
        }

        for (int f = startField; f < endField; f++) {
            renderQuestionFormField(sb, f, isPinnedQuiz, subjectName, questionText, selectedType, selectedDifficulty,
                    points, options, correctOptionIndex, explanation, focusedField);
            sb.append("\n");
        }

        if (endField < numInputs) {
            sb.append(TuiHelper.dim(String.format("  ▼ %d more fields below (Press Tab/↓ to scroll)", numInputs - endField))).append("\n");
        }

        sb.append("\n");
        sb.append(TuiHelper.buttonRow("Submit", focusedField == saveBtnIndex, "Cancel", focusedField == cancelBtnIndex)).append("\n\n");

        if (!errorMessage.isBlank()) {
            sb.append("  ").append(TuiHelper.red("✖ " + errorMessage)).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [Tab/↑/↓] Switch Field  •  [Enter] Confirm / Next  •  [Esc] Cancel\n"));
        return sb.toString();
    }

    private static void renderQuestionFormField(StringBuilder sb, int fieldIndex, boolean isPinnedQuiz,
                                                String subjectName, String questionText, QuestionType selectedType,
                                                Difficulty selectedDifficulty, String points,
                                                List<StringBuilder> options, int correctOptionIndex,
                                                String explanation, int focusedField) {
        int idx = fieldIndex;
        if (!isPinnedQuiz) {
            if (idx == 0) {
                sb.append(TuiHelper.inputBox("Subject (Required)", subjectName, focusedField == 0, 86, false, "e.g. Java, Python, English, Math"));
                return;
            }
            idx -= 1;
        }

        switch (idx) {
            case 0 -> sb.append(TuiHelper.inputBox("Question Prompt (Required)", questionText, focusedField == fieldIndex, 86, false, "enter question text"));
            case 1 -> sb.append(TuiHelper.selectBox("Question Type", selectedType.name(), focusedField == fieldIndex, 86, "Space to cycle"));
            case 2 -> sb.append(TuiHelper.selectBox("Difficulty", selectedDifficulty.name(), focusedField == fieldIndex, 86, "Space to cycle"));
            case 3 -> sb.append(TuiHelper.inputBox("Points", points, focusedField == fieldIndex, 86, false, "e.g. 2.0"));
            default -> {
                int optIdx = idx - 4;
                if (selectedType == QuestionType.MCQ) {
                    if (optIdx >= 0 && optIdx < 4) {
                        String isCorrectMark = (correctOptionIndex == optIdx) ? " [✔ CORRECT]" : "";
                        String label = "Option " + (char) ('A' + optIdx) + isCorrectMark;
                        sb.append(TuiHelper.inputBox(label, options.get(optIdx).toString(), focusedField == fieldIndex, 86, false, "leave blank to omit"));
                    } else if (optIdx == 4) {
                        String corrLabel = "Option " + (char) ('A' + correctOptionIndex);
                        sb.append(TuiHelper.selectBox("Correct Answer Selection", corrLabel, focusedField == fieldIndex, 86, "Space to cycle"));
                    } else if (optIdx == 5) {
                        sb.append(TuiHelper.inputBox("Explanation (Optional)", explanation, focusedField == fieldIndex, 86, false, "rubric context"));
                    }
                } else if (selectedType == QuestionType.TRUE_FALSE) {
                    if (optIdx == 0) {
                        String corrLabel = (correctOptionIndex == 0) ? "True" : "False";
                        sb.append(TuiHelper.selectBox("Correct Answer Selection", corrLabel, focusedField == fieldIndex, 86, "Space to cycle"));
                    } else if (optIdx == 1) {
                        sb.append(TuiHelper.inputBox("Explanation (Optional)", explanation, focusedField == fieldIndex, 86, false, "rubric context"));
                    }
                } else {
                    sb.append(TuiHelper.inputBox("Model Answer Context", explanation, focusedField == fieldIndex, 86, false, "rubric / explanation"));
                }
            }
        }
    }

    public static String renderQuestionView(Question question) {
        StringBuilder sb = new StringBuilder();
        String subj = question.getSubjectCode() != null ? question.getSubjectCode() : "No Subject";
        sb.append(TuiHelper.header("QUESTIONS"));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle("Question Details #" + question.getId(), "Subject: " + subj)).append("\n\n");

        sb.append("  " + TuiHelper.bold("Type:") + "       " + question.getQuestionType().name());
        sb.append("   " + TuiHelper.bold("Difficulty:") + " " + question.getDifficulty().name());
        sb.append("   " + TuiHelper.bold("Points:") + " " + question.getPoints());
        String status = question.isEnabled() ? TuiHelper.green("Enabled") : TuiHelper.red("Disabled");
        sb.append("   " + TuiHelper.bold("Status:") + " " + status).append("\n\n");

        sb.append("  " + TuiHelper.bold("Question Text:") + "\n");
        sb.append("  " + question.getQuestionText()).append("\n\n");

        if (question.getQuestionType() != QuestionType.SHORT_ANSWER && question.getOptions() != null) {
            sb.append("  " + TuiHelper.bold("Options:") + "\n");
            for (QuestionOption opt : question.getOptions()) {
                if (opt.isCorrect()) {
                    sb.append("   ").append(TuiHelper.green("✔ [Correct] " + opt.getOptionText())).append("\n");
                } else {
                    sb.append("   ").append(TuiHelper.dim("• [       ] " + opt.getOptionText())).append("\n");
                }
            }
            sb.append("\n");
        }

        if (question.getExplanation() != null && !question.getExplanation().isBlank()) {
            sb.append("  " + TuiHelper.bold("Explanation / Context:") + "\n");
            sb.append("  " + question.getExplanation()).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [Enter/e] Edit  •  [Space] Toggle Enabled  •  [Esc] Back\n"));
        return sb.toString();
    }

    public static String renderAIQuestionLoading(String topic, int spinnerTick) {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("AI QUESTION GENERATOR"));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle("AI Question Generator", "Generating with Local Ollama LLM")).append("\n\n");

        String[] spinners = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
        String icon = spinners[spinnerTick % spinners.length];

        sb.append("  ").append(TuiHelper.cyan(icon + " Generating questions for topic: ")).append(TuiHelper.bold(topic)).append("\n\n");
        sb.append("  ").append(TuiHelper.dim("Please wait a moment while the local Ollama LLM drafts questions, options, and explanations...")).append("\n\n");
        sb.append("  ").append(TuiHelper.dim("[Esc] Cancel generation\n"));
        return sb.toString();
    }

    public static String renderAIQuestionForm(boolean isPinnedQuiz, String pinnedQuizTitle, String subjectName,
                                             String topicBuffer, String customPrompt, String countBuffer, QuestionType selectedType,
                                             Difficulty selectedDifficulty, int mcqOptionCount,
                                             int focusedField, int generateBtnIndex, int cancelBtnIndex,
                                             String bannerMessage) {
        StringBuilder sb = new StringBuilder();
        String subtitle = isPinnedQuiz ? "Pinned to: " + pinnedQuizTitle : "Powered by Local Ollama LLM";
        sb.append(TuiHelper.header("AI QUESTION GENERATOR"));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle("AI Question Generator", subtitle)).append("\n\n");

        if (isPinnedQuiz) {
            sb.append("  ").append(TuiHelper.cyan("[Pinned to Quiz: ")).append(TuiHelper.bold(pinnedQuizTitle)).append(TuiHelper.cyan("]"))
              .append(" • Subject: ").append(subjectName.isEmpty() ? "General" : subjectName).append("\n\n");
        }

        List<String> fieldWidgets = new ArrayList<>();
        int actualField = 0;
        if (!isPinnedQuiz) {
            fieldWidgets.add(TuiHelper.inputBox("Subject (Required)", subjectName, focusedField == actualField++, 86, false, "e.g. Java, Python, English, Math"));
        }

        fieldWidgets.add(TuiHelper.inputBox("Topic / Focus Area (Required)", topicBuffer, focusedField == actualField++, 86, false, "e.g. Dynamic Programming or Recursion"));
        fieldWidgets.add(TuiHelper.inputBox("Custom Prompt / Instructions (Optional)", customPrompt, focusedField == actualField++, 86, false, "e.g. Focus on memoization, ask conceptual scenarios"));
        fieldWidgets.add(TuiHelper.inputBox("Question Count (1-10)", countBuffer, focusedField == actualField++, 86, false, "e.g. 3"));
        fieldWidgets.add(TuiHelper.selectBox("Question Type", selectedType.name(), focusedField == actualField++, 86, "Space to cycle"));
        fieldWidgets.add(TuiHelper.selectBox("Difficulty", selectedDifficulty.name(), focusedField == actualField++, 86, "Space to cycle"));

        if (selectedType == QuestionType.MCQ) {
            String optLabel = mcqOptionCount + " Options per Question";
            fieldWidgets.add(TuiHelper.selectBox("MCQ Option Count", optLabel, focusedField == actualField++, 86, "Space to cycle (2, 3, 4)"));
        }

        int numInputFields = fieldWidgets.size();
        int windowSize = 4;
        int startField = Math.max(0, Math.min(Math.min(focusedField, numInputFields - 1) - 1, numInputFields - windowSize));
        int endField = Math.min(numInputFields, startField + windowSize);

        if (startField > 0) {
            sb.append(TuiHelper.dim(String.format("  ▲ %d more fields above (Press ↑ to scroll)", startField))).append("\n");
        }

        for (int f = startField; f < endField; f++) {
            sb.append(fieldWidgets.get(f)).append("\n");
        }

        if (endField < numInputFields) {
            sb.append(TuiHelper.dim(String.format("  ▼ %d more fields below (Press Tab/↓ to scroll)", numInputFields - endField))).append("\n");
        }

        sb.append("\n");
        sb.append(TuiHelper.buttonRow("Generate Questions", focusedField == generateBtnIndex, "Cancel", focusedField == cancelBtnIndex)).append("\n\n");

        if (!bannerMessage.isBlank()) {
            sb.append("  ").append(bannerMessage).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [Tab/↑/↓] Switch Field  •  [Enter] Confirm / Next  •  [Esc] Back\n"));
        return sb.toString();
    }

    public static String renderAIQuestionReview(List<AIQuestionDraft> generatedDrafts, int selectedDraftIndex, String targetStr, String bannerMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("AI QUESTION GENERATOR"));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle("AI Generated Questions Review", String.format("Drafts: %d  •  Target: %s", generatedDrafts.size(), targetStr))).append("\n\n");

        if (bannerMessage != null && !bannerMessage.isBlank()) {
            sb.append("  ").append(bannerMessage).append("\n\n");
        }

        int pageSize = 3;
        int totalPages = Math.max(1, (int) Math.ceil((double) generatedDrafts.size() / pageSize));
        int currentPage = selectedDraftIndex / pageSize;
        int start = currentPage * pageSize;
        int end = Math.min(generatedDrafts.size(), start + pageSize);

        for (int i = start; i < end; i++) {
            AIQuestionDraft d = generatedDrafts.get(i);
            String cursor = (i == selectedDraftIndex) ? TuiHelper.cyan("▶ ") : "  ";
            sb.append(cursor).append(TuiHelper.bold(String.format("Q%d. %s [%s, %.1f pts]", i + 1, d.getQuestionText(), d.getDifficulty().name(), d.getPoints()))).append("\n");

            if (d.getOptions() != null && !d.getOptions().isEmpty()) {
                for (QuestionOption opt : d.getOptions()) {
                    if (opt.isCorrect()) {
                        sb.append("     ").append(TuiHelper.green("✔ [Correct] " + opt.getOptionText())).append("\n");
                    } else {
                        sb.append("     ").append(TuiHelper.dim("• " + opt.getOptionText())).append("\n");
                    }
                }
            }
            if (d.getExplanation() != null && !d.getExplanation().isBlank()) {
                sb.append("     ").append(TuiHelper.dim("Explanation: " + d.getExplanation())).append("\n");
            }
            sb.append("\n");
        }

        sb.append("  " + "─".repeat(95) + "\n\n");

        if (!generatedDrafts.isEmpty()) {
            sb.append(TuiHelper.paginationBar(currentPage, totalPages, generatedDrafts.size()));
        }

        String dest = "Save Drafts";
        sb.append(TuiHelper.buttonRow(dest, false, "Cancel", false)).append("\n\n");
        sb.append(TuiHelper.dim("  [↑/↓] Move  •  [←/→] Page  •  [Enter / s] Save to Quiz  •  [Esc] Cancel\n"));
        return sb.toString();
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}