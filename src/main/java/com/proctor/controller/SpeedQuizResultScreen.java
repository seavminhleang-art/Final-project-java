package com.proctor.controller;

import com.proctor.model.entity.Quiz;
import com.proctor.model.entity.AttemptAnswer;
import com.proctor.model.entity.Result;
import com.proctor.model.entity.SpeedQuizSession;
import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.service.ExamService;
import com.proctor.model.entity.Session;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.SpeedQuizViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;

import java.util.ArrayList;
import java.util.List;

public class SpeedQuizResultScreen implements Screen {
    private final Result result;
    private final SpeedQuizSession session;
    private final ExamService examService;
    private final AuthService authService;
    private final Screen returnScreen;
    private final Quiz quiz;
    private final List<AttemptAnswer> attemptAnswers;
    private String bannerMessage = "";

    public SpeedQuizResultScreen(Result result, SpeedQuizSession session, ExamService examService, AuthService authService, Screen returnScreen) {
        this.result = result;
        this.session = session;
        this.examService = examService;
        this.authService = authService;
        this.returnScreen = returnScreen;

        if (session != null) {
            this.quiz = session.getQuiz();
            this.attemptAnswers = new ArrayList<>();
        } else if (examService != null && result != null) {
            this.quiz = result.getQuizId() != null ? examService.getQuiz(result.getQuizId()).orElse(null) : null;
            this.attemptAnswers = result.getAttemptId() != null ? examService.getAttemptAnswers(result.getAttemptId()) : new ArrayList<>();
        } else {
            this.quiz = null;
            this.attemptAnswers = new ArrayList<>();
        }
    }

    public SpeedQuizResultScreen(Result result, Screen returnScreen, ExamService examService, AuthService authService) {
        this(result, null, examService, authService, returnScreen);
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
            if ("r".equalsIgnoreCase(k.key())) {
                if (examService != null && result != null && result.getQuizId() != null) {
                    User student = Session.getCurrentUser().orElse(null);
                    int studentId = (student != null && student.getId() != null) ? student.getId() : 0;
                    try {
                        SpeedQuizSession newSession = examService.startSpeedQuiz(result.getQuizId(), studentId);
                        return ScreenResult.navigate(new SpeedQuizTakerScreen(newSession, examService, authService, returnScreen));
                    } catch (Exception e) {
                        bannerMessage = TuiHelper.red("✖ Failed to restart speed quiz: " + e.getMessage());
                        return ScreenResult.stay(this);
                    }
                }
            }

            if (KeyUtil.isEnter(k) || KeyUtil.isEsc(k)) {
                if (returnScreen != null) {
                    return ScreenResult.navigate(returnScreen);
                }
                return ScreenResult.navigate(new StudentDashboardScreen(authService, examService));
            }
        }
        return ScreenResult.stay(this);
    }

    @Override
    public String view() {
        StringBuilder sb = new StringBuilder(SpeedQuizViews.renderSpeedQuizResult(result, session, quiz, attemptAnswers, returnScreen != null));
        if (!bannerMessage.isBlank()) {
            sb.append("\n  ").append(bannerMessage).append("\n");
        }
        return sb.toString();
    }
}
