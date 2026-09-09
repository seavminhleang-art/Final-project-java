package com.proctor.controller;

import com.proctor.model.service.AuthService;
import com.proctor.model.entity.Question;
import com.proctor.model.service.QuestionService;
import com.proctor.model.entity.Quiz;
import com.proctor.model.service.QuizService;
import com.proctor.model.service.SubjectService;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.QuizViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QuizQuestionAssignmentScreen implements Screen {
    private final Quiz quiz;
    private final QuizService quizService;
    private final QuestionService questionService;
    private final SubjectService subjectService;
    private final AuthService authService;

    private List<Question> bankQuestions;
    private final Set<Integer> assignedIds = new HashSet<>();
    private int selectedIndex = 0;
    private String bannerMessage = "";

    public QuizQuestionAssignmentScreen(Quiz quiz, QuizService quizService, QuestionService questionService, SubjectService subjectService, AuthService authService) {
        this.quiz = quiz;
        this.quizService = quizService;
        this.questionService = questionService;
        this.subjectService = subjectService;
        this.authService = authService;
        refreshData();
    }

    private void refreshData() {
        com.proctor.model.enums.QuestionType typeFilter =
                (quiz.getAssessmentType() == com.proctor.model.enums.AssessmentType.QUIZ) ? quiz.getQuizQuestionType() : null;
        this.bankQuestions = questionService.getQuestions(quiz.getSubjectId(), typeFilter, null, null);
        this.assignedIds.clear();
        this.assignedIds.addAll(quizService.getAssignedQuestionIds(quiz.getId()));
        if (quiz.getAssessmentType() == com.proctor.model.enums.AssessmentType.QUIZ && quiz.getQuizQuestionType() != null) {
            bannerMessage = TuiHelper.dim("Showing only " + quiz.getQuizQuestionType() + " questions for this quiz.");
        }
        if (bankQuestions.isEmpty()) {
            selectedIndex = 0;
        } else if (selectedIndex >= bankQuestions.size()) {
            selectedIndex = bankQuestions.size() - 1;
        }
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
            if (KeyUtil.isEsc(k)) {

                quizService.assignQuestions(quiz.getId(), new ArrayList<>(assignedIds));
                return ScreenResult.navigate(new QuizListScreen(quizService, questionService, subjectService, authService));
            }

            if (KeyUtil.isUp(k)) {
                if (!bankQuestions.isEmpty()) {
                    selectedIndex = (selectedIndex - 1 + bankQuestions.size()) % bankQuestions.size();
                }
            } else if (KeyUtil.isDown(k)) {
                if (!bankQuestions.isEmpty()) {
                    selectedIndex = (selectedIndex + 1) % bankQuestions.size();
                }
            } else if (KeyUtil.isEnter(k) || " ".equals(k.key())) {
                toggleSelectedQuestion();
            }
        }
        return ScreenResult.stay(this);
    }

    private void toggleSelectedQuestion() {
        if (bankQuestions.isEmpty()) return;

        Question q = bankQuestions.get(selectedIndex);
        if (assignedIds.contains(q.getId())) {
            assignedIds.remove(q.getId());
            bannerMessage = TuiHelper.yellow("Removed question #" + q.getId() + " from quiz.");
        } else {
            assignedIds.add(q.getId());
            bannerMessage = TuiHelper.green("Added question #" + q.getId() + " to quiz.");
        }
        quizService.assignQuestions(quiz.getId(), new ArrayList<>(assignedIds));
    }

    @Override
    public String view() {
        return QuizViews.renderQuizQuestionAssignment(quiz, bankQuestions, assignedIds, selectedIndex, bannerMessage);
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}