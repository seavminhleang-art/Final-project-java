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

import java.time.LocalDate;

public class RegisterScreen implements Screen {
    private final AuthService authService;
    private final UserService userService;

    private final StringBuilder fullName = new StringBuilder();
    private final StringBuilder email = new StringBuilder();
    private final StringBuilder username = new StringBuilder();
    private final StringBuilder password = new StringBuilder();
    private final StringBuilder confirmPassword = new StringBuilder();
    // raw digit characters only: DDMMYYYY, max 8
    private final StringBuilder birthday = new StringBuilder();
    private Role selectedRole = Role.STUDENT;

    private int focusedField = 0;
    private String errorMessage = "";

    public RegisterScreen(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    // 0=fullName 1=email 2=username 3=password 4=confirmPassword 5=birthday 6=role 7=register 8=back
    private int getFieldCount() {
        return 9;
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new StartupScreen(authService));
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
                if (focusedField >= 0 && focusedField <= 5) {
                    focusedField++;
                    return ScreenResult.stay(this);
                } else if (focusedField == 6 || focusedField == 7) {
                    return attemptRegister();
                } else if (focusedField == 8) {
                    return ScreenResult.navigate(new StartupScreen(authService));
                }
            }

            if (focusedField == 7 || focusedField == 8) {
                if (KeyUtil.isLeft(k) || KeyUtil.isRight(k)) {
                    focusedField = (focusedField == 7) ? 8 : 7;
                    return ScreenResult.stay(this);
                }
            }

            if (focusedField == 6) {
                if (KeyUtil.isSpace(k) || KeyUtil.isLeft(k) || KeyUtil.isRight(k)) {
                    selectedRole = (selectedRole == Role.STUDENT) ? Role.TEACHER : Role.STUDENT;
                    return ScreenResult.stay(this);
                }
            }

            if (focusedField == 5) {
                if (KeyUtil.isBackspace(k)) {
                    if (!birthday.isEmpty()) birthday.deleteCharAt(birthday.length() - 1);
                    errorMessage = "";
                    return ScreenResult.stay(this);
                }
                if (birthday.length() < 8) {
                    char c = extractChar(k);
                    if (Character.isDigit(c)) {
                        birthday.append(c);
                        errorMessage = "";
                    }
                }
                return ScreenResult.stay(this);
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

    private char extractChar(KeyPressMessage k) {
        if (k.type() == KeyType.KeyRunes && k.runes() != null && k.runes().length > 0) {
            return k.runes()[0];
        }
        if (k.key() != null && k.key().length() == 1) {
            return k.key().charAt(0);
        }
        return '\0';
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

            LocalDate dob = parseBirthday();

            userService.createUser(email.toString().trim(), username.toString().trim(),
                    password.toString(), fullName.toString().trim(), selectedRole, dob);
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

    private LocalDate parseBirthday() {
        String digits = birthday.toString();
        if (digits.length() != 8) {
            throw new ValidationException("Date of birth is required (DD - MM - YYYY).");
        }
        try {
            int day   = Integer.parseInt(digits.substring(0, 2));
            int month = Integer.parseInt(digits.substring(2, 4));
            int year  = Integer.parseInt(digits.substring(4, 8));
            return LocalDate.of(year, month, day);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid date of birth — please check day, month and year.");
        }
    }

    @Override
    public String view() {
        return AuthViews.renderRegister(
                fullName.toString(), email.toString(), username.toString(),
                password.toString(), confirmPassword.toString(),
                birthday.toString(), selectedRole, focusedField, errorMessage);
    }
}
