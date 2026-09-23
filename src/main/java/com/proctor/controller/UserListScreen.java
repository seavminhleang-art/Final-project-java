package com.proctor.controller;

import com.proctor.exception.ValidationException;
import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.enums.Role;
import com.proctor.util.KeyUtil;
import com.proctor.util.ListNavigationHelper;
import com.proctor.util.MouseUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.UserViews;
import com.proctor.model.service.UserService;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

import java.util.List;

public class UserListScreen implements Screen {
    private final UserService userService;
    private final AuthService authService;

    private Role filterRole = null;
    private List<User> users = new java.util.ArrayList<>();
    private int selectedIndex = 0;
    private final StringBuilder searchBuffer = new StringBuilder();
    private boolean searchMode = false;
    private String bannerMessage = "";

    public UserListScreen(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
        refreshList();
    }

    private void refreshList() {
        this.users = userService.getUsers(searchBuffer.toString(), filterRole);
        selectedIndex = ListNavigationHelper.clampIndex(selectedIndex, users.size());
    }

    @Override
    public ScreenResult update(Message msg) {
        if (MouseUtil.isWheelUp(msg) || MouseUtil.isWheelDown(msg)) {
            selectedIndex = ListNavigationHelper.handleWheel(msg, selectedIndex, users.size());
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isLeftClick(msg)) {
            int line = MouseUtil.getLineIndex(msg);
            int col = MouseUtil.getColInLine(msg);

            int tabLine = MouseUtil.findTabBarLine(view());
            if (tabLine != -1 && line == tabLine) {
                int clickedTab = MouseUtil.getClickedTabIndex(col, "All Roles", "Students", "Teachers", "Admins");
                if (clickedTab >= 0) {
                    Role targetRole = switch (clickedTab) {
                        case 1 -> Role.STUDENT;
                        case 2 -> Role.TEACHER;
                        case 3 -> Role.ADMIN;
                        default -> null;
                    };
                    if (targetRole != filterRole) {
                        filterRole = targetRole;
                        selectedIndex = 0;
                        refreshList();
                    }
                }
                return ScreenResult.stay(this);
            }

            int clickedIdx = ListNavigationHelper.getClickedItemIndex(line, MouseUtil.findTableStartLine(view()), users.size(), selectedIndex, TuiHelper.PAGE_SIZE);
            if (clickedIdx != -1) {
                if (selectedIndex == clickedIdx) {
                    return ScreenResult.navigate(new UserFormScreen(userService, authService, users.get(selectedIndex)));
                }
                selectedIndex = clickedIdx;
                return ScreenResult.stay(this);
            }

            int pagLine = MouseUtil.findPaginationLine(view());
            if (pagLine != -1 && line == pagLine && !users.isEmpty()) {
                selectedIndex = ListNavigationHelper.handlePaginationClick(col, selectedIndex, users.size(), TuiHelper.PAGE_SIZE);
                return ScreenResult.stay(this);
            }

            String hintAction = MouseUtil.getClickedHintAction(view(), line, col);
            if (hintAction != null) {
                if ("Esc".equals(hintAction)) {
                    return ScreenResult.navigate(new AdminDashboardScreen(authService));
                } else if ("n".equals(hintAction)) {
                    return ScreenResult.navigate(new UserFormScreen(userService, authService, null));
                }
            }

            return ScreenResult.stay(this);
        }

        if (msg instanceof KeyPressMessage k) {
            if (searchMode) {
                searchMode = ListNavigationHelper.handleSearchKey(k, searchBuffer, this::refreshList);
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new AdminDashboardScreen(authService));
            } else if (KeyUtil.isUp(k)) {
                selectedIndex = ListNavigationHelper.adjustIndex(selectedIndex, -1, users.size());
            } else if (KeyUtil.isDown(k)) {
                selectedIndex = ListNavigationHelper.adjustIndex(selectedIndex, 1, users.size());
            } else if (KeyUtil.isLeft(k)) {
                selectedIndex = ListNavigationHelper.prevPage(selectedIndex, TuiHelper.PAGE_SIZE);
            } else if (KeyUtil.isRight(k)) {
                selectedIndex = ListNavigationHelper.nextPage(selectedIndex, users.size(), TuiHelper.PAGE_SIZE);
            } else if ("n".equalsIgnoreCase(k.key())) {
                return ScreenResult.navigate(new UserFormScreen(userService, authService, null));
            } else if (KeyUtil.isEnter(k)) {
                if (!users.isEmpty()) {
                    return ScreenResult.navigate(new UserFormScreen(userService, authService, users.get(selectedIndex)));
                }
            } else if (KeyUtil.isSpace(k)) {
                if (!users.isEmpty()) {
                    User u = users.get(selectedIndex);
                    try {
                        userService.toggleUserStatus(u.getId());
                        u.setEnabled(!u.isEnabled());
                        bannerMessage = "Updated status for @" + u.getUsername() + " to " + (u.isEnabled() ? TuiHelper.green("Enabled") : TuiHelper.red("Disabled"));
                    } catch (ValidationException e) {
                        bannerMessage = TuiHelper.red("✖ " + e.getMessage());
                    }
                }
            } else if (KeyUtil.isTab(k)) {
                cycleFilterRole();
            } else if ("/".equals(k.key())) {
                searchMode = true;
                bannerMessage = "";
            }
        }
        return ScreenResult.stay(this);
    }

    private void cycleFilterRole() {
        if (filterRole == null) filterRole = Role.STUDENT;
        else if (filterRole == Role.STUDENT) filterRole = Role.TEACHER;
        else if (filterRole == Role.TEACHER) filterRole = Role.ADMIN;
        else filterRole = null;
        selectedIndex = 0;
        refreshList();
    }

    @Override
    public String view() {
        return UserViews.renderUserList(users, selectedIndex, filterRole, searchBuffer.toString(), searchMode, bannerMessage);
    }

    private String truncate(String text, int max) {
        return TuiHelper.truncate(text, max);
    }
}