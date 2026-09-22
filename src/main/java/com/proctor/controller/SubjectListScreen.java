package com.proctor.controller;

import com.proctor.exception.ValidationException;
import com.proctor.model.service.AuthService;
import com.proctor.model.entity.Subject;
import com.proctor.model.service.SubjectService;
import com.proctor.util.KeyUtil;
import com.proctor.util.ListNavigationHelper;
import com.proctor.util.MouseUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.SubjectViews;
import com.proctor.model.service.UserService;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

import java.util.List;

public class SubjectListScreen implements Screen {
    private final SubjectService subjectService;
    private final UserService userService;
    private final AuthService authService;

    private List<Subject> allSubjects = new java.util.ArrayList<>();
    private List<Subject> subjects = new java.util.ArrayList<>();
    private int selectedIndex = 0;
    private final StringBuilder searchBuffer = new StringBuilder();
    private boolean searchMode = false;
    private String bannerMessage = "";
    private int statusFilterIndex = 0;
    private static final String[] STATUS_FILTERS = {"ALL", "ENABLED", "DISABLED"};

    private boolean confirmingDelete = false;
    private boolean confirmDeleteFocused = false;
    private Subject pendingDeleteSubject = null;

    public SubjectListScreen(SubjectService subjectService, UserService userService, AuthService authService) {
        this.subjectService = subjectService;
        this.userService = userService;
        this.authService = authService;
        refreshList();
    }

    private void refreshList() {
        this.allSubjects = subjectService.getSubjects(searchBuffer.toString());
        applyFilters();
    }

    private void applyFilters() {
        String filter = STATUS_FILTERS[statusFilterIndex];
        this.subjects = allSubjects.stream().filter(s -> {
            if ("ENABLED".equals(filter) && !s.isEnabled()) {
                return false;
            }
            if ("DISABLED".equals(filter) && s.isEnabled()) {
                return false;
            }
            return true;
        }).toList();

        selectedIndex = ListNavigationHelper.clampIndex(selectedIndex, subjects.size());
    }

    @Override
    public ScreenResult update(Message msg) {
        if (MouseUtil.isWheelUp(msg) || MouseUtil.isWheelDown(msg)) {
            selectedIndex = ListNavigationHelper.handleWheel(msg, selectedIndex, subjects.size());
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isLeftClick(msg)) {
            if (confirmingDelete) {
                int action = ListNavigationHelper.handleConfirmationClick(msg, view(), "Delete", "Cancel");
                if (action == 0) {
                    executeDelete();
                }
                if (action >= 0) {
                    confirmingDelete = false;
                    pendingDeleteSubject = null;
                }
                return ScreenResult.stay(this);
            }

            int line = MouseUtil.getLineIndex(msg);
            int col = MouseUtil.getColInLine(msg);

            int tabLine = MouseUtil.findTabBarLine(view());
            if (tabLine != -1 && line == tabLine) {
                int clickedTab = MouseUtil.getClickedTabIndex(col, "All", "Enabled", "Disabled");
                if (clickedTab >= 0 && clickedTab < STATUS_FILTERS.length && clickedTab != statusFilterIndex) {
                    statusFilterIndex = clickedTab;
                    selectedIndex = 0;
                    applyFilters();
                }
                return ScreenResult.stay(this);
            }

            int clickedIdx = ListNavigationHelper.getClickedItemIndex(line, MouseUtil.findTableStartLine(view()), subjects.size(), selectedIndex, TuiHelper.PAGE_SIZE);
            if (clickedIdx != -1) {
                if (selectedIndex == clickedIdx) {
                    return ScreenResult.navigate(new SubjectFormScreen(subjectService, userService, authService, subjects.get(selectedIndex)));
                }
                selectedIndex = clickedIdx;
                return ScreenResult.stay(this);
            }

            int pagLine = MouseUtil.findPaginationLine(view());
            if (pagLine != -1 && line == pagLine && !subjects.isEmpty()) {
                selectedIndex = ListNavigationHelper.handlePaginationClick(col, selectedIndex, subjects.size(), TuiHelper.PAGE_SIZE);
                return ScreenResult.stay(this);
            }

            String hintAction = MouseUtil.getClickedHintAction(view(), line, col);
            if (hintAction != null) {
                if ("Esc".equals(hintAction)) {
                    return ScreenResult.navigate(new AdminDashboardScreen(authService));
                } else if ("n".equals(hintAction)) {
                    return ScreenResult.navigate(new SubjectFormScreen(subjectService, userService, authService, null));
                }
            }

            return ScreenResult.stay(this);
        }

        if (msg instanceof KeyPressMessage k) {
            if (confirmingDelete) {
                if (KeyUtil.isLeft(k) || KeyUtil.isRight(k)) {
                    confirmDeleteFocused = !confirmDeleteFocused;
                    return ScreenResult.stay(this);
                }
                if (KeyUtil.isEnter(k)) {
                    if (confirmDeleteFocused) {
                        executeDelete();
                    }
                    confirmingDelete = false;
                    pendingDeleteSubject = null;
                    return ScreenResult.stay(this);
                }
                if (KeyUtil.isEsc(k)) {
                    confirmingDelete = false;
                    pendingDeleteSubject = null;
                    return ScreenResult.stay(this);
                }
                return ScreenResult.stay(this);
            }

            if (searchMode) {
                searchMode = ListNavigationHelper.handleSearchKey(k, searchBuffer, this::refreshList);
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new AdminDashboardScreen(authService, userService, subjectService));
            } else if (KeyUtil.isUp(k)) {
                selectedIndex = ListNavigationHelper.adjustIndex(selectedIndex, -1, subjects.size());
            } else if (KeyUtil.isDown(k)) {
                selectedIndex = ListNavigationHelper.adjustIndex(selectedIndex, 1, subjects.size());
            } else if (KeyUtil.isLeft(k)) {
                selectedIndex = ListNavigationHelper.prevPage(selectedIndex, TuiHelper.PAGE_SIZE);
            } else if (KeyUtil.isRight(k)) {
                selectedIndex = ListNavigationHelper.nextPage(selectedIndex, subjects.size(), TuiHelper.PAGE_SIZE);
            } else if ("n".equalsIgnoreCase(k.key())) {
                return ScreenResult.navigate(new SubjectFormScreen(subjectService, userService, authService, null));
            } else if ("e".equalsIgnoreCase(k.key()) || KeyUtil.isEnter(k)) {
                if (!subjects.isEmpty()) {
                    return ScreenResult.navigate(new SubjectFormScreen(subjectService, userService, authService, subjects.get(selectedIndex)));
                }
            } else if ("d".equalsIgnoreCase(k.key()) || KeyUtil.isDelete(k)) {
                if (!subjects.isEmpty()) {
                    initiateDelete();
                }
            } else if ("t".equalsIgnoreCase(k.key()) || KeyUtil.isSpace(k)) {
                if (!subjects.isEmpty()) {
                    Subject s = subjects.get(selectedIndex);
                    subjectService.toggleSubjectStatus(s.getId());
                    bannerMessage = TuiHelper.green("Toggled status for " + s.getCode());
                    refreshList();
                }
            } else if (KeyUtil.isTab(k) || "f".equalsIgnoreCase(k.key())) {
                statusFilterIndex = (statusFilterIndex + 1) % STATUS_FILTERS.length;
                selectedIndex = 0;
                applyFilters();
            } else if ("/".equals(k.key())) {
                searchMode = true;
                bannerMessage = "";
            }
        }
        return ScreenResult.stay(this);
    }

    private void initiateDelete() {
        pendingDeleteSubject = subjects.get(selectedIndex);
        confirmingDelete = true;
        confirmDeleteFocused = false;
    }

    private void executeDelete() {
        if (pendingDeleteSubject == null) return;
        try {
            subjectService.deleteSubject(pendingDeleteSubject.getId());
            bannerMessage = TuiHelper.green("✔ Successfully deleted subject: " + pendingDeleteSubject.getCode());
            refreshList();
        } catch (ValidationException e) {
            bannerMessage = TuiHelper.red("✖ " + e.getMessage());
        } catch (Exception e) {
            bannerMessage = TuiHelper.red("✖ Delete failed: " + e.getMessage());
        }
    }

    @Override
    public String view() {
        if (confirmingDelete && pendingDeleteSubject != null) {
            return TuiHelper.confirmationModal(
                    "DELETE SUBJECT",
                    "Are you sure you want to delete subject '" + pendingDeleteSubject.getCode() + "' (" + pendingDeleteSubject.getName() + ")?",
                    "This will permanently delete the subject and unlink it from any quizzes or questions.",
                    "Delete Subject",
                    "Cancel",
                    confirmDeleteFocused
            );
        }
        return SubjectViews.renderSubjectList(subjects, selectedIndex, STATUS_FILTERS[statusFilterIndex], searchBuffer.toString(), searchMode, bannerMessage);
    }
}