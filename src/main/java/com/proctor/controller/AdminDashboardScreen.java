package com.proctor.controller;

import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.enums.AssessmentType;
import com.proctor.model.repository.InboxRepository;
import com.proctor.model.repository.QuestionRepository;
import com.proctor.model.repository.QuizRepository;
import com.proctor.model.repository.ReportRepository;
import com.proctor.model.repository.SubjectRepository;
import com.proctor.model.repository.UserRepository;
import com.proctor.model.service.AuthService;
import com.proctor.model.service.InboxService;
import com.proctor.model.service.QuestionService;
import com.proctor.model.service.QuizService;
import com.proctor.model.service.ReportService;
import com.proctor.model.service.SubjectService;
import com.proctor.model.service.UserService;
import com.proctor.util.KeyUtil;
import com.proctor.util.MouseUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.DashboardViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;

public class AdminDashboardScreen implements Screen {
    private final AuthService authService;
    private final UserService userService;
    private final SubjectService subjectService;
    private final ReportService reportService;
    private final InboxService inboxService;
    private final QuizService quizService;
    private final QuestionService questionService;
    private int selectedIndex = 0;
    private boolean showQuitModal = false;
    private boolean quitConfirmFocused = false;

    public AdminDashboardScreen(AuthService authService) {
        this(authService, new UserService(new UserRepository()), new SubjectService(new SubjectRepository()),
                new ReportService(new ReportRepository()), new InboxService(new InboxRepository(), new UserRepository()),
                new QuizService(new QuizRepository()), new QuestionService(new QuestionRepository()));
    }

    public AdminDashboardScreen(AuthService authService, UserService userService) {
        this(authService, userService, new SubjectService(new SubjectRepository()),
                new ReportService(new ReportRepository()), new InboxService(new InboxRepository(), new UserRepository()),
                new QuizService(new QuizRepository()), new QuestionService(new QuestionRepository()));
    }

    public AdminDashboardScreen(AuthService authService, UserService userService, SubjectService subjectService) {
        this(authService, userService, subjectService,
                new ReportService(new ReportRepository()), new InboxService(new InboxRepository(), new UserRepository()),
                new QuizService(new QuizRepository()), new QuestionService(new QuestionRepository()));
    }

    public AdminDashboardScreen(AuthService authService, UserService userService, SubjectService subjectService, ReportService reportService) {
        this(authService, userService, subjectService, reportService,
                new InboxService(new InboxRepository(), new UserRepository()),
                new QuizService(new QuizRepository()), new QuestionService(new QuestionRepository()));
    }

    public AdminDashboardScreen(AuthService authService, UserService userService, SubjectService subjectService,
                                ReportService reportService, InboxService inboxService) {
        this(authService, userService, subjectService, reportService, inboxService,
                new QuizService(new QuizRepository()), new QuestionService(new QuestionRepository()));
    }

    public AdminDashboardScreen(AuthService authService, UserService userService, SubjectService subjectService,
                                ReportService reportService, InboxService inboxService,
                                QuizService quizService, QuestionService questionService) {
        this.authService = authService;
        this.userService = userService;
        this.subjectService = subjectService;
        this.reportService = reportService;
        this.inboxService = inboxService;
        this.quizService = quizService;
        this.questionService = questionService;
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
        String inboxLabel = unread > 0 ? "7. Inbox (" + unread + " unread)" : "7. Inbox";
        return new String[]{
                "1. User Management",
                "2. Subject Management",
                "3. Quiz Management",
                "4. Exam Management",
                "5. Speed Quiz Management",
                "6. System Reports",
                inboxLabel,
                "8. Logout",
                "9. Exit"
        };
    }

    @Override
    public ScreenResult update(Message msg) {
        String[] menuItems = getMenuItems();

        if (MouseUtil.isWheelUp(msg)) {
            selectedIndex = (selectedIndex - 1 + menuItems.length) % menuItems.length;
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isWheelDown(msg)) {
            selectedIndex = (selectedIndex + 1) % menuItems.length;
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isLeftClick(msg)) {
            if (showQuitModal) {
                int line = MouseUtil.getLineIndex(msg);
                int col = MouseUtil.getColInLine(msg);
                int btnLine = MouseUtil.findButtonRowLine(view());
                if (btnLine != -1 && line >= btnLine && line <= btnLine + 2) {
                    int btn = MouseUtil.getClickedButtonIndex(col, "Quit Application", "Return to App");
                    if (btn == 0) {
                        return ScreenResult.quit();
                    } else if (btn == 1) {
                        showQuitModal = false;
                        return ScreenResult.stay(this);
                    }
                } else if (btnLine != -1 && (line < btnLine - 6 || line > btnLine + 4)) {
                    showQuitModal = false;
                    return ScreenResult.stay(this);
                }
                return ScreenResult.stay(this);
            }

            int line = MouseUtil.getLineIndex(msg);
            int col = MouseUtil.getColInLine(msg);
            int itemIndex = MouseUtil.findMenuItemIndex(view(), line);
            if (itemIndex >= 0 && itemIndex < menuItems.length && col >= 0 && col < 110) {
                selectedIndex = itemIndex;
                return handleSelection();
            }
            return ScreenResult.stay(this);
        }

        if (msg instanceof KeyPressMessage k) {
            if (showQuitModal) {
                if (KeyUtil.isLeft(k) || KeyUtil.isRight(k)) {
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
                return ScreenResult.navigate(new UserListScreen(userService, authService));
            }
            case 1 -> {
                return ScreenResult.navigate(new SubjectListScreen(subjectService, userService, authService));
            }
            case 2 -> {
                return ScreenResult.navigate(new QuizListScreen(quizService, questionService, subjectService, authService, AssessmentType.QUIZ));
            }
            case 3 -> {
                return ScreenResult.navigate(new QuizListScreen(quizService, questionService, subjectService, authService, AssessmentType.EXAM));
            }
            case 4 -> {
                return ScreenResult.navigate(new QuizListScreen(quizService, questionService, subjectService, authService, AssessmentType.SPEED));
            }
            case 5 -> {
                return ScreenResult.navigate(new ReportMenuScreen(reportService, authService));
            }
            case 6 -> {
                return ScreenResult.navigate(new InboxListScreen(inboxService, userService, authService, this));
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
            return TuiHelper.quitConfirmationModal(quitConfirmFocused);
        }
        User user = Session.getCurrentUser().orElse(null);
        String name = user != null ? user.getFullName() : "Admin";
        String identifier = user != null ? user.getDisplayIdentifier() : "admin";
        return DashboardViews.renderDashboard("PROCTOR - ADMIN DASHBOARD", name, identifier, getMenuItems(), selectedIndex);
    }
}