package com.proctor.controller;

import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.entity.InboxMessage;
import com.proctor.model.service.InboxService;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.InboxViews;
import com.proctor.model.service.UserService;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;

import java.util.ArrayList;
import java.util.List;

public class InboxListScreen implements Screen {

    private final InboxService inboxService;
    private final UserService userService;
    private final AuthService authService;
    private final Screen returnDashboardScreen;

    private List<InboxMessage> messages = new ArrayList<>();
    private int selectedIndex = 0;
    private int unreadCount = 0;

    private boolean showDeleteModal = false;
    private boolean deleteConfirmFocused = false;
    private String bannerMessage = "";

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
            this.messages = inboxService.getInbox(userId);
            this.unreadCount = inboxService.getUnreadCount(userId);
            if (selectedIndex >= messages.size()) {
                selectedIndex = Math.max(0, messages.size() - 1);
            }
        }
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
            if (showDeleteModal) {
                if (KeyUtil.isLeft(k) || KeyUtil.isRight(k) || KeyUtil.isTab(k)) {
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

            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(returnDashboardScreen);
            }

            if (KeyUtil.isUp(k)) {
                if (!messages.isEmpty()) {
                    selectedIndex = (selectedIndex - 1 + messages.size()) % messages.size();
                }
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isDown(k)) {
                if (!messages.isEmpty()) {
                    selectedIndex = (selectedIndex + 1) % messages.size();
                }
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isLeft(k)) {
                if (!messages.isEmpty()) {
                    int pageSize = TuiHelper.PAGE_SIZE;
                    int currentPage = selectedIndex / pageSize;
                    if (currentPage > 0) {
                        selectedIndex = (currentPage - 1) * pageSize;
                    }
                }
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isRight(k)) {
                if (!messages.isEmpty()) {
                    int pageSize = TuiHelper.PAGE_SIZE;
                    int totalPages = Math.max(1, (int) Math.ceil((double) messages.size() / pageSize));
                    int currentPage = selectedIndex / pageSize;
                    if (currentPage < totalPages - 1) {
                        selectedIndex = Math.min(messages.size() - 1, (currentPage + 1) * pageSize);
                    }
                }
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isEnter(k)) {
                if (!messages.isEmpty()) {
                    InboxMessage target = messages.get(selectedIndex);
                    return ScreenResult.navigate(new InboxDetailScreen(target, inboxService, userService, authService, this));
                }
            }

            if ("d".equalsIgnoreCase(k.key())) {
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
        refreshMessages();
        return InboxViews.renderInboxList(
                messages,
                selectedIndex,
                unreadCount,
                bannerMessage,
                showDeleteModal,
                deleteConfirmFocused
        );
    }
}