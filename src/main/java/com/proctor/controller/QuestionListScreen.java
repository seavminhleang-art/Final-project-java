package com.proctor.controller;

import com.proctor.model.service.AuthService;
import com.proctor.model.enums.Difficulty;
import com.proctor.model.enums.QuestionType;
import com.proctor.model.entity.Question;
import com.proctor.model.service.QuestionService;
import com.proctor.model.service.SubjectService;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.QuestionViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

import java.util.List;

public class QuestionListScreen implements Screen {
    private final QuestionService questionService;
    private final SubjectService subjectService;
    private final AuthService authService;

    private List<Question> questions;
    private int selectedIndex = 0;

    private QuestionType typeFilter = null;
    private Difficulty diffFilter = null;
    private final StringBuilder searchBuffer = new StringBuilder();
    private boolean searchMode = false;
    private String bannerMessage = "";

    public QuestionListScreen(QuestionService questionService, SubjectService subjectService, AuthService authService) {
        this.questionService = questionService;
        this.subjectService = subjectService;
        this.authService = authService;
        refreshList();
    }

    private void refreshList() {
        this.questions = questionService.getQuestions(null, typeFilter, diffFilter, searchBuffer.toString());
        if (questions.isEmpty()) {
            selectedIndex = 0;
        } else if (selectedIndex >= questions.size()) {
            selectedIndex = questions.size() - 1;
        }
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
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
                return ScreenResult.navigate(new TeacherDashboardScreen(authService, questionService, subjectService));
            } else if (KeyUtil.isUp(k)) {
                if (!questions.isEmpty()) {
                    selectedIndex = (selectedIndex - 1 + questions.size()) % questions.size();
                }
            } else if (KeyUtil.isDown(k)) {
                if (!questions.isEmpty()) {
                    selectedIndex = (selectedIndex + 1) % questions.size();
                }
            } else if ("n".equalsIgnoreCase(k.key())) {
                return ScreenResult.navigate(new QuestionFormScreen(questionService, subjectService, authService, null));
            } else if ("e".equalsIgnoreCase(k.key())) {
                if (!questions.isEmpty()) {
                    Question q = questionService.getQuestionById(questions.get(selectedIndex).getId()).orElse(questions.get(selectedIndex));
                    return ScreenResult.navigate(new QuestionFormScreen(questionService, subjectService, authService, q));
                }
            } else if ("v".equalsIgnoreCase(k.key()) || KeyUtil.isEnter(k)) {
                if (!questions.isEmpty()) {
                    Question q = questionService.getQuestionById(questions.get(selectedIndex).getId()).orElse(questions.get(selectedIndex));
                    return ScreenResult.navigate(new QuestionViewScreen(q, questionService, subjectService, authService));
                }
            } else if ("t".equalsIgnoreCase(k.key()) || KeyUtil.isSpace(k)) {
                if (!questions.isEmpty()) {
                    Question q = questions.get(selectedIndex);
                    questionService.toggleQuestionStatus(q.getId());
                    bannerMessage = TuiHelper.green("Toggled status for question #" + q.getId());
                    refreshList();
                }
            } else if ("f".equalsIgnoreCase(k.key())) {
                cycleTypeFilter();
                refreshList();
            } else if ("d".equalsIgnoreCase(k.key())) {
                cycleDiffFilter();
                refreshList();
            } else if ("/".equals(k.key())) {
                searchMode = true;
                bannerMessage = "";
            }
        }
        return ScreenResult.stay(this);
    }

    private void cycleTypeFilter() {
        if (typeFilter == null) typeFilter = QuestionType.MCQ;
        else if (typeFilter == QuestionType.MCQ) typeFilter = QuestionType.TRUE_FALSE;
        else if (typeFilter == QuestionType.TRUE_FALSE) typeFilter = QuestionType.SHORT_ANSWER;
        else typeFilter = null;
    }

    private void cycleDiffFilter() {
        if (diffFilter == null) diffFilter = Difficulty.EASY;
        else if (diffFilter == Difficulty.EASY) diffFilter = Difficulty.MEDIUM;
        else if (diffFilter == Difficulty.MEDIUM) diffFilter = Difficulty.HARD;
        else diffFilter = null;
    }

    @Override
    public String view() {
        return QuestionViews.renderQuestionList(questions, selectedIndex, typeFilter, diffFilter, searchBuffer.toString(), searchMode, bannerMessage);
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}