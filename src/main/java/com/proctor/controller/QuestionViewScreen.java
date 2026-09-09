package com.proctor.controller;

import com.proctor.model.service.AuthService;
import com.proctor.model.enums.QuestionType;
import com.proctor.model.entity.Question;
import com.proctor.model.entity.QuestionOption;
import com.proctor.model.service.QuestionService;
import com.proctor.model.service.SubjectService;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.QuestionViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;

public class QuestionViewScreen implements Screen {
    private final Question question;
    private final QuestionService questionService;
    private final SubjectService subjectService;
    private final AuthService authService;

    public QuestionViewScreen(Question question, QuestionService questionService, SubjectService subjectService, AuthService authService) {
        this.question = question;
        this.questionService = questionService;
        this.subjectService = subjectService;
        this.authService = authService;
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new QuestionListScreen(questionService, subjectService, authService));
            } else if ("e".equalsIgnoreCase(k.key()) || KeyUtil.isEnter(k)) {
                return ScreenResult.navigate(new QuestionFormScreen(questionService, subjectService, authService, question));
            } else if ("t".equalsIgnoreCase(k.key()) || KeyUtil.isSpace(k)) {
                questionService.toggleQuestionStatus(question.getId());
                question.setEnabled(!question.isEnabled());
                return ScreenResult.stay(this);
            }
        }
        return ScreenResult.stay(this);
    }

    @Override
    public String view() {
        return QuestionViews.renderQuestionView(question);
    }
}