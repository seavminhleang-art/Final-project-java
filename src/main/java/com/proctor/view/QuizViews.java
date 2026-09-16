package com.proctor.view;

import com.proctor.model.enums.AssessmentType;
import com.proctor.model.enums.Difficulty;
import com.proctor.model.enums.QuestionType;
import com.proctor.model.entity.Question;
import com.proctor.model.entity.Quiz;
import com.proctor.util.TuiHelper;

import java.util.ArrayList;
import java.util.List;

public class QuizViews {

    public static String renderQuizList(AssessmentType assessmentType, List<Quiz> quizzes, int selectedIndex,
                                        boolean isMyQuizzesScope, String searchBuffer, boolean searchMode, String bannerMessage) {
        return renderQuizList(assessmentType, quizzes, selectedIndex, isMyQuizzesScope, "ALL", searchBuffer, searchMode, bannerMessage, false);
    }

    public static String renderQuizList(AssessmentType assessmentType, List<Quiz> quizzes, int selectedIndex,
                                        boolean isMyQuizzesScope, String subjectFilterDisplay,
                                        String searchBuffer, boolean searchMode, String bannerMessage) {
        return renderQuizList(assessmentType, quizzes, selectedIndex, isMyQuizzesScope, subjectFilterDisplay, searchBuffer, searchMode, bannerMessage, false);
    }

    public static String renderQuizList(AssessmentType assessmentType, List<Quiz> quizzes, int selectedIndex,
                                        boolean isMyQuizzesScope, String subjectFilterDisplay,
                                        String searchBuffer, boolean searchMode, String bannerMessage, boolean isAdmin) {
        StringBuilder sb = new StringBuilder();
        String itemType = (assessmentType == AssessmentType.EXAM) ? "EXAMS" : (assessmentType == AssessmentType.SPEED ? "SPEED QUIZZES" : "QUIZZES");
        String baseItemTitle = (assessmentType == AssessmentType.EXAM) ? "Exams" : (assessmentType == AssessmentType.SPEED ? "Speed Quizzes" : "Quizzes");
        String itemTitle = isAdmin ? "All " + baseItemTitle : baseItemTitle;
        String subjLabel = (subjectFilterDisplay == null || subjectFilterDisplay.isBlank()) ? "ALL" : subjectFilterDisplay;

        sb.append(TuiHelper.header(itemType));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle(itemTitle, String.format("Subject: [ %s ]  •  Total: %d", subjLabel, quizzes.size()))).append("\n\n");
        if (!isAdmin) {
            String tabMy = "My " + baseItemTitle;
            String tabAll = "All " + baseItemTitle;
            sb.append(TuiHelper.tabBar(new String[]{tabMy, tabAll}, isMyQuizzesScope ? 0 : 1)).append("\n\n");
        }

        if (searchMode) {
            sb.append("  Search: [ ").append(TuiHelper.cyan(TuiHelper.truncate(searchBuffer, 50) + "_")).append(" ] (Press Enter to finish)\n\n");
        } else if (!searchBuffer.isEmpty()) {
            sb.append("  Search: [ ").append(TuiHelper.truncate(searchBuffer, 50)).append(" ] (Press '/' to edit)\n\n");
        }

        String timeColHeader = (assessmentType == AssessmentType.SPEED) ? "SEC/Q" : "TIME";
        if (isAdmin) {
            sb.append(String.format("  %-4s  %-6s  %-12s  %-36s  %-18s  %-8s  %-8s  %-4s  %-6s  %-10s%n",
                    "#", "ID", "SUBJ", "TITLE", "TEACHER", "TYPE", timeColHeader, "Qs", "PTS", "STATUS")).append("\n");
        } else {
            sb.append(String.format("  %-4s  %-12s  %-44s  %-18s  %-8s  %-8s  %-4s  %-6s  %-10s%n",
                    "#", "SUBJ", "TITLE", "TEACHER", "TYPE", timeColHeader, "Qs", "PTS", "STATUS")).append("\n");
        }
        sb.append("  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

        if (quizzes.isEmpty()) {
            String emptyLabel = (assessmentType == AssessmentType.EXAM) ? "exams" : (assessmentType == AssessmentType.SPEED ? "speed quizzes" : "quizzes");
            sb.append("  ").append(TuiHelper.dim("No " + emptyLabel + " found.")).append("\n");
        } else {
            int pageSize = TuiHelper.PAGE_SIZE;
            int startRow = (selectedIndex / pageSize) * pageSize;
            int endRow = Math.min(quizzes.size(), startRow + pageSize);

            for (int i = startRow; i < endRow; i++) {
                Quiz q = quizzes.get(i);
                String cursor = (i == selectedIndex) ? TuiHelper.cyan("▶ ") : "  ";
                String status = q.isPublished() ? TuiHelper.green("Published") : TuiHelper.dim("Draft");
                String subj = q.getSubjectCode() != null ? q.getSubjectCode() : "-";
                String timeStr;
                if (q.getAssessmentType() == AssessmentType.SPEED) {
                    timeStr = (q.getSpeedSecondsPerQuestion() != null ? q.getSpeedSecondsPerQuestion() : 15) + "s";
                } else {
                    timeStr = q.getTimeLimitMins() != null && q.getTimeLimitMins() > 0 ? q.getTimeLimitMins() + "m" : "Untimed";
                }
                String teacher = (q.getCreatorName() != null && !q.getCreatorName().isBlank()) ? q.getCreatorName() : "Teacher";
                String typeStr;
                if (q.getAssessmentType() == AssessmentType.SPEED) {
                    typeStr = "[SPEED]";
                } else if (q.getAssessmentType() == AssessmentType.EXAM) {
                    typeStr = "[MIXED]";
                } else {
                    typeStr = switch (q.getQuizQuestionType() != null ? q.getQuizQuestionType() : QuestionType.MCQ) {
                        case MCQ -> "[MCQ]";
                        case TRUE_FALSE -> "[T/F]";
                        case SHORT_ANSWER -> "[SHORT]";
                    };
                }

                String line;
                if (isAdmin) {
                    line = String.format("%-4d  %-6d  %-12s  %-36s  %-18s  %-8s  %-8s  %-4d  %-6.1f  %-10s",
                            (i + 1),
                            q.getId(),
                            truncate(subj, 12),
                            truncate(q.getTitle(), 36),
                            truncate(teacher, 18),
                            typeStr,
                            timeStr,
                            q.getQuestionCount(),
                            q.getTotalPoints(),
                            status);
                } else {
                    line = String.format("%-4d  %-12s  %-44s  %-18s  %-8s  %-8s  %-4d  %-6.1f  %-10s",
                            (i + 1),
                            truncate(subj, 12),
                            truncate(q.getTitle(), 44),
                            truncate(teacher, 18),
                            typeStr,
                            timeStr,
                            q.getQuestionCount(),
                            q.getTotalPoints(),
                            status);
                }

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

        if (!quizzes.isEmpty()) {
            int pageSize = TuiHelper.PAGE_SIZE;
            int totalPages = Math.max(1, (int) Math.ceil((double) quizzes.size() / pageSize));
            int currentPage = selectedIndex / pageSize;
            sb.append(TuiHelper.paginationBar(currentPage, totalPages, quizzes.size()));
        }

        if (!bannerMessage.isBlank()) {
            sb.append("  ").append(bannerMessage).append("\n\n");
        }

        String aiHint = (assessmentType == AssessmentType.EXAM) ? "[g] AI Exam" : (assessmentType == AssessmentType.SPEED ? "[g] AI Speed" : "[g] AI Quiz");
        List<String> hints;
        if (isAdmin) {
            hints = List.of(
                    "[↑/↓] Move",
                    "[←/→] Page",
                    "[Enter] Builder",
                    "[Space] Publish",
                    "[/] Search",
                    "[s] Subject",
                    "[r] Submissions",
                    "[e] Edit",
                    "[d] Delete",
                    "[Esc] Back"
            );
        } else {
            hints = List.of(
                    "[↑/↓] Move",
                    "[←/→] Page",
                    "[Enter] Builder",
                    "[Space] Publish",
                    "[/] Search",
                    "[Tab] Tab",
                    "[s] Subject",
                    "[r] Submissions",
                    "[n] New",
                    aiHint,
                    "[e] Edit",
                    "[d] Delete",
                    "[Esc] Back"
            );
        }
        sb.append(TuiHelper.wrapHints(hints));
        return sb.toString();
    }

    public static String renderQuizForm(AssessmentType assessmentType, QuestionType quizQuestionType,
                                        boolean isEditMode, String subjectName, String title, String description,
                                        String timeLimit, String activeHours, String passScore,
                                        boolean randomizeQuestions, boolean randomizeAnswers, boolean showAnswersAfter,
                                        int focusedField, String errorMessage) {
        StringBuilder sb = new StringBuilder();
        String itemType = (assessmentType == AssessmentType.EXAM) ? "EXAM" : "QUIZ";
        String headerTitle = isEditMode ? "EDIT " + itemType : "CREATE NEW " + itemType;
        int activeFieldDisplay = Math.min(10, focusedField + 1);
        sb.append(TuiHelper.header(headerTitle));
        sb.append("\n");
        String formSub = isEditMode ? "Edit " + itemType + " Settings" : "Create New " + itemType;
        sb.append(TuiHelper.boxTitle(formSub, String.format("Field %d of 10", activeFieldDisplay))).append("\n\n");

        int numInputFields = 10;
        int windowSize = 4;
        int startField = Math.max(0, Math.min(Math.min(focusedField, 9) - 1, numInputFields - windowSize));
        int endField = Math.min(numInputFields, startField + windowSize);

        if (startField > 0) {
            sb.append(TuiHelper.dim(String.format("  ▲ %d more fields above (Press ↑ to scroll)", startField))).append("\n");
        }

        for (int f = startField; f < endField; f++) {
            switch (f) {
                case 0 -> sb.append(TuiHelper.selectBox("Subject (Required)", subjectName, focusedField == 0, 102, "Space or ←/→ to cycle"));
                case 1 -> sb.append(TuiHelper.inputBox(itemType + " Title (Required)", title, focusedField == 1, 102, false, "e.g. Midterm Assessment"));
                case 2 -> {
                    if (assessmentType == AssessmentType.QUIZ) {
                        String typeLabel = (quizQuestionType != null) ? switch (quizQuestionType) {
                            case MCQ -> "Multiple Choice (MCQ)";
                            case TRUE_FALSE -> "True / False";
                            case SHORT_ANSWER -> "Short Answer";
                        } : "Multiple Choice (MCQ)";
                        sb.append(TuiHelper.selectBox("Quiz Question Type (Strict)", typeLabel, focusedField == 2, 102, "Space or ←/→ to switch"));
                    } else {
                        sb.append(TuiHelper.selectBox("Assessment Mode", "Exam (Mixed - All Question Types Allowed)", focusedField == 2, 102, "Comprehensive Exam"));
                    }
                }
                case 3 -> sb.append(TuiHelper.inputBox("Description", description, focusedField == 3, 102, false, "optional instructions"));
                case 4 -> sb.append(TuiHelper.inputBox("Time Limit (Minutes)", timeLimit, focusedField == 4, 102, false, "0 for untimed"));
                case 5 -> sb.append(TuiHelper.inputBox("Active Lifetime (Hours)", activeHours, focusedField == 5, 102, false, "0 for Available Forever"));
                case 6 -> sb.append(TuiHelper.inputBox("Passing Score (%)", passScore, focusedField == 6, 102, false, "e.g. 50"));
                case 7 -> {
                    String rqText = randomizeQuestions ? "Enabled" : "Disabled";
                    sb.append(TuiHelper.selectBox("Randomize Question Order", rqText, focusedField == 7, 102, "Space to toggle"));
                }
                case 8 -> {
                    String raText = randomizeAnswers ? "Enabled" : "Disabled";
                    sb.append(TuiHelper.selectBox("Shuffle Answer Options", raText, focusedField == 8, 102, "Space to toggle"));
                }
                case 9 -> {
                    String saText = showAnswersAfter ? "Enabled" : "Disabled";
                    sb.append(TuiHelper.selectBox("Review Answers on Submit", saText, focusedField == 9, 102, "Space to toggle"));
                }
            }
            sb.append("\n");
        }

        if (endField < numInputFields) {
            sb.append(TuiHelper.dim(String.format("  ▼ %d more fields below (Press ↓ to scroll)", numInputFields - endField))).append("\n");
        }

        sb.append("\n");
        sb.append(TuiHelper.buttonRow("Submit", focusedField == 10, "Cancel", focusedField == 11)).append("\n\n");

        if (!errorMessage.isBlank()) {
            sb.append("  ").append(TuiHelper.red("✖ " + errorMessage)).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [↑/↓] Switch Field  •  [Enter] Confirm / Next  •  [Esc] Cancel\n"));
        return sb.toString();
    }

    public static String renderQuizQuestionEditor(Quiz quiz, String subjectText, List<Question> questions,
                                                 double totalPoints, int selectedIndex, String bannerMessage) {
        StringBuilder sb = new StringBuilder();
        String subtitle = String.format("Subject: %s  •  %d Questions (%.1f pts)  •  %s",
                subjectText, questions.size(), totalPoints,
                quiz.isPublished() ? TuiHelper.green("PUBLISHED") : TuiHelper.dim("DRAFT"));

        String builderHeader = (quiz.getAssessmentType() == AssessmentType.EXAM ? "EXAMS" : (quiz.getAssessmentType() == AssessmentType.SPEED ? "SPEED QUIZZES" : "QUIZZES"));
        sb.append(TuiHelper.header(builderHeader));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle(quiz.getTitle(), subtitle)).append("\n\n");

        sb.append(String.format("  %-4s  %-12s  %-10s  %-6s  %-90s%n",
                "#", "TYPE", "DIFF", "PTS", "QUESTION TEXT")).append("\n");
        sb.append("  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

        if (questions.isEmpty()) {
            String emptyItemLabel = (quiz.getAssessmentType() == AssessmentType.EXAM ? "exam" : (quiz.getAssessmentType() == AssessmentType.SPEED ? "speed quiz" : "quiz"));
            sb.append("  ").append(TuiHelper.dim("No questions in this " + emptyItemLabel + " yet. Press 'n' to add or 'g' to generate with AI.")).append("\n");
        } else {
            int pageSize = TuiHelper.PAGE_SIZE;
            int startRow = (selectedIndex / pageSize) * pageSize;
            int endRow = Math.min(questions.size(), startRow + pageSize);

            for (int i = startRow; i < endRow; i++) {
                Question q = questions.get(i);
                String cursor = (i == selectedIndex) ? TuiHelper.cyan("▶ ") : "  ";
                String line = String.format("%-4d  %-12s  %-10s  %-6.1f  %-90s",
                        (i + 1),
                        truncate(q.getQuestionType().name(), 12),
                        truncate(q.getDifficulty().name(), 10),
                        q.getPoints(),
                        truncate(q.getQuestionText(), 90));

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

        if (!questions.isEmpty()) {
            int pageSize = TuiHelper.PAGE_SIZE;
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
                "[Enter/e] Edit",
                "[n] Add Question",
                "[b] From Bank",
                "[g] AI Generate",
                "[Space] Publish",
                "[d] Delete",
                "[Esc] Back"
        );
        sb.append(TuiHelper.wrapHints(hints));
        return sb.toString();
    }

    public static String renderAIQuizLoading(AssessmentType assessmentType, String title, int tick) {
        StringBuilder sb = new StringBuilder();
        String itemType = (assessmentType == AssessmentType.EXAM) ? "EXAM" : (assessmentType == AssessmentType.SPEED ? "SPEED QUIZ" : "QUIZ");
        String itemLabel = (assessmentType == AssessmentType.EXAM) ? "Exam" : (assessmentType == AssessmentType.SPEED ? "Speed Quiz" : "Quiz");
        sb.append(TuiHelper.header(itemType));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle("Generating " + itemLabel + ": " + title, "Local Ollama LLM is assembling questions...")).append("\n\n");

        String[] spinnerFrames = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
        String spinner = spinnerFrames[Math.abs(tick) % spinnerFrames.length];

        sb.append("  ").append(TuiHelper.cyan(spinner)).append(" ").append(TuiHelper.bold("Crafting high-quality academic questions with AI...")).append("\n\n");
        sb.append("  ").append(TuiHelper.dim("Synthesizing questions, options, rubrics, and assembling the " + itemLabel.toLowerCase() + " in the database...")).append("\n\n");
        sb.append("  ").append(TuiHelper.dim("You will be automatically redirected to the Question Editor once complete.\n\n"));
        sb.append("  ").append(TuiHelper.dim("[Esc] Cancel\n"));
        return sb.toString();
    }

    public static String renderAIQuizForm(AssessmentType assessmentType, String subjectName, String titleBuffer,
                                         String descriptionBuffer, String customPrompt, String countBuffer,
                                         String mcqCountBuffer, String tfCountBuffer, String saCountBuffer,
                                         String questionTypeLabel, Difficulty selectedDifficulty, int mcqOptionCount,
                                         String timeLimitBuffer, String activeHours, String passScore,
                                         boolean randomizeQuestions, boolean randomizeAnswers, boolean showAnswersAfter,
                                         int focusedField, String bannerMessage) {
        StringBuilder sb = new StringBuilder();
        String itemType = (assessmentType == AssessmentType.EXAM) ? "EXAM" : (assessmentType == AssessmentType.SPEED ? "SPEED QUIZ" : "QUIZ");
        String itemLabel = (assessmentType == AssessmentType.EXAM) ? "Exam" : (assessmentType == AssessmentType.SPEED ? "Speed Quiz" : "Quiz");
        boolean isMixed = questionTypeLabel.contains("MIXED");
        boolean isSpeed = (assessmentType == AssessmentType.SPEED);

        List<String> fieldWidgets = new ArrayList<>();
        fieldWidgets.add(TuiHelper.selectBox("Subject (Required)", subjectName, focusedField == 0, 102, "Space or ←/→ to cycle"));
        fieldWidgets.add(TuiHelper.inputBox(itemLabel + " Title / Topic (Required)", titleBuffer, focusedField == 1, 102, false, "e.g. Basic HTML, OOP Concepts"));
        fieldWidgets.add(TuiHelper.inputBox("Student Instructions / Description (Optional)", descriptionBuffer, focusedField == 2, 102, false, "e.g. Complete all questions, no reference materials"));
        fieldWidgets.add(TuiHelper.inputBox("AI Prompt / Custom Instructions (Optional)", customPrompt, focusedField == 3, 102, false, "e.g. Focus on edge cases, avoid multi-threading, include code snippets"));

        int curIdx = 4;
        if (isSpeed) {
            fieldWidgets.add(TuiHelper.inputBox("MCQ Question Count (0-10)", mcqCountBuffer, focusedField == curIdx++, 102, false, "e.g. 3"));
            fieldWidgets.add(TuiHelper.inputBox("True/False Question Count (0-10)", tfCountBuffer, focusedField == curIdx++, 102, false, "e.g. 3"));
        } else {
            fieldWidgets.add(TuiHelper.selectBox("Question Type", questionTypeLabel, focusedField == curIdx++, 102, "Space or ←/→ to cycle"));
            if (isMixed) {
                fieldWidgets.add(TuiHelper.inputBox("MCQ Question Count (0-10)", mcqCountBuffer, focusedField == curIdx++, 102, false, "e.g. 2"));
                fieldWidgets.add(TuiHelper.inputBox("True/False Question Count (0-10)", tfCountBuffer, focusedField == curIdx++, 102, false, "e.g. 2"));
                fieldWidgets.add(TuiHelper.inputBox("Short Answer Question Count (0-10)", saCountBuffer, focusedField == curIdx++, 102, false, "e.g. 1"));
            } else {
                fieldWidgets.add(TuiHelper.inputBox("Number of Questions (1-10)", countBuffer, focusedField == curIdx++, 102, false, "e.g. 5"));
            }
        }

        fieldWidgets.add(TuiHelper.selectBox("Difficulty Level", selectedDifficulty.name(), focusedField == curIdx++, 102, "Space to cycle"));

        boolean showMcq = (isMixed || isSpeed) ? (!"0".equals(mcqCountBuffer != null ? mcqCountBuffer.trim() : "0")) : questionTypeLabel.contains("MCQ");
        if (showMcq) {
            String optLabel = mcqOptionCount + " Options per Question";
            fieldWidgets.add(TuiHelper.selectBox("MCQ Option Count", optLabel, focusedField == curIdx++, 102, "Space to cycle (2, 3, 4)"));
        }

        if (isSpeed) {
            fieldWidgets.add(TuiHelper.inputBox("Timer Per Question (Seconds)", timeLimitBuffer, focusedField == curIdx++, 102, false, "default: 15 seconds"));
            fieldWidgets.add(TuiHelper.inputBox("Active Lifetime (Hours)", activeHours, focusedField == curIdx++, 102, false, "0 for Available Forever"));
        } else {
            fieldWidgets.add(TuiHelper.inputBox("Time Limit (Minutes)", timeLimitBuffer, focusedField == curIdx++, 102, false, "0 for untimed"));
            fieldWidgets.add(TuiHelper.inputBox("Active Lifetime (Hours)", activeHours, focusedField == curIdx++, 102, false, "0 for Available Forever"));
            fieldWidgets.add(TuiHelper.inputBox("Passing Score (%)", passScore, focusedField == curIdx++, 102, false, "e.g. 50"));
            String rqText = randomizeQuestions ? "Enabled" : "Disabled";
            fieldWidgets.add(TuiHelper.selectBox("Randomize Question Order", rqText, focusedField == curIdx++, 102, "Space to toggle"));
        }

        String raText = randomizeAnswers ? "Enabled" : "Disabled";
        fieldWidgets.add(TuiHelper.selectBox("Shuffle Answer Options", raText, focusedField == curIdx++, 102, "Space to toggle"));

        String saText = showAnswersAfter ? "Enabled" : "Disabled";
        fieldWidgets.add(TuiHelper.selectBox("Review Answers on Submit", saText, focusedField == curIdx++, 102, "Space to toggle"));

        int numInputFields = fieldWidgets.size();
        int activeFieldDisplay = Math.min(numInputFields, focusedField + 1);
        sb.append(TuiHelper.header("AI " + itemType + " GENERATOR"));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle("AI " + itemLabel + " Generator", String.format("Field %d of %d", activeFieldDisplay, numInputFields))).append("\n\n");

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
            sb.append(TuiHelper.dim(String.format("  ▼ %d more fields below (Press ↓ to scroll)", numInputFields - endField))).append("\n");
        }

        sb.append("\n");
        sb.append(TuiHelper.buttonRow("Generate " + itemLabel, focusedField == numInputFields, "Cancel", focusedField == numInputFields + 1)).append("\n\n");

        if (!bannerMessage.isBlank()) {
            sb.append("  ").append(bannerMessage).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [↑/↓] Switch Field  •  [Enter] Confirm / Next  •  [Esc] Cancel\n"));
        return sb.toString();
    }

    private static String truncate(String text, int max) {
        return TuiHelper.truncate(text, max);
    }
}