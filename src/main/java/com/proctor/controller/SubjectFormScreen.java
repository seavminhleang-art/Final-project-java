package com.proctor.controller;

import com.proctor.model.service.AuthService;
import com.proctor.exception.ValidationException;
import com.proctor.model.entity.Subject;
import com.proctor.model.service.SubjectService;
import com.proctor.util.KeyUtil;
import com.proctor.view.SubjectViews;
import com.proctor.model.service.UserService;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

public class SubjectFormScreen implements Screen {
    private final SubjectService subjectService;
    private final UserService userService;
    private final AuthService authService;
    private final Subject subjectToEdit;

    private final StringBuilder code = new StringBuilder();
    private final StringBuilder name = new StringBuilder();
    private final StringBuilder description = new StringBuilder();
    private boolean enabledStatus = true;

    private int focusedField = 0;
    private String errorMessage = "";

    public SubjectFormScreen(SubjectService subjectService, UserService userService, AuthService authService, Subject subjectToEdit) {
        this.subjectService = subjectService;
        this.userService = userService;
        this.authService = authService;
        this.subjectToEdit = subjectToEdit;

        if (subjectToEdit != null) {
            this.code.append(subjectToEdit.getCode());
            this.name.append(subjectToEdit.getName());
            if (subjectToEdit.getDescription() != null) {
                this.description.append(subjectToEdit.getDescription());
            }
            this.enabledStatus = subjectToEdit.isEnabled();
        }
    }

    private boolean isEditMode() {
        return subjectToEdit != null;
    }

    private int getNumInputFields() {
        return 3;
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
                return ScreenResult.navigate(new SubjectListScreen(subjectService, userService, authService));
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
                    return ScreenResult.navigate(new SubjectListScreen(subjectService, userService, authService));
                } else {
                    focusedField = (focusedField + 1) % getFieldCount();
                    return ScreenResult.stay(this);
                }
            }

            if (focusedField == getSaveButtonIndex() || focusedField == getCancelButtonIndex()) {
                if ("left".equals(k.key()) || "right".equals(k.key())) {
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
        StringBuilder focusedBuffer = (focusedField == 0) ? code : (focusedField == 1 ? name : description);
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
    }

    private void handleEditModeInput(KeyPressMessage k) {
        if (focusedField == 0 || focusedField == 1) {
            StringBuilder focusedBuffer = (focusedField == 0) ? name : description;
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
        } else if (focusedField == 2) {
            if (" ".equals(k.key()) || "right".equals(k.key()) || "left".equals(k.key())) {
                enabledStatus = !enabledStatus;
            }
        }
    }

    private ScreenResult handleSave() {
        try {
            if (isEditMode()) {
                subjectService.updateSubject(subjectToEdit.getId(), name.toString(), description.toString(), enabledStatus);
            } else {
                subjectService.createSubject(code.toString(), name.toString(), description.toString());
            }
            return ScreenResult.navigate(new SubjectListScreen(subjectService, userService, authService));
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
        return SubjectViews.renderSubjectForm(
                isEditMode(),
                isEditMode() ? subjectToEdit.getCode() : code.toString(),
                name.toString(),
                description.toString(),
                enabledStatus,
                focusedField,
                getSaveButtonIndex(),
                getCancelButtonIndex(),
                errorMessage
        );
    }
}