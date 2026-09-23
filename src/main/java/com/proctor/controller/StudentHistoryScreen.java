package com.proctor.controller;

import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.service.ExamService;
import com.proctor.model.service.PortalService;
import com.proctor.model.enums.AssessmentType;
import com.proctor.model.entity.Result;
import com.proctor.util.KeyUtil;
import com.proctor.util.ListNavigationHelper;
import com.proctor.util.MouseUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.ExamViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

import java.text.SimpleDateFormat;
import java.util.List;

public class StudentHistoryScreen implements Screen {
    private final PortalService portalService;
    private final ExamService examService;
    private final AuthService authService;

    private List<Result> allHistory = new java.util.ArrayList<>();
    private List<Result> historyList = new java.util.ArrayList<>();
    private int selectedIndex = 0;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private final StringBuilder searchBuffer = new StringBuilder();
    private boolean searchMode = false;
    private int statusFilterIndex = 0;
    private static final String[] STATUS_FILTERS = {"ALL", "PASSED", "FAILED", "PENDING"};

    public StudentHistoryScreen(PortalService portalService, ExamService examService, AuthService authService) {
        this.portalService = portalService;
        this.examService = examService;
        this.authService = authService;
        refreshList();
    }

    private void refreshList() {
        User student = Session.getCurrentUser().orElse(null);
        int studentId = (student != null && student.getId() != null) ? student.getId() : 0;
        this.allHistory = portalService.getStudentHistory(studentId);
        applyFilters();
    }

    private void applyFilters() {
        String filter = STATUS_FILTERS[statusFilterIndex];
        String search = searchBuffer.toString().trim().toLowerCase();

        this.historyList = allHistory.stream().filter(r -> {
            if ("SPEED".equals(filter) && r.getAssessmentType() != AssessmentType.SPEED) {
                return false;
            }
            if ("PASSED".equals(filter) && (r.getAssessmentType() == AssessmentType.SPEED || r.isPendingReview() || !r.isPassed())) {
                return false;
            }
            if ("FAILED".equals(filter) && (r.getAssessmentType() == AssessmentType.SPEED || r.isPendingReview() || r.isPassed())) {
                return false;
            }
            if ("PENDING".equals(filter) && !r.isPendingReview()) {
                return false;
            }
            if (!search.isEmpty()) {
                boolean matchTitle = r.getQuizTitle() != null && r.getQuizTitle().toLowerCase().contains(search);
                boolean matchType = r.getAssessmentType() != null && r.getAssessmentType().name().toLowerCase().contains(search);
                if (!matchTitle && !matchType) {
                    return false;
                }
            }
            return true;
        }).toList();

        selectedIndex = ListNavigationHelper.clampIndex(selectedIndex, historyList.size());
    }

    @Override
    public ScreenResult update(Message msg) {
        if (MouseUtil.isWheelUp(msg) || MouseUtil.isWheelDown(msg)) {
            selectedIndex = ListNavigationHelper.handleWheel(msg, selectedIndex, historyList.size());
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isLeftClick(msg)) {
            int line = MouseUtil.getLineIndex(msg);
            int col = MouseUtil.getColInLine(msg);

            int tabLine = MouseUtil.findTabBarLine(view());
            if (tabLine != -1 && line == tabLine) {
                int clickedTab = MouseUtil.getClickedTabIndex(col, "All", "Passed", "Failed", "Pending");
                if (clickedTab >= 0 && clickedTab < STATUS_FILTERS.length && clickedTab != statusFilterIndex) {
                    statusFilterIndex = clickedTab;
                    selectedIndex = 0;
                    applyFilters();
                }
                return ScreenResult.stay(this);
            }

            int clickedIdx = ListNavigationHelper.getClickedItemIndex(line, MouseUtil.findTableStartLine(view()), historyList.size(), selectedIndex, TuiHelper.PAGE_SIZE);
            if (clickedIdx != -1) {
                if (selectedIndex == clickedIdx) {
                    Result r = historyList.get(selectedIndex);
                    if (r.getAssessmentType() == AssessmentType.SPEED) {
                        return ScreenResult.navigate(new SpeedQuizResultScreen(r, this, examService, authService));
                    }
                    return ScreenResult.navigate(new ExamResultScreen(r, this));
                }
                selectedIndex = clickedIdx;
                return ScreenResult.stay(this);
            }

            int pagLine = MouseUtil.findPaginationLine(view());
            if (pagLine != -1 && line == pagLine && !historyList.isEmpty()) {
                selectedIndex = ListNavigationHelper.handlePaginationClick(col, selectedIndex, historyList.size(), TuiHelper.PAGE_SIZE);
                return ScreenResult.stay(this);
            }

            String hintAction = MouseUtil.getClickedHintAction(view(), line, col);
            if (hintAction != null) {
                if ("Esc".equals(hintAction)) {
                    return ScreenResult.navigate(new StudentDashboardScreen(authService, examService));
                }
            }

            return ScreenResult.stay(this);
        }

        if (msg instanceof KeyPressMessage k) {
            if (searchMode) {
                searchMode = ListNavigationHelper.handleSearchKey(k, searchBuffer, this::applyFilters);
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new StudentDashboardScreen(authService, examService, portalService));
            } else if (KeyUtil.isUp(k)) {
                selectedIndex = ListNavigationHelper.adjustIndex(selectedIndex, -1, historyList.size());
            } else if (KeyUtil.isDown(k)) {
                selectedIndex = ListNavigationHelper.adjustIndex(selectedIndex, 1, historyList.size());
            } else if (KeyUtil.isLeft(k)) {
                selectedIndex = ListNavigationHelper.prevPage(selectedIndex, TuiHelper.PAGE_SIZE);
            } else if (KeyUtil.isRight(k)) {
                selectedIndex = ListNavigationHelper.nextPage(selectedIndex, historyList.size(), TuiHelper.PAGE_SIZE);
            } else if (KeyUtil.isTab(k)) {
                statusFilterIndex = (statusFilterIndex + 1) % STATUS_FILTERS.length;
                selectedIndex = 0;
                applyFilters();
            } else if ("/".equals(k.key())) {
                searchMode = true;
                searchBuffer.setLength(0);
                applyFilters();
            } else if (KeyUtil.isEnter(k)) {
                if (!historyList.isEmpty()) {
                    Result r = historyList.get(selectedIndex);
                    if (r.getAssessmentType() == AssessmentType.SPEED) {
                        return ScreenResult.navigate(new SpeedQuizResultScreen(r, this, examService, authService));
                    }
                    return ScreenResult.navigate(new ExamResultScreen(r, this));
                }
            }
        }
        return ScreenResult.stay(this);
    }

    @Override
    public String view() {
        return ExamViews.renderStudentHistory(historyList, selectedIndex, dateFormat,
                STATUS_FILTERS[statusFilterIndex], searchBuffer.toString(), searchMode);
    }

    private String truncate(String text, int max) {
        return TuiHelper.truncate(text, max);
    }
}