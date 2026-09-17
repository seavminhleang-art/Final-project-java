package com.proctor.controller;

import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.enums.Role;
import com.proctor.exception.ValidationException;
import com.proctor.util.KeyUtil;
import com.proctor.view.AuthViews;
import com.proctor.model.service.UserService;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

import java.time.LocalDate;

public class TeacherRegisterScreen implements Screen {
    private final AuthService authService;
    private final UserService userService;

    private final StringBuilder fullName = new StringBuilder();
    private String selectedGender = "Male";
    private final StringBuilder birthday = new StringBuilder();
    private final StringBuilder academicDegree = new StringBuilder();
    private final StringBuilder educationBackground = new StringBuilder();
    private final StringBuilder specialization = new StringBuilder();
    private final StringBuilder email = new StringBuilder();
    private final StringBuilder username = new StringBuilder();
    private final StringBuilder password = new StringBuilder();
    private final StringBuilder confirmPassword = new StringBuilder();

    private int focusedField = 0;
    private String errorMessage = "";

    public TeacherRegisterScreen(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
    }

    private int getFieldCount() {
        return 12;
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new RegisterRoleScreen(authService));
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
                if (focusedField >= 0 && focusedField <= 8) {
                    focusedField++;
                    return ScreenResult.stay(this);
                } else if (focusedField == 9 || focusedField == 10) {
                    return attemptRegister();
                } else if (focusedField == 11) {
                    return ScreenResult.navigate(new RegisterRoleScreen(authService));
                }
            }

            if (focusedField == 10 || focusedField == 11) {
                if (KeyUtil.isLeft(k) || KeyUtil.isRight(k)) {
                    focusedField = (focusedField == 10) ? 11 : 10;
                    return ScreenResult.stay(this);
                }
            }

            if (focusedField == 1) {
                if (KeyUtil.isSpace(k) || KeyUtil.isRight(k)) {
                    cycleGender(true);
                    return ScreenResult.stay(this);
                } else if (KeyUtil.isLeft(k)) {
                    cycleGender(false);
                    return ScreenResult.stay(this);
                }
            }

            if (focusedField == 2) {
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

            if (focusedField == 0 || (focusedField >= 3 && focusedField <= 9)) {
                StringBuilder active = switch (focusedField) {
                    case 0 -> fullName;
                    case 3 -> academicDegree;
                    case 4 -> educationBackground;
                    case 5 -> specialization;
                    case 6 -> email;
                    case 7 -> username;
                    case 8 -> password;
                    default -> confirmPassword;
                };

                int maxLen = switch (focusedField) {
                    case 0 -> 100;
                    case 3 -> 100;
                    case 4 -> 150;
                    case 5 -> 100;
                    case 6 -> 254;
                    case 7 -> 50;
                    default -> 128;
                };

                if (KeyUtil.isBackspace(k)) {
                    if (!active.isEmpty()) active.deleteCharAt(active.length() - 1);
                    return ScreenResult.stay(this);
                }

                if (k.type() == KeyType.KeyRunes && k.runes() != null) {
                    for (char c : k.runes()) {
                        if (!Character.isISOControl(c) && active.length() < maxLen) active.append(c);
                    }
                    errorMessage = "";
                } else if (k.key() != null && k.key().length() == 1 && !Character.isISOControl(k.key().charAt(0))) {
                    if (active.length() < maxLen) {
                        active.append(k.key());
                    }
                    errorMessage = "";
                }
            }
        }
        return ScreenResult.stay(this);
    }

    private void cycleGender(boolean forward) {
        if ("Male".equals(selectedGender)) {
            selectedGender = forward ? "Female" : "Other";
        } else if ("Female".equals(selectedGender)) {
            selectedGender = forward ? "Other" : "Male";
        } else {
            selectedGender = forward ? "Male" : "Female";
        }
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
            if (academicDegree.toString().trim().isBlank()) {
                throw new ValidationException("Academic degree / qualification is required.");
            }
            if (educationBackground.toString().trim().isBlank()) {
                throw new ValidationException("Education background (university) is required.");
            }
            if (specialization.toString().trim().isBlank()) {
                throw new ValidationException("Primary subject / specialization is required.");
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

            userService.createUser(
                    email.toString().trim(),
                    username.toString().trim(),
                    password.toString(),
                    fullName.toString().trim(),
                    Role.TEACHER,
                    dob,
                    selectedGender,
                    academicDegree.toString().trim(),
                    educationBackground.toString().trim(),
                    specialization.toString().trim()
            );
            User loggedIn = authService.login(email.toString().trim(), password.toString());

            return ScreenResult.navigate(new TeacherDashboardScreen(authService));
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
        return AuthViews.renderTeacherRegister(
                fullName.toString(),
                email.toString(),
                username.toString(),
                password.toString(),
                confirmPassword.toString(),
                birthday.toString(),
                selectedGender,
                academicDegree.toString(),
                educationBackground.toString(),
                specialization.toString(),
                focusedField,
                errorMessage
        );
    }
}
