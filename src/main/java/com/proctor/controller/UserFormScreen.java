package com.proctor.controller;

import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.enums.Role;
import com.proctor.exception.ValidationException;
import com.proctor.util.KeyUtil;
import com.proctor.util.MouseUtil;
import com.proctor.view.UserViews;
import com.proctor.model.service.UserService;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

import java.time.DateTimeException;
import java.time.LocalDate;

public class UserFormScreen implements Screen {
    private final UserService userService;
    private final AuthService authService;
    private final User userToEdit;

    private final StringBuilder fullName = new StringBuilder();
    private String selectedGender = "Male";
    private final StringBuilder birthday = new StringBuilder();
    private final StringBuilder email = new StringBuilder();
    private final StringBuilder username = new StringBuilder();
    private final StringBuilder password = new StringBuilder();
    private Role selectedRole = Role.STUDENT;
    private boolean enabledStatus = true;

    private int focusedField = 0;
    private String errorMessage = "";

    public UserFormScreen(UserService userService, AuthService authService, User userToEdit) {
        this.userService = userService;
        this.authService = authService;
        this.userToEdit = userToEdit;

        if (userToEdit != null) {
            if (userToEdit.getFullName() != null) this.fullName.append(userToEdit.getFullName());
            if (userToEdit.getGender() != null && !userToEdit.getGender().isBlank()) {
                this.selectedGender = userToEdit.getGender();
            }
            if (userToEdit.getDateOfBirth() != null) {
                LocalDate dob = userToEdit.getDateOfBirth();
                this.birthday.append(String.format("%02d%02d%04d",
                        dob.getDayOfMonth(), dob.getMonthValue(), dob.getYear()));
            }
            if (userToEdit.getEmail() != null) this.email.append(userToEdit.getEmail());
            if (userToEdit.getUsername() != null) this.username.append(userToEdit.getUsername());
            this.selectedRole = userToEdit.getRole();
            this.enabledStatus = userToEdit.isEnabled();
        }
    }

    private boolean isEditMode() {
        return userToEdit != null;
    }

    private int getNumInputFields() {
        return isEditMode() ? 5 : 7;
    }

    private int getFieldCount() {
        return getNumInputFields() + 2;
    }

    private int getSaveButtonIndex() {
        return getNumInputFields();
    }

    private int getCancelButtonIndex() {
        return getNumInputFields() + 1;
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
            int btnLine = MouseUtil.findButtonRowLine(view());
            if (btnLine != -1 && line >= btnLine && line <= btnLine + 2) {
                int btn = MouseUtil.getClickedButtonIndex(col, "Submit", "Cancel");
                if (btn == 0) {
                    return handleSave();
                } else if (btn == 1) {
                    return ScreenResult.navigate(new UserListScreen(userService, authService));
                }
            }
            return ScreenResult.stay(this);
        }

        if (msg instanceof KeyPressMessage k) {
            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new UserListScreen(userService, authService));
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
                if (focusedField == getSaveButtonIndex()) {
                    return handleSave();
                } else if (focusedField == getCancelButtonIndex()) {
                    return ScreenResult.navigate(new UserListScreen(userService, authService));
                } else {
                    focusedField = (focusedField + 1) % getFieldCount();
                    return ScreenResult.stay(this);
                }
            }

            if (focusedField == getSaveButtonIndex() || focusedField == getCancelButtonIndex()) {
                if (KeyUtil.isLeft(k) || KeyUtil.isRight(k)) {
                    focusedField = (focusedField == getSaveButtonIndex()) ? getCancelButtonIndex() : getSaveButtonIndex();
                    return ScreenResult.stay(this);
                }
            }

            if (isEditMode()) {
                handleEditModeInput(k);
            } else {
                handleCreateModeInput(k);
            }
        }

        return ScreenResult.stay(this);
    }

    private void handleBirthdayInput(KeyPressMessage k) {
        if (KeyUtil.isBackspace(k)) {
            if (!birthday.isEmpty()) birthday.deleteCharAt(birthday.length() - 1);
            errorMessage = "";
        } else if (birthday.length() < 8) {
            char c = extractChar(k);
            if (Character.isDigit(c)) {
                birthday.append(c);
                errorMessage = "";
            }
        }
    }

    private void cycleGender(boolean forward) {
        if ("Male".equalsIgnoreCase(selectedGender)) {
            selectedGender = forward ? "Female" : "Other";
        } else if ("Female".equalsIgnoreCase(selectedGender)) {
            selectedGender = forward ? "Other" : "Male";
        } else {
            selectedGender = forward ? "Male" : "Female";
        }
    }

    private void handleCreateModeInput(KeyPressMessage k) {
        if (focusedField == 1) {
            if (KeyUtil.isLeft(k)) {
                cycleGender(false);
            } else if (KeyUtil.isRight(k)) {
                cycleGender(true);
            }
            return;
        }
        if (focusedField == 2) {
            handleBirthdayInput(k);
            return;
        }
        if (focusedField == 6) {
            if (KeyUtil.isLeft(k)) {
                cycleRole(false);
            } else if (KeyUtil.isRight(k)) {
                cycleRole(true);
            }
            return;
        }
        if (focusedField == 0 || (focusedField >= 3 && focusedField <= 5)) {
            StringBuilder focusedBuffer = switch (focusedField) {
                case 0 -> fullName;
                case 3 -> email;
                case 4 -> username;
                default -> password;
            };

            if (KeyUtil.handleBackspace(focusedBuffer, k)) {
                errorMessage = "";
            } else if (KeyUtil.appendInput(focusedBuffer, k)) {
                errorMessage = "";
            }
        }
    }

    private void handleEditModeInput(KeyPressMessage k) {
        if (focusedField == 0) {
            if (KeyUtil.handleBackspace(fullName, k)) {
                errorMessage = "";
            } else if (KeyUtil.appendInput(fullName, k)) {
                errorMessage = "";
            }
        } else if (focusedField == 1) {
            if (KeyUtil.isLeft(k)) {
                cycleGender(false);
            } else if (KeyUtil.isRight(k)) {
                cycleGender(true);
            }
        } else if (focusedField == 2) {
            handleBirthdayInput(k);
        } else if (focusedField == 3) {
            if (KeyUtil.isLeft(k)) {
                cycleRole(false);
            } else if (KeyUtil.isRight(k)) {
                cycleRole(true);
            }
        } else if (focusedField == 4) {
            if (KeyUtil.isSpace(k)) {
                enabledStatus = !enabledStatus;
            }
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

    private void cycleRole(boolean forward) {
        if (forward) {
            if (selectedRole == Role.STUDENT) selectedRole = Role.TEACHER;
            else if (selectedRole == Role.TEACHER) selectedRole = Role.ADMIN;
            else selectedRole = Role.STUDENT;
        } else {
            if (selectedRole == Role.STUDENT) selectedRole = Role.ADMIN;
            else if (selectedRole == Role.ADMIN) selectedRole = Role.TEACHER;
            else selectedRole = Role.STUDENT;
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
        if ((selectedRole == Role.TEACHER || selectedRole == Role.ADMIN) && parsed.isAfter(LocalDate.now().minusYears(18))) {
            throw new ValidationException("Teachers and administrators must be at least 18 years old.");
        } else if (selectedRole == Role.STUDENT && parsed.isAfter(LocalDate.now().minusYears(5))) {
            throw new ValidationException("Students must be at least 5 years old.");
        }
        return parsed;
    }

    private ScreenResult handleSave() {
        try {
            LocalDate dob = parseBirthday();
            if (isEditMode()) {
                userService.updateUser(userToEdit.getId(), fullName.toString(), selectedRole, enabledStatus, dob, selectedGender);
            } else {
                userService.createUser(email.toString().trim(), username.toString().trim(),
                        password.toString(), fullName.toString().trim(), selectedRole, dob, selectedGender);
            }
            return ScreenResult.navigate(new UserListScreen(userService, authService));
        } catch (ValidationException e) {
            errorMessage = e.getMessage();
            return ScreenResult.stay(this);
        } catch (Exception e) {
            errorMessage = "Save error: " + e.getMessage();
            return ScreenResult.stay(this);
        }
    }

    @Override
    public String view() {
        return UserViews.renderUserForm(
                isEditMode(),
                userToEdit,
                email.toString(),
                username.toString(),
                password.toString(),
                fullName.toString(),
                birthday.toString(),
                selectedGender,
                selectedRole,
                enabledStatus,
                focusedField,
                getSaveButtonIndex(),
                getCancelButtonIndex(),
                errorMessage
        );
    }
}
