package com.proctor.controller;

import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.repository.UserRepository;
import com.proctor.model.service.AuthService;
import com.proctor.model.repository.InboxRepository;
import com.proctor.model.service.InboxService;
import com.proctor.model.repository.ReportRepository;
import com.proctor.model.service.ReportService;
import com.proctor.model.repository.SubjectRepository;
import com.proctor.model.service.SubjectService;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.DashboardViews;
import com.proctor.model.service.UserService;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;

public class AdminDashboardScreen implements Screen {
    private final AuthService authService;
    private final UserService userService;
    private final SubjectService subjectService;
    private final ReportService reportService;
    private final InboxService inboxService;
    private int selectedIndex = 0;
    private boolean showQuitModal = false;
    private boolean quitConfirmFocused = false;

    public AdminDashboardScreen(AuthService authService) {
        this(authService, new UserService(new UserRepository()), new SubjectService(new SubjectRepository()),
                new ReportService(new ReportRepository()), new InboxService(new InboxRepository(), new UserRepository()));
    }

    public AdminDashboardScreen(AuthService authService, UserService userService) {
        this(authService, userService, new SubjectService(new SubjectRepository()),
                new ReportService(new ReportRepository()), new InboxService(new InboxRepository(), new UserRepository()));
    }

    public AdminDashboardScreen(AuthService authService, UserService userService, SubjectService subjectService) {
        this(authService, userService, subjectService,
                new ReportService(new ReportRepository()), new InboxService(new InboxRepository(), new UserRepository()));
    }

    public AdminDashboardScreen(AuthService authService, UserService userService, SubjectService subjectService, ReportService reportService) {
        this(authService, userService, subjectService, reportService,
                new InboxService(new InboxRepository(), new UserRepository()));
    }

    public AdminDashboardScreen(AuthService authService, UserService userService, SubjectService subjectService,
                                ReportService reportService, InboxService inboxService) {
        this.authService = authService;
        this.userService = userService;
        this.subjectService = subjectService;
        this.reportService = reportService;
        this.inboxService = inboxService;
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
        String inboxLabel = unread > 0 ? "4. Inbox (" + unread + " unread)" : "4. Inbox";
        return new String[]{
                "1. User Management",
                "2. Subject Management",
                "3. System Reports",
                inboxLabel,
                "5. Logout",
                "6. Exit"
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
                return ScreenResult.navigate(new UserListScreen(userService, authService));
            }
            case 1 -> {
                return ScreenResult.navigate(new SubjectListScreen(subjectService, userService, authService));
            }
            case 2 -> {
                return ScreenResult.navigate(new ReportMenuScreen(reportService, authService));
            }
            case 3 -> {
                return ScreenResult.navigate(new InboxListScreen(inboxService, userService, authService, this));
            }
            case 4 -> {
                authService.logout();
                return ScreenResult.navigate(new LoginScreen(authService));
            }
            case 5 -> {
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
        String name = user != null ? user.getFullName() : "Admin";
        String identifier = user != null ? user.getDisplayIdentifier() : "admin";
        return DashboardViews.renderDashboard("PROCTOR - ADMIN DASHBOARD", name, identifier, getMenuItems(), selectedIndex);
    }
}