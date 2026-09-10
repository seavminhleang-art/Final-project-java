package com.proctor.controller;

import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.repository.UserRepository;
import com.proctor.model.service.AuthService;
import com.proctor.model.enums.AssessmentType;
import com.proctor.model.enums.AttemptStatus;
import com.proctor.exception.ValidationException;
import com.proctor.model.entity.Attempt;
import com.proctor.model.entity.ExamSession;
import com.proctor.model.service.ExamService;
import com.proctor.model.repository.InboxRepository;
import com.proctor.model.service.InboxService;
import com.proctor.model.entity.Quiz;
import com.proctor.model.entity.Result;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.ExamViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AvailableQuizzesScreen implements Screen {
    private final ExamService examService;
    private final AuthService authService;
    private final AssessmentType assessmentType;
    private final InboxService inboxService;
    private final Screen returnScreen;

    private List<Quiz> quizzes;
    private int selectedIndex = 0;
    private String bannerMessage = "";

    private boolean requestingExamReason = false;
    private final StringBuilder examReasonBuffer = new StringBuilder();
    private int examReasonFocusIndex = 0;
    private boolean isMissedExam = false;
    private Timestamp examReferenceTime = null;

    public AvailableQuizzesScreen(ExamService examService, AuthService authService) {
        this(examService, authService, AssessmentType.QUIZ);
    }

    public AvailableQuizzesScreen(ExamService examService, AuthService authService, AssessmentType assessmentType) {
        this(examService, authService, assessmentType, new InboxService(new InboxRepository(), new UserRepository()));
    }

    public AvailableQuizzesScreen(ExamService examService, AuthService authService, AssessmentType assessmentType, InboxService inboxService) {
        this(examService, authService, assessmentType, inboxService, null);
    }

    public AvailableQuizzesScreen(ExamService examService, AuthService authService, AssessmentType assessmentType, InboxService inboxService, Screen returnScreen) {
        this.examService = examService;
        this.authService = authService;
        this.assessmentType = assessmentType != null ? assessmentType : AssessmentType.QUIZ;
        this.inboxService = inboxService != null ? inboxService : new InboxService(new InboxRepository(), new UserRepository());
        this.returnScreen = returnScreen;
        refreshList();
    }

    private void refreshList() {
        User student = Session.getCurrentUser().orElse(null);
        int studentId = student != null ? student.getId() : 0;
        if (assessmentType == AssessmentType.EXAM) {
            this.quizzes = examService.getAvailableExams(studentId);
        } else {
            this.quizzes = examService.getAvailableQuizzes(studentId);
        }
        if (quizzes.isEmpty()) {
            selectedIndex = 0;
        } else if (selectedIndex >= quizzes.size()) {
            selectedIndex = quizzes.size() - 1;
        }
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
            if (requestingExamReason) {
                return handleReasonDialogInput(k);
            }

            if (KeyUtil.isEsc(k)) {
                if (returnScreen != null) {
                    return ScreenResult.navigate(returnScreen);
                }
                return ScreenResult.navigate(new StudentDashboardScreen(authService, examService));
            } else if (KeyUtil.isUp(k)) {
                if (!quizzes.isEmpty()) {
                    selectedIndex = (selectedIndex - 1 + quizzes.size()) % quizzes.size();
                }
            } else if (KeyUtil.isDown(k)) {
                if (!quizzes.isEmpty()) {
                    selectedIndex = (selectedIndex + 1) % quizzes.size();
                }
            } else if (KeyUtil.isLeft(k)) {
                if (!quizzes.isEmpty()) {
                    int pageSize = 5;
                    int currentPage = selectedIndex / pageSize;
                    if (currentPage > 0) {
                        selectedIndex = (currentPage - 1) * pageSize;
                    }
                }
            } else if (KeyUtil.isRight(k)) {
                if (!quizzes.isEmpty()) {
                    int pageSize = 5;
                    int totalPages = Math.max(1, (int) Math.ceil((double) quizzes.size() / pageSize));
                    int currentPage = selectedIndex / pageSize;
                    if (currentPage < totalPages - 1) {
                        selectedIndex = Math.min(quizzes.size() - 1, (currentPage + 1) * pageSize);
                    }
                }
            } else if ("r".equalsIgnoreCase(k.key())) {
                return handleRetakeRequest();
            } else if (KeyUtil.isEnter(k) || "s".equalsIgnoreCase(k.key())) {
                return handleQuizAction();
            }
        }
        return ScreenResult.stay(this);
    }

    private ScreenResult handleRetakeRequest() {
        if (quizzes.isEmpty()) return ScreenResult.stay(this);
        Quiz q = quizzes.get(selectedIndex);
        User student = Session.getCurrentUser().orElse(null);
        int studentId = student != null ? student.getId() : 0;
        Optional<Attempt> attOpt = examService.getStudentAttempt(q.getId(), studentId);

        if (assessmentType == AssessmentType.QUIZ) {
            if (attOpt.isEmpty() || attOpt.get().getStatus() != AttemptStatus.AUTO_SUBMITTED) {
                bannerMessage = TuiHelper.yellow("● Quiz retakes can only be requested if your quiz timer expired.");
                return ScreenResult.stay(this);
            }

            if (q.getCreatedBy() == null) {
                bannerMessage = TuiHelper.red("✖ Instructor for this quiz was not found.");
                return ScreenResult.stay(this);
            }

            try {
                inboxService.sendQuizRetakeRequest(studentId, q.getCreatedBy(), q.getId(), q.getTitle());
                bannerMessage = TuiHelper.green("✔ Quiz retake request sent to instructor's inbox!");
            } catch (ValidationException e) {
                bannerMessage = TuiHelper.yellow("● " + e.getMessage());
            }
            return ScreenResult.stay(this);
        } else {

            boolean failed = false;
            Timestamp refTime = null;

            if (attOpt.isPresent()) {
                Attempt att = attOpt.get();
                Optional<Result> resOpt = examService.getResultByAttempt(att.getId());
                if (resOpt.isPresent() && !resOpt.get().isPassed()) {
                    failed = true;
                    refTime = att.getSubmittedAt() != null ? att.getSubmittedAt() : att.getStartedAt();
                }
            }

            boolean missed = false;
            if (attOpt.isEmpty() && q.isExpired()) {
                missed = true;
                refTime = q.getExpiresAt();
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

        if (KeyUtil.isTab(k) || KeyUtil.isDown(k)) {
            examReasonFocusIndex = (examReasonFocusIndex + 1) % 3;
            return ScreenResult.stay(this);
        }

        if ("shift+tab".equalsIgnoreCase(k.key()) || KeyUtil.isUp(k)) {
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
                bannerMessage = TuiHelper.red("✖ A justification reason is required.");
                return ScreenResult.stay(this);
            }

            Quiz q = quizzes.get(selectedIndex);
            User student = Session.getCurrentUser().orElse(null);
            int studentId = student != null ? student.getId() : 0;

            if (q.getCreatedBy() == null) {
                bannerMessage = TuiHelper.red("✖ Instructor for this exam was not found.");
                requestingExamReason = false;
                return ScreenResult.stay(this);
            }

            try {
                inboxService.sendExamRetakeRequest(
                        studentId,
                        q.getCreatedBy(),
                        q.getId(),
                        q.getTitle(),
                        examReasonBuffer.toString().trim(),
                        isMissedExam,
                        examReferenceTime
                );
                bannerMessage = TuiHelper.green("✔ Exam makeup request sent to instructor's inbox!");
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

    private ScreenResult handleQuizAction() {
        if (quizzes.isEmpty()) return ScreenResult.stay(this);

        Quiz q = quizzes.get(selectedIndex);
        User student = Session.getCurrentUser().orElse(null);
        int studentId = student != null ? student.getId() : 0;

        Optional<Attempt> attOpt = examService.getStudentAttempt(q.getId(), studentId);
        if (attOpt.isPresent()) {
            Attempt att = attOpt.get();
            if (att.getStatus() == AttemptStatus.GRADED) {
                Optional<Result> resOpt = examService.getResultByAttempt(att.getId());
                if (resOpt.isPresent()) {
                    return ScreenResult.navigate(new ExamResultScreen(resOpt.get(), this));
                }
            } else if (att.getStatus() == AttemptStatus.TURNED_IN || att.getStatus() == AttemptStatus.SUBMITTED) {
                bannerMessage = TuiHelper.yellow("● This assessment is Turned In and pending teacher review.");
                return ScreenResult.stay(this);
            } else if (att.getStatus() == AttemptStatus.AUTO_SUBMITTED) {
                bannerMessage = TuiHelper.yellow("● Timer expired. Press [r] to request a retake from your teacher.");
                return ScreenResult.stay(this);
            }
        }

        try {
            ExamSession session = examService.startExam(q.getId(), studentId);
            return ScreenResult.navigate(new ExamTakerScreen(session, examService, authService));
        } catch (ValidationException e) {
            bannerMessage = TuiHelper.red("✖ " + e.getMessage());
            return ScreenResult.stay(this);
        } catch (Exception e) {
            bannerMessage = TuiHelper.red("✖ Failed to start assessment: " + e.getMessage());
            return ScreenResult.stay(this);
        }
    }

    @Override
    public String view() {
        if (requestingExamReason && !quizzes.isEmpty()) {
            Quiz q = quizzes.get(selectedIndex);
            StringBuilder sb = new StringBuilder();
            sb.append(TuiHelper.header("EXAMS"));
            sb.append("\n");
            sb.append(TuiHelper.boxTitle("Request Exam Makeup", q.getTitle())).append("\n\n");
            sb.append("  ").append(TuiHelper.bold("Justification Reason (Required for Instructor Review):")).append("\n\n");
            sb.append(TuiHelper.inputBox("Reason", examReasonBuffer.toString(), examReasonFocusIndex == 0, 72, false, "e.g. Illness, technical malfunction, etc."));
            sb.append("\n");
            sb.append(TuiHelper.buttonRow("Submit Request", examReasonFocusIndex == 1, "Cancel", examReasonFocusIndex == 2)).append("\n\n");
            if (!bannerMessage.isBlank()) {
                sb.append("  ").append(bannerMessage).append("\n\n");
            }
            sb.append(TuiHelper.dim("  [Tab/↑/↓] Switch Field  •  [Enter] Confirm  •  [Esc] Cancel\n"));
            return sb.toString();
        }

        User student = Session.getCurrentUser().orElse(null);
        int studentId = student != null ? student.getId() : 0;
        Map<Integer, Attempt> attempts = new java.util.HashMap<>();
        Map<Integer, String> subjects = new java.util.HashMap<>();
        for (Quiz q : quizzes) {
            examService.getStudentAttempt(q.getId(), studentId).ifPresent(att -> attempts.put(q.getId(), att));
            if (q.getSubjectCode() != null) {
                subjects.put(q.getSubjectId(), q.getSubjectCode());
            }
        }
        return ExamViews.renderAvailableQuizzes(assessmentType, quizzes, subjects, attempts, selectedIndex, bannerMessage);
    }
}