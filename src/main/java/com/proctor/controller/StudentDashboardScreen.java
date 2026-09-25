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
import com.proctor.util.MouseUtil;
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
                "3. Speed Quizzes",
                "4. My Assessment History",
                "5. Global Leaderboard",
                inboxLabel,
                "7. Change Password",
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

            int digit = KeyUtil.getDigit(k);
            if (digit >= 1 && digit <= menuItems.length) {
                selectedIndex = digit - 1;
                return handleSelection();
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
                return ScreenResult.navigate(new AvailableQuizzesScreen(examService, authService, com.proctor.model.enums.AssessmentType.SPEED, inboxService, this));
            }
            case 3 -> {
                return ScreenResult.navigate(new StudentHistoryScreen(portalService, examService, authService));
            }
            case 4 -> {
                return ScreenResult.navigate(new GlobalLeaderboardScreen(portalService, examService, authService, this));
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
            return TuiHelper.quitConfirmationModal(quitConfirmFocused);
        }
        User user = Session.getCurrentUser().orElse(null);
        String name = user != null ? user.getFullName() : "Student";
        String identifier = user != null ? user.getDisplayIdentifier() : "student";
        return DashboardViews.renderDashboard("PROCTOR - STUDENT PORTAL", name, identifier, getMenuItems(), selectedIndex);
    }
}