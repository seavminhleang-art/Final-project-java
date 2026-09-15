package com.proctor.controller;

import com.proctor.model.entity.Question;
import com.proctor.model.entity.Session;
import com.proctor.model.entity.Subject;
import com.proctor.model.entity.User;
import com.proctor.model.enums.Difficulty;
import com.proctor.model.enums.QuestionType;
import com.proctor.model.service.AIService;
import com.proctor.model.service.AuthService;
import com.proctor.model.service.QuestionService;
import com.proctor.model.service.SubjectService;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.QuestionBankViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen for browsing and managing the teacher's personal question bank.
 * Bank questions have quiz_id = NULL and belong to the logged-in teacher.
 */
public class QuestionBankScreen implements Screen {
    private final QuestionService questionService;
    private final SubjectService subjectService;
    private final AuthService authService;

    private List<Question> questions = new ArrayList<>();
    private int selectedIndex = 0;

    // Subject filter cycling
    private final List<Subject> allSubjects;
    private int subjectFilterIndex = 0; // 0 = All, 1..N = allSubjects.get(i-1)

    // Other filters
    private QuestionType typeFilter = null;
    private Difficulty diffFilter = null;
    private final StringBuilder searchBuffer = new StringBuilder();
    private boolean searchMode = false;

    // Delete confirmation
    private boolean confirmingDelete = false;
    private boolean confirmDeleteFocused = false; // true = confirm, false = cancel
    private Question pendingDeleteQuestion = null;

    private String bannerMessage = "";

    public QuestionBankScreen(QuestionService questionService, SubjectService subjectService, AuthService authService) {
        this.questionService = questionService;
        this.subjectService = subjectService;
        this.authService = authService;
        this.allSubjects = subjectService.getSubjects(null);
        refreshData();
    }

    private void refreshData() {
        Integer currentUserId = Session.getCurrentUser().map(User::getId).orElse(null);
        Integer subjectId = (subjectFilterIndex > 0 && subjectFilterIndex <= allSubjects.size())
                ? allSubjects.get(subjectFilterIndex - 1).getId() : null;
        String search = searchBuffer.toString().trim().isEmpty() ? null : searchBuffer.toString().trim();
        this.questions = questionService.getBankQuestions(currentUserId, subjectId, typeFilter, diffFilter, search);
        if (selectedIndex >= questions.size()) {
            selectedIndex = Math.max(0, questions.size() - 1);
        }
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {

            // --- Delete confirmation modal ---
            if (confirmingDelete) {
                if (KeyUtil.isLeft(k) || KeyUtil.isRight(k)) {
                    confirmDeleteFocused = !confirmDeleteFocused;
                    return ScreenResult.stay(this);
                }
                if (KeyUtil.isEnter(k)) {
                    if (confirmDeleteFocused) {
                        if (pendingDeleteQuestion != null) {
                            questionService.deleteQuestion(pendingDeleteQuestion.getId());
                            bannerMessage = TuiHelper.green("✔ Question deleted from bank.");
                            refreshData();
                        }
                    }
                    confirmingDelete = false;
                    pendingDeleteQuestion = null;
                    return ScreenResult.stay(this);
                }
                if (KeyUtil.isEsc(k)) {
                    confirmingDelete = false;
                    pendingDeleteQuestion = null;
                    return ScreenResult.stay(this);
                }
                return ScreenResult.stay(this);
            }

            // --- Search mode ---
            if (searchMode) {
                if (KeyUtil.isEsc(k) || KeyUtil.isEnter(k)) {
                    searchMode = false;
                    refreshData();
                } else if (KeyUtil.isBackspace(k)) {
                    if (!searchBuffer.isEmpty()) {
                        searchBuffer.deleteCharAt(searchBuffer.length() - 1);
                    }
                } else if (k.type() == KeyType.KeyRunes && k.runes() != null) {
                    for (char c : k.runes()) {
                        if (!Character.isISOControl(c)) searchBuffer.append(c);
                    }
                } else if (k.key() != null && k.key().length() == 1 && !Character.isISOControl(k.key().charAt(0))) {
                    searchBuffer.append(k.key());
                }
                return ScreenResult.stay(this);
            }

            // --- Normal navigation ---
            bannerMessage = "";

            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new TeacherDashboardScreen(authService, questionService, subjectService));
            }
            if (KeyUtil.isUp(k)) {
                if (!questions.isEmpty()) selectedIndex = (selectedIndex - 1 + questions.size()) % questions.size();
            } else if (KeyUtil.isDown(k)) {
                if (!questions.isEmpty()) selectedIndex = (selectedIndex + 1) % questions.size();
            } else if (KeyUtil.isLeft(k)) {
                if (!questions.isEmpty()) {
                    int pageSize = TuiHelper.PAGE_SIZE;
                    int page = selectedIndex / pageSize;
                    selectedIndex = Math.max(0, (page - 1) * pageSize);
                }
            } else if (KeyUtil.isRight(k)) {
                if (!questions.isEmpty()) {
                    int pageSize = TuiHelper.PAGE_SIZE;
                    int page = selectedIndex / pageSize;
                    int totalPages = Math.max(1, (int) Math.ceil((double) questions.size() / pageSize));
                    if (page < totalPages - 1) {
                        selectedIndex = Math.min(questions.size() - 1, (page + 1) * pageSize);
                    }
                }
            } else if ("n".equalsIgnoreCase(k.key())) {
                // New bank question (no quiz context)
                return ScreenResult.navigate(new QuestionFormScreen(questionService, subjectService, authService, null, null));
            } else if ("g".equalsIgnoreCase(k.key())) {
                // AI generate to bank (no quiz context)
                return ScreenResult.navigate(new AIQuestionGeneratorScreen(new AIService(), questionService, subjectService, authService, null));
            } else if (("e".equalsIgnoreCase(k.key()) || KeyUtil.isEnter(k)) && !questions.isEmpty()) {
                // Edit selected bank question
                return ScreenResult.navigate(new QuestionFormScreen(questionService, subjectService, authService, questions.get(selectedIndex), null));
            } else if (("d".equalsIgnoreCase(k.key()) || KeyUtil.isDelete(k)) && !questions.isEmpty()) {
                // Delete with confirmation
                pendingDeleteQuestion = questions.get(selectedIndex);
                confirmingDelete = true;
                confirmDeleteFocused = false;
                return ScreenResult.stay(this);
            } else if (KeyUtil.isTab(k) || "f".equalsIgnoreCase(k.key())) {
                // Cycle type filter
                if (typeFilter == null) typeFilter = QuestionType.MCQ;
                else if (typeFilter == QuestionType.MCQ) typeFilter = QuestionType.TRUE_FALSE;
                else if (typeFilter == QuestionType.TRUE_FALSE) typeFilter = QuestionType.SHORT_ANSWER;
                else typeFilter = null;
                selectedIndex = 0;
                refreshData();
            } else if ("s".equalsIgnoreCase(k.key())) {
                // Cycle subject filter
                subjectFilterIndex = (subjectFilterIndex + 1) % (allSubjects.size() + 1);
                selectedIndex = 0;
                refreshData();
            } else if ("x".equalsIgnoreCase(k.key())) {
                // Cycle difficulty filter
                if (diffFilter == null) diffFilter = Difficulty.EASY;
                else if (diffFilter == Difficulty.EASY) diffFilter = Difficulty.MEDIUM;
                else if (diffFilter == Difficulty.MEDIUM) diffFilter = Difficulty.HARD;
                else diffFilter = null;
                selectedIndex = 0;
                refreshData();
            } else if ("/".equals(k.key())) {
                searchMode = true;
                searchBuffer.setLength(0);
            }
        }
        return ScreenResult.stay(this);
    }

    @Override
    public String view() {
        if (confirmingDelete && pendingDeleteQuestion != null) {
            return TuiHelper.confirmationModal(
                    "DELETE BANK QUESTION",
                    "Delete this question from your bank?",
                    truncate(pendingDeleteQuestion.getQuestionText(), 80),
                    "Delete",
                    "Cancel",
                    confirmDeleteFocused
            );
        }

        String subjectFilterDisplay = (subjectFilterIndex > 0 && subjectFilterIndex <= allSubjects.size())
                ? allSubjects.get(subjectFilterIndex - 1).getCode()
                : "";

        return QuestionBankViews.renderBankList(
                questions, selectedIndex, subjectFilterDisplay, typeFilter, diffFilter,
                searchBuffer.toString(), searchMode, bannerMessage
        );
    }

    private static String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}
