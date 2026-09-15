package com.proctor.controller;

import com.proctor.model.service.AuthService;
import com.proctor.exception.ValidationException;
import com.proctor.model.entity.Subject;
import com.proctor.model.service.SubjectService;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
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

    private boolean showDeleteModal = false;
    private boolean deleteConfirmFocused = false;

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
        return isEditMode() ? 4 : 3;
    }

    private int getFieldCount() {
        return isEditMode() ? 7 : 5;
    }

    private int getSaveButtonIndex() {
        return getNumInputFields();
    }

    private int getDeleteButtonIndex() {
        return isEditMode() ? getNumInputFields() + 1 : -1;
    }

    private int getCancelButtonIndex() {
        return isEditMode() ? getNumInputFields() + 2 : getNumInputFields() + 1;
    }

    private boolean isButton(int field) {
        return field >= getNumInputFields();
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
            if (showDeleteModal) {
                if (KeyUtil.isLeft(k) || KeyUtil.isRight(k)) {
                    deleteConfirmFocused = !deleteConfirmFocused;
                    return ScreenResult.stay(this);
                }
                if (KeyUtil.isEnter(k)) {
                    if (deleteConfirmFocused) {
                        try {
                            subjectService.deleteSubject(subjectToEdit.getId());
                            return ScreenResult.navigate(new SubjectListScreen(subjectService, userService, authService));
                        } catch (ValidationException e) {
                            showDeleteModal = false;
                            errorMessage = e.getMessage();
                            return ScreenResult.stay(this);
                        }
                    } else {
                        showDeleteModal = false;
                        return ScreenResult.stay(this);
                    }
                }
                if (KeyUtil.isEsc(k)) {
                    showDeleteModal = false;
                    return ScreenResult.stay(this);
                }
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new SubjectListScreen(subjectService, userService, authService));
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
                } else if (focusedField == getDeleteButtonIndex()) {
                    showDeleteModal = true;
                    deleteConfirmFocused = false;
                    return ScreenResult.stay(this);
                } else if (focusedField == getCancelButtonIndex()) {
                    return ScreenResult.navigate(new SubjectListScreen(subjectService, userService, authService));
                } else {
                    focusedField = (focusedField + 1) % getFieldCount();
                    return ScreenResult.stay(this);
                }
            }

            if (isButton(focusedField)) {
                if (KeyUtil.isLeft(k)) {
                    if (isEditMode()) {
                        if (focusedField == getCancelButtonIndex()) focusedField = getDeleteButtonIndex();
                        else if (focusedField == getDeleteButtonIndex()) focusedField = getSaveButtonIndex();
                    } else {
                        focusedField = (focusedField == getSaveButtonIndex()) ? getCancelButtonIndex() : getSaveButtonIndex();
                    }
                    return ScreenResult.stay(this);
                } else if (KeyUtil.isRight(k)) {
                    if (isEditMode()) {
                        if (focusedField == getSaveButtonIndex()) focusedField = getDeleteButtonIndex();
                        else if (focusedField == getDeleteButtonIndex()) focusedField = getCancelButtonIndex();
                    } else {
                        focusedField = (focusedField == getSaveButtonIndex()) ? getCancelButtonIndex() : getSaveButtonIndex();
                    }
                    return ScreenResult.stay(this);
                }
            }

            if (focusedField >= 0 && focusedField <= 2) {
                handleTextInput(k);
            } else if (isEditMode() && focusedField == 3) {
                if (KeyUtil.isSpace(k) || KeyUtil.isRight(k) || KeyUtil.isLeft(k)) {
                    enabledStatus = !enabledStatus;
                }
            }
        }

        return ScreenResult.stay(this);
    }

    private void handleTextInput(KeyPressMessage k) {
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

    private ScreenResult handleSave() {
        try {
            if (isEditMode()) {
                subjectService.updateSubject(subjectToEdit.getId(), code.toString(), name.toString(), description.toString(), enabledStatus);
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
        if (showDeleteModal && subjectToEdit != null) {
            return TuiHelper.confirmationModal(
                    "DELETE SUBJECT",
                    "Are you sure you want to delete subject '" + subjectToEdit.getCode() + "'?",
                    "This will permanently delete the subject and unlink it from any quizzes or questions.",
                    "Delete Subject",
                    "Cancel",
                    deleteConfirmFocused
            );
        }

        return SubjectViews.renderSubjectForm(
                isEditMode(),
                code.toString(),
                name.toString(),
                description.toString(),
                enabledStatus,
                focusedField,
                getSaveButtonIndex(),
                getDeleteButtonIndex(),
                getCancelButtonIndex(),
                errorMessage
        );
    }
}