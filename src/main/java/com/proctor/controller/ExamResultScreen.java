package com.proctor.controller;

import com.proctor.model.repository.UserRepository;
import com.proctor.model.service.AuthService;
import com.proctor.model.enums.AssessmentType;
import com.proctor.model.enums.AttemptStatus;
import com.proctor.exception.ValidationException;
import com.proctor.model.entity.ExamSession;
import com.proctor.model.service.ExamService;
import com.proctor.model.repository.InboxRepository;
import com.proctor.model.service.InboxService;
import com.proctor.model.entity.Quiz;
import com.proctor.model.entity.Result;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.ExamViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;

public class ExamResultScreen implements Screen {
    private final Result result;
    private final ExamSession session;
    private final ExamService examService;
    private final AuthService authService;
    private final Screen returnScreen;
    private final InboxService inboxService;
    private String bannerMessage = "";

    public ExamResultScreen(Result result, ExamSession session, ExamService examService, AuthService authService) {
        this(result, session, examService, authService, new InboxService(new InboxRepository(), new UserRepository()));
    }

    public ExamResultScreen(Result result, ExamSession session, ExamService examService, AuthService authService, InboxService inboxService) {
        this.result = result;
        this.session = session;
        this.examService = examService;
        this.authService = authService;
        this.returnScreen = null;
        this.inboxService = inboxService != null ? inboxService : new InboxService(new InboxRepository(), new UserRepository());
    }

    public ExamResultScreen(Result result, Screen returnScreen) {
        this.result = result;
        this.session = null;
        this.examService = null;
        this.authService = null;
        this.returnScreen = returnScreen;
        this.inboxService = new InboxService(new InboxRepository(), new UserRepository());
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
            if ("r".equalsIgnoreCase(k.key())) {
                if (result != null && result.isPassed()) {
                    bannerMessage = TuiHelper.yellow("● Retakes cannot be requested for assessments that have been passed.");
                    return ScreenResult.stay(this);
                }
                if (result != null && result.getAssessmentType() == AssessmentType.EXAM) {
                    bannerMessage = TuiHelper.yellow("● Exam makeup requests require a justification reason. Please submit via the Exams screen.");
                    return ScreenResult.stay(this);
                }
                if (session != null && session.getAttempt() != null && session.getAttempt().getStatus() == AttemptStatus.AUTO_SUBMITTED) {
                    Quiz q = session.getQuiz();
                    if (q != null && q.getCreatedBy() != null) {
                        try {
                            inboxService.sendQuizRetakeRequest(result.getStudentId(), q.getCreatedBy(), q.getId(), q.getTitle());
                            bannerMessage = TuiHelper.green("✔ Quiz retake request sent to teacher's inbox!");
                        } catch (ValidationException e) {
                            bannerMessage = TuiHelper.yellow("● " + e.getMessage());
                        }
                    } else {
                        bannerMessage = TuiHelper.red("✖ Teacher for this quiz was not found.");
                    }
                } else {
                    bannerMessage = TuiHelper.yellow("● Retakes can only be requested if timer expired.");
                }
                return ScreenResult.stay(this);
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
        StringBuilder sb = new StringBuilder(ExamViews.renderExamResult(result, session, returnScreen != null));
        if (session != null && session.getAttempt() != null && session.getAttempt().getStatus() == AttemptStatus.AUTO_SUBMITTED) {
            if (result != null && !result.isPassed() && result.getAssessmentType() == AssessmentType.QUIZ) {
                sb.append("\n  ").append(TuiHelper.yellow("● Timer expired. Press [r] to request a retake from your teacher.")).append("\n");
            }
        }
        if (!bannerMessage.isBlank()) {
            sb.append("\n  ").append(bannerMessage).append("\n");
        }
        return sb.toString();
    }
}