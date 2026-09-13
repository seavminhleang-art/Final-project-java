package com.proctor.controller;

import com.proctor.model.service.AuthService;
import com.proctor.model.service.ExamService;
import com.proctor.model.entity.LeaderboardEntry;
import com.proctor.model.service.PortalService;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.PortalViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;

import java.util.List;

public class GlobalLeaderboardScreen implements Screen {

    public enum Mode {
        QUIZ,
        EXAM,
        SPEED
    }

    private final PortalService portalService;
    private final ExamService examService;
    private final AuthService authService;
    private final Screen returnScreen;

    private Mode mode = Mode.QUIZ;
    private List<LeaderboardEntry> leaderboard;
    private int selectedIndex = 0;

    public GlobalLeaderboardScreen(PortalService portalService, ExamService examService, AuthService authService) {
        this(portalService, examService, authService, null);
    }

    public GlobalLeaderboardScreen(PortalService portalService, ExamService examService, AuthService authService, Screen returnScreen) {
        this.portalService = portalService;
        this.examService = examService;
        this.authService = authService;
        this.returnScreen = returnScreen;
        refreshLeaderboard();
    }

    private void refreshLeaderboard() {
        switch (mode) {
            case QUIZ -> this.leaderboard = portalService.getQuizLeaderboard();
            case EXAM -> this.leaderboard = portalService.getExamLeaderboard();
            case SPEED -> this.leaderboard = portalService.getSpeedQuizLeaderboard();
        }
        if (leaderboard.isEmpty()) {
            selectedIndex = 0;
        } else if (selectedIndex >= leaderboard.size()) {
            selectedIndex = leaderboard.size() - 1;
        }
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
            if (KeyUtil.isEsc(k)) {
                if (returnScreen != null) {
                    return ScreenResult.navigate(returnScreen);
                }
                return ScreenResult.navigate(new StudentDashboardScreen(authService, examService, portalService));
            } else if (KeyUtil.isUp(k)) {
                if (!leaderboard.isEmpty()) {
                    selectedIndex = (selectedIndex - 1 + leaderboard.size()) % leaderboard.size();
                }
            } else if (KeyUtil.isDown(k)) {
                if (!leaderboard.isEmpty()) {
                    selectedIndex = (selectedIndex + 1) % leaderboard.size();
                }
            } else if (KeyUtil.isLeft(k)) {
                if (!leaderboard.isEmpty()) {
                    int pageSize = TuiHelper.PAGE_SIZE;
                    int currentPage = selectedIndex / pageSize;
                    if (currentPage > 0) {
                        selectedIndex = (currentPage - 1) * pageSize;
                    }
                }
            } else if (KeyUtil.isRight(k)) {
                if (!leaderboard.isEmpty()) {
                    int pageSize = TuiHelper.PAGE_SIZE;
                    int totalPages = Math.max(1, (int) Math.ceil((double) leaderboard.size() / pageSize));
                    int currentPage = selectedIndex / pageSize;
                    if (currentPage < totalPages - 1) {
                        selectedIndex = Math.min(leaderboard.size() - 1, (currentPage + 1) * pageSize);
                    }
                }
            } else if (KeyUtil.isTab(k)) {
                mode = switch (mode) {
                    case QUIZ -> Mode.EXAM;
                    case EXAM -> Mode.SPEED;
                    case SPEED -> Mode.QUIZ;
                };
                selectedIndex = 0;
                refreshLeaderboard();
                return ScreenResult.stay(this);
            } else if ("r".equalsIgnoreCase(k.key())) {
                refreshLeaderboard();
                return ScreenResult.stay(this);
            }
        }
        return ScreenResult.stay(this);
    }

    public Mode getMode() {
        return mode;
    }

    @Override
    public String view() {
        return PortalViews.renderGlobalLeaderboard(leaderboard, selectedIndex, mode);
    }
}