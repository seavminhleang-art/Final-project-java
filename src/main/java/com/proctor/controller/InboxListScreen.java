package com.proctor.controller;

import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.entity.InboxMessage;
import com.proctor.model.service.InboxService;
import com.proctor.util.KeyUtil;
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
    private final StringBuilder searchBuffer = new StringBuilder();
    private boolean searchMode = false;
    private int filterIndex = 0;
    private static final String[] FILTERS = {"ALL", "UNREAD", "ACTIONABLE"};

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
            this.allMessages = inboxService.getInbox(userId);
            this.unreadCount = inboxService.getUnreadCount(userId);
            applyFilters();
        }
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

        if (messages.isEmpty()) {
            selectedIndex = 0;
        } else if (selectedIndex >= messages.size()) {
            selectedIndex = Math.max(0, messages.size() - 1);
        }
    }

    @Override
    public ScreenResult update(Message msg) {
        if (MouseUtil.isWheelUp(msg)) {
            if (!messages.isEmpty() && selectedIndex > 0) {
                selectedIndex--;
            }
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isWheelDown(msg)) {
            if (!messages.isEmpty() && selectedIndex < messages.size() - 1) {
                selectedIndex++;
            }
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isLeftClick(msg)) {
            if (showDeleteModal) {
                int line = MouseUtil.getLineIndex(msg);
                int col = MouseUtil.getColInLine(msg);
                if (line >= 17 && line <= 19) {
                    if (col >= 0 && col < 22) {
                        if (!messages.isEmpty()) {
                            InboxMessage target = messages.get(selectedIndex);
                            inboxService.deleteMessage(target.getId());
                            bannerMessage = TuiHelper.green("✔ Message deleted.");
                            showDeleteModal = false;
                            refreshMessages();
                        }
                    } else if (col >= 26 && col < 48) {
                        showDeleteModal = false;
                    }
                } else if (line < 11 || line > 21) {
                    showDeleteModal = false;
                }
                return ScreenResult.stay(this);
            }

            int line = MouseUtil.getLineIndex(msg);
            int col = MouseUtil.getColInLine(msg);

            if (line == 11) {
                int clickedTab = (col < 20) ? 0 : (col < 35 ? 1 : 2);
                if (clickedTab != filterIndex) {
                    filterIndex = clickedTab;
                    selectedIndex = 0;
                    applyFilters();
                }
                return ScreenResult.stay(this);
            }

            boolean hasSearchLine = searchMode || !searchBuffer.isEmpty();
            int headerLine = hasSearchLine ? 15 : 13;
            int itemsStartLine = headerLine + 3;

            int pageSize = TuiHelper.PAGE_SIZE;
            int totalPages = Math.max(1, (int) Math.ceil((double) messages.size() / pageSize));
            int currentPage = selectedIndex / pageSize;
            int startRow = currentPage * pageSize;
            int endRow = Math.min(messages.size(), startRow + pageSize);
            int displayedRows = endRow - startRow;

            if (line >= itemsStartLine && line < itemsStartLine + displayedRows * 2) {
                int clickedOffset = (line - itemsStartLine) / 2;
                int targetIdx = startRow + clickedOffset;
                if (targetIdx < messages.size()) {
                    if (selectedIndex == targetIdx) {
                        InboxMessage target = messages.get(selectedIndex);
                        inboxService.markAsRead(target.getId());
                        refreshMessages();
                        return ScreenResult.navigate(new InboxDetailScreen(target, inboxService, userService, authService, this));
                    } else {
                        selectedIndex = targetIdx;
                    }
                }
                return ScreenResult.stay(this);
            }

            int paginationLine = itemsStartLine + displayedRows * 2 + 2;
            if (line == paginationLine && !messages.isEmpty()) {
                if (col < 20 && currentPage > 0) {
                    selectedIndex = (currentPage - 1) * pageSize;
                } else if (col > 35 && currentPage < totalPages - 1) {
                    selectedIndex = Math.min(messages.size() - 1, (currentPage + 1) * pageSize);
                }
                return ScreenResult.stay(this);
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

            bannerMessage = "";

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