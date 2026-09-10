package com.proctor.controller;

import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.enums.Role;
import com.proctor.exception.ValidationException;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.AuthViews;
import com.proctor.model.service.UserService;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

public class RegisterScreen implements Screen {
    private final AuthService authService;
    private final UserService userService;

    private final StringBuilder fullName = new StringBuilder();
    private final StringBuilder email = new StringBuilder();
    private final StringBuilder username = new StringBuilder();
    private final StringBuilder password = new StringBuilder();
    private final StringBuilder confirmPassword = new StringBuilder();
    private Role selectedRole = Role.STUDENT;

    private int focusedField = 0;
    private String errorMessage = "";

    public RegisterScreen(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    private int getFieldCount() {
        return 8;
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new LoginScreen(authService));
            }

            if (KeyUtil.isTab(k) || KeyUtil.isDown(k)) {
                focusedField = (focusedField + 1) % getFieldCount();
                return ScreenResult.stay(this);
            }

            if ("shift+tab".equalsIgnoreCase(k.key()) || KeyUtil.isUp(k)) {
                focusedField = (focusedField - 1 + getFieldCount()) % getFieldCount();
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isEnter(k)) {
                if (focusedField >= 0 && focusedField <= 4) {
                    focusedField++;
                    return ScreenResult.stay(this);
                } else if (focusedField == 5 || focusedField == 6) {
                    return attemptRegister();
                } else if (focusedField == 7) {
                    return ScreenResult.navigate(new LoginScreen(authService));
                }
            }

            if (focusedField == 6 || focusedField == 7) {
                if (KeyUtil.isLeft(k) || KeyUtil.isRight(k)) {
                    focusedField = (focusedField == 6) ? 7 : 6;
                    return ScreenResult.stay(this);
                }
            }

            if (focusedField == 5) {
                if (KeyUtil.isSpace(k) || KeyUtil.isLeft(k) || KeyUtil.isRight(k)) {
                    selectedRole = (selectedRole == Role.STUDENT) ? Role.TEACHER : Role.STUDENT;
                    return ScreenResult.stay(this);
                }
            }

            if (focusedField >= 0 && focusedField <= 4) {
                StringBuilder active = switch (focusedField) {
                    case 0 -> fullName;
                    case 1 -> email;
                    case 2 -> username;
                    case 3 -> password;
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
                } else if (k.key() != null && k.key().length() == 1 && !Character.isISOControl(k.key().charAt(0))) {
                    active.append(k.key());
                    errorMessage = "";
                }
            }
        }
        return ScreenResult.stay(this);
    }

    private ScreenResult attemptRegister() {
        try {
            if (fullName.toString().trim().isBlank()) {
                throw new ValidationException("Full name is required.");
            }
            if (email.toString().trim().isBlank()) {
                throw new ValidationException("Email is required.");
            }
            if (username.toString().trim().isBlank()) {
                throw new ValidationException("Username is required.");
            }
            if (password.toString().isBlank()) {
                throw new ValidationException("Password is required.");
            }
            if (!password.toString().equals(confirmPassword.toString())) {
                throw new ValidationException("Passwords do not match.");
            }

            userService.createUser(email.toString().trim(), username.toString().trim(), password.toString(), fullName.toString().trim(), selectedRole);
            User loggedIn = authService.login(email.toString().trim(), password.toString());

            if (loggedIn.getRole() == Role.TEACHER) {
                return ScreenResult.navigate(new TeacherDashboardScreen(authService));
            } else {
                return ScreenResult.navigate(new StudentDashboardScreen(authService));
            }
        } catch (ValidationException e) {
            errorMessage = e.getMessage();
            return ScreenResult.stay(this);
        } catch (Exception e) {
            errorMessage = "Registration failed: " + e.getMessage();
            return ScreenResult.stay(this);
        }
    }

    @Override
    public String view() {
        return AuthViews.renderRegister(fullName.toString(), email.toString(), username.toString(), password.toString(), confirmPassword.toString(), selectedRole, focusedField, errorMessage);
    }
}