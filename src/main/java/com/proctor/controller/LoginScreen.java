package com.proctor.controller;

import com.proctor.model.entity.User;
import com.proctor.model.repository.UserRepository;
import com.proctor.model.service.AuthService;
import com.proctor.model.enums.Role;
import com.proctor.exception.AuthException;
import com.proctor.exception.ValidationException;
import com.proctor.model.repository.InboxRepository;
import com.proctor.model.service.InboxService;
import com.proctor.util.KeyUtil;
import com.proctor.util.PasswordUtils;
import com.proctor.util.TuiHelper;
import com.proctor.view.AuthViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

public class LoginScreen implements Screen {
    private final AuthService authService;
    private final InboxService inboxService;
    private final StringBuilder identifier = new StringBuilder();
    private final StringBuilder password = new StringBuilder();
    private int focusedField = 0;
    private String errorMessage = "";
    private String infoBanner = "";

    private boolean forgotPasswordMode = false;
    private final StringBuilder forgotIdentifier = new StringBuilder();
    private final StringBuilder forgotNewPassword = new StringBuilder();
    private final StringBuilder forgotConfirmPassword = new StringBuilder();
    private int forgotFocusIndex = 0;
    private String forgotMessage = "";

    public LoginScreen(AuthService authService) {
        this(authService, new InboxService(new InboxRepository(), new UserRepository()));
    }

    public LoginScreen(AuthService authService, InboxService inboxService) {
        this.authService = authService;
        this.inboxService = inboxService != null ? inboxService : new InboxService(new InboxRepository(), new UserRepository());
    }

    private int getFieldCount() {
        return 5;
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
            if (forgotPasswordMode) {
                return handleForgotPasswordInput(k);
            }

            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new StartupScreen(authService));
            }

            if (KeyUtil.isDown(k)) {
                focusedField = (focusedField + 1) % getFieldCount();
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isUp(k)) {
                focusedField = (focusedField - 1 + getFieldCount()) % getFieldCount();
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isEnter(k)) {
                if (focusedField == 0) {
                    focusedField = 1;
                    return ScreenResult.stay(this);
                } else if (focusedField == 1 || focusedField == 2) {
                    return attemptLogin();
                } else if (focusedField == 3) {
                    forgotPasswordMode = true;
                    forgotIdentifier.setLength(0);
                    forgotNewPassword.setLength(0);
                    forgotConfirmPassword.setLength(0);
                    forgotFocusIndex = 0;
                    forgotMessage = "";
                    return ScreenResult.stay(this);
                } else if (focusedField == 4) {
                    return ScreenResult.navigate(new StartupScreen(authService));
                }
            }

            if (focusedField >= 2 && focusedField <= 4) {
                if (KeyUtil.isLeft(k)) {
                    focusedField = (focusedField == 2) ? 4 : focusedField - 1;
                    return ScreenResult.stay(this);
                } else if (KeyUtil.isRight(k)) {
                    focusedField = (focusedField == 4) ? 2 : focusedField + 1;
                    return ScreenResult.stay(this);
                }
            }

            if (focusedField == 0 || focusedField == 1) {
                StringBuilder active = (focusedField == 0) ? identifier : password;
                if (KeyUtil.isBackspace(k)) {
                    if (!active.isEmpty()) {
                        active.deleteCharAt(active.length() - 1);
                    }
                    return ScreenResult.stay(this);
                }

                if (k.type() == KeyType.KeyRunes && k.runes() != null) {
                    for (char c : k.runes()) {
                        if (!Character.isISOControl(c)) {
                            active.append(c);
                        }
                    }
                    errorMessage = "";
                } else if (k.key() != null && k.key().length() == 1 && !Character.isISOControl(k.key().charAt(0))) {
                    active.append(k.key());
                    errorMessage = "";
                }
            }
        }

        return ScreenResult.stay(this);
    }

    private ScreenResult handleForgotPasswordInput(KeyPressMessage k) {
        if (KeyUtil.isEsc(k)) {
            forgotPasswordMode = false;
            return ScreenResult.stay(this);
        }

        if (KeyUtil.isDown(k)) {
            forgotFocusIndex = (forgotFocusIndex + 1) % 5;
            return ScreenResult.stay(this);
        }

        if (KeyUtil.isUp(k)) {
            forgotFocusIndex = (forgotFocusIndex - 1 + 5) % 5;
            return ScreenResult.stay(this);
        }

        if (forgotFocusIndex >= 3 && (KeyUtil.isLeft(k) || KeyUtil.isRight(k))) {
            forgotFocusIndex = (forgotFocusIndex == 3) ? 4 : 3;
            return ScreenResult.stay(this);
        }

        if (KeyUtil.isEnter(k)) {
            if (forgotFocusIndex == 0) {
                forgotFocusIndex = 1;
                return ScreenResult.stay(this);
            } else if (forgotFocusIndex == 1) {
                forgotFocusIndex = 2;
                return ScreenResult.stay(this);
            } else if (forgotFocusIndex == 4) {
                forgotPasswordMode = false;
                return ScreenResult.stay(this);
            }

            String target = forgotIdentifier.toString().trim();
            if (target.isBlank()) {
                forgotMessage = TuiHelper.red("✖ Please enter your email address or username.");
                return ScreenResult.stay(this);
            }

            String newPass = forgotNewPassword.toString();
            try {
                PasswordUtils.validatePassword(newPass);
            } catch (ValidationException e) {
                forgotMessage = TuiHelper.red("✖ " + e.getMessage());
                return ScreenResult.stay(this);
            }

            if (!newPass.equals(forgotConfirmPassword.toString())) {
                forgotMessage = TuiHelper.red("✖ Passwords do not match.");
                return ScreenResult.stay(this);
            }

            try {
                inboxService.sendPasswordResetRequest(target, newPass);
                forgotPasswordMode = false;
                errorMessage = "";
                infoBanner = TuiHelper.green("✔ Password change request sent to administrator for approval.");
            } catch (ValidationException e) {
                forgotMessage = TuiHelper.red("✖ " + e.getMessage());
            }
            return ScreenResult.stay(this);
        }

        if (forgotFocusIndex >= 0 && forgotFocusIndex <= 2) {
            StringBuilder active = switch (forgotFocusIndex) {
                case 0 -> forgotIdentifier;
                case 1 -> forgotNewPassword;
                default -> forgotConfirmPassword;
            };

            if (KeyUtil.isBackspace(k)) {
                if (!active.isEmpty()) {
                    active.deleteCharAt(active.length() - 1);
                }
                return ScreenResult.stay(this);
            }

            if (k.type() == KeyType.KeyRunes && k.runes() != null) {
                for (char c : k.runes()) {
                    if (!Character.isISOControl(c)) active.append(c);
                }
                forgotMessage = "";
            } else if (k.key() != null && k.key().length() == 1 && !Character.isISOControl(k.key().charAt(0))) {
                active.append(k.key());
                forgotMessage = "";
            }
        }

        return ScreenResult.stay(this);
    }

    private ScreenResult attemptLogin() {
        try {
            User user = authService.login(identifier.toString(), password.toString());
            errorMessage = "";
            if (user.getRole() == Role.ADMIN) {
                return ScreenResult.navigate(new AdminDashboardScreen(authService));
            } else if (user.getRole() == Role.TEACHER) {
                return ScreenResult.navigate(new TeacherDashboardScreen(authService));
            } else {
                return ScreenResult.navigate(new StudentDashboardScreen(authService));
            }
        } catch (AuthException e) {
            errorMessage = e.getMessage();
            password.setLength(0);
            return ScreenResult.stay(this);
        } catch (Exception e) {
            errorMessage = "Login failed: " + e.getMessage();
            password.setLength(0);
            return ScreenResult.stay(this);
        }
    }

    @Override
    public String view() {
        if (forgotPasswordMode) {
            StringBuilder sb = new StringBuilder();
            sb.append(TuiHelper.header("PROCTOR"));
            sb.append("\n");
            sb.append(TuiHelper.boxTitle("Password Recovery")).append("\n\n");
            sb.append("  ").append(TuiHelper.dim("Submit your registered email or username and choose your new password.\n"));
            sb.append("  ").append(TuiHelper.dim("An administrator will review and activate your new password.\n\n"));
            sb.append(TuiHelper.inputBox("Email or Username", forgotIdentifier.toString(), forgotFocusIndex == 0, 102, false, "e.g. user@proctor.edu or username"));
            sb.append("\n");
            sb.append(TuiHelper.inputBox("New Password", forgotNewPassword.toString(), forgotFocusIndex == 1, 102, true, "Min 8 chars, mix of upper/lower/numbers"));
            sb.append("\n");
            sb.append(TuiHelper.inputBox("Confirm New Password", forgotConfirmPassword.toString(), forgotFocusIndex == 2, 102, true, "Repeat new password"));
            sb.append("\n");
            sb.append(TuiHelper.buttonRow("Submit Request", forgotFocusIndex == 3, "Cancel", forgotFocusIndex == 4)).append("\n\n");
            if (!forgotMessage.isBlank()) {
                sb.append("  ").append(forgotMessage).append("\n\n");
            }
            sb.append(TuiHelper.dim("  [↑/↓] Switch Field  •  [Enter] Confirm  •  [Esc] Cancel\n"));
            return sb.toString();
        }

        return AuthViews.renderLogin(identifier.toString(), password.toString(), focusedField, errorMessage, infoBanner);
    }
}