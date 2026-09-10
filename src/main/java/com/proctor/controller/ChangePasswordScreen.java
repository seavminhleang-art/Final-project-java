package com.proctor.controller;

import com.proctor.exception.ValidationException;
import com.proctor.util.KeyUtil;
import com.proctor.util.PasswordUtils;
import com.proctor.util.TuiHelper;
import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.service.UserService;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

public class ChangePasswordScreen implements Screen {
    private final AuthService authService;
    private final UserService userService;
    private final Screen returnScreen;

    private final StringBuilder currentPassword = new StringBuilder();
    private final StringBuilder newPassword = new StringBuilder();
    private final StringBuilder confirmPassword = new StringBuilder();

    private int focusedField = 0;
    private String errorMessage = "";
    private String successBanner = "";

    public ChangePasswordScreen(AuthService authService, UserService userService, Screen returnScreen) {
        this.authService = authService;
        this.userService = userService;
        this.returnScreen = returnScreen;
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(returnScreen);
            }

            if (KeyUtil.isTab(k) || KeyUtil.isDown(k)) {
                focusedField = (focusedField + 1) % 5;
                return ScreenResult.stay(this);
            }

            if ("shift+tab".equalsIgnoreCase(k.key()) || KeyUtil.isUp(k)) {
                focusedField = (focusedField - 1 + 5) % 5;
                return ScreenResult.stay(this);
            }

            if (focusedField >= 3 && (KeyUtil.isLeft(k) || KeyUtil.isRight(k))) {
                focusedField = (focusedField == 3) ? 4 : 3;
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isEnter(k)) {
                if (focusedField == 0) {
                    focusedField = 1;
                    return ScreenResult.stay(this);
                } else if (focusedField == 1) {
                    focusedField = 2;
                    return ScreenResult.stay(this);
                } else if (focusedField == 2 || focusedField == 3) {
                    return handleSave();
                } else if (focusedField == 4) {
                    return ScreenResult.navigate(returnScreen);
                }
            }

            if (focusedField >= 0 && focusedField <= 2) {
                StringBuilder active = switch (focusedField) {
                    case 0 -> currentPassword;
                    case 1 -> newPassword;
                    default -> confirmPassword;
                };

                if (KeyUtil.isBackspace(k)) {
                    if (!active.isEmpty()) active.deleteCharAt(active.length() - 1);
                    return ScreenResult.stay(this);
                }

                if (k.type() == KeyType.KeyRunes && k.runes() != null) {
                    for (char c : k.runes()) {
                        if (!Character.isISOControl(c)) active.append(c);
                    }
                    errorMessage = "";
                    successBanner = "";
                } else if (k.key() != null && k.key().length() == 1 && !Character.isISOControl(k.key().charAt(0))) {
                    active.append(k.key());
                    errorMessage = "";
                    successBanner = "";
                }
            }
        }
        return ScreenResult.stay(this);
    }

    private ScreenResult handleSave() {
        User user = Session.getCurrentUser().orElse(null);
        if (user == null) {
            errorMessage = "No active session found.";
            return ScreenResult.stay(this);
        }

        String cur = currentPassword.toString();
        String nw = newPassword.toString();
        String conf = confirmPassword.toString();

        if (cur.isBlank()) {
            errorMessage = "Current password cannot be blank.";
            return ScreenResult.stay(this);
        }

        try {
            PasswordUtils.validatePassword(nw);
        } catch (ValidationException e) {
            errorMessage = e.getMessage();
            return ScreenResult.stay(this);
        }

        if (!nw.equals(conf)) {
            errorMessage = "Passwords do not match.";
            return ScreenResult.stay(this);
        }

        try {
            userService.changePassword(user.getId(), cur, nw);
            currentPassword.setLength(0);
            newPassword.setLength(0);
            confirmPassword.setLength(0);
            errorMessage = "";
            successBanner = TuiHelper.green("✔ Password successfully changed!");
            focusedField = 4;
        } catch (ValidationException e) {
            errorMessage = e.getMessage();
        } catch (Exception e) {
            errorMessage = "Failed to update password: " + e.getMessage();
        }

        return ScreenResult.stay(this);
    }

    @Override
    public String view() {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("ACCOUNT SECURITY", "Change Password"));
        sb.append("\n");
        sb.append(TuiHelper.inputBox("Current Password", currentPassword.toString(), focusedField == 0, 86, true, "Enter current password"));
        sb.append("\n");
        sb.append(TuiHelper.inputBox("New Password", newPassword.toString(), focusedField == 1, 86, true, "Min 8 chars, mix of upper/lower/numbers"));
        sb.append("\n");
        sb.append(TuiHelper.inputBox("Confirm New Password", confirmPassword.toString(), focusedField == 2, 86, true, "Repeat new password"));
        sb.append("\n");
        sb.append(TuiHelper.buttonRow("Save Password", focusedField == 3, "Back", focusedField == 4)).append("\n\n");

        if (!errorMessage.isBlank()) {
            sb.append("  ").append(TuiHelper.red("✖ " + errorMessage)).append("\n\n");
        }
        if (!successBanner.isBlank()) {
            sb.append("  ").append(successBanner).append("\n\n");
        }

        sb.append(TuiHelper.dim("  [Tab/↑/↓] Switch Field  •  [Enter] Confirm  •  [Esc] Back\n"));
        return sb.toString();
    }
}