package com.proctor.controller;

import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.enums.Role;
import com.proctor.exception.ValidationException;
import com.proctor.util.KeyUtil;
import com.proctor.util.MouseUtil;
import com.proctor.view.AuthViews;
import com.proctor.model.service.EmailVerificationService;
import com.proctor.model.service.UserService;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

import java.time.DateTimeException;
import java.time.LocalDate;

public class StudentRegisterScreen implements Screen {
    private final AuthService authService;
    private final UserService userService;
    private final EmailVerificationService verificationService;

    private final StringBuilder fullName = new StringBuilder();
    private String selectedGender = "Male";
    private final StringBuilder birthday = new StringBuilder();
    private final StringBuilder email = new StringBuilder();
    private final StringBuilder username = new StringBuilder();
    private final StringBuilder password = new StringBuilder();
    private final StringBuilder confirmPassword = new StringBuilder();

    private int focusedField = 0;
    private String errorMessage = "";

    public StudentRegisterScreen(AuthService authService, UserService userService) {
        this(authService, userService, new EmailVerificationService());
    }

    public StudentRegisterScreen(AuthService authService, UserService userService, EmailVerificationService verificationService) {
        this.authService = authService;
        this.userService = userService;
        this.verificationService = verificationService != null ? verificationService : new EmailVerificationService();
    }

    private int getFieldCount() {
        return 9;
    }

    @Override
    public ScreenResult update(Message msg) {
        if (MouseUtil.isWheelUp(msg)) {
            focusedField = (focusedField - 1 + getFieldCount()) % getFieldCount();
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isWheelDown(msg)) {
            focusedField = (focusedField + 1) % getFieldCount();
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isLeftClick(msg)) {
            int line = MouseUtil.getLineIndex(msg);
            int col = MouseUtil.getColInLine(msg);
            if (line >= 11 && line <= 44) {
                int offset = line - 11;
                int f = offset / 5;
                int mod = offset % 5;
                if (mod <= 3 && f >= 0 && f <= 6) {
                    if (f == 1 && focusedField == 1) {
                        cycleGender(true);
                    } else {
                        focusedField = f;
                    }
                    return ScreenResult.stay(this);
                }
            }
            int btnLine = MouseUtil.findButtonRowLine(view());
            if (btnLine != -1 && line >= btnLine && line <= btnLine + 2) {
                int btn = MouseUtil.getClickedButtonIndex(col, 106, "Register", "Back");
                if (btn == 0) {
                    focusedField = 7;
                    return attemptRegister();
                } else if (btn == 1) {
                    focusedField = 8;
                    return ScreenResult.navigate(new RegisterRoleScreen(authService));
                }
            }
            return ScreenResult.stay(this);
        }

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
                if (focusedField >= 0 && focusedField <= 5) {
                    focusedField++;
                    return ScreenResult.stay(this);
                } else if (focusedField == 6 || focusedField == 7) {
                    return attemptRegister();
                } else if (focusedField == 8) {
                    return ScreenResult.navigate(new RegisterRoleScreen(authService));
                }
            }

            if (focusedField == 7 || focusedField == 8) {
                if (KeyUtil.isLeft(k) || KeyUtil.isRight(k)) {
                    focusedField = (focusedField == 7) ? 8 : 7;
                    return ScreenResult.stay(this);
                }
            }

            if (focusedField == 1) {
                if (KeyUtil.isRight(k)) {
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

            if (focusedField == 0 || (focusedField >= 3 && focusedField <= 6)) {
                StringBuilder active = switch (focusedField) {
                    case 0 -> fullName;
                    case 3 -> email;
                    case 4 -> username;
                    case 5 -> password;
                    default -> confirmPassword;
                };

                int maxLen = switch (focusedField) {
                    case 0 -> 100;
                    case 3 -> 254;
                    case 4 -> 50;
                    default -> 128;
                };

                if (KeyUtil.handleBackspace(active, k)) {
                    errorMessage = "";
                    return ScreenResult.stay(this);
                }

                if (KeyUtil.appendInput(active, k, maxLen)) {
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

            String cleanEmail = email.toString().trim();
            String cleanName = fullName.toString().trim();
            String cleanUser = username.toString().trim();
            String rawPass = password.toString();
            String gender = selectedGender;

            userService.validateNewUser(cleanEmail, cleanUser, rawPass, cleanName, Role.STUDENT, dob, gender, null, null, null);

            verificationService.sendRegistrationCode(cleanEmail, cleanName);

            return ScreenResult.navigate(new EmailVerificationScreen(
                    cleanEmail,
                    cleanName,
                    "Student Account Registration",
                    "Complete Registration",
                    verificationService,
                    () -> {
                        userService.createUser(cleanEmail, cleanUser, rawPass, cleanName, Role.STUDENT, dob, gender);
                        authService.login(cleanEmail, rawPass);
                        return ScreenResult.navigate(new StudentDashboardScreen(authService));
                    },
                    this
            ));
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
        LocalDate parsed;
        try {
            int day   = Integer.parseInt(digits.substring(0, 2));
            int month = Integer.parseInt(digits.substring(2, 4));
            int year  = Integer.parseInt(digits.substring(4, 8));
            parsed = LocalDate.of(year, month, day);
        } catch (DateTimeException | IllegalArgumentException e) {
            throw new ValidationException("Invalid date of birth — please check day, month and year.");
        }
        if (!parsed.isBefore(LocalDate.now())) {
            throw new ValidationException("Date of birth must be in the past.");
        }
        if (parsed.isBefore(LocalDate.now().minusYears(120))) {
            throw new ValidationException("Invalid date of birth — year is too far in the past.");
        }
        if (parsed.isAfter(LocalDate.now().minusYears(5))) {
            throw new ValidationException("Students must be at least 5 years old.");
        }
        return parsed;
    }

    @Override
    public String view() {
        return AuthViews.renderRegister(
                Role.STUDENT,
                fullName.toString(), email.toString(), username.toString(),
                password.toString(), confirmPassword.toString(),
                birthday.toString(), selectedGender, focusedField, errorMessage);
    }
}
