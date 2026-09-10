package com.proctor.controller;

import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.enums.Role;
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
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

public class InboxDetailScreen implements Screen {

    private final InboxMessage message;
    private final InboxService inboxService;
    private final UserService userService;
    private final AuthService authService;
    private final Screen returnScreen;

    private boolean showDeleteModal = false;
    private boolean deleteConfirmFocused = false;

    private final StringBuilder adminTempPassword = new StringBuilder();
    private int focusedActionIndex = 0;
    private int focusedField = 0;

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

    private boolean isPasswordResetForAdmin() {
        User current = Session.getCurrentUser().orElse(null);
        return current != null && current.getRole() == Role.ADMIN
                && message.getType() == InboxMessageType.PASSWORD_RESET
                && message.getStatus() == InboxStatus.PENDING
                && message.getEffectivePasswordHash() == null;
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
                if (isPasswordResetForAdmin()) {
                    return handleAdminPasswordResetInput(k);
                } else {
                    return handleStandardActionInput(k);
                }
            }
        }
        return ScreenResult.stay(this);
    }

    private ScreenResult handleAdminPasswordResetInput(KeyPressMessage k) {
        if (KeyUtil.isTab(k) || KeyUtil.isDown(k)) {
            focusedField = (focusedField + 1) % 3;
            return ScreenResult.stay(this);
        }
        if ("shift+tab".equalsIgnoreCase(k.key()) || KeyUtil.isUp(k)) {
            focusedField = (focusedField - 1 + 3) % 3;
            return ScreenResult.stay(this);
        }

        if (focusedField == 0) {
            if (KeyUtil.isBackspace(k)) {
                if (!adminTempPassword.isEmpty()) adminTempPassword.deleteCharAt(adminTempPassword.length() - 1);
                return ScreenResult.stay(this);
            }
            if (k.type() == KeyType.KeyRunes && k.runes() != null) {
                for (char c : k.runes()) {
                    if (!Character.isISOControl(c)) adminTempPassword.append(c);
                }
                errorMessage = "";
            } else if (k.key() != null && k.key().length() == 1 && !Character.isISOControl(k.key().charAt(0))) {
                adminTempPassword.append(k.key());
                errorMessage = "";
            }
            if (KeyUtil.isEnter(k)) {
                return executeAdminPasswordResetApprove();
            }
        } else {
            if (KeyUtil.isLeft(k) || KeyUtil.isRight(k)) {
                focusedField = (focusedField == 1) ? 2 : 1;
                return ScreenResult.stay(this);
            }
            if (KeyUtil.isEnter(k)) {
                if (focusedField == 1) {
                    return executeAdminPasswordResetApprove();
                } else {
                    return executeReject();
                }
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

    private ScreenResult executeAdminPasswordResetApprove() {
        try {
            if (adminTempPassword.toString().trim().isBlank()) {
                throw new ValidationException("Please enter a new temporary password.");
            }
            int targetUserId = message.getTargetId() != null ? message.getTargetId() : message.getSenderId();
            userService.resetPasswordWithAdminPassword(targetUserId, adminTempPassword.toString().trim());

            inboxService.updateStatus(message.getId(), InboxStatus.RESOLVED);
            message.setStatus(InboxStatus.RESOLVED);

            if (message.getSenderId() != null) {
                inboxService.sendNotification(
                        message.getSenderId(),
                        "Password Reset Approved",
                        "An administrator has reset your password. Your new temporary password is: " + adminTempPassword.toString().trim()
                );
            }

            bannerMessage = TuiHelper.green("✔ Password successfully reset and user notified.");
            errorMessage = "";
        } catch (ValidationException e) {
            errorMessage = e.getMessage();
        } catch (Exception e) {
            errorMessage = "Failed to reset password: " + e.getMessage();
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
        } catch (com.proctor.exception.ValidationException e) {
            errorMessage = e.getMessage();
        } catch (Exception e) {
            errorMessage = "Action failed: " + e.getMessage();
        }
        return ScreenResult.stay(this);
    }

    private ScreenResult executeReject() {
        try {
            inboxService.rejectRequest(message.getId(), "Instructor has rejected this request.");
            message.setStatus(InboxStatus.REJECTED);
            bannerMessage = TuiHelper.red("✖ Request rejected.");
            errorMessage = "";
        } catch (com.proctor.exception.ValidationException e) {
            errorMessage = e.getMessage();
        } catch (Exception e) {
            errorMessage = "Action failed: " + e.getMessage();
        }
        return ScreenResult.stay(this);
    }

    @Override
    public String view() {
        boolean showAdminInput = isPasswordResetForAdmin();
        boolean inputFocused = showAdminInput && (focusedField == 0);
        int actionBtn = showAdminInput ? (focusedField == 1 ? 0 : (focusedField == 2 ? 1 : -1)) : focusedActionIndex;

        return InboxViews.renderInboxDetail(
                message,
                showDeleteModal,
                deleteConfirmFocused,
                showAdminInput,
                adminTempPassword.toString(),
                inputFocused,
                actionBtn,
                errorMessage,
                bannerMessage
        );
    }
}