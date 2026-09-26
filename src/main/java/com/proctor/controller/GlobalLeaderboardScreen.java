package com.proctor.controller;

import com.proctor.model.service.AuthService;
import com.proctor.model.service.ExamService;
import com.proctor.model.entity.LeaderboardEntry;
import com.proctor.model.service.PortalService;
import com.proctor.util.KeyUtil;
import com.proctor.util.ListNavigationHelper;
import com.proctor.util.MouseUtil;
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
        if (MouseUtil.isWheelUp(msg)) {
            if (!leaderboard.isEmpty() && selectedIndex > 0) {
                selectedIndex--;
            }
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isWheelDown(msg)) {
            if (!leaderboard.isEmpty() && selectedIndex < leaderboard.size() - 1) {
                selectedIndex++;
            }
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isLeftClick(msg)) {
            int line = MouseUtil.getLineIndex(msg);
            int col = MouseUtil.getColInLine(msg);

            int tabLine = MouseUtil.findTabBarLine(view());
            if (tabLine != -1 && line == tabLine) {
                int clickedTab = MouseUtil.getClickedTabIndex(col, "Quizzes", "Exams", "Speed Quizzes");
                if (clickedTab >= 0) {
                    Mode targetMode = switch (clickedTab) {
                        case 0 -> Mode.QUIZ;
                        case 1 -> Mode.EXAM;
                        case 2 -> Mode.SPEED;
                        default -> mode;
                    };
                    if (targetMode != mode) {
                        mode = targetMode;
                        selectedIndex = 0;
                        refreshLeaderboard();
                    }
                }
                return ScreenResult.stay(this);
            }

            int clickedIdx = ListNavigationHelper.getClickedItemIndex(line, MouseUtil.findTableStartLine(view()), leaderboard.size(), selectedIndex, TuiHelper.PAGE_SIZE);
            if (clickedIdx != -1) {
                selectedIndex = clickedIdx;
                return ScreenResult.stay(this);
            }

            int pagLine = MouseUtil.findPaginationLine(view());
            if (pagLine != -1 && line == pagLine && !leaderboard.isEmpty()) {
                selectedIndex = ListNavigationHelper.handlePaginationClick(col, selectedIndex, leaderboard.size(), TuiHelper.PAGE_SIZE);
                return ScreenResult.stay(this);
            }

            String hintAction = MouseUtil.getClickedHintAction(view(), line, col);
            if (hintAction != null) {
                if ("Esc".equalsIgnoreCase(hintAction)) {
                    if (returnScreen != null) {
                        return ScreenResult.navigate(returnScreen);
                    }
                    return ScreenResult.navigate(new StudentDashboardScreen(authService, examService, portalService));
                }
            }

            return ScreenResult.stay(this);
        }

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
                selectedIndex = ListNavigationHelper.prevPage(selectedIndex, TuiHelper.PAGE_SIZE);
            } else if (KeyUtil.isRight(k)) {
                selectedIndex = ListNavigationHelper.nextPage(selectedIndex, leaderboard.size(), TuiHelper.PAGE_SIZE);
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
