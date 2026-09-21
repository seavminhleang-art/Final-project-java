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

public class QuestionBankScreen implements Screen {
    private final QuestionService questionService;
    private final SubjectService subjectService;
    private final AuthService authService;

    private List<Question> questions = new ArrayList<>();
    private int selectedIndex = 0;

    private final List<Subject> allSubjects;
    private int subjectFilterIndex = 0;
    private final InlineSubjectFilter<Subject> subjectFilter;

    private QuestionType typeFilter = null;
    private Difficulty diffFilter = null;
    private final StringBuilder searchBuffer = new StringBuilder();
    private boolean searchMode = false;

    private boolean confirmingDelete = false;
    private boolean confirmDeleteFocused = false;
    private Question pendingDeleteQuestion = null;

    private String bannerMessage = "";

    public QuestionBankScreen(QuestionService questionService, SubjectService subjectService, AuthService authService) {
        this.questionService = questionService;
        this.subjectService = subjectService;
        this.authService = authService;
        this.allSubjects = subjectService.getSubjects(null);

        List<InlineSubjectFilter.Item<Subject>> items = new java.util.ArrayList<>();
        items.add(new InlineSubjectFilter.Item<>(null, "ALL", "All Subjects"));
        for (Subject s : this.allSubjects) {
            items.add(new InlineSubjectFilter.Item<>(s, s.getCode(), s.getName()));
        }
        this.subjectFilter = new InlineSubjectFilter<>(items);

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
            if (subjectFilter.isActive()) {
                boolean handled = subjectFilter.handleKey(k);
                if (handled) {
                    subjectFilterIndex = subjectFilter.getSelectedOriginalIndex();
                    selectedIndex = 0;
                    refreshData();
                    return ScreenResult.stay(this);
                }
            }

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
                    if (page > 0) {
                        selectedIndex = (page - 1) * pageSize;
                    }
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
                return ScreenResult.navigate(new QuestionFormScreen(questionService, subjectService, authService, null, null));
            } else if ("g".equalsIgnoreCase(k.key())) {
                return ScreenResult.navigate(new AIQuestionGeneratorScreen(new AIService(), questionService, subjectService, authService, null));
            } else if (("e".equalsIgnoreCase(k.key()) || KeyUtil.isEnter(k)) && !questions.isEmpty()) {
                return ScreenResult.navigate(new QuestionFormScreen(questionService, subjectService, authService, questions.get(selectedIndex), null));
            } else if (("d".equalsIgnoreCase(k.key()) || KeyUtil.isDelete(k)) && !questions.isEmpty()) {
                pendingDeleteQuestion = questions.get(selectedIndex);
                confirmingDelete = true;
                confirmDeleteFocused = false;
                return ScreenResult.stay(this);
            } else if (KeyUtil.isTab(k) || "f".equalsIgnoreCase(k.key())) {
                if (typeFilter == null) typeFilter = QuestionType.MCQ;
                else if (typeFilter == QuestionType.MCQ) typeFilter = QuestionType.TRUE_FALSE;
                else if (typeFilter == QuestionType.TRUE_FALSE) typeFilter = QuestionType.SHORT_ANSWER;
                else typeFilter = null;
                selectedIndex = 0;
                refreshData();
            } else if ("s".equalsIgnoreCase(k.key())) {
                subjectFilter.startSearch(subjectFilterIndex);
                selectedIndex = 0;
                refreshData();
            } else if ("x".equalsIgnoreCase(k.key())) {
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

        String subjectFilterDisplay = subjectFilter.getHeaderDisplay();

        return QuestionBankViews.renderBankList(
                questions, selectedIndex, subjectFilterDisplay, typeFilter, diffFilter,
                searchBuffer.toString(), searchMode, bannerMessage
        );
    }

    private static String truncate(String text, int max) {
        return TuiHelper.truncate(text, max);
    }
}
