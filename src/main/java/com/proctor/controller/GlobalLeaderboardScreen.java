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
    private final PortalService portalService;
    private final ExamService examService;
    private final AuthService authService;
    private final Screen returnScreen;

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
        this.leaderboard = portalService.getGlobalLeaderboard();
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
            } else if ("r".equalsIgnoreCase(k.key())) {
                refreshLeaderboard();
                return ScreenResult.stay(this);
            }
        }
        return ScreenResult.stay(this);
    }

    @Override
    public String view() {
        return PortalViews.renderGlobalLeaderboard(leaderboard, selectedIndex);
    }
}