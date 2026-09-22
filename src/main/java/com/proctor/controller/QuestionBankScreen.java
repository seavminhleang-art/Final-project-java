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
import com.proctor.util.ListNavigationHelper;
import com.proctor.util.MouseUtil;
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
    private final StringBuilder searchBuffer = new StringBuilder();
    private boolean searchMode = false;
    private String bannerMessage = "";

    private List<Subject> allSubjects;
    private int subjectFilterIndex = 0;
    private final InlineSubjectFilter<Subject> subjectFilter;

    private QuestionType typeFilter = null;
    private Difficulty diffFilter = null;

    private boolean confirmingDelete = false;
    private boolean confirmDeleteFocused = false;
    private Question pendingDeleteQuestion = null;

    public QuestionBankScreen(QuestionService questionService, SubjectService subjectService, AuthService authService) {
        this.questionService = questionService;
        this.subjectService = subjectService;
        this.authService = authService;
        this.allSubjects = subjectService.getSubjects("");

        List<InlineSubjectFilter.Item<Subject>> items = new ArrayList<>();
        items.add(new InlineSubjectFilter.Item<>(null, "ALL", "All Subjects"));
        for (Subject s : this.allSubjects) {
            items.add(new InlineSubjectFilter.Item<>(s, s.getCode(), s.getName()));
        }
        this.subjectFilter = new InlineSubjectFilter<>(items);

        refreshData();
    }

    private void refreshData() {
        User user = Session.getCurrentUser().orElse(null);
        Integer currentUserId = (user != null) ? user.getId() : null;
        Integer subjectId = (subjectFilterIndex > 0 && subjectFilterIndex <= allSubjects.size())
                ? allSubjects.get(subjectFilterIndex - 1).getId() : null;
        String search = searchBuffer.toString().trim().isEmpty() ? null : searchBuffer.toString().trim();
        this.questions = questionService.getBankQuestions(currentUserId, subjectId, typeFilter, diffFilter, search);
        selectedIndex = ListNavigationHelper.clampIndex(selectedIndex, questions.size());
    }

    @Override
    public ScreenResult update(Message msg) {
        if (MouseUtil.isWheelUp(msg) || MouseUtil.isWheelDown(msg)) {
            selectedIndex = ListNavigationHelper.handleWheel(msg, selectedIndex, questions.size());
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isLeftClick(msg)) {
            if (confirmingDelete) {
                int action = ListNavigationHelper.handleConfirmationClick(msg, view(), "Delete", "Cancel");
                if (action == 0 && pendingDeleteQuestion != null) {
                    questionService.deleteQuestion(pendingDeleteQuestion.getId());
                    bannerMessage = TuiHelper.green("✔ Question deleted from bank.");
                    refreshData();
                }
                if (action >= 0) {
                    confirmingDelete = false;
                    pendingDeleteQuestion = null;
                }
                return ScreenResult.stay(this);
            }

            int line = MouseUtil.getLineIndex(msg);
            int col = MouseUtil.getColInLine(msg);

            int clickedIdx = ListNavigationHelper.getClickedItemIndex(line, MouseUtil.findTableStartLine(view()), questions.size(), selectedIndex, TuiHelper.PAGE_SIZE);
            if (clickedIdx != -1) {
                if (selectedIndex == clickedIdx) {
                    return ScreenResult.navigate(new QuestionFormScreen(questionService, subjectService, authService, questions.get(selectedIndex)));
                }
                selectedIndex = clickedIdx;
                return ScreenResult.stay(this);
            }

            int pagLine = MouseUtil.findPaginationLine(view());
            if (pagLine != -1 && line == pagLine && !questions.isEmpty()) {
                selectedIndex = ListNavigationHelper.handlePaginationClick(col, selectedIndex, questions.size(), TuiHelper.PAGE_SIZE);
                return ScreenResult.stay(this);
            }

            String hintAction = MouseUtil.getClickedHintAction(view(), line, col);
            if (hintAction != null) {
                if ("Esc".equals(hintAction)) {
                    return ScreenResult.navigate(new TeacherDashboardScreen(authService, questionService, subjectService));
                } else if ("n".equals(hintAction)) {
                    return ScreenResult.navigate(new QuestionFormScreen(questionService, subjectService, authService, null, null));
                } else if ("g".equals(hintAction)) {
                    return ScreenResult.navigate(new AIQuestionGeneratorScreen(new AIService(), questionService, subjectService, authService, null));
                } else if (("e".equals(hintAction) || "Enter".equals(hintAction)) && !questions.isEmpty() && selectedIndex < questions.size()) {
                    return ScreenResult.navigate(new QuestionFormScreen(questionService, subjectService, authService, questions.get(selectedIndex), null));
                } else if ("d".equals(hintAction) && !questions.isEmpty() && selectedIndex < questions.size()) {
                    pendingDeleteQuestion = questions.get(selectedIndex);
                    confirmingDelete = true;
                    confirmDeleteFocused = false;
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
                searchMode = ListNavigationHelper.handleSearchKey(k, searchBuffer, this::refreshData);
                return ScreenResult.stay(this);
            }

            bannerMessage = "";

            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new TeacherDashboardScreen(authService, questionService, subjectService));
            }
            if (KeyUtil.isUp(k)) {
                selectedIndex = ListNavigationHelper.adjustIndex(selectedIndex, -1, questions.size());
            } else if (KeyUtil.isDown(k)) {
                selectedIndex = ListNavigationHelper.adjustIndex(selectedIndex, 1, questions.size());
            } else if (KeyUtil.isLeft(k)) {
                selectedIndex = ListNavigationHelper.prevPage(selectedIndex, TuiHelper.PAGE_SIZE);
            } else if (KeyUtil.isRight(k)) {
                selectedIndex = ListNavigationHelper.nextPage(selectedIndex, questions.size(), TuiHelper.PAGE_SIZE);
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
