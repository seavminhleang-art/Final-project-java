package com.proctor.view;

import com.proctor.model.enums.AssessmentType;
import com.proctor.model.entity.Attempt;
import com.proctor.model.entity.ExamSession;
import com.proctor.model.entity.Question;
import com.proctor.model.entity.QuestionOption;
import com.proctor.model.entity.Quiz;
import com.proctor.model.entity.Result;
import com.proctor.util.TuiHelper;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

public class ExamViews {

    public static String renderAvailableQuizzes(List<Quiz> quizzes, Map<Integer, String> subjectNames,
                                               Map<Integer, Attempt> studentAttempts, int selectedIndex,
                                               String bannerMessage) {
        return renderAvailableQuizzes(AssessmentType.QUIZ, quizzes, subjectNames, studentAttempts, selectedIndex, bannerMessage);
    }

    public static String renderAvailableQuizzes(AssessmentType assessmentType, List<Quiz> quizzes, Map<Integer, String> subjectNames,
                                               Map<Integer, Attempt> studentAttempts, int selectedIndex,
                                               String bannerMessage) {
        StringBuilder sb = new StringBuilder();
        String typeLabel = (assessmentType == AssessmentType.EXAM) ? "EXAMS" : "QUIZZES";
        sb.append(TuiHelper.header("AVAILABLE " + typeLabel, String.format("Total: %d  •  Scroll with [↑/↓]", quizzes.size())));
        sb.append("\n");

        sb.append(String.format("  %-4s  %-14s  %-40s  %-10s  %-6s  %-12s%n",
                "ID", "SUBJ", "TITLE", "TIME", "PTS", "STATUS")).append("\n");
        sb.append("  " + "─".repeat(95) + "\n\n");

        if (quizzes.isEmpty()) {
            sb.append("  ").append(TuiHelper.dim("No " + typeLabel.toLowerCase() + " currently available.")).append("\n");
        } else {
            int windowSize = 5;
            int startRow = Math.max(0, Math.min(selectedIndex - 2, quizzes.size() - windowSize));
            int endRow = Math.min(quizzes.size(), startRow + windowSize);

            if (startRow > 0) {
                sb.append(TuiHelper.dim(String.format("  ▲ %d more quizzes above (Press ↑ to scroll)", startRow))).append("\n\n");
            }

            for (int i = startRow; i < endRow; i++) {
                Quiz q = quizzes.get(i);
                String cursor = (i == selectedIndex) ? TuiHelper.cyan("▶ ") : "  ";
                String subj = subjectNames.getOrDefault(q.getSubjectId(), "-");
                String time = (q.getTimeLimitMins() != null && q.getTimeLimitMins() > 0) ? q.getTimeLimitMins() + "m" : "Untimed";

                Attempt att = studentAttempts.get(q.getId());
                String statusStr;
                if (att == null) {
                    statusStr = q.isExpired() ? TuiHelper.red("Expired") : TuiHelper.green("Available");
                } else {
                    statusStr = switch (att.getStatus()) {
                        case IN_PROGRESS -> TuiHelper.yellow("In Progress");
                        case SUBMITTED, AUTO_SUBMITTED, TURNED_IN -> TuiHelper.cyan("Turned In");
                        case GRADED -> TuiHelper.bold("Graded");
                    };
                }

                String line = String.format("%-4d  %-14s  %-40s  %-10s  %-6.1f  %-12s",
                        q.getId(),
                        truncate(subj, 14),
                        truncate(q.getTitle(), 40),
                        time,
                        q.getTotalPoints(),
                        statusStr);

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
                sb.append("\n").append(TuiHelper.dim(String.format("  ▼ %d more quizzes below (Press ↓ to scroll)", quizzes.size() - endRow))).append("\n");
            }
        }

        sb.append("\n  " + "─".repeat(95) + "\n\n");

        if (!bannerMessage.isBlank()) {
            sb.append("  ").append(bannerMessage).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [↑/↓] Move  •  [Enter] Start / View Result  •  [r] Request Retake  •  [Esc] Back\n"));
        return sb.toString();
    }

    public static String renderExamTaker(ExamSession session, int currentQuestionIndex, int focusedOptionIndex,
                                        String shortAnswerText, boolean confirmSubmitMode, boolean confirmSubmitFocused) {
        if (confirmSubmitMode) {
            return TuiHelper.confirmationModal(
                    session.getQuiz().getTitle(),
                    "Are you sure you want to SUBMIT your quiz?",
                    "Your answers will be locked in for evaluation.",
                    "Submit Quiz",
                    "Return to Quiz",
                    confirmSubmitFocused
            );
        }

        StringBuilder sb = new StringBuilder();
        int totalQ = session.getQuestions().size();
        int curQNum = currentQuestionIndex + 1;

        String timerInfo = session.isTimed()
                ? String.format("Time Remaining: %02d:%02d", session.getRemainingSeconds() / 60, session.getRemainingSeconds() % 60)
                : "Untimed Exam";

        sb.append(TuiHelper.header("ASSESSMENT: " + session.getQuiz().getTitle(),
                String.format("Question %d of %d  •  %s", curQNum, totalQ, timerInfo)));
        sb.append("\n");

        Question q = session.getQuestions().get(currentQuestionIndex);

        sb.append("  ").append(TuiHelper.bold(String.format("Q%d. %s", curQNum, q.getQuestionText()))).append(" ")
                .append(TuiHelper.dim(String.format("(%.1f pts)", q.getPoints()))).append("\n\n");

        if (q.getQuestionType() == com.proctor.model.enums.QuestionType.SHORT_ANSWER) {
            sb.append(TuiHelper.inputBox("Your Written Answer", shortAnswerText, true, 68, false, "type your response here..."));
            sb.append("\n");
        } else if (q.getOptions() != null) {
            Integer selectedOptId = session.getSelectedOptions().get(q.getId());
            for (int i = 0; i < q.getOptions().size(); i++) {
                QuestionOption opt = q.getOptions().get(i);
                boolean isSelected = selectedOptId != null && selectedOptId.equals(opt.getId());
                boolean isFocused = (i == focusedOptionIndex);

                String radio = isSelected ? TuiHelper.green("(•) ") : "( ) ";
                String optText = radio + opt.getOptionText();

                if (isFocused) {
                    sb.append("  ").append(TuiHelper.cyan("▶ ")).append(TuiHelper.bold(optText)).append("\n");
                } else {
                    sb.append("    ").append(optText).append("\n");
                }
            }
            sb.append("\n");
        }

        sb.append("\n  " + "─".repeat(95) + "\n\n");
        sb.append(TuiHelper.dim("  [↑/↓] Select Answer  •  [←/→] Prev/Next Question  •  [Esc] Submit Quiz\n"));
        return sb.toString();
    }

    public static String renderExamResult(Result result, ExamSession session, boolean hasReturnScreen) {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("QUIZ RESULTS", result.getQuizTitle()));
        sb.append("\n");

        String badge = result.isPassed()
                ? TuiHelper.green(TuiHelper.bold("  ✔ PASSED  "))
                : TuiHelper.red(TuiHelper.bold("  ✖ FAILED  "));

        sb.append("   Student:            ").append(result.getStudentName() != null ? result.getStudentName() : "Student #" + result.getStudentId()).append("\n");
        sb.append("   Result Status:      ").append(badge).append("\n");
        sb.append("   Score:              ").append(TuiHelper.bold(String.format("%.1f / %.1f points", result.getTotalPoints(), result.getMaxPoints()))).append("\n");
        sb.append("   Percentage:         ").append(TuiHelper.bold(String.format("%.1f%%", result.getPercentage()))).append("\n\n");

        if (session != null && session.getQuiz().isShowAnswersAfter()) {
            sb.append("  " + TuiHelper.bold("Question-by-Question Review:") + "\n\n");
            sb.append("  " + "─".repeat(95) + "\n\n");

            for (int i = 0; i < session.getQuestions().size(); i++) {
                Question q = session.getQuestions().get(i);
                Integer selectedOptId = session.getSelectedOptions().get(q.getId());
                String textAns = session.getTextAnswers().get(q.getId());

                sb.append(String.format("   Q%d: %s (%.1f pts)\n", i + 1, q.getQuestionText(), q.getPoints()));

                if (q.getQuestionType() == com.proctor.model.enums.QuestionType.MCQ || q.getQuestionType() == com.proctor.model.enums.QuestionType.TRUE_FALSE) {
                    for (QuestionOption opt : q.getOptions()) {
                        boolean isSelected = selectedOptId != null && selectedOptId.equals(opt.getId());
                        if (opt.isCorrect()) {
                            sb.append("     ").append(TuiHelper.green("[✔ Correct] " + opt.getOptionText() + (isSelected ? " (Your choice)" : ""))).append("\n");
                        } else if (isSelected) {
                            sb.append("     ").append(TuiHelper.red("[✖ Incorrect] " + opt.getOptionText() + " (Your choice)")).append("\n");
                        }
                    }
                } else {
                    sb.append("     Your Answer: ").append(textAns != null ? textAns : "(No answer provided)").append("\n");
                    if (q.getExplanation() != null && !q.getExplanation().isBlank()) {
                        sb.append("     Model Context: ").append(TuiHelper.dim(q.getExplanation())).append("\n");
                    }
                }
                sb.append("\n");
            }
            sb.append("\n  " + "─".repeat(95) + "\n\n");
        }

        String returnMsg = hasReturnScreen ? "Back" : "Back to Portal";
        sb.append(TuiHelper.dim("  [Enter / Esc] " + returnMsg + "\n"));
        return sb.toString();
    }

    public static String renderStudentHistory(List<Result> historyList, int selectedIndex, SimpleDateFormat dateFormat) {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("MY QUIZ HISTORY", String.format("Total: %d  •  Scroll with [↑/↓]", historyList.size())));
        sb.append("\n");

        sb.append(String.format("  %-8s  %-34s  %-12s  %-8s  %-8s  %-16s%n",
                "ATTEMPT", "QUIZ TITLE", "SCORE", "PCT", "STATUS", "DATE")).append("\n");
        sb.append("  " + "─".repeat(95) + "\n\n");

        if (historyList.isEmpty()) {
            sb.append("  ").append(TuiHelper.dim("No past quiz attempts found.")).append("\n");
        } else {
            int windowSize = 5;
            int startRow = Math.max(0, Math.min(selectedIndex - 2, historyList.size() - windowSize));
            int endRow = Math.min(historyList.size(), startRow + windowSize);

            if (startRow > 0) {
                sb.append(TuiHelper.dim(String.format("  ▲ %d more records above (Press ↑ to scroll)", startRow))).append("\n\n");
            }

            for (int i = startRow; i < endRow; i++) {
                Result r = historyList.get(i);
                String cursor = (i == selectedIndex) ? TuiHelper.cyan("▶ ") : "  ";
                String status = r.isPassed() ? TuiHelper.green(String.format("%-8s", "PASSED")) : TuiHelper.red(String.format("%-8s", "FAILED"));
                String dateStr = r.getGradedAt() != null ? dateFormat.format(r.getGradedAt()) : "-";

                String line = String.format("#%-7d  %-34s  %-12s  %-8s  %s  %-16s",
                        r.getAttemptId(),
                        truncate(r.getQuizTitle(), 34),
                        String.format("%.1f/%.1f", r.getTotalPoints(), r.getMaxPoints()),
                        String.format("%.1f%%", r.getPercentage()),
                        status,
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

            if (endRow < historyList.size()) {
                sb.append("\n").append(TuiHelper.dim(String.format("  ▼ %d more records below (Press ↓ to scroll)", historyList.size() - endRow))).append("\n");
            }
        }

        sb.append("\n  " + "─".repeat(95) + "\n\n");
        sb.append(TuiHelper.dim("  [↑/↓] Move  •  [Enter] View Result  •  [Esc] Back\n"));
        return sb.toString();
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}