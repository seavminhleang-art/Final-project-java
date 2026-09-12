package com.proctor.view;

import com.proctor.model.entity.User;
import com.proctor.model.entity.Attempt;
import com.proctor.model.entity.AttemptAnswer;
import com.proctor.model.entity.Question;
import com.proctor.model.entity.QuestionOption;
import com.proctor.model.entity.Quiz;
import com.proctor.util.TuiHelper;

import java.text.SimpleDateFormat;
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
        String title = (specificQuiz != null)
                ? "Quiz: " + specificQuiz.getTitle()
                : "Student Submissions & Grading";

        sb.append(TuiHelper.header("SUBMISSIONS"));
        sb.append("\n");
        String statusLabel = (statusFilterDisplay == null || statusFilterDisplay.isBlank()) ? "ALL" : statusFilterDisplay;
        sb.append(TuiHelper.boxTitle(title, String.format("Status: [ %s ]  •  Total Submissions: %d", statusLabel, submissions.size()))).append("\n\n");

        if (searchMode) {
            sb.append("  Search: [ ").append(TuiHelper.cyan(searchBuffer + "_")).append(" ] (Press Enter to finish)\n\n");
        } else if (searchBuffer != null && !searchBuffer.isEmpty()) {
            sb.append("  Search: [ ").append(searchBuffer).append(" ] (Press '/' to edit)\n\n");
        }

        if (specificQuiz == null) {
            sb.append(String.format("  %-7s  %-42s  %-36s  %-18s  %-21s%n",
                    "#", "ASSESSMENT", "STUDENT", "STATUS", "SUBMITTED AT")).append("\n");
        } else {
            sb.append(String.format("  %-7s  %-68s  %-24s  %-25s%n",
                    "#", "STUDENT", "STATUS", "SUBMITTED AT")).append("\n");
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
                String cursor = (i == selectedIndex) ? TuiHelper.cyan("▶ ") : "  ";
                User student = studentMap.get(a.getStudentId());
                String studentName = (a.getStudentName() != null && !a.getStudentName().isBlank())
                        ? a.getStudentName()
                        : (student != null ? student.getFullName() : "Student #" + a.getStudentId());
                String dateStr = a.getSubmittedAt() != null ? dateFormat.format(a.getSubmittedAt()) : "-";

                int statusWidth = (specificQuiz == null) ? 18 : 24;
                String statusStr;
                if (a.getStatus() == com.proctor.model.enums.AttemptStatus.GRADED) {
                    statusStr = TuiHelper.green(String.format("%-" + statusWidth + "s", "GRADED"));
                } else if (a.getStatus() == com.proctor.model.enums.AttemptStatus.AUTO_SUBMITTED || a.getStatus() == com.proctor.model.enums.AttemptStatus.TURNED_IN) {
                    statusStr = TuiHelper.yellow(String.format("%-" + statusWidth + "s", "PENDING REVIEW"));
                } else {
                    statusStr = TuiHelper.dim(String.format("%-" + statusWidth + "s", "IN PROGRESS"));
                }

                String line;
                if (specificQuiz == null) {
                    String quizTitle = a.getQuizTitle() != null ? a.getQuizTitle() : "Quiz #" + a.getQuizId();
                    line = String.format("%-7d  %-42s  %-36s  %s  %-21s",
                            (i + 1),
                            truncate(quizTitle, 42),
                            truncate(studentName, 36),
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

        if (!submissions.isEmpty()) {
            int pageSize = TuiHelper.PAGE_SIZE;
            int totalPages = Math.max(1, (int) Math.ceil((double) submissions.size() / pageSize));
            int currentPage = selectedIndex / pageSize;
            sb.append(TuiHelper.paginationBar(currentPage, totalPages, submissions.size()));
        }

        if (!bannerMessage.isBlank()) {
            sb.append("  ").append(bannerMessage).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [↑/↓] Move  •  [←/→] Page  •  [/] Search  •  [f] Filter  •  [Enter] Inspect  •  [g] AI Grade  •  [r] Return Grade  •  [Esc] Back\n"));
        return sb.toString();
    }

    public static String renderAnswerSheet(Quiz specificQuiz, Attempt attempt,
                                           Map<Integer, AttemptAnswer> answerMap,
                                           int inspectingAnswerIndex, String bannerMessage) {
        StringBuilder sb = new StringBuilder();
        List<Question> questions = (specificQuiz != null) ? specificQuiz.getQuestions() : List.of();
        String studentLabel = (attempt.getStudentName() != null && !attempt.getStudentName().isBlank())
                ? attempt.getStudentName()
                : "Student #" + attempt.getStudentId();
        String subtitle = String.format("%s  •  Status: %s  •  Question %d of %d",
                studentLabel, attempt.getStatus().name(),
                questions.isEmpty() ? 0 : inspectingAnswerIndex + 1, questions.size());

        sb.append(TuiHelper.header("SUBMISSIONS"));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle("Student Answer Sheet: Attempt #" + attempt.getId(), subtitle)).append("\n\n");

        if (questions.isEmpty()) {
            sb.append("  ").append(TuiHelper.dim("No questions attached to this quiz assessment.")).append("\n");
        } else {
            int currentPage = Math.max(0, Math.min(inspectingAnswerIndex, questions.size() - 1));
            Question q = questions.get(currentPage);
            AttemptAnswer ans = answerMap.get(q.getId());

            sb.append(TuiHelper.cyan("▶ ")).append(TuiHelper.bold(String.format("Q%d. %s [%s, %.1f pts]", currentPage + 1, q.getQuestionText(), q.getDifficulty().name(), q.getPoints()))).append("\n\n");

            if (q.getQuestionType() == com.proctor.model.enums.QuestionType.MCQ || q.getQuestionType() == com.proctor.model.enums.QuestionType.TRUE_FALSE) {
                Integer chosenOptId = (ans != null) ? ans.getSelectedOptionId() : null;
                if (q.getOptions() != null) {
                    for (QuestionOption opt : q.getOptions()) {
                        boolean isChosen = chosenOptId != null && chosenOptId.equals(opt.getId());
                        if (opt.isCorrect()) {
                            sb.append("     ").append(TuiHelper.green("✔ [Model Answer] " + opt.getOptionText() + (isChosen ? " (Student Choice)" : ""))).append("\n");
                        } else if (isChosen) {
                            sb.append("     ").append(TuiHelper.red("✖ [Student Choice] " + opt.getOptionText())).append("\n");
                        } else {
                            sb.append("     ").append(TuiHelper.dim("• " + opt.getOptionText())).append("\n");
                        }
                    }
                }
            } else {
                String textAns = (ans != null && ans.getTextAnswer() != null) ? ans.getTextAnswer() : "(No answer provided)";
                sb.append("     ").append(TuiHelper.cyan("Student Answer: ")).append(textAns).append("\n");
                if (q.getExplanation() != null && !q.getExplanation().isBlank()) {
                    sb.append("     ").append(TuiHelper.dim("Grading Context: " + q.getExplanation())).append("\n");
                }
            }

            if (ans != null) {
                String feedback = ans.getAiFeedback() != null ? " - " + ans.getAiFeedback() : "";
                sb.append("\n     ").append(TuiHelper.bold(TuiHelper.green(String.format("Score: %.1f / %.1f pts%s", ans.getPointsAwarded(), q.getPoints(), feedback)))).append("\n");
            }
        }

        sb.append("\n  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

        if (!questions.isEmpty()) {
            sb.append(TuiHelper.paginationBar(inspectingAnswerIndex, questions.size(), questions.size()));
        }

        if (!bannerMessage.isBlank()) {
            sb.append("  ").append(bannerMessage).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [↑/↓/←/→] Navigate  •  [g] AI Grade  •  [r] Return Grade  •  [Esc] Back\n"));
        return sb.toString();
    }

    public static String renderAIGradingLoading(String studentName, String quizTitle, int spinnerTick) {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("SUBMISSIONS"));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle("AI Grading & Evaluation", "Local Ollama LLM is evaluating short answers...")).append("\n\n");

        String[] spinners = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
        String icon = spinners[Math.abs(spinnerTick) % spinners.length];

        String studentInfo = (studentName != null && !studentName.isBlank()) ? studentName : "Student Submission";
        String quizInfo = (quizTitle != null && !quizTitle.isBlank()) ? " (" + quizTitle + ")" : "";

        sb.append("  ").append(TuiHelper.cyan(icon)).append(" ").append(TuiHelper.bold("Evaluating: " + studentInfo + quizInfo)).append("\n\n");
        sb.append("  ").append(TuiHelper.dim("Ollama LLM is assessing short-answer conceptual accuracy against model answers and rubrics...")).append("\n\n");
        sb.append("  ").append(TuiHelper.dim("Please wait, results and suggested points will appear once evaluation completes.\n\n"));
        sb.append("  ").append(TuiHelper.dim("[Esc] Cancel evaluation\n"));
        return sb.toString();
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}