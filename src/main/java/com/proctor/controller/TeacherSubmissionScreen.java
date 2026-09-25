package com.proctor.controller;

import com.proctor.exception.ValidationException;
import com.proctor.model.entity.Attempt;
import com.proctor.model.entity.AttemptAnswer;
import com.proctor.model.entity.Question;
import com.proctor.model.entity.Quiz;
import com.proctor.model.entity.Result;
import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.enums.AssessmentType;
import com.proctor.model.enums.AttemptStatus;
import com.proctor.model.enums.QuestionType;
import com.proctor.model.enums.Role;
import com.proctor.model.service.AuthService;
import com.proctor.model.service.ExamService;
import com.proctor.model.service.QuestionService;
import com.proctor.model.service.QuizService;
import com.proctor.model.service.SubjectService;
import com.proctor.util.KeyUtil;
import com.proctor.util.ListNavigationHelper;
import com.proctor.util.MouseUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.TeacherSubmissionViews;
import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class TeacherSubmissionScreen implements Screen {
    private final Quiz specificQuiz;
    private final ExamService examService;
    private final QuizService quizService;
    private final QuestionService questionService;
    private final SubjectService subjectService;
    private final AuthService authService;

    private List<Attempt> allSubmissions = new java.util.ArrayList<>();
    private List<Attempt> submissions = new java.util.ArrayList<>();
    private int selectedIndex = 0;
    private String bannerMessage = "";
    private final StringBuilder searchBuffer = new StringBuilder();
    private boolean searchMode = false;
    private int statusFilterIndex = 0;
    private static final String[] STATUS_FILTERS = {"ALL", "PENDING REVIEW", "GRADED"};
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private boolean isGrading = false;
    private int spinnerTick = 0;
    private long gradingStartTime = 0;
    private int activeGradingId = 0;
    private AtomicBoolean activeCancellation = null;

    private boolean inspectingAnswerSheet = false;
    private int inspectingAnswerIndex = 0;

    private boolean showManualGradingModal = false;
    private int manualGradingField = 0;
    private final StringBuilder manualPointsBuffer = new StringBuilder();
    private final StringBuilder manualFeedbackBuffer = new StringBuilder();
    private String manualGradingError = "";

    public record GradingTickMessage(int gradingId) implements Message {}
    public record AIGradingCompletedMessage(int gradingId, boolean success, String errorMessage) implements Message {
        public AIGradingCompletedMessage(boolean success, String errorMessage) {
            this(0, success, errorMessage);
        }
    }

    public TeacherSubmissionScreen(Quiz specificQuiz, ExamService examService, QuizService quizService, QuestionService questionService, SubjectService subjectService, AuthService authService) {
        this.specificQuiz = specificQuiz;
        this.examService = examService;
        this.quizService = quizService;
        this.questionService = questionService;
        this.subjectService = subjectService;
        this.authService = authService;
        refreshList();
    }

    private void refreshList() {
        if (specificQuiz != null) {
            this.allSubmissions = examService.getSubmissionsForQuiz(specificQuiz.getId());
        } else {
            User teacher = Session.getCurrentUser().orElse(null);
            Integer teacherId = (teacher != null && teacher.getRole() != Role.ADMIN) ? teacher.getId() : null;
            this.allSubmissions = examService.getAllSubmissions(teacherId);
        }
        applyFilters();
    }

    private void applyFilters() {
        String filter = STATUS_FILTERS[statusFilterIndex];
        String search = searchBuffer.toString().trim().toLowerCase();

        this.submissions = allSubmissions.stream().filter(a -> {
            if ("PENDING REVIEW".equals(filter)) {
                if (a.isGraded() || a.getStatus() == AttemptStatus.GRADED) {
                    return false;
                }
                if (a.getStatus() != AttemptStatus.AUTO_SUBMITTED && a.getStatus() != AttemptStatus.TURNED_IN) {
                    return false;
                }
            } else if ("GRADED".equals(filter)) {
                if (!a.isGraded() && a.getStatus() != AttemptStatus.GRADED) {
                    return false;
                }
            }
            if (!search.isEmpty()) {
                boolean matchStudent = a.getStudentName() != null && a.getStudentName().toLowerCase().contains(search);
                boolean matchQuiz = a.getQuizTitle() != null && a.getQuizTitle().toLowerCase().contains(search);
                boolean matchType = a.getAssessmentType() != null && a.getAssessmentType().name().toLowerCase().contains(search);
                if (!matchStudent && !matchQuiz && !matchType) {
                    return false;
                }
            }
            return true;
        }).toList();

        selectedIndex = ListNavigationHelper.clampIndex(selectedIndex, submissions.size());
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof GradingTickMessage t) {
            if (isGrading && t.gradingId() == this.activeGradingId) {
                spinnerTick++;
                return ScreenResult.stay(this, Command.tick(Duration.ofMillis(80), time -> new GradingTickMessage(this.activeGradingId)));
            }
            return ScreenResult.stay(this);
        }

        if (msg instanceof AIGradingCompletedMessage m) {
            if (!isGrading || (m.gradingId() != 0 && m.gradingId() != this.activeGradingId)) {
                return ScreenResult.stay(this);
            }
            isGrading = false;
            if (m.success()) {
                bannerMessage = TuiHelper.green("✔ Auto-graded with AI! Press [r] to return grade.");
                refreshList();
            } else {
                String err = (m.errorMessage() != null && !m.errorMessage().isBlank())
                        ? m.errorMessage()
                        : "AI grading failed. Please check Ollama.";
                bannerMessage = TuiHelper.red("✖ " + err);
            }
            return ScreenResult.stay(this);
        }

        if (isGrading) {
            if (msg instanceof KeyPressMessage k && KeyUtil.isEsc(k)) {
                isGrading = false;
                activeGradingId++;
                if (activeCancellation != null) {
                    activeCancellation.set(true);
                }
                bannerMessage = TuiHelper.yellow("AI grading cancelled.");
            } else if (MouseUtil.isLeftClick(msg)) {
                int line = MouseUtil.getLineIndex(msg);
                int col = MouseUtil.getColInLine(msg);
                int btnLine = MouseUtil.findButtonRowLine(view());
                String hintAction = MouseUtil.getClickedHintAction(view(), line, col);
                if ((btnLine != -1 && line >= btnLine && line <= btnLine + 2) || "Esc".equals(hintAction)) {
                    isGrading = false;
                    activeGradingId++;
                    if (activeCancellation != null) {
                        activeCancellation.set(true);
                    }
                    bannerMessage = TuiHelper.yellow("AI grading cancelled.");
                }
            }
            return ScreenResult.stay(this);
        }

        if (showManualGradingModal) {
            if (MouseUtil.isWheelUp(msg) || MouseUtil.isWheelDown(msg)) {
                return ScreenResult.stay(this);
            }
            if (msg instanceof KeyPressMessage k) {
                if (KeyUtil.isEsc(k)) {
                    showManualGradingModal = false;
                    manualGradingError = "";
                    return ScreenResult.stay(this);
                }
                if (KeyUtil.isTab(k)) {
                    manualGradingField = (manualGradingField + 1) % 4;
                    return ScreenResult.stay(this);
                }
                if (KeyUtil.isDown(k)) {
                    if (manualGradingField == 0) manualGradingField = 1;
                    else if (manualGradingField == 1) manualGradingField = 2;
                    return ScreenResult.stay(this);
                }
                if (KeyUtil.isUp(k)) {
                    if (manualGradingField == 2 || manualGradingField == 3) manualGradingField = 1;
                    else if (manualGradingField == 1) manualGradingField = 0;
                    return ScreenResult.stay(this);
                }
                if (manualGradingField == 2 || manualGradingField == 3) {
                    if (KeyUtil.isLeft(k) || KeyUtil.isRight(k)) {
                        manualGradingField = (manualGradingField == 2) ? 3 : 2;
                        return ScreenResult.stay(this);
                    }
                }
                if (KeyUtil.isEnter(k)) {
                    if (manualGradingField == 0) {
                        manualGradingField = 1;
                        return ScreenResult.stay(this);
                    } else if (manualGradingField == 1 || manualGradingField == 2) {
                        return executeSaveManualGrade();
                    } else if (manualGradingField == 3) {
                        showManualGradingModal = false;
                        manualGradingError = "";
                        return ScreenResult.stay(this);
                    }
                }
                if (manualGradingField == 0) {
                    if (KeyUtil.handleBackspace(manualPointsBuffer, k)) {
                        manualGradingError = "";
                        return ScreenResult.stay(this);
                    }
                    if (k.runes() != null) {
                        for (char c : k.runes()) {
                            if ((Character.isDigit(c) || c == '.') && manualPointsBuffer.length() < 6) {
                                manualPointsBuffer.append(c);
                                manualGradingError = "";
                            }
                        }
                    }
                    return ScreenResult.stay(this);
                }
                if (manualGradingField == 1) {
                    if (KeyUtil.handleBackspace(manualFeedbackBuffer, k)) {
                        manualGradingError = "";
                        return ScreenResult.stay(this);
                    }
                    if (KeyUtil.appendInput(manualFeedbackBuffer, k, 500)) {
                        manualGradingError = "";
                    }
                    return ScreenResult.stay(this);
                }
                return ScreenResult.stay(this);
            }
            if (MouseUtil.isLeftClick(msg)) {
                int line = MouseUtil.getLineIndex(msg);
                int col = MouseUtil.getColInLine(msg);
                int btnLine = MouseUtil.findButtonRowLine(view());
                if (btnLine != -1 && line >= btnLine && line <= btnLine + 2) {
                    int btn = MouseUtil.getClickedButtonIndex(col, "Save Grade", "Cancel");
                    if (btn == 0) {
                        return executeSaveManualGrade();
                    } else if (btn == 1) {
                        showManualGradingModal = false;
                        manualGradingError = "";
                        return ScreenResult.stay(this);
                    }
                }
                String hintAction = MouseUtil.getClickedHintAction(view(), line, col);
                if ("Esc".equals(hintAction)) {
                    showManualGradingModal = false;
                    manualGradingError = "";
                    return ScreenResult.stay(this);
                }
                String[] lines = view().split("\n", -1);
                if (line >= 0 && line < lines.length) {
                    for (int i = 0; i <= line && i < lines.length; i++) {
                        if (lines[i].contains("Points Awarded") && line >= i && line <= i + 3) {
                            manualGradingField = 0;
                            break;
                        }
                        if (lines[i].contains("Teacher Feedback") && line >= i && line <= i + 3) {
                            manualGradingField = 1;
                            break;
                        }
                    }
                }
                return ScreenResult.stay(this);
            }
            return ScreenResult.stay(this);
        }

        if (inspectingAnswerSheet) {
            int qCount = 0;
            if (selectedIndex >= 0 && selectedIndex < submissions.size()) {
                Attempt att = submissions.get(selectedIndex);
                Quiz qz = getEffectiveQuiz(att);
                if (qz != null && qz.getQuestions() != null) {
                    qCount = qz.getQuestions().size();
                }
            }

            if (msg instanceof KeyPressMessage k) {
                if (KeyUtil.isEsc(k)) {
                    inspectingAnswerSheet = false;
                    bannerMessage = "";
                    return ScreenResult.stay(this);
                }
                if (KeyUtil.isLeft(k)) {
                    inspectingAnswerIndex = ListNavigationHelper.prevPage(inspectingAnswerIndex, 1);
                    return ScreenResult.stay(this);
                }
                if (KeyUtil.isRight(k)) {
                    inspectingAnswerIndex = ListNavigationHelper.nextPage(inspectingAnswerIndex, qCount, 1);
                    return ScreenResult.stay(this);
                }
                if ("e".equalsIgnoreCase(k.key())) {
                    return openManualGradingModal();
                }
                if ("g".equalsIgnoreCase(k.key())) {
                    return startAsyncGrading();
                }
                if ("r".equalsIgnoreCase(k.key())) {
                    executeReturnGrade();
                    return ScreenResult.stay(this);
                }
            }
            if (MouseUtil.isWheelUp(msg) || MouseUtil.isWheelDown(msg)) {
                inspectingAnswerIndex = ListNavigationHelper.handleWheel(msg, inspectingAnswerIndex, qCount);
                return ScreenResult.stay(this);
            }
            if (MouseUtil.isLeftClick(msg)) {
                int line = MouseUtil.getLineIndex(msg);
                int col = MouseUtil.getColInLine(msg);
                int pagLine = MouseUtil.findPaginationLine(view());
                if (pagLine != -1 && line == pagLine && qCount > 1) {
                    inspectingAnswerIndex = ListNavigationHelper.handlePaginationClick(col, inspectingAnswerIndex, qCount, 1);
                    return ScreenResult.stay(this);
                }
                int btnLine = MouseUtil.findButtonRowLine(view());
                if (btnLine != -1 && line >= btnLine && line <= btnLine + 2) {
                    inspectingAnswerSheet = false;
                    bannerMessage = "";
                    return ScreenResult.stay(this);
                }
                String hintAction = MouseUtil.getClickedHintAction(view(), line, col);
                if (hintAction != null) {
                    if ("Esc".equals(hintAction)) {
                        inspectingAnswerSheet = false;
                        bannerMessage = "";
                        return ScreenResult.stay(this);
                    } else if ("e".equals(hintAction)) {
                        return openManualGradingModal();
                    } else if ("g".equals(hintAction)) {
                        return startAsyncGrading();
                    } else if ("r".equals(hintAction)) {
                        executeReturnGrade();
                        return ScreenResult.stay(this);
                    }
                }
            }
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isWheelUp(msg) || MouseUtil.isWheelDown(msg)) {
            selectedIndex = ListNavigationHelper.handleWheel(msg, selectedIndex, submissions.size());
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isLeftClick(msg)) {
            int line = MouseUtil.getLineIndex(msg);
            int col = MouseUtil.getColInLine(msg);

            int tabLine = MouseUtil.findTabBarLine(view());
            if (tabLine != -1 && line == tabLine) {
                int clickedTab = MouseUtil.getClickedTabIndex(col, "All", "Pending Review", "Graded");
                if (clickedTab >= 0 && clickedTab < STATUS_FILTERS.length && clickedTab != statusFilterIndex) {
                    statusFilterIndex = clickedTab;
                    selectedIndex = 0;
                    applyFilters();
                }
                return ScreenResult.stay(this);
            }

            int clickedIdx = ListNavigationHelper.getClickedItemIndex(line, MouseUtil.findTableStartLine(view()), submissions.size(), selectedIndex, TuiHelper.PAGE_SIZE);
            if (clickedIdx != -1) {
                if (selectedIndex == clickedIdx) {
                    return openSubmissionDetail();
                }
                selectedIndex = clickedIdx;
                return ScreenResult.stay(this);
            }

            int pagLine = MouseUtil.findPaginationLine(view());
            if (pagLine != -1 && line == pagLine && !submissions.isEmpty()) {
                selectedIndex = ListNavigationHelper.handlePaginationClick(col, selectedIndex, submissions.size(), TuiHelper.PAGE_SIZE);
                return ScreenResult.stay(this);
            }

            String hintAction = MouseUtil.getClickedHintAction(view(), line, col);
            if (hintAction != null) {
                if ("Esc".equals(hintAction)) {
                    return navigateBack();
                } else if ("Enter".equals(hintAction) || "e".equals(hintAction)) {
                    return openSubmissionDetail();
                } else if ("g".equals(hintAction)) {
                    return startAsyncGrading();
                } else if ("r".equals(hintAction)) {
                    executeReturnGrade();
                    return ScreenResult.stay(this);
                }
            }

            return ScreenResult.stay(this);
        }

        if (msg instanceof KeyPressMessage k) {
            if (searchMode) {
                searchMode = ListNavigationHelper.handleSearchKey(k, searchBuffer, this::applyFilters);
                return ScreenResult.stay(this);
            }

            bannerMessage = "";

            if (KeyUtil.isEsc(k)) {
                return navigateBack();
            }

            if (KeyUtil.isUp(k)) {
                selectedIndex = ListNavigationHelper.adjustIndex(selectedIndex, -1, submissions.size());
            } else if (KeyUtil.isDown(k)) {
                selectedIndex = ListNavigationHelper.adjustIndex(selectedIndex, 1, submissions.size());
            } else if (KeyUtil.isLeft(k)) {
                selectedIndex = ListNavigationHelper.prevPage(selectedIndex, TuiHelper.PAGE_SIZE);
            } else if (KeyUtil.isRight(k)) {
                selectedIndex = ListNavigationHelper.nextPage(selectedIndex, submissions.size(), TuiHelper.PAGE_SIZE);
            } else if (KeyUtil.isTab(k)) {
                statusFilterIndex = (statusFilterIndex + 1) % STATUS_FILTERS.length;
                selectedIndex = 0;
                applyFilters();
            } else if ("/".equals(k.key())) {
                searchMode = true;
                searchBuffer.setLength(0);
                applyFilters();
            } else if (KeyUtil.isEnter(k) || "e".equalsIgnoreCase(k.key())) {
                return openSubmissionDetail();
            } else if ("g".equalsIgnoreCase(k.key())) {
                return startAsyncGrading();
            } else if ("r".equalsIgnoreCase(k.key())) {
                executeReturnGrade();
            }
        }
        return ScreenResult.stay(this);
    }

    private ScreenResult openSubmissionDetail() {
        if (!submissions.isEmpty() && selectedIndex < submissions.size()) {
            inspectingAnswerSheet = true;
            inspectingAnswerIndex = 0;
            bannerMessage = "";
        }
        return ScreenResult.stay(this);
    }

    private ScreenResult navigateBack() {
        if (specificQuiz != null) {
            return ScreenResult.navigate(new QuizListScreen(quizService, questionService, subjectService, authService, specificQuiz.getAssessmentType()));
        }
        User user = Session.getCurrentUser().orElse(null);
        if (user != null && user.getRole() == Role.ADMIN) {
            return ScreenResult.navigate(new AdminDashboardScreen(authService));
        }
        return ScreenResult.navigate(new TeacherDashboardScreen(authService, questionService, subjectService, quizService));
    }

    private ScreenResult startAsyncGrading() {
        if (submissions.isEmpty() || selectedIndex < 0 || selectedIndex >= submissions.size()) {
            return ScreenResult.stay(this);
        }
        Attempt att = submissions.get(selectedIndex);
        if (att.getAssessmentType() == AssessmentType.SPEED) {
            bannerMessage = TuiHelper.yellow("● Speed Quizzes are objective only. AI grading is not applicable.");
            return ScreenResult.stay(this);
        }
        if (att.getStatus() == AttemptStatus.GRADED || att.isGraded()) {
            bannerMessage = TuiHelper.yellow("● This submission is already graded. AI grading cannot be re-run.");
            return ScreenResult.stay(this);
        }
        if (att.getStatus() == AttemptStatus.IN_PROGRESS) {
            bannerMessage = TuiHelper.yellow("● Cannot grade an assessment that is still in progress by the student.");
            return ScreenResult.stay(this);
        }
        isGrading = true;
        spinnerTick = 0;
        gradingStartTime = System.currentTimeMillis();
        final int gId = ++activeGradingId;
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        this.activeCancellation = cancelled;
        bannerMessage = "";

        Command gradeCmd = () -> {
            try {
                boolean ok = examService.gradeWithAI(att.getId());
                if (cancelled.get()) {
                    return new AIGradingCompletedMessage(gId, false, "Cancelled");
                }
                return new AIGradingCompletedMessage(gId, ok, ok ? null : "AI grading failed. Please check Ollama.");
            } catch (Exception e) {
                if (cancelled.get()) {
                    return new AIGradingCompletedMessage(gId, false, "Cancelled");
                }
                String err = e.getMessage();
                if (err == null || err.isBlank()) {
                    Throwable cause = e.getCause();
                    if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
                        err = cause.getMessage();
                    } else {
                        err = "Failed to grade with AI: " + e.getClass().getSimpleName();
                    }
                }
                return new AIGradingCompletedMessage(gId, false, err);
            }
        };

        Command tickCmd = Command.tick(Duration.ofMillis(80), time -> new GradingTickMessage(gId));
        return ScreenResult.stay(this, Command.batch(gradeCmd, tickCmd));
    }

    private void executeReturnGrade() {
        if (submissions.isEmpty() || selectedIndex >= submissions.size()) {
            return;
        }
        Attempt att = submissions.get(selectedIndex);
        if (att.getAssessmentType() == AssessmentType.SPEED) {
            bannerMessage = TuiHelper.yellow("● Speed Quizzes are auto-scored objective assessments. Grade is already finalized.");
            return;
        }
        if (att.getStatus() == AttemptStatus.GRADED || att.isGraded()) {
            bannerMessage = TuiHelper.yellow("● Grade has already been returned for this submission.");
            return;
        }
        if (att.getStatus() == AttemptStatus.IN_PROGRESS) {
            bannerMessage = TuiHelper.yellow("● Cannot return grade for an assessment that is still in progress.");
            return;
        }

        Quiz quiz = getEffectiveQuiz(att);
        if (quiz != null && quiz.getQuestions() != null) {
            List<AttemptAnswer> answers = examService.getAttemptAnswers(att.getId());
            Map<Integer, AttemptAnswer> ansMap = new HashMap<>();
            for (AttemptAnswer a : answers) {
                ansMap.put(a.getQuestionId(), a);
            }
            boolean hasUnevaluatedShortAnswer = false;
            for (Question q : quiz.getQuestions()) {
                if (q.getQuestionType() == QuestionType.SHORT_ANSWER) {
                    AttemptAnswer ans = ansMap.get(q.getId());
                    if (ans != null && ans.getTextAnswer() != null && !ans.getTextAnswer().isBlank()) {
                        if (ans.getCorrect() == null && ans.getAiScore() == null && ans.getTeacherFeedback() == null && ans.getPointsAwarded() == 0.0) {
                            hasUnevaluatedShortAnswer = true;
                            break;
                        }
                    }
                }
            }
            if (hasUnevaluatedShortAnswer) {
                bannerMessage = TuiHelper.yellow("● Written answers have not been evaluated. Press [Enter] or [g] to grade with AI first.");
                return;
            }
        }

        try {
            Result res = examService.returnGrade(att.getId());
            bannerMessage = TuiHelper.green(String.format("✔ Grade returned: %.1f/%.1f points (%.1f%%) - %s",
                    res.getTotalPoints(), res.getMaxPoints(), res.getPercentage(),
                    res.isPassed() ? "PASSED" : "FAILED"));
            inspectingAnswerSheet = false;
            inspectingAnswerIndex = 0;
            refreshList();
        } catch (ValidationException e) {
            bannerMessage = TuiHelper.red("✖ " + e.getMessage());
        }
    }

    private ScreenResult openManualGradingModal() {
        if (submissions.isEmpty() || selectedIndex < 0 || selectedIndex >= submissions.size()) {
            return ScreenResult.stay(this);
        }
        Attempt att = submissions.get(selectedIndex);
        if (att.isGraded() || att.getStatus() == AttemptStatus.GRADED) {
            bannerMessage = TuiHelper.yellow("● This submission is already graded. Grades cannot be modified.");
            return ScreenResult.stay(this);
        }
        Quiz qz = getEffectiveQuiz(att);
        if (qz != null && qz.getAssessmentType() == AssessmentType.SPEED) {
            bannerMessage = TuiHelper.yellow("● Speed Quizzes are auto-scored objective assessments.");
            return ScreenResult.stay(this);
        }
        List<Question> questions = (qz != null && qz.getQuestions() != null) ? qz.getQuestions() : List.of();
        if (questions.isEmpty() || inspectingAnswerIndex >= questions.size()) {
            bannerMessage = TuiHelper.red("✖ Question not found for manual grading.");
            return ScreenResult.stay(this);
        }

        Question q = questions.get(inspectingAnswerIndex);
        List<AttemptAnswer> answers = examService.getAttemptAnswers(att.getId());
        AttemptAnswer ans = null;
        for (AttemptAnswer a : answers) {
            if (java.util.Objects.equals(a.getQuestionId(), q.getId())) {
                ans = a;
                break;
            }
        }

        manualPointsBuffer.setLength(0);
        if (ans != null && ans.getPointsAwarded() > 0.0) {
            if (ans.getPointsAwarded() == Math.floor(ans.getPointsAwarded())) {
                manualPointsBuffer.append(String.format(java.util.Locale.US, "%.0f", ans.getPointsAwarded()));
            } else {
                manualPointsBuffer.append(String.format(java.util.Locale.US, "%.1f", ans.getPointsAwarded()));
            }
        } else {
            manualPointsBuffer.append("0");
        }

        manualFeedbackBuffer.setLength(0);
        if (ans != null && ans.getTeacherFeedback() != null) {
            manualFeedbackBuffer.append(ans.getTeacherFeedback());
        }

        manualGradingField = 0;
        manualGradingError = "";
        showManualGradingModal = true;
        return ScreenResult.stay(this);
    }

    private ScreenResult executeSaveManualGrade() {
        if (submissions.isEmpty() || selectedIndex < 0 || selectedIndex >= submissions.size()) {
            showManualGradingModal = false;
            return ScreenResult.stay(this);
        }
        Attempt att = submissions.get(selectedIndex);
        Quiz qz = getEffectiveQuiz(att);
        List<Question> questions = (qz != null && qz.getQuestions() != null) ? qz.getQuestions() : List.of();
        if (questions.isEmpty() || inspectingAnswerIndex >= questions.size()) {
            showManualGradingModal = false;
            return ScreenResult.stay(this);
        }
        Question q = questions.get(inspectingAnswerIndex);

        String ptsStr = manualPointsBuffer.toString().trim();
        if (ptsStr.isEmpty()) {
            manualGradingError = "Please enter points awarded.";
            return ScreenResult.stay(this);
        }

        double pts;
        try {
            pts = Double.parseDouble(ptsStr);
        } catch (NumberFormatException e) {
            manualGradingError = "Invalid points format. Enter a valid number.";
            return ScreenResult.stay(this);
        }

        try {
            examService.gradeAnswerManually(att.getId(), q.getId(), pts, manualFeedbackBuffer.toString());
            showManualGradingModal = false;
            manualGradingError = "";
            bannerMessage = TuiHelper.green(String.format(java.util.Locale.US, "✔ Score updated: %.1f/%.1f pts.", pts, q.getPoints()));
            refreshList();
        } catch (ValidationException e) {
            manualGradingError = e.getMessage();
        }
        return ScreenResult.stay(this);
    }

    private Quiz getEffectiveQuiz(Attempt attempt) {
        Quiz currentQuiz = specificQuiz;
        if (currentQuiz == null && attempt != null) {
            currentQuiz = quizService.getQuizById(attempt.getQuizId()).orElse(null);
        } else if (currentQuiz != null && (currentQuiz.getQuestions() == null || currentQuiz.getQuestions().isEmpty())) {
            currentQuiz = quizService.getQuizById(currentQuiz.getId()).orElse(currentQuiz);
        }
        return currentQuiz;
    }

    @Override
    public String view() {
        if (isGrading) {
            Attempt attempt = (!submissions.isEmpty() && selectedIndex < submissions.size()) ? submissions.get(selectedIndex) : null;
            String studentName = (attempt != null && attempt.getStudentName() != null && !attempt.getStudentName().isBlank())
                    ? attempt.getStudentName() : (attempt != null ? "Student #" + attempt.getStudentId() : null);
            String quizTitle = (specificQuiz != null) ? specificQuiz.getTitle()
                    : (attempt != null && attempt.getQuizTitle() != null ? attempt.getQuizTitle() : (attempt != null ? "Quiz #" + attempt.getQuizId() : null));
            int elapsedSeconds = (int) Math.max(0, (System.currentTimeMillis() - gradingStartTime) / 1000);
            return TeacherSubmissionViews.renderAIGradingLoading(studentName, quizTitle, spinnerTick, elapsedSeconds);
        }
        if (showManualGradingModal && !submissions.isEmpty() && selectedIndex < submissions.size()) {
            Attempt attempt = submissions.get(selectedIndex);
            Quiz currentQuiz = getEffectiveQuiz(attempt);
            List<Question> questions = (currentQuiz != null && currentQuiz.getQuestions() != null) ? currentQuiz.getQuestions() : List.of();
            Question q = (!questions.isEmpty() && inspectingAnswerIndex < questions.size()) ? questions.get(inspectingAnswerIndex) : null;
            List<AttemptAnswer> answers = examService.getAttemptAnswers(attempt.getId());
            AttemptAnswer ans = null;
            if (q != null) {
                for (AttemptAnswer a : answers) {
                    if (java.util.Objects.equals(a.getQuestionId(), q.getId())) {
                        ans = a;
                        break;
                    }
                }
            }
            if (q != null) {
                return TeacherSubmissionViews.renderManualGradingModal(
                        q, ans, inspectingAnswerIndex, questions.size(),
                        manualPointsBuffer.toString(), manualFeedbackBuffer.toString(),
                        manualGradingField, manualGradingError
                );
            }
        }
        if (inspectingAnswerSheet && !submissions.isEmpty() && selectedIndex < submissions.size()) {
            Attempt attempt = submissions.get(selectedIndex);
            Quiz currentQuiz = getEffectiveQuiz(attempt);
            List<AttemptAnswer> answers = examService.getAttemptAnswers(attempt.getId());
            Map<Integer, AttemptAnswer> answerMap = new HashMap<>();
            for (AttemptAnswer a : answers) {
                answerMap.put(a.getQuestionId(), a);
            }
            return TeacherSubmissionViews.renderAnswerSheet(currentQuiz, attempt, answerMap, inspectingAnswerIndex, bannerMessage);
        }
        return TeacherSubmissionViews.renderSubmissionList(specificQuiz, submissions, Collections.emptyMap(),
                selectedIndex, dateFormat, STATUS_FILTERS[statusFilterIndex], searchBuffer.toString(), searchMode, bannerMessage);
    }
}