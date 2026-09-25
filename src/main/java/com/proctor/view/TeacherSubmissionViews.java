package com.proctor.view;

import com.proctor.model.entity.User;
import com.proctor.model.entity.Attempt;
import com.proctor.model.entity.AttemptAnswer;
import com.proctor.model.entity.Question;
import com.proctor.model.entity.QuestionOption;
import com.proctor.model.entity.Quiz;
import com.proctor.model.enums.AssessmentType;
import com.proctor.util.TuiHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TeacherSubmissionViews {

    public static String renderSubmissionList(Quiz specificQuiz, List<Attempt> submissions,
                                             Map<Integer, User> studentMap, int selectedIndex,
                                             SimpleDateFormat dateFormat, String bannerMessage) {
        return renderSubmissionList(specificQuiz, submissions, studentMap, selectedIndex, dateFormat, "ALL", "", false, bannerMessage);
    }

    public static String renderSubmissionList(Quiz specificQuiz, List<Attempt> submissions,
                                             Map<Integer, User> studentMap, int selectedIndex,
                                             SimpleDateFormat dateFormat, String statusFilterDisplay,
                                             String searchBuffer, boolean searchMode, String bannerMessage) {
        StringBuilder sb = new StringBuilder();
        String quizTypePrefix = (specificQuiz != null)
                ? (specificQuiz.getAssessmentType() == AssessmentType.SPEED ? "Speed Quiz: " : (specificQuiz.getAssessmentType() == AssessmentType.EXAM ? "Exam: " : "Quiz: "))
                : "";
        String title = (specificQuiz != null)
                ? quizTypePrefix + specificQuiz.getTitle()
                : "Student Submissions & Grading";

        sb.append(TuiHelper.header("SUBMISSIONS"));
        sb.append("\n");
        String statusLabel = (statusFilterDisplay == null || statusFilterDisplay.isBlank()) ? "ALL" : statusFilterDisplay;
        int activeTab = switch (statusLabel.toUpperCase()) {
            case "PENDING REVIEW" -> 1;
            case "GRADED" -> 2;
            default -> 0;
        };
        sb.append(TuiHelper.boxTitle(title, String.format("Total Submissions: %d", submissions.size()))).append("\n\n");
        sb.append(TuiHelper.tabBar(new String[]{"All", "Pending Review", "Graded"}, activeTab)).append("\n\n");

        if (searchMode) {
            sb.append("  Search: [ ").append(TuiHelper.cyan(TuiHelper.truncate(searchBuffer, 50) + "_")).append(" ] (Press Enter to finish)\n\n");
        } else if (searchBuffer != null && !searchBuffer.isEmpty()) {
            sb.append("  Search: [ ").append(TuiHelper.truncate(searchBuffer, 50)).append(" ] (Press '/' to edit)\n\n");
        }

        if (specificQuiz == null) {
            sb.append(String.format("  %-5s  %-7s  %-36s  %-32s  %-18s  %-19s\n",
                    "#", "TYPE", "ASSESSMENT", "STUDENT", "STATUS", "SUBMITTED AT"));
        } else {
            sb.append(String.format("  %-7s  %-68s  %-24s  %-25s\n",
                    "#", "STUDENT", "STATUS", "SUBMITTED AT"));
        }
        sb.append("  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

        if (submissions.isEmpty()) {
            sb.append("  ").append(TuiHelper.dim("No student submissions found for this assessment.")).append("\n");
        } else {
            int pageSize = TuiHelper.PAGE_SIZE;
            int startRow = (selectedIndex / pageSize) * pageSize;
            int endRow = Math.min(submissions.size(), startRow + pageSize);

            for (int i = startRow; i < endRow; i++) {
                Attempt a = submissions.get(i);
                User student = studentMap.get(a.getStudentId());
                String studentName = (a.getStudentName() != null && !a.getStudentName().isBlank())
                        ? a.getStudentName()
                        : (student != null ? student.getFullName() : "Student #" + a.getStudentId());
                String dateStr = a.getSubmittedAt() != null ? dateFormat.format(a.getSubmittedAt()) : "-";

                int statusWidth = (specificQuiz == null) ? 18 : 24;
                String statusStr;
                if (a.getStatus() == com.proctor.model.enums.AttemptStatus.GRADED) {
                    statusStr = TuiHelper.green(String.format("%-" + statusWidth + "s", "GRADED"));
                } else if (a.getStatus() == com.proctor.model.enums.AttemptStatus.AUTO_SUBMITTED) {
                    if (a.isGraded()) {
                        statusStr = TuiHelper.green(String.format("%-" + statusWidth + "s", "AUTO (GRADED)"));
                    } else {
                        statusStr = TuiHelper.yellow(String.format("%-" + statusWidth + "s", "AUTO (PENDING)"));
                    }
                } else if (a.getStatus() == com.proctor.model.enums.AttemptStatus.TURNED_IN) {
                    statusStr = TuiHelper.yellow(String.format("%-" + statusWidth + "s", "PENDING REVIEW"));
                } else {
                    statusStr = TuiHelper.dim(String.format("%-" + statusWidth + "s", "IN PROGRESS"));
                }

                String line;
                if (specificQuiz == null) {
                    String quizTitle = a.getQuizTitle() != null ? a.getQuizTitle() : "Quiz #" + a.getQuizId();
                    String typeStr = (a.getAssessmentType() == AssessmentType.SPEED) ? "SPEED"
                            : (a.getAssessmentType() == AssessmentType.EXAM ? "EXAM" : "QUIZ");
                    line = String.format("%-5d  %-7s  %-36s  %-32s  %s  %-19s",
                            (i + 1),
                            typeStr,
                            truncate(quizTitle, 36),
                            truncate(studentName, 32),
                            statusStr,
                            dateStr);
                } else {
                    line = String.format("%-7d  %-68s  %s  %-25s",
                            (i + 1),
                            truncate(studentName, 68),
                            statusStr,
                            dateStr);
                }

                if (i == selectedIndex) {
                    sb.append("  ").append(TuiHelper.bold(line)).append("\n");
                } else {
                    sb.append("  ").append(line).append("\n");
                }
                if (i < endRow - 1) {
                    sb.append("\n");
                }
            }
        }

        sb.append("\n  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

        if (!submissions.isEmpty()) {
            int pageSize = TuiHelper.PAGE_SIZE;
            int totalPages = Math.max(1, (int) Math.ceil((double) submissions.size() / pageSize));
            int currentPage = selectedIndex / pageSize;
            sb.append(TuiHelper.paginationBar(currentPage, totalPages, submissions.size()));
        }

        if (!bannerMessage.isBlank()) {
            sb.append("  ").append(bannerMessage).append("\n\n");
        }

        boolean isSpeedContext = (specificQuiz != null && specificQuiz.getAssessmentType() == AssessmentType.SPEED);
        if (isSpeedContext) {
            sb.append(TuiHelper.dim("  [↑/↓] Move  •  [←/→] Page  •  [/] Search  •  [Tab] Tab  •  [Enter] Inspect  •  [Esc] Back\n"));
        } else {
            sb.append(TuiHelper.dim("  [↑/↓] Move  •  [←/→] Page  •  [/] Search  •  [Tab] Tab  •  [Enter] Inspect  •  [g] AI Grade  •  [r] Return Grade  •  [Esc] Back\n"));
        }
        return sb.toString();
    }

    public static String renderAnswerSheet(Quiz specificQuiz, Attempt attempt,
                                           Map<Integer, AttemptAnswer> answerMap,
                                           int inspectingAnswerIndex, String bannerMessage) {
        StringBuilder sb = new StringBuilder();
        List<Question> questions = (specificQuiz != null && specificQuiz.getQuestions() != null) ? specificQuiz.getQuestions() : List.of();
        String studentLabel = (attempt.getStudentName() != null && !attempt.getStudentName().isBlank())
                ? attempt.getStudentName()
                : "Student #" + attempt.getStudentId();
        boolean isSpeedQuiz = (specificQuiz != null && specificQuiz.getAssessmentType() == AssessmentType.SPEED)
                || (attempt != null && attempt.getAssessmentType() == AssessmentType.SPEED);
        String typeLabel = isSpeedQuiz ? "Speed Quiz"
                : ((specificQuiz != null && specificQuiz.getAssessmentType() == AssessmentType.EXAM)
                || (attempt != null && attempt.getAssessmentType() == AssessmentType.EXAM) ? "Exam" : "Quiz");
        String subtitle = String.format("%s  •  Type: %s  •  Status: %s  •  Question %d of %d",
                studentLabel, typeLabel, (attempt != null ? attempt.getStatus().name() : "UNKNOWN"),
                questions.isEmpty() ? 0 : inspectingAnswerIndex + 1, questions.size());

        sb.append(TuiHelper.header("SUBMISSIONS"));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle("Student Answer Sheet: Attempt #" + (attempt != null ? attempt.getId() : "-"), subtitle)).append("\n\n");

        if (questions.isEmpty()) {
            sb.append("  ").append(TuiHelper.dim("No questions attached to this quiz assessment.")).append("\n");
        } else {
            int currentPage = Math.max(0, Math.min(inspectingAnswerIndex, questions.size() - 1));
            Question q = questions.get(currentPage);
            AttemptAnswer ans = answerMap.get(q.getId());

            String qHeader = String.format("Q%d. %s [%s, %.1f pts]", currentPage + 1, q.getQuestionText(), q.getDifficulty().name(), q.getPoints());
            List<String> qLines = wrapText(qHeader, 120);
            for (int k = 0; k < qLines.size(); k++) {
                if (k == 0) {
                    sb.append("  ").append(TuiHelper.bold(TuiHelper.NAVY_BLUE + qLines.get(k))).append("\n");
                } else {
                    sb.append("  ").append(TuiHelper.bold(qLines.get(k))).append("\n");
                }
            }
            sb.append("\n");

            if (q.getQuestionType() == com.proctor.model.enums.QuestionType.MCQ || q.getQuestionType() == com.proctor.model.enums.QuestionType.TRUE_FALSE) {
                Integer chosenOptId = (ans != null) ? ans.getSelectedOptionId() : null;
                if (q.getOptions() != null) {
                    for (int i = 0; i < q.getOptions().size(); i++) {
                        QuestionOption opt = q.getOptions().get(i);
                        boolean isChosen = chosenOptId != null && chosenOptId.equals(opt.getId());
                        String prefix = opt.isCorrect()
                                ? "✔ [Model Answer] " + opt.getOptionText() + (isChosen ? " (Student Choice)" : "")
                                : (isChosen ? "✖ [Student Choice] " + opt.getOptionText() : "• " + opt.getOptionText());
                        List<String> optLines = wrapText(prefix, 118);
                        for (int j = 0; j < optLines.size(); j++) {
                            String formatted = (j == 0 ? "     " : "       ") + optLines.get(j);
                            if (opt.isCorrect()) {
                                sb.append(TuiHelper.green(formatted)).append("\n");
                            } else if (isChosen) {
                                sb.append(TuiHelper.red(formatted)).append("\n");
                            } else {
                                sb.append(TuiHelper.dim(formatted)).append("\n");
                            }
                        }
                        if (i < q.getOptions().size() - 1) {
                            sb.append("\n");
                        }
                    }
                }
            } else {
                String textAns = (ans != null && ans.getTextAnswer() != null) ? ans.getTextAnswer() : "(No answer provided)";
                sb.append("     ").append(TuiHelper.cyan("Student Answer:")).append("\n");
                for (String line : wrapText(textAns, 116)) {
                    sb.append("       ").append(line).append("\n");
                }
                if (q.getExplanation() != null && !q.getExplanation().isBlank()) {
                    sb.append("     ").append(TuiHelper.dim("Grading Context:")).append("\n");
                    for (String line : wrapText(q.getExplanation(), 116)) {
                        sb.append("       ").append(TuiHelper.dim(line)).append("\n");
                    }
                }
            }

            if (ans != null) {
                boolean isCorrect = Boolean.TRUE.equals(ans.getCorrect());
                if (isSpeedQuiz) {
                    String statusText = isCorrect ? "Correct" : (ans.getSelectedOptionId() == null ? "Timed Out" : "Incorrect");
                    String scoreText = String.format("Score: %.1f pts (%s)", ans.getPointsAwarded(), statusText);
                    sb.append("\n     ").append(isCorrect ? TuiHelper.green(TuiHelper.bold(scoreText)) : TuiHelper.red(TuiHelper.bold(scoreText))).append("\n");
                } else {
                    String scoreHeader = String.format("Score: %.1f / %.1f pts", ans.getPointsAwarded(), q.getPoints());
                    if (ans.getAiScore() != null) {
                        scoreHeader += String.format(" (AI: %d%%)", ans.getAiScore());
                    }
                    sb.append("\n     ").append(isCorrect ? TuiHelper.green(TuiHelper.bold(scoreHeader)) : TuiHelper.dim(scoreHeader)).append("\n");
                    if (ans.getAiFeedback() != null && !ans.getAiFeedback().isBlank()) {
                        sb.append("     ").append(TuiHelper.dim("Feedback:")).append("\n");
                        for (String fLine : wrapText(ans.getAiFeedback(), 116)) {
                            sb.append("       ").append(TuiHelper.dim(fLine)).append("\n");
                        }
                    }
                    if (ans.getTeacherFeedback() != null && !ans.getTeacherFeedback().isBlank()) {
                        sb.append("     ").append(TuiHelper.dim("Teacher Notes:")).append("\n");
                        for (String fLine : wrapText(ans.getTeacherFeedback(), 116)) {
                            sb.append("       ").append(TuiHelper.dim(fLine)).append("\n");
                        }
                    }
                }
            } else {
                sb.append("\n     ").append(TuiHelper.dim("(Not attempted or reached)")).append("\n");
            }
        }

        sb.append("\n  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

        if (!questions.isEmpty()) {
            if (questions.size() <= 1) {
                sb.append("  ").append(TuiHelper.bold("Question 1 of 1")).append("\n\n");
            } else {
                String prevLabel = (inspectingAnswerIndex > 0) ? TuiHelper.cyan("◀ [←] Prev") : TuiHelper.dim("  [←] Prev");
                String nextLabel = (inspectingAnswerIndex < questions.size() - 1) ? TuiHelper.cyan("[→] Next ▶") : TuiHelper.dim("[→] Next  ");
                String qInfo = TuiHelper.bold(String.format("Question %d of %d", inspectingAnswerIndex + 1, questions.size()));
                sb.append(String.format("  " + TuiHelper.BUTTON_MARKER + "%s   %s   %s%n%n", prevLabel, qInfo, nextLabel));
            }
        }

        if (!bannerMessage.isBlank()) {
            sb.append("  ").append(bannerMessage).append("\n\n");
        }

        if (isSpeedQuiz) {
            sb.append(TuiHelper.dim("  [←/→] Navigate Questions  •  [Esc] Back\n"));
        } else {
            sb.append(TuiHelper.dim("  [←/→] Navigate Questions  •  [e] Grade Answer  •  [g] AI Grade  •  [r] Return Grade  •  [Esc] Back\n"));
        }
        return sb.toString();
    }

    public static String renderManualGradingModal(
            Question question,
            AttemptAnswer answer,
            int questionIndex,
            int totalQuestions,
            String pointsBuffer,
            String feedbackBuffer,
            int focusedField,
            String modalError
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("SUBMISSIONS"));
        sb.append("\n");
        String subtitle = String.format("Question %d of %d  •  Maximum: %.1f pts  •  Type: %s",
                questionIndex + 1, totalQuestions, question.getPoints(), question.getQuestionType().name());
        sb.append(TuiHelper.boxTitle("Manual Grade & Score Override", subtitle)).append("\n\n");

        String qHeader = String.format("Q%d. %s [%.1f pts]", questionIndex + 1, question.getQuestionText(), question.getPoints());
        for (String qLine : wrapText(qHeader, 120)) {
            sb.append("  ").append(TuiHelper.bold(TuiHelper.NAVY_BLUE + qLine)).append("\n");
        }
        sb.append("\n");

        if (question.getQuestionType() == com.proctor.model.enums.QuestionType.SHORT_ANSWER) {
            String textAns = (answer != null && answer.getTextAnswer() != null && !answer.getTextAnswer().isBlank())
                    ? answer.getTextAnswer() : "(No answer provided)";
            sb.append("  ").append(TuiHelper.cyan("Student Answer:")).append("\n");
            for (String line : wrapText(textAns, 116)) {
                sb.append("    ").append(line).append("\n");
            }
            if (question.getExplanation() != null && !question.getExplanation().isBlank()) {
                sb.append("  ").append(TuiHelper.dim("Grading Context:")).append("\n");
                for (String line : wrapText(question.getExplanation(), 116)) {
                    sb.append("    ").append(TuiHelper.dim(line)).append("\n");
                }
            }
        } else {
            Integer chosenOptId = (answer != null) ? answer.getSelectedOptionId() : null;
            if (question.getOptions() != null) {
                for (int i = 0; i < question.getOptions().size(); i++) {
                    QuestionOption opt = question.getOptions().get(i);
                    boolean isChosen = chosenOptId != null && chosenOptId.equals(opt.getId());
                    String prefix = opt.isCorrect()
                            ? "✔ [Model Answer] " + opt.getOptionText() + (isChosen ? " (Student Choice)" : "")
                            : (isChosen ? "✖ [Student Choice] " + opt.getOptionText() : "• " + opt.getOptionText());
                    sb.append("  ").append(opt.isCorrect() ? TuiHelper.green(prefix) : (isChosen ? TuiHelper.red(prefix) : TuiHelper.dim(prefix))).append("\n");
                }
            }
        }

        if (answer != null && answer.getAiScore() != null) {
            sb.append("\n  ").append(TuiHelper.cyan(String.format("AI Evaluation: %d%% score", answer.getAiScore())));
            if (answer.getAiFeedback() != null && !answer.getAiFeedback().isBlank()) {
                sb.append("  ").append(TuiHelper.dim("— " + answer.getAiFeedback()));
            }
            sb.append("\n");
        }

        sb.append("\n  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

        if (modalError != null && !modalError.isBlank()) {
            sb.append("  ").append(TuiHelper.red(modalError)).append("\n\n");
        }

        String ptsLabel = String.format("Points Awarded (0.0 - %.1f)", question.getPoints());
        sb.append(TuiHelper.inputBox(ptsLabel, pointsBuffer, focusedField == 0, 106, false, "e.g. " + question.getPoints())).append("\n");
        sb.append(TuiHelper.inputBox("Teacher Feedback / Notes (Optional)", feedbackBuffer, focusedField == 1, 106, false, "Enter feedback notes for student...")).append("\n\n");

        sb.append(TuiHelper.buttonRow("Save Grade", focusedField == 2, "Cancel", focusedField == 3, 106)).append("\n\n");

        sb.append(TuiHelper.dim("  [Tab/↑/↓] Switch Field  •  [Enter] Save / Next  •  [Esc] Cancel\n"));
        return sb.toString();
    }

    public static String renderAIGradingLoading(String studentName, String quizTitle, int spinnerTick) {
        return renderAIGradingLoading(studentName, quizTitle, spinnerTick, 0);
    }

    public static String renderAIGradingLoading(String studentName, String quizTitle, int spinnerTick, int elapsedSeconds) {
        String[] spinners = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
        String icon = spinners[Math.abs(spinnerTick) % spinners.length];

        String studentInfo = (studentName != null && !studentName.isBlank()) ? studentName : "Student Submission";
        String quizInfo = (quizTitle != null && !quizTitle.isBlank()) ? " (" + quizTitle + ")" : "";

        return TuiHelper.aiLoadingModal(
                "SUBMISSIONS",
                "AI Grading & Evaluation",
                "Local Ollama LLM is evaluating short answers...",
                icon,
                "Evaluating: " + studentInfo + quizInfo,
                "Local Ollama LLM",
                "Assessing short answers against model answers and rubrics...",
                "Suggested scores and actionable feedback will appear once evaluation completes.",
                "[Esc] Cancel Evaluation",
                elapsedSeconds
        );
    }

    private static String truncate(String text, int max) {
        return TuiHelper.truncate(text, max);
    }

    private static List<String> wrapText(String text, int maxLen) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isBlank()) {
            lines.add("");
            return lines;
        }
        String[] words = text.split("\\s+");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            if (cur.length() == 0) {
                cur.append(w);
            } else if (cur.length() + 1 + w.length() <= maxLen) {
                cur.append(" ").append(w);
            } else {
                lines.add(cur.toString());
                cur.setLength(0);
                cur.append(w);
            }
        }
        if (cur.length() > 0) {
            lines.add(cur.toString());
        }
        return lines;
    }
}