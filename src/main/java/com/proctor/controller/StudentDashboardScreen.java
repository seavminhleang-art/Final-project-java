package com.proctor.controller;

import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.repository.AttemptRepository;
import com.proctor.model.service.ExamService;
import com.proctor.model.repository.PortalRepository;
import com.proctor.model.service.PortalService;
import com.proctor.model.repository.QuizRepository;
import com.proctor.model.repository.ResultRepository;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.DashboardViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;

import com.proctor.model.repository.UserRepository;
import com.proctor.model.repository.InboxRepository;
import com.proctor.model.service.InboxService;
import com.proctor.model.service.UserService;

public class StudentDashboardScreen implements Screen {
    private final AuthService authService;
    private final ExamService examService;
    private final PortalService portalService;
    private final InboxService inboxService;
    private final UserService userService;
    private int selectedIndex = 0;
    private boolean showQuitModal = false;
    private boolean quitConfirmFocused = false;

    public StudentDashboardScreen(AuthService authService) {
        this(authService,
             new ExamService(new QuizRepository(), new AttemptRepository(), new ResultRepository()),
             new PortalService(new PortalRepository()),
             new InboxService(new InboxRepository(), new UserRepository()),
             new UserService(new UserRepository()));
    }

    public StudentDashboardScreen(AuthService authService, ExamService examService) {
        this(authService, examService, new PortalService(new PortalRepository()),
             new InboxService(new InboxRepository(), new UserRepository()),
             new UserService(new UserRepository()));
    }

    public StudentDashboardScreen(AuthService authService, ExamService examService, PortalService portalService) {
        this(authService, examService, portalService,
             new InboxService(new InboxRepository(), new UserRepository()),
             new UserService(new UserRepository()));
    }

    public StudentDashboardScreen(AuthService authService, ExamService examService, PortalService portalService,
                                  InboxService inboxService, UserService userService) {
        this.authService = authService;
        this.examService = examService;
        this.portalService = portalService;
        this.inboxService = inboxService;
        this.userService = userService;
    }

    private int getUnreadCount() {
        return Session.getCurrentUser().map(u -> inboxService.getUnreadCount(u.getId())).orElse(0);
    }

    private String[] getMenuItems() {
        int unread = getUnreadCount();
        String inboxLabel = unread > 0 ? "5. Inbox (" + unread + " unread)" : "5. Inbox";
        return new String[]{
                "1. Available Quizzes",
                "2. Available Exams",
                "3. My Assessment History",
                "4. Global Leaderboard",
                inboxLabel,
                "6. Change Password",
                "7. Logout",
                "8. Exit"
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

            if ("i".equalsIgnoreCase(k.key())) {
                return ScreenResult.navigate(new InboxListScreen(inboxService, userService, authService, this));
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
                return ScreenResult.navigate(new AvailableQuizzesScreen(examService, authService, com.proctor.model.enums.AssessmentType.QUIZ, inboxService, this));
            }
            case 1 -> {
                return ScreenResult.navigate(new AvailableQuizzesScreen(examService, authService, com.proctor.model.enums.AssessmentType.EXAM, inboxService, this));
            }
            case 2 -> {
                return ScreenResult.navigate(new StudentHistoryScreen(portalService, examService, authService));
            }
            case 3 -> {
                return ScreenResult.navigate(new GlobalLeaderboardScreen(portalService, examService, authService, this));
            }
            case 4 -> {
                return ScreenResult.navigate(new InboxListScreen(inboxService, userService, authService, this));
            }
            case 5 -> {
                return ScreenResult.navigate(new ChangePasswordScreen(authService, userService, this));
            }
            case 6 -> {
                authService.logout();
                return ScreenResult.navigate(new LoginScreen(authService));
            }
            case 7 -> {
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
        String name = user != null ? user.getFullName() : "Student";
        String identifier = user != null ? user.getDisplayIdentifier() : "student";
        return DashboardViews.renderDashboard("PROCTOR - STUDENT PORTAL", name, identifier, getMenuItems(), selectedIndex);
    }
}