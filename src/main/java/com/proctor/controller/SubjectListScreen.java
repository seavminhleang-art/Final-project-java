package com.proctor.controller;

import com.proctor.model.entity.User;
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

    private List<Subject> subjects;
    private int selectedIndex = 0;
    private final StringBuilder searchBuffer = new StringBuilder();
    private boolean searchMode = false;
    private String bannerMessage = "";

    public SubjectListScreen(SubjectService subjectService, UserService userService, AuthService authService) {
        this.subjectService = subjectService;
        this.userService = userService;
        this.authService = authService;
        refreshList();
    }

    private void refreshList() {
        this.subjects = subjectService.getSubjects(searchBuffer.toString());
        if (subjects.isEmpty()) {
            selectedIndex = 0;
        } else if (selectedIndex >= subjects.size()) {
            selectedIndex = subjects.size() - 1;
        }
    }

    @Override
    public ScreenResult update(Message msg) {
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
                    int pageSize = 5;
                    int currentPage = selectedIndex / pageSize;
                    if (currentPage > 0) {
                        selectedIndex = (currentPage - 1) * pageSize;
                    }
                }
            } else if (KeyUtil.isRight(k)) {
                if (!subjects.isEmpty()) {
                    int pageSize = 5;
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
            } else if ("t".equalsIgnoreCase(k.key()) || KeyUtil.isSpace(k)) {
                if (!subjects.isEmpty()) {
                    Subject s = subjects.get(selectedIndex);
                    subjectService.toggleSubjectStatus(s.getId());
                    bannerMessage = TuiHelper.green("Toggled status for " + s.getCode());
                    refreshList();
                }
            } else if ("a".equalsIgnoreCase(k.key())) {
                if (!subjects.isEmpty()) {
                    return ScreenResult.navigate(new TeacherAssignmentScreen(subjects.get(selectedIndex), subjectService, userService, authService));
                }
            } else if ("/".equals(k.key())) {
                searchMode = true;
                bannerMessage = "";
            }
        }
        return ScreenResult.stay(this);
    }

    @Override
    public String view() {
        return SubjectViews.renderSubjectList(subjects, selectedIndex, searchBuffer.toString(), searchMode, bannerMessage);
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}