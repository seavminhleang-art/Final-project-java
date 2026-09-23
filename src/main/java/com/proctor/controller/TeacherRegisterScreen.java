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
import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

public class TeacherRegisterScreen implements Screen {

    public record TeacherRegisterSendTickMessage(int generationId) implements Message {}
    public record TeacherRegisterSendResultMessage(int generationId, boolean success, String error, LocalDate dob, String cleanEmail, String cleanName, String cleanUser, String rawPass, String gender, String degree, String edu, String spec) implements Message {}

    private final AuthService authService;
    private final UserService userService;
    private final EmailVerificationService verificationService;

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

    private boolean isSending = false;
    private int sendGenerationId = 0;
    private long sendStartTime = 0;
    private int spinnerTick = 0;
    private AtomicBoolean activeCancellation = null;

    public TeacherRegisterScreen(AuthService authService, UserService userService) {
        this(authService, userService, new EmailVerificationService());
    }

    public TeacherRegisterScreen(AuthService authService, UserService userService, EmailVerificationService verificationService) {
        this.authService = authService;
        this.userService = userService;
        this.verificationService = verificationService != null ? verificationService : new EmailVerificationService();
    }

    public boolean isSending() {
        return isSending;
    }

    public int getSpinnerTick() {
        return spinnerTick;
    }

    private int getFieldCount() {
        return 12;
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof TeacherRegisterSendTickMessage t) {
            if (isSending && t.generationId() == sendGenerationId) {
                spinnerTick++;
                return ScreenResult.stay(this, Command.tick(Duration.ofMillis(80), time -> new TeacherRegisterSendTickMessage(sendGenerationId)));
            }
            return ScreenResult.stay(this);
        }

        if (msg instanceof TeacherRegisterSendResultMessage res) {
            if (!isSending || res.generationId() != sendGenerationId) {
                return ScreenResult.stay(this);
            }
            isSending = false;
            if (res.success()) {
                return ScreenResult.navigate(new EmailVerificationScreen(
                        res.cleanEmail(),
                        res.cleanName(),
                        "Teacher Account Registration",
                        "Complete Registration",
                        verificationService,
                        () -> {
                            userService.createUser(
                                    res.cleanEmail(), res.cleanUser(), res.rawPass(), res.cleanName(), Role.TEACHER,
                                    res.dob(), res.gender(), res.degree(), res.edu(), res.spec()
                            );
                            authService.login(res.cleanEmail(), res.rawPass());
                            return ScreenResult.navigate(new TeacherDashboardScreen(authService));
                        },
                        this
                ));
            } else {
                errorMessage = "Registration failed: " + res.error();
                return ScreenResult.stay(this);
            }
        }

        if (isSending) {
            if (msg instanceof KeyPressMessage k && KeyUtil.isEsc(k)) {
                if (activeCancellation != null) {
                    activeCancellation.set(true);
                }
                isSending = false;
                sendGenerationId++;
                errorMessage = "Sending cancelled.";
                return ScreenResult.stay(this);
            }
            if (MouseUtil.isLeftClick(msg)) {
                int line = MouseUtil.getLineIndex(msg);
                int btnLine = MouseUtil.findButtonRowLine(view());
                if (btnLine != -1 && line >= btnLine && line <= btnLine + 2) {
                    if (activeCancellation != null) {
                        activeCancellation.set(true);
                    }
                    isSending = false;
                    sendGenerationId++;
                    errorMessage = "Sending cancelled.";
                    return ScreenResult.stay(this);
                }
            }
            return ScreenResult.stay(this);
        }
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

            int windowSize = 5;
            int numInputFields = 10;
            int startField = Math.max(0, Math.min(Math.min(focusedField, numInputFields - 1) - 1, numInputFields - windowSize));
            int endField = Math.min(numInputFields, startField + windowSize);

            int curL = 11;
            if (startField > 0) {
                if (line == curL) {
                    focusedField = Math.max(0, focusedField - 1);
                    return ScreenResult.stay(this);
                }
                curL++;
            }

            for (int f = startField; f < endField; f++) {
                if (line >= curL && line <= curL + 3) {
                    if (f == 1 && focusedField == 1) {
                        cycleGender(true);
                    } else {
                        focusedField = f;
                    }
                    return ScreenResult.stay(this);
                }
                curL += 5;
            }

            if (endField < numInputFields) {
                if (line == curL) {
                    focusedField = Math.min(numInputFields - 1, focusedField + 1);
                    return ScreenResult.stay(this);
                }
                curL++;
            }

            int btnLine = MouseUtil.findButtonRowLine(view());
            if (btnLine != -1 && line >= btnLine && line <= btnLine + 2) {
                int btn = MouseUtil.getClickedButtonIndex(col, 106, "Register", "Back");
                if (btn == 0) {
                    focusedField = 10;
                    return attemptRegister();
                } else if (btn == 1) {
                    focusedField = 11;
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
            if (academicDegree.toString().trim().isBlank()) {
                throw new ValidationException("Academic degree is required.");
            }
            if (educationBackground.toString().trim().isBlank()) {
                throw new ValidationException("Education background (university) is required.");
            }
            if (specialization.toString().trim().isBlank()) {
                throw new ValidationException("Specialization is required.");
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
            String cleanUser = username.toString().trim();
            String rawPass = password.toString();
            String cleanName = fullName.toString().trim();
            String gender = selectedGender;
            String degree = academicDegree.toString().trim();
            String edu = educationBackground.toString().trim();
            String spec = specialization.toString().trim();

            userService.validateNewUser(cleanEmail, cleanUser, rawPass, cleanName, Role.TEACHER, dob, gender, degree, edu, spec);

            isSending = true;
            sendStartTime = System.currentTimeMillis();
            spinnerTick = 0;
            final int genId = ++sendGenerationId;
            final AtomicBoolean cancelled = new AtomicBoolean(false);
            this.activeCancellation = cancelled;
            errorMessage = "";

            Command sendCmd = () -> {
                try {
                    verificationService.sendRegistrationCode(cleanEmail, cleanName);
                    if (cancelled.get()) {
                        return new TeacherRegisterSendResultMessage(genId, false, "Cancelled", dob, cleanEmail, cleanName, cleanUser, rawPass, gender, degree, edu, spec);
                    }
                    return new TeacherRegisterSendResultMessage(genId, true, null, dob, cleanEmail, cleanName, cleanUser, rawPass, gender, degree, edu, spec);
                } catch (Exception e) {
                    if (cancelled.get()) {
                        return new TeacherRegisterSendResultMessage(genId, false, "Cancelled", dob, cleanEmail, cleanName, cleanUser, rawPass, gender, degree, edu, spec);
                    }
                    return new TeacherRegisterSendResultMessage(genId, false, e.getMessage(), dob, cleanEmail, cleanName, cleanUser, rawPass, gender, degree, edu, spec);
                }
            };

            Command tickCmd = Command.tick(Duration.ofMillis(80), time -> new TeacherRegisterSendTickMessage(genId));
            return ScreenResult.stay(this, Command.batch(sendCmd, tickCmd));
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
        if (parsed.isAfter(LocalDate.now().minusYears(18))) {
            throw new ValidationException("Teachers must be at least 18 years old.");
        }
        return parsed;
    }

    @Override
    public String view() {
        if (isSending) {
            int elapsed = (int) ((System.currentTimeMillis() - sendStartTime) / 1000);
            return AuthViews.renderOtpLoading("TEACHER REGISTRATION", "Account Registration", email.toString().trim(), spinnerTick, elapsed);
        }
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
