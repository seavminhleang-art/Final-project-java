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
            }
            return ScreenResult.stay(this);
        }

        if (inspectingAnswerSheet) {
            int qCount = 0;
            if (selectedIndex >= 0 && selectedIndex < submissions.size()) {
                Attempt att = submissions.get(selectedIndex);
                Quiz qz = specificQuiz;
                if (qz == null || qz.getQuestions() == null || qz.getQuestions().isEmpty()) {
                    qz = quizService.getQuizById(att.getQuizId()).orElse(qz);
                }
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
                } else if ("Enter".equals(hintAction)) {
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
            } else if (KeyUtil.isEnter(k)) {
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
        if (submissions.isEmpty() || selectedIndex >= submissions.size()) {
            return ScreenResult.stay(this);
        }
        Attempt att = submissions.get(selectedIndex);
        if (att.getAssessmentType() == AssessmentType.SPEED) {
            bannerMessage = TuiHelper.yellow("● Speed Quizzes are objective (MCQ / T&F) only. AI grading is not applicable.");
            return ScreenResult.stay(this);
        }
        if (att.getStatus() == AttemptStatus.GRADED) {
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

        Quiz quiz = specificQuiz;
        if (quiz == null) {
            quiz = quizService.getQuizById(att.getQuizId()).orElse(null);
        } else if (quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
            quiz = quizService.getQuizById(quiz.getId()).orElse(quiz);
        }
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
                        if (ans.getAiScore() == null && ans.getTeacherFeedback() == null && ans.getPointsAwarded() == 0.0) {
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
        if (inspectingAnswerSheet && !submissions.isEmpty() && selectedIndex < submissions.size()) {
            Attempt attempt = submissions.get(selectedIndex);
            Quiz currentQuiz = specificQuiz;
            if (currentQuiz == null) {
                currentQuiz = quizService.getQuizById(attempt.getQuizId()).orElse(null);
            } else if (currentQuiz.getQuestions() == null || currentQuiz.getQuestions().isEmpty()) {
                currentQuiz = quizService.getQuizById(currentQuiz.getId()).orElse(currentQuiz);
            }
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