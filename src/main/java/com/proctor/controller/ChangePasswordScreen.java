package com.proctor.controller;

import com.proctor.exception.ValidationException;
import com.proctor.util.KeyUtil;
import com.proctor.util.MouseUtil;
import com.proctor.util.PasswordUtils;
import com.proctor.util.TuiHelper;
import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.enums.Role;
import com.proctor.model.service.AuthService;
import com.proctor.model.service.UserService;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;
import com.williamcallahan.tui4j.compat.bubbletea.PasteMessage;

public class ChangePasswordScreen implements Screen {
    private final AuthService authService;
    private final UserService userService;
    private final Screen returnScreen;

    private final StringBuilder currentPassword = new StringBuilder();
    private final StringBuilder newPassword = new StringBuilder();
    private final StringBuilder confirmPassword = new StringBuilder();

    private int focusedField = 0;
    private boolean showCurrentPassword = false;
    private boolean showNewPassword = false;
    private boolean showConfirmPassword = false;
    private String errorMessage = "";
    private String successBanner = "";

    public ChangePasswordScreen(AuthService authService, UserService userService, Screen returnScreen) {
        this.authService = authService;
        this.userService = userService;
        this.returnScreen = returnScreen;
    }

    @Override
    public ScreenResult update(Message msg) {
                if (msg instanceof PasteMessage paste) {
            switch (focusedField) {
                case 0 -> KeyUtil.pasteToBuffer(currentPassword, paste.content(), 128);
                case 1 -> KeyUtil.pasteToBuffer(newPassword, paste.content(), 128);
                case 2 -> KeyUtil.pasteToBuffer(confirmPassword, paste.content(), 128);
                default -> { /* buttons */ }
            }
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isWheelUp(msg)) {
            focusedField = (focusedField - 1 + 5) % 5;
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isWheelDown(msg)) {
            focusedField = (focusedField + 1) % 5;
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isLeftClick(msg)) {
            int line = MouseUtil.getLineIndex(msg);
            int col = MouseUtil.getColInLine(msg);
            if (line >= 11 && line <= 14) {
                focusedField = 0;
                return ScreenResult.stay(this);
            } else if (line >= 16 && line <= 19) {
                focusedField = 1;
                return ScreenResult.stay(this);
            } else if (line >= 21 && line <= 24) {
                focusedField = 2;
                return ScreenResult.stay(this);
            }
            int btnLine = MouseUtil.findButtonRowLine(view());
            if (btnLine != -1 && line >= btnLine && line <= btnLine + 2) {
                int btn = MouseUtil.getClickedButtonIndex(col, "Save Password", "Back");
                if (btn == 0) {
                    focusedField = 3;
                    return handleSave();
                } else if (btn == 1) {
                    focusedField = 4;
                    return navigateBack();
                }
            }
            return ScreenResult.stay(this);
        }

        if (msg instanceof KeyPressMessage k) {
            if (focusedField >= 0 && focusedField <= 2 && KeyUtil.isPasswordToggle(k)) {
                if (focusedField == 0) showCurrentPassword = !showCurrentPassword;
                else if (focusedField == 1) showNewPassword = !showNewPassword;
                else showConfirmPassword = !showConfirmPassword;
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isEsc(k)) {
                return navigateBack();
            }

            if (KeyUtil.isDown(k)) {
                focusedField = (focusedField + 1) % 5;
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isUp(k)) {
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
                    return navigateBack();
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
                        if (!Character.isISOControl(c) && active.length() < 128) active.append(c);
                    }
                    errorMessage = "";
                    successBanner = "";
                } else if (k.key() != null && k.key().length() == 1 && !Character.isISOControl(k.key().charAt(0))) {
                    if (active.length() < 128) {
                        active.append(k.key());
                    }
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

    private ScreenResult navigateBack() {
        if (returnScreen != null) {
            return ScreenResult.navigate(returnScreen);
        }
        User u = Session.getCurrentUser().orElse(null);
        if (u != null && u.getRole() == Role.ADMIN) {
            return ScreenResult.navigate(new AdminDashboardScreen(authService));
        } else if (u != null && u.getRole() == Role.TEACHER) {
            return ScreenResult.navigate(new TeacherDashboardScreen(authService));
        } else {
            return ScreenResult.navigate(new StudentDashboardScreen(authService));
        }
    }

    @Override
    public String view() {
        StringBuilder sb = new StringBuilder();
        sb.append(TuiHelper.header("ACCOUNT SECURITY"));
        sb.append("\n");
        sb.append(TuiHelper.boxTitle("Change Account Password")).append("\n\n");
        sb.append(TuiHelper.inputBox("Current Password", currentPassword.toString(), focusedField == 0, 102, true, "Enter current password", showCurrentPassword));
        sb.append("\n");
        sb.append(TuiHelper.inputBox("New Password", newPassword.toString(), focusedField == 1, 102, true, "Min 8 chars, letters and numbers", showNewPassword));
        sb.append("\n");
        sb.append(TuiHelper.inputBox("Confirm New Password", confirmPassword.toString(), focusedField == 2, 102, true, "Repeat new password", showConfirmPassword));
        sb.append("\n");
        sb.append(TuiHelper.buttonRow("Save Password", focusedField == 3, "Back", focusedField == 4)).append("\n\n");

        if (!errorMessage.isBlank()) {
            sb.append("  ").append(TuiHelper.red("✖ " + errorMessage)).append("\n\n");
        }
        if (!successBanner.isBlank()) {
            sb.append("  ").append(successBanner).append("\n\n");
        }

        String hint = "  [↑/↓] Switch Field  •  [Enter] Confirm  •  [Esc] Back";
        if (focusedField >= 0 && focusedField <= 2) {
            hint += "  •  [F3] Show/Hide";
        }
        sb.append(TuiHelper.dim(hint + "\n"));
        return sb.toString();
    }
}