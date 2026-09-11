package com.proctor.controller;

import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.enums.Role;
import com.proctor.model.entity.Subject;
import com.proctor.model.service.SubjectService;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.SubjectViews;
import com.proctor.model.service.UserService;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TeacherAssignmentScreen implements Screen {
    private final Subject subject;
    private final SubjectService subjectService;
    private final UserService userService;
    private final AuthService authService;

    private List<User> allTeachers;
    private final Set<Integer> assignedTeacherIds = new HashSet<>();
    private int selectedIndex = 0;
    private String bannerMessage = "";

    public TeacherAssignmentScreen(Subject subject, SubjectService subjectService, UserService userService, AuthService authService) {
        this.subject = subject;
        this.subjectService = subjectService;
        this.userService = userService;
        this.authService = authService;
        refreshAssignments();
    }

    private void refreshAssignments() {
        this.allTeachers = userService.getUsers(null, Role.TEACHER);
        this.assignedTeacherIds.clear();
        List<User> assigned = subjectService.getAssignedTeachers(subject.getId());
        for (User u : assigned) {
            assignedTeacherIds.add(u.getId());
        }
        if (allTeachers.isEmpty()) {
            selectedIndex = 0;
        } else if (selectedIndex >= allTeachers.size()) {
            selectedIndex = allTeachers.size() - 1;
        }
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new SubjectListScreen(subjectService, userService, authService));
            }

            if (KeyUtil.isUp(k)) {
                if (!allTeachers.isEmpty()) {
                    selectedIndex = (selectedIndex - 1 + allTeachers.size()) % allTeachers.size();
                }
            } else if (KeyUtil.isDown(k)) {
                if (!allTeachers.isEmpty()) {
                    selectedIndex = (selectedIndex + 1) % allTeachers.size();
                }
            } else if (KeyUtil.isLeft(k)) {
                if (!allTeachers.isEmpty()) {
                    int pageSize = TuiHelper.PAGE_SIZE;
                    int currentPage = selectedIndex / pageSize;
                    if (currentPage > 0) {
                        selectedIndex = (currentPage - 1) * pageSize;
                    }
                }
            } else if (KeyUtil.isRight(k)) {
                if (!allTeachers.isEmpty()) {
                    int pageSize = TuiHelper.PAGE_SIZE;
                    int totalPages = Math.max(1, (int) Math.ceil((double) allTeachers.size() / pageSize));
                    int currentPage = selectedIndex / pageSize;
                    if (currentPage < totalPages - 1) {
                        selectedIndex = Math.min(allTeachers.size() - 1, (currentPage + 1) * pageSize);
                    }
                }
            } else if (KeyUtil.isEnter(k) || " ".equals(k.key())) {
                toggleSelectedTeacher();
            }
        }
        return ScreenResult.stay(this);
    }

    private void toggleSelectedTeacher() {
        if (allTeachers.isEmpty()) return;

        User teacher = allTeachers.get(selectedIndex);
        if (assignedTeacherIds.contains(teacher.getId())) {
            subjectService.unassignTeacher(teacher.getId(), subject.getId());
            assignedTeacherIds.remove(teacher.getId());
            bannerMessage = TuiHelper.yellow("Unassigned " + teacher.getFullName() + " from " + subject.getCode());
        } else {
            subjectService.assignTeacher(teacher.getId(), subject.getId());
            assignedTeacherIds.add(teacher.getId());
            bannerMessage = TuiHelper.green("Assigned " + teacher.getFullName() + " to " + subject.getCode());
        }
    }

    @Override
    public String view() {
        return SubjectViews.renderTeacherAssignment(subject, allTeachers, assignedTeacherIds, selectedIndex, bannerMessage);
    }
}