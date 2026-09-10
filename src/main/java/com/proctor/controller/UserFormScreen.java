package com.proctor.controller;

import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.enums.Role;
import com.proctor.exception.ValidationException;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.UserViews;
import com.proctor.model.service.UserService;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

public class UserFormScreen implements Screen {
    private final UserService userService;
    private final AuthService authService;
    private final User userToEdit;

    private final StringBuilder email = new StringBuilder();
    private final StringBuilder username = new StringBuilder();
    private final StringBuilder password = new StringBuilder();
    private final StringBuilder fullName = new StringBuilder();
    private Role selectedRole = Role.STUDENT;
    private boolean enabledStatus = true;

    private int focusedField = 0;
    private String errorMessage = "";

    public UserFormScreen(UserService userService, AuthService authService, User userToEdit) {
        this.userService = userService;
        this.authService = authService;
        this.userToEdit = userToEdit;

        if (userToEdit != null) {
            if (userToEdit.getEmail() != null) this.email.append(userToEdit.getEmail());
            if (userToEdit.getUsername() != null) this.username.append(userToEdit.getUsername());
            if (userToEdit.getFullName() != null) this.fullName.append(userToEdit.getFullName());
            this.selectedRole = userToEdit.getRole();
            this.enabledStatus = userToEdit.isEnabled();
        }
    }

    private boolean isEditMode() {
        return userToEdit != null;
    }

    private int getNumInputFields() {
        return isEditMode() ? 3 : 5;
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
        if (msg instanceof KeyPressMessage k) {
            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new UserListScreen(userService, authService));
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
                if (focusedField == getSaveButtonIndex() || focusedField == getNumInputFields() - 1) {
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

    private void handleCreateModeInput(KeyPressMessage k) {
        if (focusedField >= 0 && focusedField <= 3) {
            StringBuilder focusedBuffer = switch (focusedField) {
                case 0 -> email;
                case 1 -> username;
                case 2 -> password;
                default -> fullName;
            };

            if (KeyUtil.isBackspace(k)) {
                if (!focusedBuffer.isEmpty()) focusedBuffer.deleteCharAt(focusedBuffer.length() - 1);
            } else if (k.type() == KeyType.KeyRunes && k.runes() != null) {
                for (char c : k.runes()) {
                    if (!Character.isISOControl(c)) focusedBuffer.append(c);
                }
                errorMessage = "";
            } else if (k.key() != null && k.key().length() == 1 && !Character.isISOControl(k.key().charAt(0))) {
                focusedBuffer.append(k.key());
                errorMessage = "";
            }
        } else if (focusedField == 4) {
            if (KeyUtil.isLeft(k)) {
                cycleRole(false);
            } else if (KeyUtil.isRight(k) || KeyUtil.isSpace(k)) {
                cycleRole(true);
            }
        }
    }

    private void handleEditModeInput(KeyPressMessage k) {
        if (focusedField == 0) {
            if (KeyUtil.isBackspace(k)) {
                if (!fullName.isEmpty()) fullName.deleteCharAt(fullName.length() - 1);
            } else if (k.type() == KeyType.KeyRunes && k.runes() != null) {
                for (char c : k.runes()) {
                    if (!Character.isISOControl(c)) fullName.append(c);
                }
                errorMessage = "";
            } else if (k.key() != null && k.key().length() == 1 && !Character.isISOControl(k.key().charAt(0))) {
                fullName.append(k.key());
                errorMessage = "";
            }
        } else if (focusedField == 1) {
            if (KeyUtil.isLeft(k)) {
                cycleRole(false);
            } else if (KeyUtil.isRight(k) || KeyUtil.isSpace(k)) {
                cycleRole(true);
            }
        } else if (focusedField == 2) {
            if (KeyUtil.isLeft(k) || KeyUtil.isRight(k) || KeyUtil.isSpace(k)) {
                enabledStatus = !enabledStatus;
            }
        }
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

    private ScreenResult handleSave() {
        try {
            if (isEditMode()) {
                userService.updateUser(userToEdit.getId(), fullName.toString(), selectedRole, enabledStatus);
            } else {
                userService.createUser(email.toString().trim(), username.toString().trim(), password.toString(), fullName.toString().trim(), selectedRole);
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
                selectedRole,
                enabledStatus,
                focusedField,
                getSaveButtonIndex(),
                getCancelButtonIndex(),
                errorMessage
        );
    }
}