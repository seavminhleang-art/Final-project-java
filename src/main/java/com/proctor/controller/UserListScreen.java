package com.proctor.controller;

import com.proctor.exception.ValidationException;
import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.enums.Role;
import com.proctor.util.KeyUtil;
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

    private List<User> users;
    private int selectedIndex = 0;
    private Role filterRole = null;
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
        if (users.isEmpty()) {
            selectedIndex = 0;
        } else if (selectedIndex >= users.size()) {
            selectedIndex = users.size() - 1;
        }
    }

    @Override
    public ScreenResult update(Message msg) {
        if (MouseUtil.isWheelUp(msg)) {
            if (!users.isEmpty() && selectedIndex > 0) {
                selectedIndex--;
            }
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isWheelDown(msg)) {
            if (!users.isEmpty() && selectedIndex < users.size() - 1) {
                selectedIndex++;
            }
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

            int itemsStartLine = MouseUtil.findTableStartLine(view());
            int pageSize = TuiHelper.PAGE_SIZE;
            int totalPages = Math.max(1, (int) Math.ceil((double) users.size() / pageSize));
            int currentPage = selectedIndex / pageSize;
            int startRow = currentPage * pageSize;
            int endRow = Math.min(users.size(), startRow + pageSize);
            int displayedRows = endRow - startRow;

            if (itemsStartLine != -1 && line >= itemsStartLine && line < itemsStartLine + displayedRows * 2) {
                int clickedOffset = (line - itemsStartLine) / 2;
                int targetIdx = startRow + clickedOffset;
                if (targetIdx < users.size()) {
                    if (selectedIndex == targetIdx) {
                        return ScreenResult.navigate(new UserFormScreen(userService, authService, users.get(selectedIndex)));
                    } else {
                        selectedIndex = targetIdx;
                    }
                }
                return ScreenResult.stay(this);
            }

            int pagLine = MouseUtil.findPaginationLine(view());
            if (pagLine != -1 && line == pagLine && !users.isEmpty()) {
                int action = MouseUtil.getClickedPaginationAction(col, currentPage, totalPages);
                if (action < 0) {
                    selectedIndex = (currentPage - 1) * pageSize;
                } else if (action > 0) {
                    selectedIndex = Math.min(users.size() - 1, (currentPage + 1) * pageSize);
                }
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
                if (KeyUtil.isEnter(k) || KeyUtil.isEsc(k)) {
                    searchMode = false;
                    refreshList();
                } else if (KeyUtil.isBackspace(k)) {
                    if (!searchBuffer.isEmpty()) {
                        searchBuffer.deleteCharAt(searchBuffer.length() - 1);
                        refreshList();
                    }
                } else if (k.type() == KeyType.KeyRunes && k.runes() != null) {
                    for (char c : k.runes()) {
                        if (!Character.isISOControl(c)) searchBuffer.append(c);
                    }
                    refreshList();
                } else if (k.key() != null && k.key().length() == 1 && !Character.isISOControl(k.key().charAt(0))) {
                    searchBuffer.append(k.key());
                    refreshList();
                }
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new AdminDashboardScreen(authService));
            } else if (KeyUtil.isUp(k)) {
                if (!users.isEmpty()) {
                    selectedIndex = (selectedIndex - 1 + users.size()) % users.size();
                }
            } else if (KeyUtil.isDown(k)) {
                if (!users.isEmpty()) {
                    selectedIndex = (selectedIndex + 1) % users.size();
                }
            } else if (KeyUtil.isLeft(k)) {
                if (!users.isEmpty()) {
                    int pageSize = TuiHelper.PAGE_SIZE;
                    int currentPage = selectedIndex / pageSize;
                    if (currentPage > 0) {
                        selectedIndex = (currentPage - 1) * pageSize;
                    }
                }
            } else if (KeyUtil.isRight(k)) {
                if (!users.isEmpty()) {
                    int pageSize = TuiHelper.PAGE_SIZE;
                    int totalPages = Math.max(1, (int) Math.ceil((double) users.size() / pageSize));
                    int currentPage = selectedIndex / pageSize;
                    if (currentPage < totalPages - 1) {
                        selectedIndex = Math.min(users.size() - 1, (currentPage + 1) * pageSize);
                    }
                }
            } else if ("n".equalsIgnoreCase(k.key())) {
                return ScreenResult.navigate(new UserFormScreen(userService, authService, null));
            } else if ("e".equalsIgnoreCase(k.key()) || KeyUtil.isEnter(k)) {
                if (!users.isEmpty()) {
                    return ScreenResult.navigate(new UserFormScreen(userService, authService, users.get(selectedIndex)));
                }
            } else if ("t".equalsIgnoreCase(k.key()) || KeyUtil.isSpace(k)) {
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
            } else if (KeyUtil.isTab(k) || "f".equalsIgnoreCase(k.key())) {
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