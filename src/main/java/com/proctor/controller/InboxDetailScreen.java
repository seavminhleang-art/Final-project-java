package com.proctor.controller;

import com.proctor.model.service.AuthService;
import com.proctor.exception.ValidationException;
import com.proctor.model.entity.InboxMessage;
import com.proctor.model.enums.InboxMessageType;
import com.proctor.model.enums.InboxStatus;
import com.proctor.model.service.InboxService;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.InboxViews;
import com.proctor.model.service.UserService;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;

public class InboxDetailScreen implements Screen {

    private final InboxMessage message;
    private final InboxService inboxService;
    private final UserService userService;
    private final AuthService authService;
    private final Screen returnScreen;

    private boolean showDeleteModal = false;
    private boolean deleteConfirmFocused = false;

    private int focusedActionIndex = 0;

    private String errorMessage = "";
    private String bannerMessage = "";

    public InboxDetailScreen(InboxMessage message, InboxService inboxService, UserService userService,
                             AuthService authService, Screen returnScreen) {
        this.message = message;
        this.inboxService = inboxService;
        this.userService = userService;
        this.authService = authService;
        this.returnScreen = returnScreen;

        if (!message.isRead()) {
            inboxService.markAsRead(message.getId());
            message.setRead(true);
        }
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
            if (showDeleteModal) {
                if (KeyUtil.isLeft(k) || KeyUtil.isRight(k)) {
                    deleteConfirmFocused = !deleteConfirmFocused;
                    return ScreenResult.stay(this);
                }
                if (KeyUtil.isEnter(k)) {
                    if (deleteConfirmFocused) {
                        inboxService.deleteMessage(message.getId());
                        return ScreenResult.navigate(returnScreen);
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
                return ScreenResult.navigate(returnScreen);
            }

            if ("d".equalsIgnoreCase(k.key()) || KeyUtil.isDelete(k)) {
                showDeleteModal = true;
                deleteConfirmFocused = false;
                return ScreenResult.stay(this);
            }

            if (message.isActionable()) {
                return handleStandardActionInput(k);
            } else if ("a".equalsIgnoreCase(k.key()) || "r".equalsIgnoreCase(k.key())) {
                bannerMessage = TuiHelper.yellow("● This request has already been processed (status: " + message.getStatus() + ").");
                return ScreenResult.stay(this);
            }
        }
        return ScreenResult.stay(this);
    }

    private ScreenResult handleStandardActionInput(KeyPressMessage k) {
        if (KeyUtil.isLeft(k) || KeyUtil.isRight(k)) {
            focusedActionIndex = (focusedActionIndex == 0) ? 1 : 0;
            return ScreenResult.stay(this);
        }
        if ("a".equalsIgnoreCase(k.key())) {
            focusedActionIndex = 0;
            return executeStandardApprove();
        }
        if ("r".equalsIgnoreCase(k.key())) {
            focusedActionIndex = 1;
            return executeReject();
        }
        if (KeyUtil.isEnter(k)) {
            if (focusedActionIndex == 0) {
                return executeStandardApprove();
            } else {
                return executeReject();
            }
        }
        return ScreenResult.stay(this);
    }

    private ScreenResult executeStandardApprove() {
        try {
            if (message.getType() == InboxMessageType.QUIZ_RETAKE) {
                inboxService.approveQuizRetake(message.getId());
                message.setStatus(InboxStatus.APPROVED);
            } else if (message.getType() == InboxMessageType.EXAM_RETAKE) {
                inboxService.approveExamRetake(message.getId());
                message.setStatus(InboxStatus.APPROVED);
            } else {
                inboxService.updateStatus(message.getId(), InboxStatus.APPROVED);
                if (message.getSenderId() != null) {
                    inboxService.sendNotification(
                            message.getSenderId(),
                            "Request Approved",
                            "Your request for " + message.getTitle() + " has been approved."
                    );
                }
                message.setStatus(InboxStatus.APPROVED);
            }
            bannerMessage = TuiHelper.green("✔ Request approved.");
            errorMessage = "";
        } catch (ValidationException e) {
            errorMessage = e.getMessage();
        } catch (Exception e) {
            errorMessage = "Action failed: " + e.getMessage();
        }
        return ScreenResult.stay(this);
    }

    private ScreenResult executeReject() {
        try {
            inboxService.rejectRequest(message.getId(), "Administrator has rejected this request.");
            message.setStatus(InboxStatus.REJECTED);
            bannerMessage = TuiHelper.yellow("Request rejected.");
            errorMessage = "";
        } catch (ValidationException e) {
            errorMessage = e.getMessage();
        } catch (Exception e) {
            errorMessage = "Action failed: " + e.getMessage();
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
        return InboxViews.renderInboxDetail(
                message,
                focusedActionIndex,
                errorMessage,
                bannerMessage
        );
    }
}