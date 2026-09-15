package com.proctor.controller;

import com.proctor.exception.ValidationException;
import com.proctor.model.entity.*;
import com.proctor.model.enums.AssessmentType;
import com.proctor.model.enums.AttemptStatus;
import com.proctor.model.service.AuthService;
import com.proctor.model.service.ExamService;
import com.proctor.model.service.InboxService;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.ExamViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AssessmentOverviewScreen implements Screen {
    private final Quiz quiz;
    private final ExamService examService;
    private final AuthService authService;
    private final InboxService inboxService;
    private final Screen returnScreen;

    private AssessmentOverviewDTO overview;
    private int focusedButton = 0;
    private String bannerMessage = "";

    private boolean requestingExamReason = false;
    private final StringBuilder examReasonBuffer = new StringBuilder();
    private int examReasonFocusIndex = 0;
    private boolean isMissedExam = false;
    private Timestamp examReferenceTime = null;

    public AssessmentOverviewScreen(Quiz quiz, ExamService examService, AuthService authService, InboxService inboxService, Screen returnScreen) {
        this.quiz = quiz;
        this.examService = examService;
        this.authService = authService;
        this.inboxService = inboxService;
        this.returnScreen = returnScreen;
        refreshOverview();
    }

    private void refreshOverview() {
        User student = Session.getCurrentUser().orElse(null);
        int studentId = (student != null && student.getId() != null) ? student.getId() : 0;
        this.overview = examService.getAssessmentOverview(quiz.getId(), studentId)
                .orElseGet(() -> AssessmentOverviewDTO.builder()
                        .quiz(quiz)
                        .subjectCode(quiz.getSubjectCode())
                        .subjectName(quiz.getSubjectName() != null ? quiz.getSubjectName() : quiz.getSubjectCode())
                        .teacherName(quiz.getCreatorName() != null ? quiz.getCreatorName() : "Teacher")
                        .questionCount(quiz.getQuestionCount())
                        .totalPoints(quiz.getTotalPoints())
                        .timeLimitMins(quiz.getTimeLimitMins())
                        .speedSecondsPerQuestion(quiz.getSpeedSecondsPerQuestion())
                        .passScorePercent(quiz.getPassScore())
                        .questionTypesSummary("Multiple Choice")
                        .overallDifficulty("MEDIUM")
                        .canStart(!quiz.isExpired())
                        .build());
    }

    private List<String> getButtonLabels() {
        List<String> labels = new ArrayList<>();
        AssessmentType aType = quiz.getAssessmentType() != null ? quiz.getAssessmentType() : AssessmentType.QUIZ;

        if (aType == AssessmentType.SPEED) {
            labels.add(overview.isCanViewResult() ? "Play Again" : "Start Speed Run");
            if (overview.isCanViewResult()) {
                labels.add("View Scorecard");
            }
        } else {
            if (overview.isCanStart()) {
                boolean inProgress = overview.getStudentAttempt() != null
                        && overview.getStudentAttempt().getStatus() == AttemptStatus.IN_PROGRESS;
                if (inProgress) {
                    labels.add("Resume Assessment");
                } else if (aType == AssessmentType.EXAM) {
                    labels.add("Start Exam");
                } else {
                    labels.add("Start Quiz");
                }
            }

            if (overview.isCanViewResult()) {
                labels.add("View Scorecard");
            }

            if (overview.isCanRetake()) {
                labels.add(aType == AssessmentType.EXAM ? "Request Makeup" : "Request Retake");
            }
        }

        labels.add("Back to List");
        return labels;
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
            if (requestingExamReason) {
                return handleReasonDialogInput(k);
            }

            List<String> buttons = getButtonLabels();

            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(returnScreen);
            }

            if (KeyUtil.isLeft(k)) {
                if (!buttons.isEmpty()) {
                    focusedButton = (focusedButton - 1 + buttons.size()) % buttons.size();
                }
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isRight(k)) {
                if (!buttons.isEmpty()) {
                    focusedButton = (focusedButton + 1) % buttons.size();
                }
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isEnter(k)) {
                if (focusedButton >= 0 && focusedButton < buttons.size()) {
                    String action = buttons.get(focusedButton);
                    return handleAction(action);
                }
            }
        }
        return ScreenResult.stay(this);
    }

    private ScreenResult handleAction(String action) {
        if ("Back to List".equalsIgnoreCase(action)) {
            return ScreenResult.navigate(returnScreen);
        }

        User student = Session.getCurrentUser().orElse(null);
        int studentId = (student != null && student.getId() != null) ? student.getId() : 0;
        AssessmentType aType = quiz.getAssessmentType() != null ? quiz.getAssessmentType() : AssessmentType.QUIZ;

        if (action.startsWith("Start") || action.startsWith("Resume") || "Play Again".equalsIgnoreCase(action)) {
            if (aType == AssessmentType.SPEED) {
                if (quiz.isExpired()) {
                    bannerMessage = TuiHelper.red("✖ This speed quiz has expired.");
                    return ScreenResult.stay(this);
                }
                try {
                    SpeedQuizSession speedSession = examService.startSpeedQuiz(quiz.getId(), studentId);
                    return ScreenResult.navigate(new SpeedQuizTakerScreen(speedSession, examService, authService, returnScreen));
                } catch (ValidationException e) {
                    bannerMessage = TuiHelper.red("✖ " + e.getMessage());
                    return ScreenResult.stay(this);
                } catch (Exception e) {
                    bannerMessage = TuiHelper.red("✖ Failed to start speed quiz: " + e.getMessage());
                    return ScreenResult.stay(this);
                }
            }

            try {
                ExamSession session = examService.startExam(quiz.getId(), studentId);
                return ScreenResult.navigate(new ExamTakerScreen(session, examService, authService));
            } catch (ValidationException e) {
                bannerMessage = TuiHelper.red("✖ " + e.getMessage());
                return ScreenResult.stay(this);
            } catch (Exception e) {
                bannerMessage = TuiHelper.red("✖ Failed to start assessment: " + e.getMessage());
                return ScreenResult.stay(this);
            }
        }

        if ("View Scorecard".equalsIgnoreCase(action)) {
            if (overview.getStudentAttempt() != null) {
                Optional<Result> resOpt = examService.getResultByAttempt(overview.getStudentAttempt().getId());
                if (resOpt.isPresent()) {
                    if (aType == AssessmentType.SPEED) {
                        return ScreenResult.navigate(new SpeedQuizResultScreen(resOpt.get(), this, examService, authService));
                    }
                    return ScreenResult.navigate(new ExamResultScreen(resOpt.get(), this));
                }
            }
            bannerMessage = TuiHelper.yellow("● Result record not found.");
            return ScreenResult.stay(this);
        }

        if ("Request Retake".equalsIgnoreCase(action) || "Request Makeup".equalsIgnoreCase(action)) {
            return initiateRetake(aType, studentId);
        }

        return ScreenResult.stay(this);
    }

    private ScreenResult initiateRetake(AssessmentType aType, int studentId) {
        if (aType == AssessmentType.QUIZ) {
            if (overview.getStudentAttempt() == null || overview.getStudentAttempt().getStatus() != AttemptStatus.AUTO_SUBMITTED) {
                bannerMessage = TuiHelper.yellow("● Quiz retakes can only be requested if your quiz timer expired.");
                return ScreenResult.stay(this);
            }
            if (quiz.getCreatedBy() == null) {
                bannerMessage = TuiHelper.red("✖ Teacher for this quiz was not found.");
                return ScreenResult.stay(this);
            }
            try {
                inboxService.sendQuizRetakeRequest(studentId, quiz.getCreatedBy(), quiz.getId(), quiz.getTitle());
                bannerMessage = TuiHelper.green("✔ Quiz retake request sent to teacher's inbox!");
                refreshOverview();
            } catch (ValidationException e) {
                bannerMessage = TuiHelper.yellow("● " + e.getMessage());
            }
            return ScreenResult.stay(this);
        } else {
            boolean failed = false;
            Timestamp refTime = null;

            if (overview.getStudentAttempt() != null) {
                Attempt att = overview.getStudentAttempt();
                Optional<Result> resOpt = examService.getResultByAttempt(att.getId());
                if (resOpt.isPresent() && !resOpt.get().isPassed()) {
                    failed = true;
                    refTime = att.getSubmittedAt() != null ? att.getSubmittedAt() : att.getStartedAt();
                }
            }

            boolean missed = false;
            if (overview.getStudentAttempt() == null && quiz.isExpired()) {
                missed = true;
                refTime = quiz.getExpiresAt();
            }

            if (!failed && !missed) {
                bannerMessage = TuiHelper.yellow("● Exam makeup can only be requested if you failed or missed the exam.");
                return ScreenResult.stay(this);
            }

            long now = System.currentTimeMillis();
            long threeDaysMillis = 3L * 24 * 3600 * 1000;
            if (refTime != null && (now - refTime.getTime() > threeDaysMillis)) {
                bannerMessage = TuiHelper.red("✖ Exam makeup request rejected: must be requested within 3 days of the exam.");
                return ScreenResult.stay(this);
            }

            requestingExamReason = true;
            examReasonBuffer.setLength(0);
            examReasonFocusIndex = 0;
            isMissedExam = missed;
            examReferenceTime = refTime;
            bannerMessage = "";
            return ScreenResult.stay(this);
        }
    }

    private ScreenResult handleReasonDialogInput(KeyPressMessage k) {
        if (KeyUtil.isEsc(k)) {
            requestingExamReason = false;
            bannerMessage = TuiHelper.yellow("Makeup request cancelled.");
            return ScreenResult.stay(this);
        }

        if (KeyUtil.isDown(k)) {
            examReasonFocusIndex = (examReasonFocusIndex + 1) % 3;
            return ScreenResult.stay(this);
        }

        if (KeyUtil.isUp(k)) {
            examReasonFocusIndex = (examReasonFocusIndex - 1 + 3) % 3;
            return ScreenResult.stay(this);
        }

        if (examReasonFocusIndex > 0 && (KeyUtil.isLeft(k) || KeyUtil.isRight(k))) {
            examReasonFocusIndex = (examReasonFocusIndex == 1) ? 2 : 1;
            return ScreenResult.stay(this);
        }

        if (KeyUtil.isEnter(k)) {
            if (examReasonFocusIndex == 2) {
                requestingExamReason = false;
                bannerMessage = TuiHelper.yellow("Makeup request cancelled.");
                return ScreenResult.stay(this);
            }

            if (examReasonBuffer.toString().trim().isBlank()) {
                bannerMessage = TuiHelper.red("✖ A reason is required.");
                return ScreenResult.stay(this);
            }

            User student = Session.getCurrentUser().orElse(null);
            int studentId = (student != null && student.getId() != null) ? student.getId() : 0;

            if (quiz.getCreatedBy() == null) {
                bannerMessage = TuiHelper.red("✖ Teacher for this exam was not found.");
                requestingExamReason = false;
                return ScreenResult.stay(this);
            }

            try {
                inboxService.sendExamRetakeRequest(
                        studentId,
                        quiz.getCreatedBy(),
                        quiz.getId(),
                        quiz.getTitle(),
                        examReasonBuffer.toString().trim(),
                        isMissedExam,
                        examReferenceTime
                );
                bannerMessage = TuiHelper.green("✔ Exam makeup request sent to teacher's inbox!");
                refreshOverview();
            } catch (ValidationException e) {
                bannerMessage = TuiHelper.yellow("● " + e.getMessage());
            }
            requestingExamReason = false;
            return ScreenResult.stay(this);
        }

        if (examReasonFocusIndex == 0) {
            if (KeyUtil.isBackspace(k)) {
                if (!examReasonBuffer.isEmpty()) {
                    examReasonBuffer.deleteCharAt(examReasonBuffer.length() - 1);
                }
                return ScreenResult.stay(this);
            }

            if (k.type() == KeyType.KeyRunes && k.runes() != null) {
                for (char c : k.runes()) {
                    if (!Character.isISOControl(c)) examReasonBuffer.append(c);
                }
            } else if (k.key() != null && k.key().length() == 1 && !Character.isISOControl(k.key().charAt(0))) {
                examReasonBuffer.append(k.key());
            }
        }

        return ScreenResult.stay(this);
    }

    @Override
    public String view() {
        if (requestingExamReason) {
            StringBuilder sb = new StringBuilder();
            sb.append(TuiHelper.header("EXAMS"));
            sb.append("\n");
            sb.append(TuiHelper.boxTitle("Request Exam Makeup", quiz.getTitle())).append("\n\n");
            sb.append("  ").append(TuiHelper.bold("Reason (Required for Teacher Review):")).append("\n\n");
            sb.append(TuiHelper.inputBox("Reason", examReasonBuffer.toString(), examReasonFocusIndex == 0, 102, false, "e.g. Illness, technical malfunction, etc."));
            sb.append("\n");
            sb.append(TuiHelper.buttonRow("Submit Request", examReasonFocusIndex == 1, "Cancel", examReasonFocusIndex == 2)).append("\n\n");
            if (!bannerMessage.isBlank()) {
                sb.append("  ").append(bannerMessage).append("\n\n");
            }
            sb.append(TuiHelper.dim("  [↑/↓] Switch Field  •  [Enter] Confirm  •  [Esc] Cancel\n"));
            return sb.toString();
        }

        List<String> buttons = getButtonLabels();
        if (focusedButton >= buttons.size()) {
            focusedButton = Math.max(0, buttons.size() - 1);
        }
        return ExamViews.renderAssessmentOverview(overview, focusedButton, buttons, bannerMessage);
    }
}
