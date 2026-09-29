package com.proctor.controller;

import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.repository.UserRepository;
import com.proctor.model.service.AuthService;
import com.proctor.model.enums.AssessmentType;
import com.proctor.model.enums.AttemptStatus;
import com.proctor.exception.ValidationException;
import com.proctor.model.entity.ExamSession;
import com.proctor.model.service.ExamService;
import com.proctor.model.repository.InboxRepository;
import com.proctor.model.service.InboxService;
import com.proctor.model.entity.Quiz;
import com.proctor.model.entity.Result;
import com.proctor.util.KeyUtil;
import com.proctor.util.MouseUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.ExamViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.PasteMessage;

public class ExamResultScreen implements Screen {
    private final Result result;
    private final ExamSession session;
    private final ExamService examService;
    private final AuthService authService;
    private final Screen returnScreen;
    private final InboxService inboxService;
    private String bannerMessage = "";

    private boolean requestingExamReason = false;
    private final StringBuilder examReasonBuffer = new StringBuilder();
    private int examReasonFocusIndex = 0;

    public ExamResultScreen(Result result, ExamSession session, ExamService examService, AuthService authService) {
        this(result, session, examService, authService, new InboxService(new InboxRepository(), new UserRepository()));
    }

    public ExamResultScreen(Result result, ExamSession session, ExamService examService, AuthService authService, InboxService inboxService) {
        this.result = result;
        this.session = session;
        this.examService = examService;
        this.authService = authService;
        this.returnScreen = null;
        this.inboxService = inboxService != null ? inboxService : new InboxService(new InboxRepository(), new UserRepository());

        if (this.result != null && (this.result.getStudentName() == null || this.result.getStudentName().isBlank())) {
            Session.getCurrentUser().ifPresent(u -> this.result.setStudentName(u.getFullName()));
        }
    }

    public ExamResultScreen(Result result, Screen returnScreen) {
        this.result = result;
        this.session = null;
        this.examService = null;
        this.authService = null;
        this.returnScreen = returnScreen;
        this.inboxService = new InboxService(new InboxRepository(), new UserRepository());

        if (this.result != null && (this.result.getStudentName() == null || this.result.getStudentName().isBlank())) {
            Session.getCurrentUser().ifPresent(u -> this.result.setStudentName(u.getFullName()));
        }
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof PasteMessage paste && requestingExamReason && examReasonFocusIndex == 0) {
            KeyUtil.pasteToBuffer(examReasonBuffer, paste.content(), 500);
            bannerMessage = "";
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isLeftClick(msg)) {
            if (requestingExamReason) {
                int line = MouseUtil.getLineIndex(msg);
                int col = MouseUtil.getColInLine(msg);
                int btnLine = MouseUtil.findButtonRowLine(view());
                if (btnLine != -1 && line >= btnLine && line <= btnLine + 2) {
                    int btn = MouseUtil.getClickedButtonIndex(col, "Submit Request", "Cancel");
                    if (btn == 0) {
                        return submitExamReason();
                    } else if (btn == 1) {
                        requestingExamReason = false;
                        bannerMessage = TuiHelper.yellow("● Makeup request cancelled.");
                        return ScreenResult.stay(this);
                    }
                }
                return ScreenResult.stay(this);
            }

            int line = MouseUtil.getLineIndex(msg);
            int col = MouseUtil.getColInLine(msg);
            String hintAction = MouseUtil.getClickedHintAction(view(), line, col);
            if ("r".equalsIgnoreCase(hintAction)) {
                return handleRetakeRequest();
            }
            if ("Esc".equalsIgnoreCase(hintAction)) {
                if (returnScreen != null) {
                    return ScreenResult.navigate(returnScreen);
                }
                return ScreenResult.navigate(new StudentDashboardScreen(authService, examService));
            }
            if (line >= 0 && !canRequestRetake()) {
                if (returnScreen != null) {
                    return ScreenResult.navigate(returnScreen);
                }
                return ScreenResult.navigate(new StudentDashboardScreen(authService, examService));
            }
            return ScreenResult.stay(this);
        }

        if (msg instanceof KeyPressMessage k) {
            if (requestingExamReason) {
                return handleReasonDialogInput(k);
            }

            if ("r".equalsIgnoreCase(k.key())) {
                return handleRetakeRequest();
            }

            if (KeyUtil.isEnter(k) || KeyUtil.isEsc(k)) {
                if (returnScreen != null) {
                    return ScreenResult.navigate(returnScreen);
                }
                return ScreenResult.navigate(new StudentDashboardScreen(authService, examService));
            }
        }
        return ScreenResult.stay(this);
    }

    private ScreenResult handleReasonDialogInput(KeyPressMessage k) {
        if (KeyUtil.isEsc(k)) {
            requestingExamReason = false;
            bannerMessage = TuiHelper.yellow("● Makeup request cancelled.");
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
                bannerMessage = TuiHelper.yellow("● Makeup request cancelled.");
                return ScreenResult.stay(this);
            } else if (examReasonFocusIndex == 1) {
                return submitExamReason();
            } else {
                examReasonFocusIndex = 1;
                return ScreenResult.stay(this);
            }
        }

        if (examReasonFocusIndex == 0) {
            if (KeyUtil.handleBackspace(examReasonBuffer, k)) {
                bannerMessage = "";
            } else if (KeyUtil.appendInput(examReasonBuffer, k)) {
                bannerMessage = "";
            }
        }
        return ScreenResult.stay(this);
    }

    private ScreenResult submitExamReason() {
        if (examReasonBuffer.toString().trim().isBlank()) {
            bannerMessage = TuiHelper.red("✖ A reason is required.");
            return ScreenResult.stay(this);
        }
        if (examReasonBuffer.toString().trim().length() > 500) {
            bannerMessage = TuiHelper.red("✖ Reason must not exceed 500 characters.");
            return ScreenResult.stay(this);
        }

        Quiz q = (session != null) ? session.getQuiz() : null;
        Integer teacherId = (q != null) ? q.getCreatedBy() : null;
        if (teacherId == null && result != null && result.getQuizId() != null && examService != null) {
            teacherId = examService.getQuiz(result.getQuizId()).map(Quiz::getCreatedBy).orElse(null);
        }
        if (teacherId == null) {
            bannerMessage = TuiHelper.red("✖ Teacher for this exam was not found.");
            requestingExamReason = false;
            return ScreenResult.stay(this);
        }

        try {
            java.sql.Timestamp refTime = (session != null && session.getAttempt() != null && session.getAttempt().getSubmittedAt() != null)
                    ? session.getAttempt().getSubmittedAt()
                    : (result != null && result.getGradedAt() != null ? result.getGradedAt() : new java.sql.Timestamp(System.currentTimeMillis()));
            boolean isMissed = false;
            inboxService.sendExamRetakeRequest(
                    result.getStudentId(),
                    teacherId,
                    result.getQuizId(),
                    result.getQuizTitle(),
                    examReasonBuffer.toString().trim(),
                    isMissed,
                    refTime
            );
            bannerMessage = TuiHelper.green("✔ Exam makeup request sent to teacher's inbox!");
        } catch (ValidationException e) {
            bannerMessage = TuiHelper.yellow("● " + e.getMessage());
        }
        requestingExamReason = false;
        return ScreenResult.stay(this);
    }

    private boolean canRequestRetake() {
        if (result == null) return false;
        if (result.isPassed()) return false;
        if (result.isPendingReview()) return false;

        if (result.getAssessmentType() == AssessmentType.QUIZ) {
            return session != null && session.getAttempt() != null && session.getAttempt().getStatus() == AttemptStatus.AUTO_SUBMITTED;
        } else if (result.getAssessmentType() == AssessmentType.EXAM) {
            boolean timedOut = session != null && session.getAttempt() != null && session.getAttempt().getStatus() == AttemptStatus.AUTO_SUBMITTED;
            boolean failed = !result.isPassed();
            return timedOut || failed;
        }
        return false;
    }

    private ScreenResult handleRetakeRequest() {
        if (result != null && result.isPassed()) {
            bannerMessage = TuiHelper.yellow("● Retakes cannot be requested for assessments that have been passed.");
            return ScreenResult.stay(this);
        }
        if (result != null && result.isPendingReview()) {
            bannerMessage = TuiHelper.yellow("● This assessment is pending teacher review. Makeups can only be requested if you fail after grading.");
            return ScreenResult.stay(this);
        }
        if (result != null && result.getAssessmentType() == AssessmentType.EXAM) {
            if (!canRequestRetake()) {
                bannerMessage = TuiHelper.yellow("● Exam makeup can only be requested if you failed or timed out.");
                return ScreenResult.stay(this);
            }
            requestingExamReason = true;
            examReasonBuffer.setLength(0);
            examReasonFocusIndex = 0;
            bannerMessage = "";
            return ScreenResult.stay(this);
        }
        if (session != null && session.getAttempt() != null && session.getAttempt().getStatus() == AttemptStatus.AUTO_SUBMITTED) {
            Quiz q = session.getQuiz();
            if (q != null && q.getCreatedBy() != null) {
                try {
                    inboxService.sendQuizRetakeRequest(result.getStudentId(), q.getCreatedBy(), q.getId(), q.getTitle());
                    bannerMessage = TuiHelper.green("✔ Quiz retake request sent to teacher's inbox!");
                } catch (ValidationException e) {
                    bannerMessage = TuiHelper.yellow("● " + e.getMessage());
                }
            } else {
                bannerMessage = TuiHelper.red("✖ Teacher for this quiz was not found.");
            }
        } else {
            bannerMessage = TuiHelper.yellow("● Retakes can only be requested if timer expired.");
        }
        return ScreenResult.stay(this);
    }

    @Override
    public String view() {
        if (requestingExamReason) {
            String title = (session != null && session.getQuiz() != null) ? session.getQuiz().getTitle() : (result != null ? result.getQuizTitle() : "Exam");
            return ExamViews.renderExamReasonDialog(title, examReasonBuffer.toString(), examReasonFocusIndex, bannerMessage);
        }

        boolean canRetake = canRequestRetake();
        String retakeNotice = "";
        if (canRetake && bannerMessage.isBlank()) {
            if (result != null && result.getAssessmentType() == AssessmentType.EXAM) {
                retakeNotice = TuiHelper.yellow("● Assessment completed. Press [r] to request an exam makeup from your teacher.");
            } else {
                retakeNotice = TuiHelper.yellow("● Timer expired. Press [r] to request a retake from your teacher.");
            }
        }
        String msg = !bannerMessage.isBlank() ? bannerMessage : retakeNotice;
        return ExamViews.renderExamResult(result, session, returnScreen != null, msg, canRetake);
    }
}
