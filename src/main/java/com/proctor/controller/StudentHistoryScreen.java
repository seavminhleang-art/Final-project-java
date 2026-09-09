package com.proctor.controller;

import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.service.ExamService;
import com.proctor.model.service.PortalService;
import com.proctor.model.entity.Result;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.ExamViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;

import java.text.SimpleDateFormat;
import java.util.List;

public class StudentHistoryScreen implements Screen {
    private final PortalService portalService;
    private final ExamService examService;
    private final AuthService authService;

    private List<Result> historyList;
    private int selectedIndex = 0;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public StudentHistoryScreen(PortalService portalService, ExamService examService, AuthService authService) {
        this.portalService = portalService;
        this.examService = examService;
        this.authService = authService;
        refreshHistory();
    }

    private void refreshHistory() {
        User student = Session.getCurrentUser().orElse(null);
        int studentId = student != null ? student.getId() : 0;
        this.historyList = portalService.getStudentHistory(studentId);
        if (historyList.isEmpty()) {
            selectedIndex = 0;
        } else if (selectedIndex >= historyList.size()) {
            selectedIndex = historyList.size() - 1;
        }
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new StudentDashboardScreen(authService, examService, portalService));
            } else if (KeyUtil.isUp(k)) {
                if (!historyList.isEmpty()) {
                    selectedIndex = (selectedIndex - 1 + historyList.size()) % historyList.size();
                }
            } else if (KeyUtil.isDown(k)) {
                if (!historyList.isEmpty()) {
                    selectedIndex = (selectedIndex + 1) % historyList.size();
                }
            } else if (KeyUtil.isEnter(k)) {
                if (!historyList.isEmpty()) {
                    Result r = historyList.get(selectedIndex);
                    return ScreenResult.navigate(new ExamResultScreen(r, this));
                }
            }
        }
        return ScreenResult.stay(this);
    }

    @Override
    public String view() {
        return ExamViews.renderStudentHistory(historyList, selectedIndex, dateFormat);
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}