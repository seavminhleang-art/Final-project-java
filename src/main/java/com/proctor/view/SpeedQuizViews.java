package com.proctor.view;

import com.proctor.model.entity.Quiz;
import com.proctor.model.entity.AttemptAnswer;
import com.proctor.model.entity.Question;
import com.proctor.model.entity.QuestionOption;
import com.proctor.model.entity.Result;
import com.proctor.model.entity.SpeedQuizAnswerRecord;
import com.proctor.model.entity.SpeedQuizSession;
import com.proctor.model.enums.Difficulty;
import com.proctor.util.TuiHelper;

import java.util.List;
import java.util.Map;

public class SpeedQuizViews {

    public static String renderSpeedQuizForm(
            boolean isEdit,
            String subjectDisplay,
            String title,
            String description,
            String secondsPerQuestion,
            String activeHours,
            boolean randomizeAnswers,
            boolean showAnswersAfter,
            int focusedField,
            String errorMessage) {

        StringBuilder sb = new StringBuilder();
        String headerLabel = "SPEED QUIZZES";
        sb.append(TuiHelper.header(headerLabel));
        sb.append("\n");

        String titleHeader = isEdit ? "Edit Speed Quiz" : "Create New Speed Quiz";
        sb.append(TuiHelper.boxTitle(titleHeader, "Adaptive difficulty challenge with per-question timers")).append("\n\n");

        int numInputFields = 7;
        int maxVisible = 6;
        int startField = 0;
        if (focusedField >= maxVisible && focusedField < numInputFields) {
            startField = focusedField - maxVisible + 1;
        } else if (focusedField >= numInputFields) {
            startField = Math.max(0, numInputFields - maxVisible);
        }
        int endField = Math.min(numInputFields, startField + maxVisible);

        if (startField > 0) {
            sb.append(TuiHelper.dim(String.format("  ▲ %d more fields above (Press ↑ to scroll)", startField))).append("\n");
        }

        for (int f = startField; f < endField; f++) {
            switch (f) {
                case 0 -> sb.append(TuiHelper.selectBox("Subject (Required)", subjectDisplay, focusedField == 0, 102, "Space or ←/→ to cycle"));
                case 1 -> sb.append(TuiHelper.inputBox("Speed Quiz Title (Required)", title, focusedField == 1, 102, false, "e.g. Rapid Chemistry Challenge"));
                case 2 -> sb.append(TuiHelper.inputBox("Description", description, focusedField == 2, 102, false, "optional rules or instructions"));
                case 3 -> sb.append(TuiHelper.inputBox("Timer Per Question (Seconds)", secondsPerQuestion, focusedField == 3, 102, false, "default: 15 seconds"));
                case 4 -> sb.append(TuiHelper.inputBox("Active Lifetime (Hours)", activeHours, focusedField == 4, 102, false, "0 for Available Forever"));
                case 5 -> {
                    String raText = randomizeAnswers ? "Enabled" : "Disabled";
                    sb.append(TuiHelper.selectBox("Shuffle Answer Options", raText, focusedField == 5, 102, "Space to toggle"));
                }
                case 6 -> {
                    String saText = showAnswersAfter ? "Enabled" : "Disabled";
                    sb.append(TuiHelper.selectBox("Review Answers on Submit", saText, focusedField == 6, 102, "Space to toggle"));
                }
            }
            sb.append("\n");
        }

        if (endField < numInputFields) {
            sb.append(TuiHelper.dim(String.format("  ▼ %d more fields below (Press ↓ to scroll)", numInputFields - endField))).append("\n");
        }

        sb.append("\n");
        sb.append(TuiHelper.buttonRow("Save", focusedField == 7, "Cancel", focusedField == 8)).append("\n\n");

        if (!errorMessage.isBlank()) {
            sb.append("  ").append(TuiHelper.red("✖ " + errorMessage)).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [↑/↓] Switch Field  •  [Enter] Confirm / Next  •  [Esc] Cancel\n"));
        return sb.toString();
    }

    public static String renderSpeedQuizTaker(SpeedQuizSession session, int focusedOptionIndex,
                                             boolean confirmForfeitMode, boolean confirmForfeitFocused) {
        return renderSpeedQuizTaker(session, focusedOptionIndex, confirmForfeitMode, confirmForfeitFocused, null, 0);
    }

    public static String renderSpeedQuizTaker(SpeedQuizSession session, int focusedOptionIndex,
                                             boolean confirmForfeitMode, boolean confirmForfeitFocused,
                                             SpeedQuizAnswerRecord toastRecord) {
        return renderSpeedQuizTaker(session, focusedOptionIndex, confirmForfeitMode, confirmForfeitFocused, toastRecord, 0);
    }

    public static String renderSpeedQuizTaker(SpeedQuizSession session, int focusedOptionIndex,
                                             boolean confirmForfeitMode, boolean confirmForfeitFocused,
                                             SpeedQuizAnswerRecord toastRecord, int revealSecondsRemaining) {
        if (confirmForfeitMode) {
            return TuiHelper.confirmationModal(
                    session.getQuiz().getTitle(),
                    "Are you sure you want to FORFEIT this speed quiz?",
                    "Your current score will be submitted as your final result.",
                    "Forfeit & Submit",
                    "Continue Quiz",
                    confirmForfeitFocused
            );
        }

        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("SPEED QUIZ"));
        sb.append("\n");

        Question q = (toastRecord != null) ? toastRecord.getQuestion() : session.getCurrentQuestion();
        if (q == null) {
            sb.append(TuiHelper.boxTitle(session.getQuiz().getTitle(), "Speed Quiz Complete")).append("\n\n");
            sb.append("  ").append(TuiHelper.green("All questions completed!")).append("\n\n");
            sb.append(TuiHelper.dim("  [Enter] View Results\n"));
            return sb.toString();
        }

        int curQNum = (toastRecord != null) ? session.getAnsweredCount() : session.getAnsweredCount() + 1;
        int totalQ = session.getTotalQuestionsCount();

        String subInfo = String.format("Question %d of %d  •  Score: %.1f pts  •  Streak: 🔥 %d",
                curQNum, totalQ, session.getTotalScore(), session.getCurrentStreak());
        sb.append(TuiHelper.boxTitle(session.getQuiz().getTitle(), subInfo)).append("\n\n");

        Difficulty diff = (toastRecord != null && toastRecord.getTierShown() != null)
                ? toastRecord.getTierShown()
                : (q.getDifficulty() != null ? q.getDifficulty() : Difficulty.MEDIUM);
        String diffBadge = switch (diff) {
            case EASY -> TuiHelper.green(TuiHelper.bold(" EASY ")) + TuiHelper.dim(" (1.0x Base)");
            case MEDIUM -> TuiHelper.yellow(TuiHelper.bold(" MEDIUM ")) + TuiHelper.dim(" (1.5x Multiplier)");
            case HARD -> TuiHelper.red(TuiHelper.bold(" HARD ")) + TuiHelper.dim(" (2.0x Multiplier 🔥)");
        };
        sb.append("  Difficulty Tier: [ ").append(diffBadge).append(" ]\n\n");

        int totalSec = session.getSecondsPerQuestion() > 0 ? session.getSecondsPerQuestion() : 15;
        int remSec = (toastRecord != null)
                ? Math.max(0, toastRecord.getSecondsRemaining())
                : Math.max(0, session.getQuestionSecondsRemaining());
        int barWidth = 28;
        int filled = Math.min(barWidth, (int) Math.ceil(((double) remSec / totalSec) * barWidth));
        int empty = barWidth - filled;
        String bar = "█".repeat(filled) + "░".repeat(empty);

        String timerColored;
        if (toastRecord != null) {
            if (toastRecord.getSecondsRemaining() <= 0 && toastRecord.getSelectedOptionId() == null) {
                timerColored = TuiHelper.red(TuiHelper.bold(String.format("⏱  00s / %02ds  [%s]  TIME'S UP!", totalSec, bar)));
            } else {
                timerColored = TuiHelper.cyan(String.format("⏱  %02ds / %02ds  [%s]  (Locked In)", remSec, totalSec, bar));
            }
        } else {
            if (remSec <= 3) {
                timerColored = TuiHelper.red(TuiHelper.bold(String.format("⏱  %02ds / %02ds  [%s]  HURRY!", remSec, totalSec, bar)));
            } else if (remSec <= totalSec / 2) {
                timerColored = TuiHelper.yellow(String.format("⏱  %02ds / %02ds  [%s]", remSec, totalSec, bar));
            } else {
                timerColored = TuiHelper.cyan(String.format("⏱  %02ds / %02ds  [%s]", remSec, totalSec, bar));
            }
        }
        sb.append("  Timer: ").append(timerColored).append("\n\n");

        sb.append("  ").append(TuiHelper.bold(String.format("Q%d. %s", curQNum, q.getQuestionText()))).append(" ")
                .append(TuiHelper.dim(String.format("(%.1f base pts)", q.getPoints()))).append("\n\n");

        if (q.getOptions() != null) {
            Integer pickedId = (toastRecord != null) ? toastRecord.getSelectedOptionId() : null;
            for (int i = 0; i < q.getOptions().size(); i++) {
                QuestionOption opt = q.getOptions().get(i);
                String numTag = String.format("[%d] ", i + 1);
                String optText = numTag + opt.getOptionText();

                if (toastRecord != null) {
                    boolean isPicked = (pickedId != null && pickedId.equals(opt.getId()));
                    boolean isCorrect = opt.isCorrect();

                    if (isPicked && isCorrect) {
                        sb.append("  ").append(TuiHelper.green(TuiHelper.bold("✔ " + optText + "  (Your Answer - Correct!)"))).append("\n");
                    } else if (isPicked && !isCorrect) {
                        sb.append("  ").append(TuiHelper.red(TuiHelper.bold("✖ " + optText + "  (Your Answer - Incorrect)"))).append("\n");
                    } else if (!isPicked && isCorrect) {
                        sb.append("  ").append(TuiHelper.green("✔ " + optText + "  (Correct Answer)")).append("\n");
                    } else {
                        sb.append("    ").append(TuiHelper.dim(optText)).append("\n");
                    }
                } else {
                    boolean isFocused = (i == focusedOptionIndex);
                    if (isFocused) {
                        sb.append("    ").append(TuiHelper.bold(TuiHelper.NAVY_BLUE + optText)).append("\n");
                    } else {
                        sb.append("    ").append(optText).append("\n");
                    }
                }
                if (i < q.getOptions().size() - 1) {
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }

        if (toastRecord != null) {
            if (session.isCompleted() || session.getCurrentQuestion() == null) {
                sb.append("  ").append(TuiHelper.cyan(TuiHelper.bold(String.format("Finishing quiz in %d...", revealSecondsRemaining)))).append("\n\n");
            } else {
                sb.append("  ").append(TuiHelper.cyan(TuiHelper.bold(String.format("Next question in %d...", revealSecondsRemaining)))).append("\n\n");
            }
        }

        sb.append("  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");
        if (toastRecord != null) {
            sb.append(TuiHelper.dim("  [Esc] Forfeit\n"));
        } else {
            sb.append(TuiHelper.dim("  [↑/↓] Move Focus  •  [1-4] Quick Select & Lock  •  [Space/Enter] Lock In  •  [Esc] Forfeit\n"));
        }
        return sb.toString();
    }

    public static String renderSpeedQuizReveal(SpeedQuizSession session, SpeedQuizAnswerRecord record,
                                              int revealSecondsRemaining) {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("SPEED QUIZ — ROUND RESULT"));
        sb.append("\n");

        String subInfo = String.format("Score: %.1f pts  •  Streak: 🔥 %d",
                session.getTotalScore(), session.getCurrentStreak());
        sb.append(TuiHelper.boxTitle(session.getQuiz().getTitle(), subInfo)).append("\n\n");

        if (record == null) {
            sb.append("  ").append(TuiHelper.dim("No answer recorded.")).append("\n\n");
            return sb.toString();
        }

        Question q = record.getQuestion();

        if (record.isCorrect()) {
            sb.append("  ").append(TuiHelper.green(TuiHelper.bold("✔ CORRECT!"))).append("\n\n");

            sb.append(String.format("   Points Earned:     %s%n%n",
                    TuiHelper.green(TuiHelper.bold(String.format("+%.1f pts", record.getTotalPointsAwarded())))));

            String multStr = switch (record.getTierShown()) {
                case HARD -> "2.0x (Hard Tier)";
                case MEDIUM -> "1.5x (Medium Tier)";
                case EASY -> "1.0x (Easy Tier)";
            };
            sb.append(String.format("     Base Points:     +%.1f (%.1f base pts × %s)%n%n",
                    record.getBasePoints(), q.getPoints(), multStr));
            sb.append(String.format("     Speed Bonus:     +%.1f pts (%ds remaining)%n",
                    record.getSpeedBonus(), record.getSecondsRemaining()));

            if (record.getStreakBonus() > 0) {
                sb.append(String.format("%n     Streak Bonus:    %s (+%.1f pts!)%n",
                        TuiHelper.yellow(TuiHelper.bold("🔥 " + session.getCurrentStreak() + " STREAK BONUS")),
                        record.getStreakBonus()));
            }
        } else {
            if (record.getSecondsRemaining() <= 0 && record.getSelectedOptionId() == null) {
                sb.append("  ").append(TuiHelper.red(TuiHelper.bold("⏱ TIME'S UP!"))).append("\n\n");
                sb.append("   You did not select an answer in time.\n\n");
            } else {
                sb.append("  ").append(TuiHelper.red(TuiHelper.bold("✖ INCORRECT!"))).append("\n\n");

                QuestionOption picked = findOption(q, record.getSelectedOptionId());
                String pickedText = picked != null ? picked.getOptionText() : "None";
                sb.append("   Your Answer:       ").append(TuiHelper.red(pickedText)).append("\n\n");
            }

            QuestionOption correctOpt = findCorrectOption(q);
            String correctText = correctOpt != null ? correctOpt.getOptionText() : "-";
            sb.append("   Correct Answer:    ").append(TuiHelper.green(TuiHelper.bold(correctText))).append("\n\n");
            sb.append("   Points Earned:     ").append(TuiHelper.dim("+0.0 pts")).append("\n\n");
            sb.append("   Streak:            ").append(TuiHelper.dim("Reset to 0")).append("\n");
        }

        sb.append("\n");

        Difficulty oldTier = record.getTierShown();
        Difficulty newTier = session.getCurrentTier();
        if (record.isCorrect()) {
            if (oldTier != newTier) {
                sb.append("   Adaptive Ladder:   ").append(TuiHelper.green(TuiHelper.bold(String.format("%s ➔ %s (LEVEL UP! ⬆)", oldTier, newTier)))).append("\n");
            } else {
                sb.append("   Adaptive Ladder:   ").append(TuiHelper.cyan(String.format("%s (MAX DIFFICULTY REACHED 🔥)", oldTier))).append("\n");
            }
        } else {
            if (oldTier != newTier) {
                sb.append("   Adaptive Ladder:   ").append(TuiHelper.yellow(String.format("%s ➔ %s (STEP DOWN ⬇)", oldTier, newTier))).append("\n");
            } else {
                sb.append("   Adaptive Ladder:   ").append(TuiHelper.dim(String.format("%s (BASE TIER)", oldTier))).append("\n");
            }
        }

        sb.append("\n  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

        if (session.isCompleted()) {
            sb.append(TuiHelper.cyan(String.format("  Finishing Speed Quiz in %ds... (Press [Enter] or [Space] to view scorecard)\n", revealSecondsRemaining)));
        } else {
            sb.append(TuiHelper.dim(String.format("  Next question in %ds... (Press [Enter] or [Space] to continue)\n", revealSecondsRemaining)));
        }

        return sb.toString();
    }

    public static String renderSpeedQuizResult(Result result, SpeedQuizSession session, boolean hasReturnScreen) {
        Quiz q = session != null ? session.getQuiz() : null;
        return renderSpeedQuizResult(result, session, q, null, hasReturnScreen);
    }

    public static String renderSpeedQuizResult(Result result, SpeedQuizSession session, Quiz quiz, List<AttemptAnswer> answers, boolean hasReturnScreen) {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("SPEED QUIZ RESULTS"));
        sb.append("\n");

        String titleStr = (result != null && result.getQuizTitle() != null && !result.getQuizTitle().isBlank())
                ? result.getQuizTitle() : "Speed Quiz Results";
        sb.append(TuiHelper.boxTitle(titleStr, "High Speed Adaptive Scorecard")).append("\n\n");

        double totalScore = result != null ? result.getTotalPoints() : (session != null ? session.getTotalScore() : 0.0);

        sb.append("   Student:            ").append(result != null && result.getStudentName() != null ? result.getStudentName() : "Student").append("\n\n");
        sb.append("   Final Score:        ").append(TuiHelper.bold(TuiHelper.cyan(String.format("%.1f points", totalScore)))).append("\n\n");

        if (session != null) {
            long correctCount = session.getAnswerRecords().stream().filter(SpeedQuizAnswerRecord::isCorrect).count();
            int totalAns = session.getAnswerRecords().size();
            double accuracy = totalAns > 0 ? ((double) correctCount / totalAns) * 100.0 : 0.0;
            sb.append("   Accuracy:           ").append(String.format("%d / %d (%.1f%%)", correctCount, totalAns, accuracy)).append("\n\n");
            sb.append("   Max Streak:         ").append(TuiHelper.yellow(String.format("🔥 %d consecutive correct", session.getMaxStreak()))).append("\n\n");
        } else if (answers != null && !answers.isEmpty()) {
            long correctCount = answers.stream().filter(a -> Boolean.TRUE.equals(a.getCorrect())).count();
            int totalAns = answers.size();
            double accuracy = ((double) correctCount / totalAns) * 100.0;
            sb.append("   Accuracy:           ").append(String.format("%d / %d (%.1f%%)", correctCount, totalAns, accuracy)).append("\n\n");
        }

        sb.append("\n  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

        boolean canShowReview = (quiz == null || quiz.isShowAnswersAfter());

        if (!canShowReview) {
            sb.append("  ").append(TuiHelper.yellow("● Answer review has been disabled by the instructor for this speed quiz.")).append("\n\n");
            sb.append("  ").append(TuiHelper.dim("Your final score has been officially recorded.")).append("\n\n");
            sb.append("  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");
        } else {
            if (session != null && !session.getAnswerRecords().isEmpty()) {
                sb.append(String.format("  %-4s  %-8s  %-48s  %-18s  %-10s  %-10s  %-10s%n",
                        "#", "TIER", "QUESTION", "YOUR ANSWER", "STATUS", "TIME LEFT", "POINTS")).append("\n");
                sb.append("  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

                List<SpeedQuizAnswerRecord> records = session.getAnswerRecords();
                for (int i = 0; i < records.size(); i++) {
                    SpeedQuizAnswerRecord rec = records.get(i);
                    Question q = rec.getQuestion();
                    String qText = q != null ? q.getQuestionText() : "Question #" + (i + 1);

                    String yourAns = "-";
                    if (rec.getSelectedOptionId() != null && q != null) {
                        QuestionOption picked = findOption(q, rec.getSelectedOptionId());
                        if (picked != null) yourAns = picked.getOptionText();
                    }

                    String status = rec.isCorrect() ? TuiHelper.green("✔ Correct") : TuiHelper.red("✖ Wrong");
                    String timeStr = rec.getSecondsRemaining() + "s left";
                    String tierStr = rec.getTierShown() != null ? rec.getTierShown().name() : "MED";

                    sb.append(String.format("  %-4d  %-8s  %-48s  %-18s  %-10s  %-10s  %-10s%n",
                            (i + 1),
                            tierStr,
                            truncate(qText, 48),
                            truncate(yourAns, 18),
                            status,
                            timeStr,
                            String.format("+%.1f", rec.getTotalPointsAwarded())));
                    if (i < records.size() - 1) {
                        sb.append("\n");
                    }
                }
                sb.append("\n  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");
            } else if (answers != null && !answers.isEmpty() && quiz != null && quiz.getQuestions() != null) {
                sb.append(String.format("  %-4s  %-8s  %-48s  %-18s  %-10s  %-10s  %-10s%n",
                        "#", "TIER", "QUESTION", "YOUR ANSWER", "STATUS", "TIME LEFT", "POINTS")).append("\n");
                sb.append("  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");

                Map<Integer, Question> qMap = new java.util.HashMap<>();
                for (Question q : quiz.getQuestions()) {
                    qMap.put(q.getId(), q);
                }

                for (int i = 0; i < answers.size(); i++) {
                    AttemptAnswer ans = answers.get(i);
                    Question q = qMap.get(ans.getQuestionId());
                    String qText = q != null ? q.getQuestionText() : "Question #" + (i + 1);
                    String yourAns = "-";
                    if (ans.getSelectedOptionId() != null && q != null) {
                        QuestionOption picked = findOption(q, ans.getSelectedOptionId());
                        if (picked != null) yourAns = picked.getOptionText();
                    }
                    boolean isCorrect = Boolean.TRUE.equals(ans.getCorrect());
                    String status = isCorrect ? TuiHelper.green("✔ Correct") : TuiHelper.red("✖ Wrong");
                    String tierStr = (q != null && q.getDifficulty() != null) ? q.getDifficulty().name() : "-";

                    sb.append(String.format("  %-4d  %-8s  %-48s  %-18s  %-10s  %-10s  %-10s%n",
                            (i + 1),
                            tierStr,
                            truncate(qText, 48),
                            truncate(yourAns, 18),
                            status,
                            "-",
                            String.format("+%.1f", ans.getPointsAwarded())));
                    if (i < answers.size() - 1) {
                        sb.append("\n");
                    }
                }
                sb.append("\n  " + "─".repeat(TuiHelper.TABLE_WIDTH) + "\n\n");
            }
        }

        String returnMsg = hasReturnScreen ? "Back" : "Back to Speed Quizzes";
        sb.append(TuiHelper.dim("  [Enter / Esc] " + returnMsg + "  •  [r] Play Again\n"));
        return sb.toString();
    }

    private static QuestionOption findOption(Question q, Integer optId) {
        if (q == null || q.getOptions() == null || optId == null) return null;
        for (QuestionOption opt : q.getOptions()) {
            if (opt.getId() != null && opt.getId().equals(optId)) {
                return opt;
            }
        }
        return null;
    }

    private static QuestionOption findCorrectOption(Question q) {
        if (q == null || q.getOptions() == null) return null;
        for (QuestionOption opt : q.getOptions()) {
            if (opt.isCorrect()) {
                return opt;
            }
        }
        return null;
    }

    private static String truncate(String text, int maxLen) {
        return TuiHelper.truncate(text, maxLen);
    }
}
