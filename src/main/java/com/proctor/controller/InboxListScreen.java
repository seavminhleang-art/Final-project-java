package com.proctor.controller;

import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.entity.InboxMessage;
import com.proctor.model.service.InboxService;
import com.proctor.util.KeyUtil;
import com.proctor.util.ListNavigationHelper;
import com.proctor.util.MouseUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.InboxViews;
import com.proctor.model.service.UserService;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

import java.util.ArrayList;
import java.util.List;

public class InboxListScreen implements Screen {

    private final InboxService inboxService;
    private final UserService userService;
    private final AuthService authService;
    private final Screen returnDashboardScreen;

    private List<InboxMessage> allMessages = new ArrayList<>();
    private List<InboxMessage> messages = new ArrayList<>();
    private int selectedIndex = 0;
    private int unreadCount = 0;
    private int actionRequiredCount = 0;
    private int filterIndex = 0;
    private final StringBuilder searchBuffer = new StringBuilder();
    private boolean searchMode = false;
    private String bannerMessage = "";

    private boolean showDeleteModal = false;
    private boolean deleteConfirmFocused = false;

    private static final String[] FILTERS = {"ALL", "UNREAD", "ACTIONABLE"};

    public InboxListScreen(InboxService inboxService, UserService userService, AuthService authService, Screen returnDashboardScreen) {
        this.inboxService = inboxService;
        this.userService = userService;
        this.authService = authService;
        this.returnDashboardScreen = returnDashboardScreen;
        refreshMessages();
    }

    private int getCurrentUserId() {
        return Session.getCurrentUser().map(User::getId).orElse(-1);
    }

    private void refreshMessages() {
        int userId = getCurrentUserId();
        if (userId != -1) {
            this.allMessages = inboxService.getInbox(userId);
            this.unreadCount = inboxService.getUnreadCount(userId);
        }
        applyFilters();
    }

    private void applyFilters() {
        String filter = FILTERS[filterIndex];
        String search = searchBuffer.toString().trim().toLowerCase();

        this.messages = allMessages.stream().filter(m -> {
            if ("UNREAD".equals(filter) && m.isRead()) {
                return false;
            }
            if ("ACTIONABLE".equals(filter) && !m.isActionable()) {
                return false;
            }
            if (!search.isEmpty()) {
                boolean matchTitle = m.getTitle() != null && m.getTitle().toLowerCase().contains(search);
                boolean matchSender = m.getSenderName() != null && m.getSenderName().toLowerCase().contains(search);
                boolean matchBody = m.getBody() != null && m.getBody().toLowerCase().contains(search);
                if (!matchTitle && !matchSender && !matchBody) {
                    return false;
                }
            }
            return true;
        }).toList();

        selectedIndex = ListNavigationHelper.clampIndex(selectedIndex, messages.size());
    }

    @Override
    public ScreenResult update(Message msg) {
        if (MouseUtil.isWheelUp(msg) || MouseUtil.isWheelDown(msg)) {
            selectedIndex = ListNavigationHelper.handleWheel(msg, selectedIndex, messages.size());
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isLeftClick(msg)) {
            if (showDeleteModal) {
                int action = ListNavigationHelper.handleConfirmationClick(msg, view(), "Delete", "Cancel");
                if (action == 0 && !messages.isEmpty()) {
                    InboxMessage target = messages.get(selectedIndex);
                    inboxService.deleteMessage(target.getId());
                    bannerMessage = TuiHelper.green("✔ Message deleted.");
                    showDeleteModal = false;
                    refreshMessages();
                } else if (action >= 0) {
                    showDeleteModal = false;
                }
                return ScreenResult.stay(this);
            }

            int line = MouseUtil.getLineIndex(msg);
            int col = MouseUtil.getColInLine(msg);

            int tabLine = MouseUtil.findTabBarLine(view());
            if (tabLine != -1 && line == tabLine) {
                int clickedTab = MouseUtil.getClickedTabIndex(col, "All", "Unread", "Actionable");
                if (clickedTab >= 0 && clickedTab < 3 && clickedTab != filterIndex) {
                    filterIndex = clickedTab;
                    selectedIndex = 0;
                    applyFilters();
                }
                return ScreenResult.stay(this);
            }

            int clickedIdx = ListNavigationHelper.getClickedItemIndex(line, MouseUtil.findTableStartLine(view()), messages.size(), selectedIndex, TuiHelper.PAGE_SIZE);
            if (clickedIdx != -1) {
                if (selectedIndex == clickedIdx) {
                    InboxMessage target = messages.get(selectedIndex);
                    inboxService.markAsRead(target.getId());
                    refreshMessages();
                    return ScreenResult.navigate(new InboxDetailScreen(target, inboxService, userService, authService, this));
                }
                selectedIndex = clickedIdx;
                return ScreenResult.stay(this);
            }

            int paginationLine = MouseUtil.findPaginationLine(view());
            if (paginationLine != -1 && line == paginationLine && !messages.isEmpty()) {
                selectedIndex = ListNavigationHelper.handlePaginationClick(col, selectedIndex, messages.size(), TuiHelper.PAGE_SIZE);
                return ScreenResult.stay(this);
            }

            String hintAction = MouseUtil.getClickedHintAction(view(), line, col);
            if (hintAction != null) {
                if ("Esc".equals(hintAction)) {
                    return ScreenResult.navigate(returnDashboardScreen);
                } else if ("m".equals(hintAction)) {
                    int userId = getCurrentUserId();
                    if (userId != -1) {
                        inboxService.markAllAsRead(userId);
                        bannerMessage = TuiHelper.green("✔ All messages marked as read.");
                        refreshMessages();
                    }
                    return ScreenResult.stay(this);
                } else if ("d".equals(hintAction) && !messages.isEmpty()) {
                    showDeleteModal = true;
                    deleteConfirmFocused = false;
                    return ScreenResult.stay(this);
                }
            }

            return ScreenResult.stay(this);
        }

        if (msg instanceof KeyPressMessage k) {
            if (showDeleteModal) {
                if (KeyUtil.isLeft(k) || KeyUtil.isRight(k)) {
                    deleteConfirmFocused = !deleteConfirmFocused;
                    return ScreenResult.stay(this);
                }
                if (KeyUtil.isEnter(k)) {
                    if (deleteConfirmFocused && !messages.isEmpty()) {
                        InboxMessage target = messages.get(selectedIndex);
                        inboxService.deleteMessage(target.getId());
                        bannerMessage = TuiHelper.green("✔ Message deleted.");
                        showDeleteModal = false;
                        refreshMessages();
                        return ScreenResult.stay(this);
                    } else {
                        showDeleteModal = false;
                        return ScreenResult.stay(this);
                    }
                }
                if (KeyUtil.isEsc(k)) {
                    showDeleteModal = false;
                    return ScreenResult.stay(this);
                }
                return ScreenResult.stay(this);
            }

            if (searchMode) {
                searchMode = ListNavigationHelper.handleSearchKey(k, searchBuffer, this::applyFilters);
                return ScreenResult.stay(this);
            }

            bannerMessage = "";

            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(returnDashboardScreen);
            }

            if (KeyUtil.isUp(k)) {
                selectedIndex = ListNavigationHelper.adjustIndex(selectedIndex, -1, messages.size());
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isDown(k)) {
                selectedIndex = ListNavigationHelper.adjustIndex(selectedIndex, 1, messages.size());
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isLeft(k)) {
                selectedIndex = ListNavigationHelper.prevPage(selectedIndex, TuiHelper.PAGE_SIZE);
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isRight(k)) {
                selectedIndex = ListNavigationHelper.nextPage(selectedIndex, messages.size(), TuiHelper.PAGE_SIZE);
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isTab(k) || "f".equalsIgnoreCase(k.key())) {
                filterIndex = (filterIndex + 1) % FILTERS.length;
                selectedIndex = 0;
                applyFilters();
                return ScreenResult.stay(this);
            }

            if ("/".equals(k.key())) {
                searchMode = true;
                searchBuffer.setLength(0);
                applyFilters();
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isEnter(k)) {
                if (!messages.isEmpty()) {
                    InboxMessage target = messages.get(selectedIndex);
                    return ScreenResult.navigate(new InboxDetailScreen(target, inboxService, userService, authService, this));
                }
            }

            if ("d".equalsIgnoreCase(k.key()) || KeyUtil.isDelete(k)) {
                if (!messages.isEmpty()) {
                    showDeleteModal = true;
                    deleteConfirmFocused = false;
                }
                return ScreenResult.stay(this);
            }

            if ("m".equalsIgnoreCase(k.key())) {
                int userId = getCurrentUserId();
                if (userId != -1) {
                    inboxService.markAllAsRead(userId);
                    bannerMessage = TuiHelper.green("✔ All messages marked as read.");
                    refreshMessages();
                }
                return ScreenResult.stay(this);
            }
        }
        return ScreenResult.stay(this);
    }

    @Override
    public String view() {
        if (showDeleteModal) {
            return TuiHelper.confirmationModal(
                    "DELETE MESSAGE",
                    "Are you sure you want to delete this message?",
                    "This message will be permanently removed from your inbox.",
                    "Delete",
                    "Cancel",
                    deleteConfirmFocused
            );
        }
        if (!searchMode) {
            refreshMessages();
        }
        return InboxViews.renderInboxList(
                messages,
                selectedIndex,
                unreadCount,
                FILTERS[filterIndex],
                searchBuffer.toString(),
                searchMode,
                bannerMessage
        );
    }
}