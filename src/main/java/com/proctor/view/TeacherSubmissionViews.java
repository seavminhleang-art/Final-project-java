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
        StringBuilder sb = new StringBuilder();
        String title = (specificQuiz != null)
                ? "Quiz: " + specificQuiz.getTitle()
                : "Student Submissions & Grading";

        sb.append(TuiHelper.header("SUBMISSIONS"));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle(title, String.format("Total Submissions: %d", submissions.size()))).append("\n\n");

        sb.append(String.format("  %-8s  %-34s  %-24s  %-22s%n",
                "ID", "STUDENT", "STATUS", "SUBMITTED AT")).append("\n");
        sb.append("  " + "─".repeat(95) + "\n\n");

        if (submissions.isEmpty()) {
            sb.append("  ").append(TuiHelper.dim("No student submissions found for this assessment.")).append("\n");
        } else {
            int pageSize = 5;
            int startRow = (selectedIndex / pageSize) * pageSize;
            int endRow = Math.min(submissions.size(), startRow + pageSize);

            for (int i = startRow; i < endRow; i++) {
                Attempt a = submissions.get(i);
                String cursor = (i == selectedIndex) ? TuiHelper.cyan("▶ ") : "  ";
                User student = studentMap.get(a.getStudentId());
                String studentName = student != null ? student.getFullName() : "Student #" + a.getStudentId();
                String dateStr = a.getSubmittedAt() != null ? dateFormat.format(a.getSubmittedAt()) : "-";

                String statusStr;
                if (a.getStatus() == com.proctor.model.enums.AttemptStatus.GRADED) {
                    statusStr = TuiHelper.green(String.format("%-24s", "GRADED"));
                } else if (a.getStatus() == com.proctor.model.enums.AttemptStatus.SUBMITTED || a.getStatus() == com.proctor.model.enums.AttemptStatus.AUTO_SUBMITTED || a.getStatus() == com.proctor.model.enums.AttemptStatus.TURNED_IN) {
                    statusStr = TuiHelper.yellow(String.format("%-24s", "PENDING REVIEW"));
                } else {
                    statusStr = TuiHelper.dim(String.format("%-24s", "IN PROGRESS"));
                }

                String line = String.format("#%-7d  %-34s  %s  %-22s",
                        a.getId(),
                        truncate(studentName, 34),
                        statusStr,
                        dateStr);

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

        if (!submissions.isEmpty()) {
            int pageSize = 5;
            int totalPages = Math.max(1, (int) Math.ceil((double) submissions.size() / pageSize));
            int currentPage = selectedIndex / pageSize;
            sb.append(TuiHelper.paginationBar(currentPage, totalPages, submissions.size()));
        }

        if (!bannerMessage.isBlank()) {
            sb.append("  ").append(bannerMessage).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [↑/↓] Move  •  [←/→] Page  •  [Enter] Inspect  •  [g] AI Grade  •  [r] Return Grade  •  [Esc] Back\n"));
        return sb.toString();
    }

    public static String renderAnswerSheet(Quiz specificQuiz, Attempt attempt,
                                           Map<Integer, AttemptAnswer> answerMap,
                                           int inspectingAnswerIndex, String bannerMessage) {
        StringBuilder sb = new StringBuilder();
        List<Question> questions = (specificQuiz != null) ? specificQuiz.getQuestions() : List.of();
        String subtitle = String.format("Student #%d  •  Status: %s  •  Question %d of %d",
                attempt.getStudentId(), attempt.getStatus().name(),
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

        sb.append("\n  " + "─".repeat(95) + "\n\n");

        if (!questions.isEmpty()) {
            sb.append(TuiHelper.paginationBar(inspectingAnswerIndex, questions.size(), questions.size()));
        }

        if (!bannerMessage.isBlank()) {
            sb.append("  ").append(bannerMessage).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [↑/↓/←/→] Navigate  •  [g] AI Grade  •  [r] Return Grade  •  [Esc] Back\n"));
        return sb.toString();
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}