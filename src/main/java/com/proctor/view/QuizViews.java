package com.proctor.view;

import com.proctor.model.enums.AssessmentType;
import com.proctor.model.enums.Difficulty;
import com.proctor.model.enums.QuestionType;
import com.proctor.model.entity.Question;
import com.proctor.model.entity.Quiz;
import com.proctor.util.TuiHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QuizViews {

    public static String renderQuizList(List<Quiz> quizzes, int selectedIndex,
                                        boolean isMyQuizzesScope, String searchBuffer, boolean searchMode, String bannerMessage) {
        return renderQuizList(AssessmentType.QUIZ, quizzes, selectedIndex, isMyQuizzesScope, searchBuffer, searchMode, bannerMessage);
    }

    public static String renderQuizList(AssessmentType assessmentType, List<Quiz> quizzes, int selectedIndex,
                                        boolean isMyQuizzesScope, String searchBuffer, boolean searchMode, String bannerMessage) {
        StringBuilder sb = new StringBuilder();
        String itemType = (assessmentType == AssessmentType.EXAM) ? "EXAMS" : "QUIZZES";
        String scopeLabel = isMyQuizzesScope
                ? "Scope: [ MY " + itemType + " ]  •  Press 'f' for All Global"
                : "Scope: [ ALL GLOBAL " + itemType + " ]  •  Press 'f' for My " + itemType;

        sb.append(TuiHelper.header(itemType, String.format("%s  •  Total: %d", scopeLabel, quizzes.size())));
        sb.append("\n");

        if (searchMode) {
            sb.append("  Search: [ ").append(TuiHelper.cyan(searchBuffer + "_")).append(" ] (Press Enter to finish)\n\n");
        } else if (!searchBuffer.isEmpty()) {
            sb.append("  Search: [ ").append(searchBuffer).append(" ] (Press '/' to edit)\n\n");
        }

        sb.append(String.format("  %-4s  %-12s  %-30s  %-10s  %-8s  %-4s  %-5s  %-9s%n",
                "ID", "SUBJ", "TITLE", "TYPE", "TIME", "Qs", "PTS", "STATUS")).append("\n");
        sb.append("  " + "─".repeat(95) + "\n\n");

        if (quizzes.isEmpty()) {
            String emptyLabel = (assessmentType == AssessmentType.EXAM) ? "exams" : "quizzes";
            sb.append("  ").append(TuiHelper.dim("No " + emptyLabel + " found. Press 'n' to create your first one!")).append("\n");
        } else {
            int windowSize = 5;
            int startRow = Math.max(0, Math.min(selectedIndex - 2, quizzes.size() - windowSize));
            int endRow = Math.min(quizzes.size(), startRow + windowSize);

            if (startRow > 0) {
                sb.append(TuiHelper.dim(String.format("  ▲ %d more %s above (Press ↑ to scroll)", startRow, itemType.toLowerCase()))).append("\n\n");
            }

            for (int i = startRow; i < endRow; i++) {
                Quiz q = quizzes.get(i);
                String cursor = (i == selectedIndex) ? TuiHelper.cyan("▶ ") : "  ";
                String status = q.isPublished() ? TuiHelper.green("Published") : TuiHelper.dim("Draft");
                String subj = q.getSubjectCode() != null ? q.getSubjectCode() : "-";
                String timeStr = q.getTimeLimitMins() != null && q.getTimeLimitMins() > 0 ? q.getTimeLimitMins() + "m" : "Untimed";
                String typeStr = q.getAssessmentType() == AssessmentType.EXAM
                        ? "[MIXED]"
                        : (q.getQuizQuestionType() != null ? "[" + q.getQuizQuestionType().name() + "]" : "[QUIZ]");

                String line = String.format("%-4d  %-12s  %-30s  %-10s  %-8s  %-4d  %-5.1f  %-9s",
                        q.getId(),
                        truncate(subj, 12),
                        truncate(q.getTitle(), 30),
                        typeStr,
                        timeStr,
                        q.getQuestionCount(),
                        q.getTotalPoints(),
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

            if (endRow < quizzes.size()) {
                sb.append("\n").append(TuiHelper.dim(String.format("  ▼ %d more %s below (Press ↓ to scroll)", quizzes.size() - endRow, itemType.toLowerCase()))).append("\n");
            }
        }

        sb.append("\n  " + "─".repeat(95) + "\n\n");

        if (!bannerMessage.isBlank()) {
            sb.append("  ").append(bannerMessage).append("\n\n");
        }

        String aiHint = (assessmentType == AssessmentType.EXAM) ? "[g] AI Exam" : "[g] AI Quiz";
        List<String> hints = List.of(
                "[↑/↓] Move",
                "[Enter] Builder",
                "[Space] Publish",
                "[n] New",
                aiHint,
                "[e] Edit",
                "[d] Delete",
                "[Esc] Back"
        );
        sb.append(TuiHelper.wrapHints(hints, 90));
        return sb.toString();
    }

    public static String renderQuizForm(boolean isEditMode, String subjectName, String title, String description,
                                        String timeLimit, String activeHours, String passScore,
                                        boolean randomizeQuestions, boolean randomizeAnswers, boolean showAnswersAfter,
                                        int focusedField, String errorMessage) {
        return renderQuizForm(AssessmentType.QUIZ, QuestionType.MCQ, isEditMode, subjectName, title, description,
                timeLimit, activeHours, passScore, randomizeQuestions, randomizeAnswers, showAnswersAfter, focusedField, errorMessage);
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
        sb.append(TuiHelper.header(headerTitle, String.format("Field %d of 10  •  Scroll with [Tab/↑/↓]", activeFieldDisplay)));
        sb.append("\n");

        int numInputFields = 10;
        int windowSize = 4;
        int startField = Math.max(0, Math.min(Math.min(focusedField, 9) - 1, numInputFields - windowSize));
        int endField = Math.min(numInputFields, startField + windowSize);

        if (startField > 0) {
            sb.append(TuiHelper.dim(String.format("  ▲ %d more fields above (Press ↑ to scroll)", startField))).append("\n");
        }

        for (int f = startField; f < endField; f++) {
            switch (f) {
                case 0 -> sb.append(TuiHelper.inputBox("Subject (Required)", subjectName, focusedField == 0, 86, false, "e.g. Java, Python, English, Math"));
                case 1 -> sb.append(TuiHelper.inputBox(itemType + " Title (Required)", title, focusedField == 1, 86, false, "e.g. Midterm Assessment"));
                case 2 -> {
                    if (assessmentType == AssessmentType.QUIZ) {
                        String typeLabel = (quizQuestionType != null) ? switch (quizQuestionType) {
                            case MCQ -> "Multiple Choice (MCQ)";
                            case TRUE_FALSE -> "True / False";
                            case SHORT_ANSWER -> "Short Answer";
                        } : "Multiple Choice (MCQ)";
                        sb.append(TuiHelper.selectBox("Quiz Question Type (Strict)", typeLabel, focusedField == 2, 86, "Space or ←/→ to switch"));
                    } else {
                        sb.append(TuiHelper.selectBox("Assessment Mode", "Exam (Mixed - All Question Types Allowed)", focusedField == 2, 86, "Comprehensive Exam"));
                    }
                }
                case 3 -> sb.append(TuiHelper.inputBox("Description", description, focusedField == 3, 86, false, "optional instructions"));
                case 4 -> sb.append(TuiHelper.inputBox("Time Limit (Minutes)", timeLimit, focusedField == 4, 86, false, "0 for untimed"));
                case 5 -> sb.append(TuiHelper.inputBox("Active Lifetime (Hours)", activeHours, focusedField == 5, 86, false, "0 for Available Forever"));
                case 6 -> sb.append(TuiHelper.inputBox("Passing Score (%)", passScore, focusedField == 6, 86, false, "e.g. 50"));
                case 7 -> {
                    String rqText = randomizeQuestions ? "Enabled" : "Disabled";
                    sb.append(TuiHelper.selectBox("Randomize Question Order", rqText, focusedField == 7, 86, "Space to toggle"));
                }
                case 8 -> {
                    String raText = randomizeAnswers ? "Enabled" : "Disabled";
                    sb.append(TuiHelper.selectBox("Shuffle Answer Options", raText, focusedField == 8, 86, "Space to toggle"));
                }
                case 9 -> {
                    String saText = showAnswersAfter ? "Enabled" : "Disabled";
                    sb.append(TuiHelper.selectBox("Review Answers on Submit", saText, focusedField == 9, 86, "Space to toggle"));
                }
            }
            sb.append("\n");
        }

        if (endField < numInputFields) {
            sb.append(TuiHelper.dim(String.format("  ▼ %d more fields below (Press Tab/↓ to scroll)", numInputFields - endField))).append("\n");
        }

        sb.append("\n");
        sb.append(TuiHelper.buttonRow("Submit", focusedField == 10, "Cancel", focusedField == 11)).append("\n\n");

        if (!errorMessage.isBlank()) {
            sb.append("  ").append(TuiHelper.red("✖ " + errorMessage)).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [Tab/↑/↓] Switch Field  •  [Enter] Confirm / Next  •  [Esc] Cancel\n"));
        return sb.toString();
    }

    public static String renderQuizQuestionEditor(Quiz quiz, String subjectText, List<Question> questions,
                                                 double totalPoints, int selectedIndex, String bannerMessage) {
        StringBuilder sb = new StringBuilder();
        String subtitle = String.format("Subject: %s  •  %d Questions (%.1f pts)  •  %s",
                subjectText, questions.size(), totalPoints,
                quiz.isPublished() ? TuiHelper.green("PUBLISHED") : TuiHelper.dim("DRAFT"));

        String builderTitle = (quiz.getAssessmentType() == AssessmentType.EXAM ? "EXAM BUILDER: " : "QUIZ BUILDER: ") + quiz.getTitle();
        sb.append(TuiHelper.header(builderTitle, subtitle));
        sb.append("\n");

        sb.append(String.format("  %-4s  %-12s  %-10s  %-6s  %-54s%n",
                "ID", "TYPE", "DIFF", "PTS", "QUESTION TEXT")).append("\n");
        sb.append("  " + "─".repeat(95) + "\n\n");

        if (questions.isEmpty()) {
            sb.append("  ").append(TuiHelper.dim("No questions in this quiz yet. Press 'n' to add or 'g' to generate with AI.")).append("\n");
        } else {
            int windowSize = 5;
            int startRow = Math.max(0, Math.min(selectedIndex - 2, questions.size() - windowSize));
            int endRow = Math.min(questions.size(), startRow + windowSize);

            if (startRow > 0) {
                sb.append(TuiHelper.dim(String.format("  ▲ %d more questions above (Press ↑ to scroll)", startRow))).append("\n\n");
            }

            for (int i = startRow; i < endRow; i++) {
                Question q = questions.get(i);
                String cursor = (i == selectedIndex) ? TuiHelper.cyan("▶ ") : "  ";
                String line = String.format("%-4d  %-12s  %-10s  %-6.1f  %-54s",
                        q.getId(),
                        truncate(q.getQuestionType().name(), 12),
                        truncate(q.getDifficulty().name(), 10),
                        q.getPoints(),
                        truncate(q.getQuestionText(), 54));

                if (i == selectedIndex) {
                    sb.append(TuiHelper.cyan(cursor + line)).append("\n");
                } else {
                    sb.append(cursor).append(line).append("\n");
                }
                if (i < endRow - 1) {
                    sb.append("\n");
                }
            }

            if (endRow < questions.size()) {
                sb.append("\n").append(TuiHelper.dim(String.format("  ▼ %d more questions below (Press ↓ to scroll)", questions.size() - endRow))).append("\n");
            }
        }

        sb.append("\n  " + "─".repeat(95) + "\n\n");

        if (!bannerMessage.isBlank()) {
            sb.append("  ").append(bannerMessage).append("\n\n");
        }

        List<String> hints = List.of(
                "[↑/↓] Move",
                "[Enter/e] Edit",
                "[n] Add Question",
                "[g] AI Generate",
                "[Space] Publish",
                "[d] Delete",
                "[Esc] Back"
        );
        sb.append(TuiHelper.wrapHints(hints, 90));
        return sb.toString();
    }

    public static String renderQuizQuestionAssignment(Quiz quiz, List<Question> bankQuestions, Set<Integer> assignedIds,
                                                     int selectedIndex, String bannerMessage) {
        StringBuilder sb = new StringBuilder();
        double totalPts = 0;
        for (Question q : bankQuestions) {
            if (assignedIds.contains(q.getId())) {
                totalPts += q.getPoints();
            }
        }

        String subtitle = String.format("Assigned: %d Questions (%.1f pts)  •  Bank: %d Available",
                assignedIds.size(), totalPts, bankQuestions.size());
        sb.append(TuiHelper.header("MANAGE QUESTIONS: " + quiz.getTitle(), subtitle));
        sb.append("\n");

        sb.append(String.format("  %-4s  %-14s  %-12s  %-10s  %-6s  %-40s%n",
                "SEL", "ID", "TYPE", "DIFF", "PTS", "QUESTION TEXT")).append("\n");
        sb.append("  " + "─".repeat(95) + "\n\n");

        if (bankQuestions.isEmpty()) {
            sb.append("  ").append(TuiHelper.dim("No questions available for this subject. Create questions first in Question Bank.")).append("\n\n");
        } else {
            int windowSize = 5;
            int startRow = Math.max(0, Math.min(selectedIndex - 2, bankQuestions.size() - windowSize));
            int endRow = Math.min(bankQuestions.size(), startRow + windowSize);

            for (int i = startRow; i < endRow; i++) {
                Question q = bankQuestions.get(i);
                boolean isAssigned = assignedIds.contains(q.getId());
                String checkbox = isAssigned ? TuiHelper.green("[✔] Assigned  ") : TuiHelper.dim("[ ] Unassigned");
                String cursor = (i == selectedIndex) ? TuiHelper.cyan("▶ ") : "  ";

                String line = String.format("%s  %-4d  %-12s  %-10s  %-6.1f  %-40s",
                        checkbox,
                        q.getId(),
                        truncate(q.getQuestionType().name(), 12),
                        truncate(q.getDifficulty().name(), 10),
                        q.getPoints(),
                        truncate(q.getQuestionText(), 40));

                if (i == selectedIndex) {
                    sb.append(TuiHelper.cyan(cursor + line)).append("\n");
                } else {
                    sb.append(cursor).append(line).append("\n");
                }
                if (i < bankQuestions.size() - 1 && i < endRow - 1) {
                    sb.append("\n");
                }
            }
            if (endRow < bankQuestions.size()) {
                sb.append("\n").append(TuiHelper.dim(String.format("  ▼ %d more questions below (Press ↓ to scroll)", bankQuestions.size() - endRow))).append("\n");
            }
            sb.append("\n  " + "─".repeat(95) + "\n\n");
        }

        if (!bannerMessage.isBlank()) {
            sb.append("  ").append(bannerMessage).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [↑/↓] Move  •  [Space/Enter] Toggle Question  •  [Esc] Back\n"));
        return sb.toString();
    }

    public static String renderAIQuizLoading(AssessmentType assessmentType, String title, int tick) {
        StringBuilder sb = new StringBuilder();
        String itemType = (assessmentType == AssessmentType.EXAM) ? "EXAM" : "QUIZ";
        String itemLabel = (assessmentType == AssessmentType.EXAM) ? "Exam" : "Quiz";
        sb.append(TuiHelper.header("GENERATING " + itemType + ": " + title.toUpperCase(), "Local Ollama LLM is assembling questions..."));
        sb.append("\n\n");

        String[] spinnerFrames = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
        String spinner = spinnerFrames[Math.abs(tick) % spinnerFrames.length];

        sb.append("  ").append(TuiHelper.cyan(spinner)).append(" ").append(TuiHelper.bold("Crafting high-quality academic questions with AI...")).append("\n\n");
        sb.append("  ").append(TuiHelper.dim("Synthesizing questions, options, rubrics, and assembling the " + itemLabel.toLowerCase() + " in the database...")).append("\n\n");
        sb.append("  ").append(TuiHelper.dim("You will be automatically redirected to the Question Editor once complete.\n\n"));
        sb.append("  ").append(TuiHelper.dim("[Esc] Cancel\n"));
        return sb.toString();
    }

    public static String renderAIQuizForm(AssessmentType assessmentType, String subjectName, String titleBuffer, String customPrompt, String countBuffer,
                                         String mcqCountBuffer, String tfCountBuffer, String saCountBuffer,
                                         String questionTypeLabel, Difficulty selectedDifficulty, int mcqOptionCount,
                                         String timeLimitBuffer, String activeHours, String passScore,
                                         boolean randomizeQuestions, boolean randomizeAnswers, boolean showAnswersAfter,
                                         int focusedField, String bannerMessage) {
        StringBuilder sb = new StringBuilder();
        String itemType = (assessmentType == AssessmentType.EXAM) ? "EXAM" : "QUIZ";
        String itemLabel = (assessmentType == AssessmentType.EXAM) ? "Exam" : "Quiz";
        boolean isMixed = questionTypeLabel.contains("MIXED");

        List<String> fieldWidgets = new ArrayList<>();
        fieldWidgets.add(TuiHelper.inputBox("Subject (Required)", subjectName, focusedField == 0, 86, false, "e.g. Java, Python, English, Math"));
        fieldWidgets.add(TuiHelper.inputBox(itemLabel + " Title / Topic (Required)", titleBuffer, focusedField == 1, 86, false, "e.g. Basic HTML, OOP Concepts"));
        fieldWidgets.add(TuiHelper.inputBox("Custom Prompt / Instructions (Optional)", customPrompt, focusedField == 2, 86, false, "e.g. Focus on edge cases, avoid multi-threading, include code snippets"));
        fieldWidgets.add(TuiHelper.selectBox("Question Type", questionTypeLabel, focusedField == 3, 86, "Space or ←/→ to cycle"));

        int curIdx = 4;
        if (isMixed) {
            fieldWidgets.add(TuiHelper.inputBox("MCQ Question Count (0-10)", mcqCountBuffer, focusedField == curIdx++, 86, false, "e.g. 2"));
            fieldWidgets.add(TuiHelper.inputBox("True/False Question Count (0-10)", tfCountBuffer, focusedField == curIdx++, 86, false, "e.g. 2"));
            fieldWidgets.add(TuiHelper.inputBox("Short Answer Question Count (0-10)", saCountBuffer, focusedField == curIdx++, 86, false, "e.g. 1"));
        } else {
            fieldWidgets.add(TuiHelper.inputBox("Number of Questions (1-10)", countBuffer, focusedField == curIdx++, 86, false, "e.g. 5"));
        }

        fieldWidgets.add(TuiHelper.selectBox("Difficulty Level", selectedDifficulty.name(), focusedField == curIdx++, 86, "Space to cycle"));

        boolean showMcq = isMixed ? (!"0".equals(mcqCountBuffer != null ? mcqCountBuffer.trim() : "0")) : questionTypeLabel.contains("MCQ");
        if (showMcq) {
            String optLabel = mcqOptionCount + " Options per Question";
            fieldWidgets.add(TuiHelper.selectBox("MCQ Option Count", optLabel, focusedField == curIdx++, 86, "Space to cycle (2, 3, 4)"));
        }

        fieldWidgets.add(TuiHelper.inputBox("Time Limit (Minutes)", timeLimitBuffer, focusedField == curIdx++, 86, false, "0 for untimed"));
        fieldWidgets.add(TuiHelper.inputBox("Active Lifetime (Hours)", activeHours, focusedField == curIdx++, 86, false, "0 for Available Forever"));
        fieldWidgets.add(TuiHelper.inputBox("Passing Score (%)", passScore, focusedField == curIdx++, 86, false, "e.g. 50"));

        String rqText = randomizeQuestions ? "Enabled" : "Disabled";
        fieldWidgets.add(TuiHelper.selectBox("Randomize Question Order", rqText, focusedField == curIdx++, 86, "Space to toggle"));

        String raText = randomizeAnswers ? "Enabled" : "Disabled";
        fieldWidgets.add(TuiHelper.selectBox("Shuffle Answer Options", raText, focusedField == curIdx++, 86, "Space to toggle"));

        String saText = showAnswersAfter ? "Enabled" : "Disabled";
        fieldWidgets.add(TuiHelper.selectBox("Review Answers on Submit", saText, focusedField == curIdx++, 86, "Space to toggle"));

        int numInputFields = fieldWidgets.size();
        int activeFieldDisplay = Math.min(numInputFields, focusedField + 1);
        sb.append(TuiHelper.header("AI " + itemType + " GENERATOR", String.format("Field %d of %d  •  Scroll with [Tab/↑/↓]", activeFieldDisplay, numInputFields)));
        sb.append("\n");

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
        sb.append(TuiHelper.buttonRow("Generate " + itemLabel, focusedField == numInputFields, "Cancel", focusedField == numInputFields + 1)).append("\n\n");

        if (!bannerMessage.isBlank()) {
            sb.append("  ").append(bannerMessage).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [Tab/↑/↓] Switch Field  •  [Enter] Confirm / Next  •  [Esc] Cancel\n"));
        return sb.toString();
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}