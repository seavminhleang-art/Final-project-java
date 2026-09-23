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
import com.proctor.util.ListNavigationHelper;
import com.proctor.util.MouseUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.ExamViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;
import com.proctor.model.entity.SpeedQuizSession;

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

    private List<Quiz> allQuizzes = new java.util.ArrayList<>();
    private List<Quiz> quizzes = new java.util.ArrayList<>();
    private int selectedIndex = 0;
    private String bannerMessage = "";
    private final StringBuilder searchBuffer = new StringBuilder();
    private boolean searchMode = false;
    private int subjectFilterIndex = 0;
    private final List<String> subjectCodes = new java.util.ArrayList<>();
    private final Map<Integer, String> subjects = new java.util.HashMap<>();
    private final Map<Integer, Attempt> attempts = new java.util.HashMap<>();
    private InlineSubjectFilter<String> subjectFilter;

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
        int studentId = (student != null && student.getId() != null) ? student.getId() : 0;
        if (assessmentType == AssessmentType.EXAM) {
            this.allQuizzes = examService.getAvailableExams(studentId);
        } else if (assessmentType == AssessmentType.SPEED) {
            this.allQuizzes = examService.getAvailableSpeedQuizzes(studentId);
        } else {
            this.allQuizzes = examService.getAvailableQuizzes(studentId);
        }
        subjects.clear();
        subjectCodes.clear();
        for (Quiz q : allQuizzes) {
            if (q.getSubjectCode() != null && !q.getSubjectCode().isBlank()) {
                if (q.getSubjectId() != null) {
                    subjects.put(q.getSubjectId(), q.getSubjectCode());
                }
                if (!subjectCodes.contains(q.getSubjectCode())) {
                    subjectCodes.add(q.getSubjectCode());
                }
            }
        }
        java.util.Collections.sort(subjectCodes);
        List<InlineSubjectFilter.Item<String>> items = new java.util.ArrayList<>();
        items.add(new InlineSubjectFilter.Item<>(null, "ALL", "All Subjects"));
        for (String code : subjectCodes) {
            items.add(new InlineSubjectFilter.Item<>(code, code, ""));
        }
        if (subjectFilter == null) {
            this.subjectFilter = new InlineSubjectFilter<>(items);
        } else {
            this.subjectFilter.setItems(items);
        }
        applyFilters();
        refreshAttempts();
    }

    private void refreshAttempts() {
        attempts.clear();
        User student = Session.getCurrentUser().orElse(null);
        int studentId = (student != null && student.getId() != null) ? student.getId() : 0;
        for (Quiz q : allQuizzes) {
            examService.getStudentAttempt(q.getId(), studentId).ifPresent(att -> attempts.put(q.getId(), att));
        }
    }

    private void applyFilters() {
        String filterCode = (subjectFilterIndex > 0 && subjectFilterIndex <= subjectCodes.size())
                ? subjectCodes.get(subjectFilterIndex - 1) : null;
        String search = searchBuffer.toString().trim().toLowerCase();

        this.quizzes = allQuizzes.stream().filter(q -> {
            if (filterCode != null && !filterCode.equalsIgnoreCase(q.getSubjectCode())) {
                return false;
            }
            if (!search.isEmpty()) {
                boolean matchTitle = q.getTitle() != null && q.getTitle().toLowerCase().contains(search);
                boolean matchCreator = q.getCreatorName() != null && q.getCreatorName().toLowerCase().contains(search);
                boolean matchSubject = q.getSubjectCode() != null && q.getSubjectCode().toLowerCase().contains(search);
                if (!matchTitle && !matchCreator && !matchSubject) {
                    return false;
                }
            }
            return true;
        }).toList();

        if (quizzes.isEmpty()) {
            selectedIndex = 0;
        } else if (selectedIndex >= quizzes.size()) {
            selectedIndex = quizzes.size() - 1;
        }
    }

    @Override
    public ScreenResult update(Message msg) {
        if (MouseUtil.isWheelUp(msg)) {
            if (!quizzes.isEmpty() && selectedIndex > 0) {
                selectedIndex--;
            }
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isWheelDown(msg)) {
            if (!quizzes.isEmpty() && selectedIndex < quizzes.size() - 1) {
                selectedIndex++;
            }
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
                } else if (line >= 11 && line <= 15) {
                    examReasonFocusIndex = 0;
                }
                return ScreenResult.stay(this);
            }

            int line = MouseUtil.getLineIndex(msg);
            int col = MouseUtil.getColInLine(msg);

            int itemsStartLine = MouseUtil.findTableStartLine(view());
            int pageSize = TuiHelper.PAGE_SIZE;
            int totalPages = Math.max(1, (int) Math.ceil((double) quizzes.size() / pageSize));
            int currentPage = selectedIndex / pageSize;
            int startRow = currentPage * pageSize;
            int endRow = Math.min(quizzes.size(), startRow + pageSize);
            int displayedRows = endRow - startRow;

            if (itemsStartLine != -1 && line >= itemsStartLine && line < itemsStartLine + displayedRows * 2) {
                int clickedOffset = (line - itemsStartLine) / 2;
                int targetIdx = startRow + clickedOffset;
                if (targetIdx < quizzes.size()) {
                    if (selectedIndex == targetIdx) {
                        Quiz q = quizzes.get(selectedIndex);
                        return ScreenResult.navigate(new AssessmentOverviewScreen(q, examService, authService, inboxService, this));
                    } else {
                        selectedIndex = targetIdx;
                    }
                }
                return ScreenResult.stay(this);
            }

            int pagLine = MouseUtil.findPaginationLine(view());
            if (pagLine != -1 && line == pagLine && !quizzes.isEmpty()) {
                selectedIndex = ListNavigationHelper.handlePaginationClick(col, selectedIndex, quizzes.size(), TuiHelper.PAGE_SIZE);
                return ScreenResult.stay(this);
            }

            String hintAction = MouseUtil.getClickedHintAction(view(), line, col);
            if (hintAction != null) {
                if ("Esc".equals(hintAction)) {
                    return ScreenResult.navigate(new StudentDashboardScreen(authService, examService));
                }
            }

            return ScreenResult.stay(this);
        }

        if (msg instanceof KeyPressMessage k) {
            if (subjectFilter != null && subjectFilter.isActive()) {
                boolean handled = subjectFilter.handleKey(k);
                if (handled) {
                    subjectFilterIndex = subjectFilter.getSelectedOriginalIndex();
                    selectedIndex = 0;
                    applyFilters();
                    return ScreenResult.stay(this);
                }
            }

            if (requestingExamReason) {
                return handleReasonDialogInput(k);
            }

            if (searchMode) {
                if (KeyUtil.isEsc(k) || KeyUtil.isEnter(k)) {
                    searchMode = false;
                    applyFilters();
                } else if (KeyUtil.handleBackspace(searchBuffer, k)) {
                    applyFilters();
                } else if (KeyUtil.appendInput(searchBuffer, k)) {
                    applyFilters();
                }
                return ScreenResult.stay(this);
            }

            bannerMessage = "";

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
                selectedIndex = ListNavigationHelper.prevPage(selectedIndex, TuiHelper.PAGE_SIZE);
            } else if (KeyUtil.isRight(k)) {
                selectedIndex = ListNavigationHelper.nextPage(selectedIndex, quizzes.size(), TuiHelper.PAGE_SIZE);
            } else if ("r".equalsIgnoreCase(k.key())) {
                return handleRetakeRequest();
            } else if ("s".equalsIgnoreCase(k.key())) {
                if (subjectFilter != null) {
                    subjectFilter.startSearch(subjectFilterIndex);
                    selectedIndex = 0;
                    applyFilters();
                }
            } else if ("/".equals(k.key())) {
                searchMode = true;
                searchBuffer.setLength(0);
                applyFilters();
            } else if ("v".equalsIgnoreCase(k.key()) && assessmentType == AssessmentType.SPEED) {
                if (!quizzes.isEmpty()) {
                    Quiz q = quizzes.get(selectedIndex);
                    User student = Session.getCurrentUser().orElse(null);
                    int studentId = (student != null && student.getId() != null) ? student.getId() : 0;
                    Optional<Attempt> attOpt = examService.getStudentAttempt(q.getId(), studentId);
                    if (attOpt.isPresent() && attOpt.get().getStatus() == AttemptStatus.GRADED) {
                        Optional<Result> resOpt = examService.getResultByAttempt(attOpt.get().getId());
                        if (resOpt.isPresent()) {
                            return ScreenResult.navigate(new SpeedQuizResultScreen(resOpt.get(), this, examService, authService));
                        }
                    }
                    bannerMessage = TuiHelper.yellow("● No completed run found for this speed quiz.");
                    return ScreenResult.stay(this);
                }
            } else if (KeyUtil.isEnter(k)) {
                if (!quizzes.isEmpty()) {
                    Quiz q = quizzes.get(selectedIndex);
                    return ScreenResult.navigate(new AssessmentOverviewScreen(q, examService, authService, inboxService, this));
                }
                return ScreenResult.stay(this);
            }
        }
        return ScreenResult.stay(this);
    }

    private ScreenResult handleRetakeRequest() {
        if (quizzes.isEmpty()) return ScreenResult.stay(this);
        Quiz q = quizzes.get(selectedIndex);
        User student = Session.getCurrentUser().orElse(null);
        int studentId = (student != null && student.getId() != null) ? student.getId() : 0;
        Optional<Attempt> attOpt = examService.getStudentAttempt(q.getId(), studentId);

        if (assessmentType == AssessmentType.SPEED) {
            bannerMessage = TuiHelper.yellow("● Speed quizzes are score challenges and do not require retake requests.");
            return ScreenResult.stay(this);
        }

        if (assessmentType == AssessmentType.QUIZ) {
            if (attOpt.isEmpty() || attOpt.get().getStatus() != AttemptStatus.AUTO_SUBMITTED) {
                bannerMessage = TuiHelper.yellow("● Quiz retakes can only be requested if your quiz timer expired.");
                return ScreenResult.stay(this);
            }

            if (q.getCreatedBy() == null) {
                bannerMessage = TuiHelper.red("✖ Teacher for this quiz was not found.");
                return ScreenResult.stay(this);
            }

            try {
                inboxService.sendQuizRetakeRequest(studentId, q.getCreatedBy(), q.getId(), q.getTitle());
                bannerMessage = TuiHelper.green("✔ Quiz retake request sent to teacher's inbox!");
                refreshAttempts();
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
            if (refTime != null && (now - refTime.getTime() > InboxService.THREE_DAYS_MILLIS)) {
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
            }
            return submitExamReason();
        }

        if (examReasonFocusIndex == 0) {
            if (KeyUtil.handleBackspace(examReasonBuffer, k)) {
                return ScreenResult.stay(this);
            }

            KeyUtil.appendInput(examReasonBuffer, k, 500);
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

        Quiz q = quizzes.get(selectedIndex);
        User student = Session.getCurrentUser().orElse(null);
        int studentId = (student != null && student.getId() != null) ? student.getId() : 0;

        if (q.getCreatedBy() == null) {
            bannerMessage = TuiHelper.red("✖ Teacher for this exam was not found.");
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
            bannerMessage = TuiHelper.green("✔ Exam makeup request sent to teacher's inbox!");
            refreshAttempts();
        } catch (ValidationException e) {
            bannerMessage = TuiHelper.yellow("● " + e.getMessage());
        }
        requestingExamReason = false;
        return ScreenResult.stay(this);
    }

    private ScreenResult handleQuizAction() {
        if (quizzes.isEmpty()) return ScreenResult.stay(this);

        Quiz q = quizzes.get(selectedIndex);
        User student = Session.getCurrentUser().orElse(null);
        int studentId = (student != null && student.getId() != null) ? student.getId() : 0;

        if (assessmentType == AssessmentType.SPEED) {
            if (q.isExpired()) {
                bannerMessage = TuiHelper.red("✖ This speed quiz has expired and is no longer available.");
                return ScreenResult.stay(this);
            }
            try {
                SpeedQuizSession speedSession = examService.startSpeedQuiz(q.getId(), studentId);
                return ScreenResult.navigate(new SpeedQuizTakerScreen(speedSession, examService, authService, this));
            } catch (ValidationException e) {
                bannerMessage = TuiHelper.red("✖ " + e.getMessage());
                return ScreenResult.stay(this);
            } catch (Exception e) {
                bannerMessage = TuiHelper.red("✖ Failed to start speed quiz: " + e.getMessage());
                return ScreenResult.stay(this);
            }
        }

        Optional<Attempt> attOpt = examService.getStudentAttempt(q.getId(), studentId);
        if (attOpt.isPresent()) {
            Attempt att = attOpt.get();
            if (att.getStatus() == AttemptStatus.GRADED) {
                Optional<Result> resOpt = examService.getResultByAttempt(att.getId());
                if (resOpt.isPresent()) {
                    return ScreenResult.navigate(new ExamResultScreen(resOpt.get(), this));
                }
                bannerMessage = TuiHelper.yellow("● You have already completed this assessment. View your scorecard in History.");
                return ScreenResult.stay(this);
            } else if (att.getStatus() == AttemptStatus.TURNED_IN) {
                bannerMessage = TuiHelper.yellow("● This assessment is turned in and pending teacher review.");
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
            return ExamViews.renderExamReasonDialog(q.getTitle(), examReasonBuffer.toString(), examReasonFocusIndex, bannerMessage);
        }

        String subjectFilterDisplay = (subjectFilter != null) ? subjectFilter.getHeaderDisplay() : "ALL";
        return ExamViews.renderAvailableQuizzes(assessmentType, quizzes, subjects, attempts, selectedIndex,
                subjectFilterDisplay, searchBuffer.toString(), searchMode, bannerMessage);
    }
}