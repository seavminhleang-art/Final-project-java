package com.proctor.controller;

import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.service.ExamService;
import com.proctor.model.service.PortalService;
import com.proctor.model.enums.AssessmentType;
import com.proctor.model.entity.Result;
import com.proctor.util.KeyUtil;
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
        refreshHistory();
    }

    private void refreshHistory() {
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

        if (historyList.isEmpty()) {
            selectedIndex = 0;
        } else if (selectedIndex >= historyList.size()) {
            selectedIndex = historyList.size() - 1;
        }
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
            if (searchMode) {
                if (KeyUtil.isEsc(k) || KeyUtil.isEnter(k)) {
                    searchMode = false;
                    applyFilters();
                } else if (KeyUtil.isBackspace(k)) {
                    if (!searchBuffer.isEmpty()) {
                        searchBuffer.deleteCharAt(searchBuffer.length() - 1);
                        applyFilters();
                    }
                } else if (k.type() == KeyType.KeyRunes && k.runes() != null) {
                    for (char c : k.runes()) {
                        if (!Character.isISOControl(c)) searchBuffer.append(c);
                    }
                    applyFilters();
                } else if (k.key() != null && k.key().length() == 1 && !Character.isISOControl(k.key().charAt(0))) {
                    searchBuffer.append(k.key());
                    applyFilters();
                }
                return ScreenResult.stay(this);
            }

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
            } else if (KeyUtil.isLeft(k)) {
                if (!historyList.isEmpty()) {
                    int pageSize = TuiHelper.PAGE_SIZE;
                    int currentPage = selectedIndex / pageSize;
                    if (currentPage > 0) {
                        selectedIndex = (currentPage - 1) * pageSize;
                    }
                }
            } else if (KeyUtil.isRight(k)) {
                if (!historyList.isEmpty()) {
                    int pageSize = TuiHelper.PAGE_SIZE;
                    int totalPages = Math.max(1, (int) Math.ceil((double) historyList.size() / pageSize));
                    int currentPage = selectedIndex / pageSize;
                    if (currentPage < totalPages - 1) {
                        selectedIndex = Math.min(historyList.size() - 1, (currentPage + 1) * pageSize);
                    }
                }
            } else if (KeyUtil.isTab(k) || "f".equalsIgnoreCase(k.key())) {
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
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, Math.max(0, max - 3)) + "...";
    }
}