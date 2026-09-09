package com.proctor.controller;

import com.proctor.model.service.AuthService;
import com.proctor.model.enums.AttemptStatus;
import com.proctor.exception.ValidationException;
import com.proctor.model.entity.Attempt;
import com.proctor.model.entity.AttemptAnswer;
import com.proctor.model.service.ExamService;
import com.proctor.model.entity.Question;
import com.proctor.model.service.QuestionService;
import com.proctor.model.entity.Quiz;
import com.proctor.model.service.QuizService;
import com.proctor.model.entity.Result;
import com.proctor.model.service.SubjectService;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.TeacherSubmissionViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TeacherSubmissionScreen implements Screen {
    private final Quiz specificQuiz;
    private final ExamService examService;
    private final QuizService quizService;
    private final QuestionService questionService;
    private final SubjectService subjectService;
    private final AuthService authService;

    private List<Attempt> submissions;
    private int selectedIndex = 0;
    private boolean inspectingAnswerSheet = false;
    private int inspectingAnswerIndex = 0;
    private String bannerMessage = "";

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
            this.submissions = examService.getSubmissionsForQuiz(specificQuiz.getId());
        } else {
            this.submissions = List.of();
        }
        if (submissions.isEmpty()) {
            selectedIndex = 0;
        } else if (selectedIndex >= submissions.size()) {
            selectedIndex = submissions.size() - 1;
        }
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
            if (inspectingAnswerSheet) {
                List<Question> questions = (specificQuiz != null) ? specificQuiz.getQuestions() : List.of();
                if (KeyUtil.isEsc(k)) {
                    inspectingAnswerSheet = false;
                    return ScreenResult.stay(this);
                } else if (KeyUtil.isUp(k)) {
                    if (!questions.isEmpty()) {
                        inspectingAnswerIndex = (inspectingAnswerIndex - 1 + questions.size()) % questions.size();
                    }
                } else if (KeyUtil.isDown(k)) {
                    if (!questions.isEmpty()) {
                        inspectingAnswerIndex = (inspectingAnswerIndex + 1) % questions.size();
                    }
                } else if ("g".equalsIgnoreCase(k.key())) {
                    if (!submissions.isEmpty()) {
                        Attempt att = submissions.get(selectedIndex);
                        boolean ok = examService.gradeWithAI(att.getId());
                        bannerMessage = ok ? TuiHelper.green("✔ AI evaluation complete! Press [r] to finalize and return grade.")
                                           : TuiHelper.red("✖ AI grading failed. Please check Ollama.");
                    }
                } else if ("r".equalsIgnoreCase(k.key())) {
                    if (!submissions.isEmpty()) {
                        Attempt att = submissions.get(selectedIndex);
                        try {
                            Result res = examService.returnGrade(att.getId());
                            bannerMessage = TuiHelper.green(String.format("✔ Grade returned: %.1f/%.1f points (%.1f%%) - %s",
                                    res.getTotalPoints(), res.getMaxPoints(), res.getPercentage(),
                                    res.isPassed() ? "PASSED" : "FAILED"));
                            refreshList();
                        } catch (ValidationException e) {
                            bannerMessage = TuiHelper.red("✖ " + e.getMessage());
                        }
                    }
                }
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isEsc(k)) {
                if (specificQuiz != null) {
                    return ScreenResult.navigate(new QuizListScreen(quizService, questionService, subjectService, authService));
                }
                return ScreenResult.navigate(new TeacherDashboardScreen(authService, questionService, subjectService, quizService));
            }

            if (KeyUtil.isUp(k)) {
                if (!submissions.isEmpty()) {
                    selectedIndex = (selectedIndex - 1 + submissions.size()) % submissions.size();
                }
            } else if (KeyUtil.isDown(k)) {
                if (!submissions.isEmpty()) {
                    selectedIndex = (selectedIndex + 1) % submissions.size();
                }
            } else if (KeyUtil.isEnter(k)) {
                if (!submissions.isEmpty()) {
                    inspectingAnswerSheet = true;
                    inspectingAnswerIndex = 0;
                    bannerMessage = "";
                }
            } else if ("g".equalsIgnoreCase(k.key())) {
                if (!submissions.isEmpty()) {
                    Attempt att = submissions.get(selectedIndex);
                    boolean ok = examService.gradeWithAI(att.getId());
                    bannerMessage = ok ? TuiHelper.green("✔ Auto-graded with AI! Press [r] to return grade.")
                                       : TuiHelper.red("✖ AI grading failed. Please check Ollama.");
                }
            } else if ("r".equalsIgnoreCase(k.key())) {
                if (!submissions.isEmpty()) {
                    Attempt att = submissions.get(selectedIndex);
                    try {
                        Result res = examService.returnGrade(att.getId());
                        bannerMessage = TuiHelper.green(String.format("✔ Grade returned: %.1f/%.1f (%.1f%%) - %s",
                                res.getTotalPoints(), res.getMaxPoints(), res.getPercentage(),
                                res.isPassed() ? "PASSED" : "FAILED"));
                        refreshList();
                    } catch (ValidationException e) {
                        bannerMessage = TuiHelper.red("✖ " + e.getMessage());
                    }
                }
            }
        }
        return ScreenResult.stay(this);
    }

    @Override
    public String view() {
        if (inspectingAnswerSheet && !submissions.isEmpty()) {
            Attempt attempt = submissions.get(selectedIndex);
            List<AttemptAnswer> answers = examService.getAttemptAnswers(attempt.getId());
            Map<Integer, AttemptAnswer> answerMap = new java.util.HashMap<>();
            for (AttemptAnswer a : answers) {
                answerMap.put(a.getQuestionId(), a);
            }
            return TeacherSubmissionViews.renderAnswerSheet(specificQuiz, attempt, answerMap, inspectingAnswerIndex, bannerMessage);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return TeacherSubmissionViews.renderSubmissionList(specificQuiz, submissions, java.util.Collections.emptyMap(), selectedIndex, sdf, bannerMessage);
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}