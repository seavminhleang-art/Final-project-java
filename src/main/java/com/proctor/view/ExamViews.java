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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExamViews {

    public static String renderAvailableQuizzes(AssessmentType assessmentType, List<Quiz> quizzes, Map<Integer, String> subjectNames,
                                               Map<Integer, Attempt> studentAttempts, int selectedIndex,
                                               String bannerMessage) {
        return renderAvailableQuizzes(assessmentType, quizzes, subjectNames, studentAttempts, selectedIndex, "ALL", "", false, bannerMessage);
    }

    public static String renderAvailableQuizzes(AssessmentType assessmentType, List<Quiz> quizzes, Map<Integer, String> subjectNames,
                                               Map<Integer, Attempt> studentAttempts, int selectedIndex,
                                               String subjectFilterDisplay, String searchBuffer, boolean searchMode,
                                               String bannerMessage) {
        StringBuilder sb = new StringBuilder();
        String typeLabel = (assessmentType == AssessmentType.EXAM) ? "EXAMS" : (assessmentType == AssessmentType.SPEED ? "SPEED QUIZZES" : "QUIZZES");
        sb.append(TuiHelper.header(typeLabel));
        sb.append("\n");
        String subjLabel = (subjectFilterDisplay == null || subjectFilterDisplay.isBlank()) ? "ALL" : subjectFilterDisplay;
        String boxTitle = (assessmentType == AssessmentType.EXAM) ? "Exams" : (assessmentType == AssessmentType.SPEED ? "Speed Quizzes" : "Quizzes");
        sb.append(TuiHelper.boxTitle(boxTitle,
                String.format("Subject: [ %s ]  •  Total: %d", subjLabel, quizzes.size()))).append("\n\n");

        if (searchMode) {
            sb.append("  Search: [ ").append(TuiHelper.cyan(searchBuffer + "_")).append(" ] (Press Enter to finish)\n\n");
        } else if (!searchBuffer.isEmpty()) {
            sb.append("  Search: [ ").append(searchBuffer).append(" ] (Press '/' to edit)\n\n");
        }

        String timeColHeader = (assessmentType == AssessmentType.SPEED) ? "SEC/Q" : "TIME";
        sb.append(String.format("  %-4s  %-14s  %-52s  %-18s  %-10s  %-6s  %-14s%n",
                "#", "SUBJ", "TITLE", "TEACHER", timeColHeader, "PTS", "STATUS")).append("\n");
        sb.append("  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

        if (quizzes.isEmpty()) {
            sb.append("  ").append(TuiHelper.dim("No " + typeLabel.toLowerCase() + " currently available.")).append("\n");
        } else {
            int pageSize = TuiHelper.PAGE_SIZE;
            int totalPages = Math.max(1, (int) Math.ceil((double) quizzes.size() / pageSize));
            int currentPage = selectedIndex / pageSize;
            int startRow = currentPage * pageSize;
            int endRow = Math.min(quizzes.size(), startRow + pageSize);

            for (int i = startRow; i < endRow; i++) {
                Quiz q = quizzes.get(i);
                String cursor = (i == selectedIndex) ? TuiHelper.cyan("▶ ") : "  ";
                String subj = (q.getSubjectId() != null) ? subjectNames.getOrDefault(q.getSubjectId(), "-") : "-";
                String time;
                if (assessmentType == AssessmentType.SPEED) {
                    int sec = (q.getSpeedSecondsPerQuestion() != null && q.getSpeedSecondsPerQuestion() > 0)
                            ? q.getSpeedSecondsPerQuestion() : 15;
                    time = sec + "s";
                } else {
                    time = (q.getTimeLimitMins() != null && q.getTimeLimitMins() > 0) ? q.getTimeLimitMins() + "m" : "Untimed";
                }
                String teacher = (q.getCreatorName() != null && !q.getCreatorName().isBlank()) ? q.getCreatorName() : "Teacher";

                Attempt att = studentAttempts.get(q.getId());
                String statusStr;
                if (att == null) {
                    statusStr = q.isExpired() ? TuiHelper.red("EXPIRED") : TuiHelper.green("AVAILABLE");
                } else {
                    statusStr = switch (att.getStatus()) {
                        case IN_PROGRESS -> TuiHelper.dim("IN PROGRESS");
                        case AUTO_SUBMITTED, TURNED_IN -> TuiHelper.yellow("PENDING REVIEW");
                        case GRADED -> TuiHelper.green("GRADED");
                    };
                }

                String line = String.format("%-4d  %-14s  %-52s  %-18s  %-10s  %-6.1f  %-14s",
                        (i + 1),
                        truncate(subj, 14),
                        truncate(q.getTitle(), 52),
                        truncate(teacher, 18),
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

        if (assessmentType == AssessmentType.SPEED) {
            sb.append(TuiHelper.dim("  [↑/↓] Move  •  [←/→] Page  •  [/] Search  •  [s] Subject  •  [Enter] Start Run  •  [v] View Result  •  [Esc] Back\n"));
        } else {
            sb.append(TuiHelper.dim("  [↑/↓] Move  •  [←/→] Page  •  [/] Search  •  [s] Subject  •  [Enter] Start / View Result  •  [r] Request Retake  •  [Esc] Back\n"));
        }
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

        String takerHeader = (session.getQuiz() != null && session.getQuiz().getAssessmentType() == AssessmentType.EXAM) ? "EXAMS" : "QUIZZES";
        sb.append(TuiHelper.header(takerHeader));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle(session.getQuiz().getTitle(), String.format("Question %d of %d  •  %s", curQNum, totalQ, timerInfo))).append("\n\n");

        Question q = session.getQuestions().get(currentQuestionIndex);

        sb.append("  ").append(TuiHelper.bold(String.format("Q%d. %s", curQNum, q.getQuestionText()))).append(" ")
                .append(TuiHelper.dim(String.format("(%.1f pts)", q.getPoints()))).append("\n\n");

        if (q.getQuestionType() == com.proctor.model.enums.QuestionType.SHORT_ANSWER) {
            sb.append(TuiHelper.inputBox("Your Written Answer", shortAnswerText, true, 102, false, "type your response here..."));
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
                if (i < q.getOptions().size() - 1) {
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }

        sb.append("\n  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");
        sb.append(TuiHelper.dim("  [↑/↓] Move Focus  •  [Space] Select  •  [Enter] Confirm & Next  •  [←/→] Prev/Next  •  [Esc] Submit\n"));
        return sb.toString();
    }

    public static String renderExamResult(Result result, ExamSession session, boolean hasReturnScreen) {
        StringBuilder sb = new StringBuilder();
        String typeLabel = (result != null && result.getAssessmentType() == AssessmentType.EXAM) ? "EXAM" : "QUIZ";

        if (result != null && result.isPendingReview()) {
            sb.append(TuiHelper.header(typeLabel + " SUBMISSION"));
            sb.append("\n");
            String titleStr = (result.getQuizTitle() != null && !result.getQuizTitle().isBlank())
                    ? result.getQuizTitle() : (typeLabel + " Submission");
            sb.append(TuiHelper.boxTitle(titleStr, "Assessment Submitted - Pending Teacher Evaluation")).append("\n\n");

            String badge = TuiHelper.yellow(TuiHelper.bold("  ⏳ UNDER REVIEW  "));

            sb.append("   Student:            ").append(result.getStudentName() != null ? result.getStudentName() : "Student #" + result.getStudentId()).append("\n\n");
            sb.append("   Result Status:      ").append(badge).append("\n\n");
            sb.append("   ").append(TuiHelper.bold("Notice:")).append("\n");
            sb.append("   Your assessment has been submitted. Because this assessment includes written response\n");
            sb.append("   (short answer) questions, your submission requires grading by your teacher.\n");
            sb.append("   Your final score and question review will be available in History once all written questions\n");
            sb.append("   have been evaluated and returned.\n\n");
            sb.append("  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

            String returnMsg = hasReturnScreen ? "Back" : "Back to Dashboard";
            sb.append(TuiHelper.dim("  [Enter / Esc] " + returnMsg + "\n"));
            return sb.toString();
        }

        sb.append(TuiHelper.header(typeLabel + " RESULTS"));
        sb.append("\n");

        String titleStr = (result != null && result.getQuizTitle() != null && !result.getQuizTitle().isBlank())
                ? result.getQuizTitle() : (typeLabel + " Results");
        sb.append(TuiHelper.boxTitle(titleStr)).append("\n\n");

        String badge;
        if (result != null && result.getAssessmentType() == AssessmentType.SPEED) {
            badge = TuiHelper.cyan(TuiHelper.bold("  SPEED RUN COMPLETE  "));
        } else {
            badge = result != null && result.isPassed()
                    ? TuiHelper.green(TuiHelper.bold("  ✔ PASSED  "))
                    : TuiHelper.red(TuiHelper.bold("  ✖ FAILED  "));
        }

        sb.append("   Student:            ").append(result.getStudentName() != null ? result.getStudentName() : "Student #" + result.getStudentId()).append("\n\n");
        sb.append("   Result Status:      ").append(badge).append("\n\n");
        sb.append("   Score:              ").append(TuiHelper.bold(String.format("%.1f / %.1f points", result.getTotalPoints(), result.getMaxPoints()))).append("\n\n");
        sb.append("   Percentage:         ").append(TuiHelper.bold(String.format("%.1f%%", result.getPercentage()))).append("\n\n");

        if (session != null && session.getQuiz().isShowAnswersAfter()) {
            sb.append("  " + TuiHelper.bold("Question-by-Question Review:") + "\n\n");
            sb.append("  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

            for (int i = 0; i < session.getQuestions().size(); i++) {
                Question q = session.getQuestions().get(i);
                Integer selectedOptId = session.getSelectedOptions().get(q.getId());
                String textAns = session.getTextAnswers().get(q.getId());

                sb.append(String.format("   Q%d: %s (%.1f pts)\n", i + 1, q.getQuestionText(), q.getPoints()));

                if (q.getQuestionType() == com.proctor.model.enums.QuestionType.MCQ || q.getQuestionType() == com.proctor.model.enums.QuestionType.TRUE_FALSE) {
                    List<String> reviewLines = new ArrayList<>();
                    for (QuestionOption opt : q.getOptions()) {
                        boolean isSelected = selectedOptId != null && selectedOptId.equals(opt.getId());
                        if (opt.isCorrect()) {
                            reviewLines.add("     " + TuiHelper.green("[✔ Correct] " + opt.getOptionText() + (isSelected ? " (Your choice)" : "")));
                        } else if (isSelected) {
                            reviewLines.add("     " + TuiHelper.red("[✖ Incorrect] " + opt.getOptionText() + " (Your choice)"));
                        }
                    }
                    for (int rIdx = 0; rIdx < reviewLines.size(); rIdx++) {
                        sb.append(reviewLines.get(rIdx)).append("\n");
                        if (rIdx < reviewLines.size() - 1) {
                            sb.append("\n");
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
            sb.append("\n  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");
        }

        String returnMsg = hasReturnScreen ? "Back" : "Back to Portal";
        sb.append(TuiHelper.dim("  [Enter / Esc] " + returnMsg + "\n"));
        return sb.toString();
    }

    public static String renderStudentHistory(List<Result> historyList, int selectedIndex, SimpleDateFormat dateFormat) {
        return renderStudentHistory(historyList, selectedIndex, dateFormat, "ALL", "", false);
    }

    public static String renderStudentHistory(List<Result> historyList, int selectedIndex, SimpleDateFormat dateFormat,
                                             String statusFilterDisplay, String searchBuffer, boolean searchMode) {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("ASSESSMENT HISTORY"));
        sb.append("\n");
        String statusLabel = (statusFilterDisplay == null || statusFilterDisplay.isBlank()) ? "ALL" : statusFilterDisplay;
        int activeTab = switch (statusLabel.toUpperCase()) {
            case "PASSED" -> 1;
            case "FAILED" -> 2;
            case "PENDING" -> 3;
            default -> 0;
        };
        sb.append(TuiHelper.boxTitle("Past Assessment Attempts",
                String.format("Total Attempts: %d", historyList.size()))).append("\n\n");
        sb.append(TuiHelper.tabBar(new String[]{"All", "Passed", "Failed", "Pending"}, activeTab)).append("\n\n");

        if (searchMode) {
            sb.append("  Search: [ ").append(TuiHelper.cyan(searchBuffer + "_")).append(" ] (Press Enter to finish)\n\n");
        } else if (searchBuffer != null && !searchBuffer.isEmpty()) {
            sb.append("  Search: [ ").append(searchBuffer).append(" ] (Press '/' to edit)\n\n");
        }

        sb.append(String.format("  %-4s  %-8s  %-60s  %-14s  %-8s  %-10s  %-18s%n",
                "#", "TYPE", "TITLE", "SCORE", "PCT", "STATUS", "DATE")).append("\n");
        sb.append("  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

        if (historyList.isEmpty()) {
            sb.append("  ").append(TuiHelper.dim("No past assessment attempts found.")).append("\n");
        } else {
            int pageSize = TuiHelper.PAGE_SIZE;
            int totalPages = Math.max(1, (int) Math.ceil((double) historyList.size() / pageSize));
            int currentPage = selectedIndex / pageSize;
            int startRow = currentPage * pageSize;
            int endRow = Math.min(historyList.size(), startRow + pageSize);

            for (int i = startRow; i < endRow; i++) {
                Result r = historyList.get(i);
                String cursor = (i == selectedIndex) ? TuiHelper.cyan("▶ ") : "  ";
                String status;
                String scoreStr;
                String pctStr;
                if (r.isPendingReview()) {
                    status = TuiHelper.yellow(String.format("%-10s", "PENDING"));
                    scoreStr = "- / -";
                    pctStr = "-";
                } else if (r.getAssessmentType() == AssessmentType.SPEED) {
                    status = TuiHelper.green(String.format("%-10s", "GRADED"));
                    scoreStr = String.format("%.1f pts", r.getTotalPoints());
                    pctStr = "-";
                } else {
                    status = r.isPassed() ? TuiHelper.green(String.format("%-10s", "PASSED")) : TuiHelper.red(String.format("%-10s", "FAILED"));
                    scoreStr = String.format("%.1f/%.1f", r.getTotalPoints(), r.getMaxPoints());
                    pctStr = String.format("%.1f%%", r.getPercentage());
                }
                String dateStr = r.getGradedAt() != null ? dateFormat.format(r.getGradedAt()) : "-";
                String typeStr = (r.getAssessmentType() == AssessmentType.EXAM) ? "EXAM" : (r.getAssessmentType() == AssessmentType.SPEED ? "SPEED" : "QUIZ");

                String line = String.format("%-4d  %-8s  %-60s  %-14s  %-8s  %s  %-18s",
                        (i + 1),
                        typeStr,
                        truncate(r.getQuizTitle(), 60),
                        scoreStr,
                        pctStr,
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
        }

        sb.append("\n  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");
        if (!historyList.isEmpty()) {
            int pageSize = TuiHelper.PAGE_SIZE;
            int totalPages = Math.max(1, (int) Math.ceil((double) historyList.size() / pageSize));
            int currentPage = selectedIndex / pageSize;
            sb.append(TuiHelper.paginationBar(currentPage, totalPages, historyList.size()));
        }

        sb.append(TuiHelper.dim("  [↑/↓] Move  •  [←/→] Page  •  [/] Search  •  [Tab] Tab  •  [Enter] View Result  •  [Esc] Back\n"));
        return sb.toString();
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}