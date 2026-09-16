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

    private final List<Subject> allSubjects;
    private int subjectFilterIndex = 0;
    private List<Quiz> quizzes;
    private int selectedIndex = 0;
    private final StringBuilder searchBuffer = new StringBuilder();
    private boolean searchMode = false;
    private QuizScope currentScope = QuizScope.MY_QUIZZES;
    private boolean confirmingDelete = false;
    private boolean confirmDeleteFocused = false;
    private Quiz pendingDeleteQuiz = null;
    private String bannerMessage = "";

    public QuizListScreen(QuizService quizService, QuestionService questionService, SubjectService subjectService, AuthService authService) {
        this(quizService, questionService, subjectService, authService, AssessmentType.QUIZ);
    }

    public QuizListScreen(QuizService quizService, QuestionService questionService, SubjectService subjectService,
                          AuthService authService, AssessmentType assessmentType) {
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

        if (quizzes.isEmpty()) {
            selectedIndex = 0;
        } else if (selectedIndex >= quizzes.size()) {
            selectedIndex = quizzes.size() - 1;
        }
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
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
                } else if ("y".equalsIgnoreCase(k.key())) {
                    executeDelete();
                    confirmingDelete = false;
                    pendingDeleteQuiz = null;
                    return ScreenResult.stay(this);
                } else if ("n".equalsIgnoreCase(k.key()) || KeyUtil.isEsc(k)) {
                    confirmingDelete = false;
                    pendingDeleteQuiz = null;
                    bannerMessage = TuiHelper.yellow("Deletion cancelled.");
                    return ScreenResult.stay(this);
                }
                return ScreenResult.stay(this);
            }

            if (searchMode) {
                if (KeyUtil.isEnter(k) || KeyUtil.isEsc(k)) {
                    searchMode = false;
                    refreshList();
                } else if (KeyUtil.isBackspace(k)) {
                    if (!searchBuffer.isEmpty()) {
                        searchBuffer.deleteCharAt(searchBuffer.length() - 1);
                        refreshList();
                    }
                } else if (k.type() == KeyType.KeyRunes && k.runes() != null) {
                    for (char c : k.runes()) {
                        if (!Character.isISOControl(c)) searchBuffer.append(c);
                    }
                    refreshList();
                } else if (k.key() != null && k.key().length() == 1 && !Character.isISOControl(k.key().charAt(0))) {
                    searchBuffer.append(k.key());
                    refreshList();
                }
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
                if (!quizzes.isEmpty()) {
                    selectedIndex = (selectedIndex - 1 + quizzes.size()) % quizzes.size();
                }
            } else if (KeyUtil.isDown(k)) {
                if (!quizzes.isEmpty()) {
                    selectedIndex = (selectedIndex + 1) % quizzes.size();
                }
            } else if (KeyUtil.isLeft(k)) {
                if (!quizzes.isEmpty()) {
                    int pageSize = TuiHelper.PAGE_SIZE;
                    int currentPage = selectedIndex / pageSize;
                    if (currentPage > 0) {
                        selectedIndex = (currentPage - 1) * pageSize;
                    }
                }
            } else if (KeyUtil.isRight(k)) {
                if (!quizzes.isEmpty()) {
                    int pageSize = TuiHelper.PAGE_SIZE;
                    int totalPages = Math.max(1, (int) Math.ceil((double) quizzes.size() / pageSize));
                    int currentPage = selectedIndex / pageSize;
                    if (currentPage < totalPages - 1) {
                        selectedIndex = Math.min(quizzes.size() - 1, (currentPage + 1) * pageSize);
                    }
                }
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
            } else if (KeyUtil.isTab(k) || "f".equalsIgnoreCase(k.key())) {
                User user = Session.getCurrentUser().orElse(null);
                if (user == null || user.getRole() != Role.ADMIN) {
                    toggleScope();
                }
            } else if ("d".equalsIgnoreCase(k.key()) || KeyUtil.isDelete(k)) {
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
            } else if ("q".equalsIgnoreCase(k.key()) || KeyUtil.isEnter(k)) {
                if (!quizzes.isEmpty()) {
                    return ScreenResult.navigate(new QuizQuestionEditorScreen(quizzes.get(selectedIndex), quizService, questionService, subjectService, authService));
                }
            } else if ("r".equalsIgnoreCase(k.key())) {
                if (!quizzes.isEmpty()) {
                    ExamService examService = new ExamService(new QuizRepository(), new AttemptRepository(), new ResultRepository());
                    return ScreenResult.navigate(new TeacherSubmissionScreen(quizzes.get(selectedIndex), examService, quizService, questionService, subjectService, authService));
                }
            } else if ("s".equalsIgnoreCase(k.key())) {
                if (!allSubjects.isEmpty()) {
                    subjectFilterIndex = (subjectFilterIndex + 1) % (allSubjects.size() + 1);
                } else {
                    subjectFilterIndex = 0;
                }
                selectedIndex = 0;
                refreshList();
            } else if ("p".equalsIgnoreCase(k.key()) || KeyUtil.isSpace(k)) {
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
        String subjectFilterDisplay = (subjectFilterIndex > 0 && subjectFilterIndex <= allSubjects.size())
                ? allSubjects.get(subjectFilterIndex - 1).getCode() : "ALL";
        return QuizViews.renderQuizList(assessmentType, quizzes, selectedIndex, currentScope == QuizScope.MY_QUIZZES,
                subjectFilterDisplay, searchBuffer.toString(), searchMode, bannerMessage, isAdmin);
    }

    private String truncate(String text, int max) {
        return TuiHelper.truncate(text, max);
    }
}