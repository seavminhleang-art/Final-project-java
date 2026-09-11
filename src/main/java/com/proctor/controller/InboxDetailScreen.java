package com.proctor.controller;

import com.proctor.model.entity.User;
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
                if (KeyUtil.isLeft(k) || KeyUtil.isRight(k) || KeyUtil.isTab(k)) {
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

            if ("d".equalsIgnoreCase(k.key())) {
                showDeleteModal = true;
                deleteConfirmFocused = false;
                return ScreenResult.stay(this);
            }

            if (message.isActionable()) {
                return handleStandardActionInput(k);
            }
        }
        return ScreenResult.stay(this);
    }

    private ScreenResult handleStandardActionInput(KeyPressMessage k) {
        if (KeyUtil.isLeft(k) || KeyUtil.isRight(k) || KeyUtil.isTab(k)) {
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
            } else if (message.getType() == InboxMessageType.PASSWORD_RESET) {
                inboxService.approvePasswordReset(message.getId());
                message.setStatus(InboxStatus.RESOLVED);
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
        return InboxViews.renderInboxDetail(
                message,
                showDeleteModal,
                deleteConfirmFocused,
                false,
                "",
                false,
                focusedActionIndex,
                errorMessage,
                bannerMessage
        );
    }
}