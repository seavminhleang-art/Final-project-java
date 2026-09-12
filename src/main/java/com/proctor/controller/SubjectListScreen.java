package com.proctor.controller;

import com.proctor.exception.ValidationException;
import com.proctor.model.service.AuthService;
import com.proctor.model.entity.Subject;
import com.proctor.model.service.SubjectService;
import com.proctor.util.KeyUtil;
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

        if (subjects.isEmpty()) {
            selectedIndex = 0;
        } else if (selectedIndex >= subjects.size()) {
            selectedIndex = subjects.size() - 1;
        }
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
            if (confirmingDelete) {
                if (KeyUtil.isLeft(k) || KeyUtil.isRight(k) || KeyUtil.isTab(k)) {
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
                return ScreenResult.navigate(new AdminDashboardScreen(authService, userService, subjectService));
            } else if (KeyUtil.isUp(k)) {
                if (!subjects.isEmpty()) {
                    selectedIndex = (selectedIndex - 1 + subjects.size()) % subjects.size();
                }
            } else if (KeyUtil.isDown(k)) {
                if (!subjects.isEmpty()) {
                    selectedIndex = (selectedIndex + 1) % subjects.size();
                }
            } else if (KeyUtil.isLeft(k)) {
                if (!subjects.isEmpty()) {
                    int pageSize = TuiHelper.PAGE_SIZE;
                    int currentPage = selectedIndex / pageSize;
                    if (currentPage > 0) {
                        selectedIndex = (currentPage - 1) * pageSize;
                    }
                }
            } else if (KeyUtil.isRight(k)) {
                if (!subjects.isEmpty()) {
                    int pageSize = TuiHelper.PAGE_SIZE;
                    int totalPages = Math.max(1, (int) Math.ceil((double) subjects.size() / pageSize));
                    int currentPage = selectedIndex / pageSize;
                    if (currentPage < totalPages - 1) {
                        selectedIndex = Math.min(subjects.size() - 1, (currentPage + 1) * pageSize);
                    }
                }
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
            } else if ("f".equalsIgnoreCase(k.key())) {
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