package com.proctor.controller;

import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.repository.AttemptRepository;
import com.proctor.model.repository.ResultRepository;
import com.proctor.model.service.ExamService;
import com.proctor.model.repository.QuestionRepository;
import com.proctor.model.service.QuestionService;
import com.proctor.model.repository.QuizRepository;
import com.proctor.model.service.QuizService;
import com.proctor.model.repository.ReportRepository;
import com.proctor.model.service.ReportService;
import com.proctor.model.repository.SubjectRepository;
import com.proctor.model.service.SubjectService;
import com.proctor.model.repository.UserRepository;
import com.proctor.model.repository.InboxRepository;
import com.proctor.model.service.InboxService;
import com.proctor.model.service.UserService;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.DashboardViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;

public class TeacherDashboardScreen implements Screen {
    private final AuthService authService;
    private final QuestionService questionService;
    private final SubjectService subjectService;
    private final QuizService quizService;
    private final ReportService reportService;
    private final InboxService inboxService;
    private final UserService userService;
    private int selectedIndex = 0;
    private boolean showQuitModal = false;
    private boolean quitConfirmFocused = false;

    public TeacherDashboardScreen(AuthService authService) {
        this(authService,
             new QuestionService(new QuestionRepository()),
             new SubjectService(new SubjectRepository()),
             new QuizService(new QuizRepository()),
             new ReportService(new ReportRepository()),
             new InboxService(new InboxRepository(), new UserRepository()),
             new UserService(new UserRepository()));
    }

    public TeacherDashboardScreen(AuthService authService, QuestionService questionService, SubjectService subjectService) {
        this(authService, questionService, subjectService, new QuizService(new QuizRepository()),
             new ReportService(new ReportRepository()),
             new InboxService(new InboxRepository(), new UserRepository()),
             new UserService(new UserRepository()));
    }

    public TeacherDashboardScreen(AuthService authService, QuestionService questionService, SubjectService subjectService, QuizService quizService) {
        this(authService, questionService, subjectService, quizService, new ReportService(new ReportRepository()),
             new InboxService(new InboxRepository(), new UserRepository()),
             new UserService(new UserRepository()));
    }

    public TeacherDashboardScreen(AuthService authService, QuestionService questionService, SubjectService subjectService, QuizService quizService, ReportService reportService) {
        this(authService, questionService, subjectService, quizService, reportService,
             new InboxService(new InboxRepository(), new UserRepository()),
             new UserService(new UserRepository()));
    }

    public TeacherDashboardScreen(AuthService authService, QuestionService questionService, SubjectService subjectService,
                                  QuizService quizService, ReportService reportService, InboxService inboxService, UserService userService) {
        this.authService = authService;
        this.questionService = questionService;
        this.subjectService = subjectService;
        this.quizService = quizService;
        this.reportService = reportService;
        this.inboxService = inboxService;
        this.userService = userService;
    }

    private int getUnreadCount() {
        try {
            return Session.getCurrentUser()
                    .filter(u -> u.getId() != null)
                    .map(u -> inboxService.getUnreadCount(u.getId()))
                    .orElse(0);
        } catch (Exception e) {
            return 0;
        }
    }

    private String[] getMenuItems() {
        int unread = getUnreadCount();
        String inboxLabel = unread > 0 ? "6. Inbox (" + unread + " unread)" : "6. Inbox";
        return new String[]{
                "1. Quizzes",
                "2. Exams",
                "3. Question Bank",
                "4. Submissions & Grading",
                "5. Performance Reports",
                inboxLabel,
                "7. Change Password",
                "8. Logout",
                "9. Exit"
        };
    }

    @Override
    public ScreenResult update(Message msg) {
        String[] menuItems = getMenuItems();
        if (msg instanceof KeyPressMessage k) {
            if (showQuitModal) {
                if (KeyUtil.isLeft(k) || KeyUtil.isRight(k) || KeyUtil.isTab(k)) {
                    quitConfirmFocused = !quitConfirmFocused;
                    return ScreenResult.stay(this);
                }
                if (KeyUtil.isEnter(k)) {
                    if (quitConfirmFocused) {
                        return ScreenResult.quit();
                    } else {
                        showQuitModal = false;
                        return ScreenResult.stay(this);
                    }
                }
                if (KeyUtil.isEsc(k)) {
                    showQuitModal = false;
                    return ScreenResult.stay(this);
                }
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isUp(k)) {
                selectedIndex = (selectedIndex - 1 + menuItems.length) % menuItems.length;
            } else if (KeyUtil.isDown(k)) {
                selectedIndex = (selectedIndex + 1) % menuItems.length;
            } else if (KeyUtil.isEnter(k)) {
                return handleSelection();
            } else if (KeyUtil.isEsc(k)) {
                showQuitModal = true;
                quitConfirmFocused = false;
                return ScreenResult.stay(this);
            }
        }
        return ScreenResult.stay(this);
    }

    private ScreenResult handleSelection() {
        switch (selectedIndex) {
            case 0 -> {
                return ScreenResult.navigate(new QuizListScreen(quizService, questionService, subjectService, authService, com.proctor.model.enums.AssessmentType.QUIZ));
            }
            case 1 -> {
                return ScreenResult.navigate(new QuizListScreen(quizService, questionService, subjectService, authService, com.proctor.model.enums.AssessmentType.EXAM));
            }
            case 2 -> {
                return ScreenResult.navigate(new QuestionBankScreen(questionService, subjectService, authService));
            }
            case 3 -> {
                ExamService examService = new ExamService(new QuizRepository(), new AttemptRepository(), new ResultRepository());
                return ScreenResult.navigate(new TeacherSubmissionScreen(null, examService, quizService, questionService, subjectService, authService));
            }
            case 4 -> {
                return ScreenResult.navigate(new ReportMenuScreen(reportService, authService));
            }
            case 5 -> {
                return ScreenResult.navigate(new InboxListScreen(inboxService, userService, authService, this));
            }
            case 6 -> {
                return ScreenResult.navigate(new ChangePasswordScreen(authService, userService, this));
            }
            case 7 -> {
                authService.logout();
                return ScreenResult.navigate(new LoginScreen(authService));
            }
            case 8 -> {
                showQuitModal = true;
                quitConfirmFocused = false;
                return ScreenResult.stay(this);
            }
        }
        return ScreenResult.stay(this);
    }

    @Override
    public String view() {
        if (showQuitModal) {
            return TuiHelper.confirmationModal(
                    "QUIT APPLICATION",
                    "Are you sure you want to quit?",
                    "Any unsaved progress will be lost.",
                    "Quit",
                    "Cancel",
                    quitConfirmFocused
            );
        }
        User user = Session.getCurrentUser().orElse(null);
        String name = user != null ? user.getFullName() : "Teacher";
        String identifier = user != null ? user.getDisplayIdentifier() : "teacher";
        return DashboardViews.renderDashboard("PROCTOR - TEACHER DASHBOARD", name, identifier, getMenuItems(), selectedIndex);
    }
}