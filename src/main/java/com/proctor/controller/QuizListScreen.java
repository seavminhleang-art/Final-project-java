package com.proctor.controller;

import com.proctor.model.service.AIService;
import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.enums.AssessmentType;
import com.proctor.model.enums.Role;
import com.proctor.exception.ValidationException;
import com.proctor.model.repository.AttemptRepository;
import com.proctor.model.repository.ResultRepository;
import com.proctor.model.service.ExamService;
import com.proctor.model.service.QuestionService;
import com.proctor.model.entity.Subject;
import com.proctor.model.entity.Quiz;
import com.proctor.model.repository.QuizRepository;
import com.proctor.model.service.QuizService;
import com.proctor.model.service.SubjectService;
import com.proctor.util.KeyUtil;
import com.proctor.util.ListNavigationHelper;
import com.proctor.util.MouseUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.QuizViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

import java.util.List;

public class QuizListScreen implements Screen {
    public enum QuizScope {
        MY_QUIZZES,
        ALL_GLOBAL
    }

    private final QuizService quizService;
    private final QuestionService questionService;
    private final SubjectService subjectService;
    private final AuthService authService;
    private final AssessmentType assessmentType;

    private List<Subject> allSubjects;
    private int subjectFilterIndex = 0;
    private final InlineSubjectFilter<Subject> subjectFilter;

    private QuizScope currentScope = QuizScope.MY_QUIZZES;
    private List<Quiz> quizzes = new java.util.ArrayList<>();
    private int selectedIndex = 0;
    private final StringBuilder searchBuffer = new StringBuilder();
    private boolean searchMode = false;
    private String bannerMessage = "";

    private boolean confirmingDelete = false;
    private boolean confirmDeleteFocused = false;
    private Quiz pendingDeleteQuiz = null;

    public QuizListScreen(QuizService quizService, QuestionService questionService, SubjectService subjectService, AuthService authService) {
        this(quizService, questionService, subjectService, authService, AssessmentType.QUIZ);
    }

    public QuizListScreen(QuizService quizService, QuestionService questionService, SubjectService subjectService, AuthService authService, AssessmentType assessmentType) {
        this.quizService = quizService;
        this.questionService = questionService;
        this.subjectService = subjectService;
        this.authService = authService;
        this.assessmentType = assessmentType != null ? assessmentType : AssessmentType.QUIZ;
        this.allSubjects = subjectService != null ? subjectService.getSubjects("") : List.of();
        User user = Session.getCurrentUser().orElse(null);
        if (user != null && user.getRole() == Role.ADMIN) {
            this.currentScope = QuizScope.ALL_GLOBAL;
        }

        List<InlineSubjectFilter.Item<Subject>> items = new java.util.ArrayList<>();
        items.add(new InlineSubjectFilter.Item<>(null, "ALL", "All Subjects"));
        for (Subject s : this.allSubjects) {
            items.add(new InlineSubjectFilter.Item<>(s, s.getCode(), s.getName()));
        }
        this.subjectFilter = new InlineSubjectFilter<>(items);

        refreshList();
    }

    private void refreshList() {
        User user = Session.getCurrentUser().orElse(null);
        boolean isAdmin = user != null && user.getRole() == Role.ADMIN;
        Integer teacherId = (currentScope == QuizScope.MY_QUIZZES && user != null) ? user.getId() : null;
        Integer subjectId = (subjectFilterIndex > 0 && subjectFilterIndex <= allSubjects.size())
                ? allSubjects.get(subjectFilterIndex - 1).getId() : null;

        if (teacherId != null) {
            this.quizzes = quizService.getAssessments(assessmentType, subjectId, teacherId, null, searchBuffer.toString());
        } else if (isAdmin) {
            this.quizzes = quizService.getAssessments(assessmentType, subjectId, null, searchBuffer.toString());
        } else {
            Integer currentUserId = (user != null) ? user.getId() : null;
            this.quizzes = quizService.getAssessmentsVisibleTo(assessmentType, subjectId, currentUserId, searchBuffer.toString());
        }

        selectedIndex = ListNavigationHelper.clampIndex(selectedIndex, quizzes.size());
    }

    @Override
    public ScreenResult update(Message msg) {
        if (MouseUtil.isWheelUp(msg) || MouseUtil.isWheelDown(msg)) {
            selectedIndex = ListNavigationHelper.handleWheel(msg, selectedIndex, quizzes.size());
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isLeftClick(msg)) {
            if (confirmingDelete) {
                int action = ListNavigationHelper.handleConfirmationClick(msg, view(), "Delete", "Cancel");
                if (action == 0) {
                    executeDelete();
                } else if (action == 1) {
                    bannerMessage = TuiHelper.yellow("Deletion cancelled.");
                }
                if (action >= 0) {
                    confirmingDelete = false;
                    pendingDeleteQuiz = null;
                }
                return ScreenResult.stay(this);
            }

            int line = MouseUtil.getLineIndex(msg);
            int col = MouseUtil.getColInLine(msg);

            int tabLine = MouseUtil.findTabBarLine(view());
            if (tabLine != -1 && line == tabLine) {
                String baseItemTitle = (assessmentType == AssessmentType.EXAM) ? "Exams" : (assessmentType == AssessmentType.SPEED ? "Speed Quizzes" : "Quizzes");
                String tabMy = "My " + baseItemTitle;
                String tabAll = "All " + baseItemTitle;
                int clickedTab = MouseUtil.getClickedTabIndex(col, tabMy, tabAll);
                if (clickedTab >= 0) {
                    QuizScope targetScope = (clickedTab == 0) ? QuizScope.MY_QUIZZES : QuizScope.ALL_GLOBAL;
                    if (targetScope != currentScope) {
                        currentScope = targetScope;
                        selectedIndex = 0;
                        bannerMessage = (currentScope == QuizScope.MY_QUIZZES)
                                ? TuiHelper.cyan("Switched scope to: MY " + baseItemTitle.toUpperCase())
                                : TuiHelper.cyan("Switched scope to: ALL GLOBAL " + baseItemTitle.toUpperCase());
                        refreshList();
                    }
                }
                return ScreenResult.stay(this);
            }

            int clickedIdx = ListNavigationHelper.getClickedItemIndex(line, MouseUtil.findTableStartLine(view()), quizzes.size(), selectedIndex, TuiHelper.PAGE_SIZE);
            if (clickedIdx != -1) {
                if (selectedIndex == clickedIdx) {
                    return ScreenResult.navigate(new QuizQuestionEditorScreen(quizzes.get(selectedIndex), quizService, questionService, subjectService, authService));
                }
                selectedIndex = clickedIdx;
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
                    User user = Session.getCurrentUser().orElse(null);
                    boolean isAdmin = user != null && user.getRole() == Role.ADMIN;
                    if (isAdmin) {
                        return ScreenResult.navigate(new AdminDashboardScreen(authService));
                    }
                    return ScreenResult.navigate(new TeacherDashboardScreen(authService, questionService, subjectService, quizService));
                } else if ("n".equals(hintAction)) {
                    User user = Session.getCurrentUser().orElse(null);
                    if (user == null || user.getRole() != Role.ADMIN) {
                        if (assessmentType == AssessmentType.SPEED) {
                            return ScreenResult.navigate(new SpeedQuizFormScreen(quizService, questionService, subjectService, authService, null));
                        }
                        return ScreenResult.navigate(new QuizFormScreen(quizService, questionService, subjectService, authService, null, assessmentType));
                    }
                } else if ("g".equals(hintAction)) {
                    User user = Session.getCurrentUser().orElse(null);
                    if (user == null || user.getRole() != Role.ADMIN) {
                        return ScreenResult.navigate(new AIQuizGeneratorScreen(new AIService(), quizService, questionService, subjectService, authService, assessmentType));
                    }
                } else if ("d".equals(hintAction) && !quizzes.isEmpty() && selectedIndex < quizzes.size()) {
                    Quiz q = quizzes.get(selectedIndex);
                    if (canModify(q)) {
                        pendingDeleteQuiz = q;
                        confirmingDelete = true;
                        confirmDeleteFocused = false;
                        return ScreenResult.stay(this);
                    } else {
                        bannerMessage = TuiHelper.red("✖ You can only delete assessments you created.");
                        return ScreenResult.stay(this);
                    }
                } else if ("e".equals(hintAction) && !quizzes.isEmpty() && selectedIndex < quizzes.size()) {
                    Quiz q = quizzes.get(selectedIndex);
                    if (canModify(q)) {
                        if (assessmentType == AssessmentType.SPEED) {
                            return ScreenResult.navigate(new SpeedQuizFormScreen(quizService, questionService, subjectService, authService, q));
                        }
                        return ScreenResult.navigate(new QuizFormScreen(quizService, questionService, subjectService, authService, q, assessmentType));
                    } else {
                        bannerMessage = TuiHelper.red("✖ You can only edit assessments you created.");
                        return ScreenResult.stay(this);
                    }
                } else if (("Enter".equals(hintAction) || "q".equals(hintAction)) && !quizzes.isEmpty() && selectedIndex < quizzes.size()) {
                    return ScreenResult.navigate(new QuizQuestionEditorScreen(quizzes.get(selectedIndex), quizService, questionService, subjectService, authService));
                } else if ("r".equals(hintAction) && !quizzes.isEmpty() && selectedIndex < quizzes.size()) {
                    ExamService examService = new ExamService(new QuizRepository(), new AttemptRepository(), new ResultRepository());
                    return ScreenResult.navigate(new TeacherSubmissionScreen(quizzes.get(selectedIndex), examService, quizService, questionService, subjectService, authService));
                } else if ("Space".equals(hintAction) && !quizzes.isEmpty() && selectedIndex < quizzes.size()) {
                    Quiz q = quizzes.get(selectedIndex);
                    if (canModify(q)) {
                        togglePublishSelectedQuiz();
                    } else {
                        bannerMessage = TuiHelper.red("✖ You can only publish/unpublish quizzes you created.");
                    }
                    return ScreenResult.stay(this);
                } else if ("Tab".equals(hintAction)) {
                    User user = Session.getCurrentUser().orElse(null);
                    if (user == null || user.getRole() != Role.ADMIN) {
                        toggleScope();
                    }
                    return ScreenResult.stay(this);
                } else if ("/".equals(hintAction)) {
                    searchMode = true;
                    bannerMessage = "";
                    return ScreenResult.stay(this);
                }
            }

            return ScreenResult.stay(this);
        }

        if (msg instanceof KeyPressMessage k) {
            if (subjectFilter.isActive()) {
                boolean handled = subjectFilter.handleKey(k);
                if (handled) {
                    subjectFilterIndex = subjectFilter.getSelectedOriginalIndex();
                    selectedIndex = 0;
                    refreshList();
                    return ScreenResult.stay(this);
                }
            }

            if (confirmingDelete) {
                if (KeyUtil.isLeft(k) || KeyUtil.isRight(k)) {
                    confirmDeleteFocused = !confirmDeleteFocused;
                    return ScreenResult.stay(this);
                } else if (KeyUtil.isEnter(k)) {
                    if (confirmDeleteFocused) {
                        executeDelete();
                    } else {
                        bannerMessage = TuiHelper.yellow("Deletion cancelled.");
                    }
                    confirmingDelete = false;
                    pendingDeleteQuiz = null;
                    return ScreenResult.stay(this);
                } else if (KeyUtil.isEsc(k)) {
                    confirmingDelete = false;
                    pendingDeleteQuiz = null;
                    bannerMessage = TuiHelper.yellow("Deletion cancelled.");
                    return ScreenResult.stay(this);
                }
                return ScreenResult.stay(this);
            }

            if (searchMode) {
                searchMode = ListNavigationHelper.handleSearchKey(k, searchBuffer, this::refreshList);
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isEsc(k)) {
                User user = Session.getCurrentUser().orElse(null);
                boolean isAdmin = user != null && user.getRole() == Role.ADMIN;
                if (isAdmin) {
                    return ScreenResult.navigate(new AdminDashboardScreen(authService));
                }
                return ScreenResult.navigate(new TeacherDashboardScreen(authService, questionService, subjectService, quizService));
            } else if (KeyUtil.isUp(k)) {
                selectedIndex = ListNavigationHelper.adjustIndex(selectedIndex, -1, quizzes.size());
            } else if (KeyUtil.isDown(k)) {
                selectedIndex = ListNavigationHelper.adjustIndex(selectedIndex, 1, quizzes.size());
            } else if (KeyUtil.isLeft(k)) {
                selectedIndex = ListNavigationHelper.prevPage(selectedIndex, TuiHelper.PAGE_SIZE);
            } else if (KeyUtil.isRight(k)) {
                selectedIndex = ListNavigationHelper.nextPage(selectedIndex, quizzes.size(), TuiHelper.PAGE_SIZE);
            } else if ("n".equalsIgnoreCase(k.key())) {
                User user = Session.getCurrentUser().orElse(null);
                if (user == null || user.getRole() != Role.ADMIN) {
                    if (assessmentType == AssessmentType.SPEED) {
                        return ScreenResult.navigate(new SpeedQuizFormScreen(quizService, questionService, subjectService, authService, null));
                    }
                    return ScreenResult.navigate(new QuizFormScreen(quizService, questionService, subjectService, authService, null, assessmentType));
                }
            } else if ("g".equalsIgnoreCase(k.key())) {
                User user = Session.getCurrentUser().orElse(null);
                if (user == null || user.getRole() != Role.ADMIN) {
                    return ScreenResult.navigate(new AIQuizGeneratorScreen(new AIService(), quizService, questionService, subjectService, authService, assessmentType));
                }
            } else if (KeyUtil.isTab(k)) {
                User user = Session.getCurrentUser().orElse(null);
                if (user == null || user.getRole() != Role.ADMIN) {
                    toggleScope();
                }
            } else if ("d".equalsIgnoreCase(k.key())) {
                if (!quizzes.isEmpty()) {
                    initiateDelete();
                }
            } else if ("e".equalsIgnoreCase(k.key())) {
                if (!quizzes.isEmpty()) {
                    Quiz q = quizzes.get(selectedIndex);
                    if (canModify(q)) {
                        if (assessmentType == AssessmentType.SPEED || q.getAssessmentType() == AssessmentType.SPEED) {
                            return ScreenResult.navigate(new SpeedQuizFormScreen(quizService, questionService, subjectService, authService, q));
                        }
                        return ScreenResult.navigate(new QuizFormScreen(quizService, questionService, subjectService, authService, q, assessmentType));
                    } else {
                        bannerMessage = TuiHelper.red("✖ You can only edit assessments you created.");
                    }
                }
            } else if (KeyUtil.isEnter(k)) {
                if (!quizzes.isEmpty()) {
                    return ScreenResult.navigate(new QuizQuestionEditorScreen(quizzes.get(selectedIndex), quizService, questionService, subjectService, authService));
                }
            } else if ("r".equalsIgnoreCase(k.key())) {
                if (!quizzes.isEmpty()) {
                    ExamService examService = new ExamService(new QuizRepository(), new AttemptRepository(), new ResultRepository());
                    return ScreenResult.navigate(new TeacherSubmissionScreen(quizzes.get(selectedIndex), examService, quizService, questionService, subjectService, authService));
                }
            } else if ("s".equalsIgnoreCase(k.key())) {
                subjectFilter.startSearch(subjectFilterIndex);
                selectedIndex = 0;
                refreshList();
            } else if (KeyUtil.isSpace(k)) {
                if (!quizzes.isEmpty()) {
                    Quiz q = quizzes.get(selectedIndex);
                    if (canModify(q)) {
                        togglePublishSelectedQuiz();
                    } else {
                        bannerMessage = TuiHelper.red("✖ You can only publish/unpublish quizzes you created.");
                    }
                }
            } else if ("/".equals(k.key())) {
                searchMode = true;
                bannerMessage = "";
            }
        }
        return ScreenResult.stay(this);
    }

    private boolean canModify(Quiz q) {
        User user = Session.getCurrentUser().orElse(null);
        if (user == null) return false;
        if (user.getRole() == Role.ADMIN) return true;
        return q.getCreatedBy() != null && q.getCreatedBy().equals(user.getId());
    }

    private void toggleScope() {
        User user = Session.getCurrentUser().orElse(null);
        if (user != null && user.getRole() == Role.ADMIN) {
            currentScope = QuizScope.ALL_GLOBAL;
            return;
        }
        currentScope = (currentScope == QuizScope.MY_QUIZZES) ? QuizScope.ALL_GLOBAL : QuizScope.MY_QUIZZES;
        selectedIndex = 0;
        String baseItemTitle = (assessmentType == AssessmentType.EXAM) ? "EXAMS" : (assessmentType == AssessmentType.SPEED ? "SPEED QUIZZES" : "QUIZZES");
        bannerMessage = (currentScope == QuizScope.MY_QUIZZES)
                ? TuiHelper.cyan("Switched scope to: MY " + baseItemTitle)
                : TuiHelper.cyan("Switched scope to: ALL GLOBAL " + baseItemTitle);
        refreshList();
    }

    private void initiateDelete() {
        Quiz q = quizzes.get(selectedIndex);
        if (!canModify(q)) {
            bannerMessage = TuiHelper.red("✖ You can only delete quizzes you created.");
            return;
        }
        confirmingDelete = true;
        confirmDeleteFocused = false;
        pendingDeleteQuiz = q;
    }

    private void executeDelete() {
        if (pendingDeleteQuiz == null) return;
        try {
            User user = Session.getCurrentUser().orElse(null);
            quizService.deleteQuiz(pendingDeleteQuiz.getId(), user);
            bannerMessage = TuiHelper.green("✔ Successfully deleted quiz: " + pendingDeleteQuiz.getTitle());
            refreshList();
        } catch (ValidationException e) {
            bannerMessage = TuiHelper.red("✖ " + e.getMessage());
        }
    }

    private void togglePublishSelectedQuiz() {
        Quiz q = quizzes.get(selectedIndex);
        try {
            quizService.togglePublishStatus(q.getId());
            boolean nowPublished = !q.isPublished();
            bannerMessage = nowPublished ? TuiHelper.green("✔ Published quiz: " + q.getTitle())
                                         : TuiHelper.yellow("Unpublished quiz: " + q.getTitle());
            refreshList();
        } catch (ValidationException e) {
            bannerMessage = TuiHelper.red("✖ " + e.getMessage());
        }
    }

    @Override
    public String view() {
        if (confirmingDelete && pendingDeleteQuiz != null) {
            return TuiHelper.confirmationModal(
                    pendingDeleteQuiz.getTitle(),
                    "Are you sure you want to delete this quiz?",
                    "This will permanently delete the quiz, its questions, and all student submissions.",
                    "Delete Quiz",
                    "Cancel",
                    confirmDeleteFocused
            );
        }

        User user = Session.getCurrentUser().orElse(null);
        boolean isAdmin = user != null && user.getRole() == Role.ADMIN;
        String subjectFilterDisplay = subjectFilter.getHeaderDisplay();
        return QuizViews.renderQuizList(assessmentType, quizzes, selectedIndex, currentScope == QuizScope.MY_QUIZZES,
                subjectFilterDisplay, searchBuffer.toString(), searchMode, bannerMessage, isAdmin);
    }

    private String truncate(String text, int max) {
        return TuiHelper.truncate(text, max);
    }
}