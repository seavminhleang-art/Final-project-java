package com.proctor.controller;

import com.proctor.exception.ValidationException;
import com.proctor.model.entity.Quiz;
import com.proctor.model.entity.Session;
import com.proctor.model.entity.Subject;
import com.proctor.model.entity.User;
import com.proctor.model.enums.AssessmentType;
import com.proctor.model.service.AuthService;
import com.proctor.model.service.QuestionService;
import com.proctor.model.service.QuizService;
import com.proctor.model.service.SubjectService;
import com.proctor.util.KeyUtil;
import com.proctor.util.MouseUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.SpeedQuizViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

import java.util.List;

public class SpeedQuizFormScreen implements Screen {
    private final QuizService quizService;
    private final QuestionService questionService;
    private final SubjectService subjectService;
    private final AuthService authService;
    private final Quiz quizToEdit;

    private final List<Subject> subjects;
    private int selectedSubjectIndex = 0;
    private final StringBuilder title = new StringBuilder();
    private final StringBuilder description = new StringBuilder();
    private final StringBuilder secondsPerQuestion = new StringBuilder("15");
    private final StringBuilder activeHours = new StringBuilder("0");
    private boolean randomizeAnswers = true;
    private boolean showAnswersAfter = true;

    private int focusedField = 0;
    private String errorMessage = "";
    private final InlineSubjectFilter<Subject> subjectFilter;

    public SpeedQuizFormScreen(QuizService quizService, QuestionService questionService, SubjectService subjectService, AuthService authService, Quiz quizToEdit) {
        this.quizService = quizService;
        this.questionService = questionService;
        this.subjectService = subjectService;
        this.authService = authService;
        this.quizToEdit = quizToEdit;
        this.subjects = subjectService.getSubjects(null);

        if (quizToEdit != null) {
            if (quizToEdit.getSubjectId() != null) {
                final Integer sid = quizToEdit.getSubjectId();
                for (int i = 0; i < this.subjects.size(); i++) {
                    if (this.subjects.get(i).getId().equals(sid)) {
                        this.selectedSubjectIndex = i + 1;
                        break;
                    }
                }
            }
            this.title.append(quizToEdit.getTitle());
            if (quizToEdit.getDescription() != null) this.description.append(quizToEdit.getDescription());
            this.secondsPerQuestion.setLength(0);
            this.secondsPerQuestion.append(quizToEdit.getSpeedSecondsPerQuestion() != null ? quizToEdit.getSpeedSecondsPerQuestion() : 15);
            this.activeHours.setLength(0);
            this.activeHours.append(quizToEdit.getActiveDurationHours() != null ? quizToEdit.getActiveDurationHours() : 0);
            this.randomizeAnswers = quizToEdit.isRandomizeAnswers();
            this.showAnswersAfter = quizToEdit.isShowAnswersAfter();
        }

        List<InlineSubjectFilter.Item<Subject>> items = new java.util.ArrayList<>();
        items.add(new InlineSubjectFilter.Item<>(null, "", "(No Subject)"));
        for (Subject s : this.subjects) {
            items.add(new InlineSubjectFilter.Item<>(s, s.getCode(), s.getCode() + " - " + s.getName()));
        }
        this.subjectFilter = new InlineSubjectFilter<>(items);
        this.subjectFilter.setSelectedOriginalIndex(this.selectedSubjectIndex);
    }

    private int getFieldCount() {
        return 9;
    }

    @Override
    public ScreenResult update(Message msg) {
        if (MouseUtil.isWheelUp(msg)) {
            subjectFilter.confirmSearch();
            focusedField = (focusedField - 1 + getFieldCount()) % getFieldCount();
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isWheelDown(msg)) {
            subjectFilter.confirmSearch();
            focusedField = (focusedField + 1) % getFieldCount();
            return ScreenResult.stay(this);
        }

        if (MouseUtil.isLeftClick(msg)) {
            int line = MouseUtil.getLineIndex(msg);
            int col = MouseUtil.getColInLine(msg);
            int btnLine = MouseUtil.findButtonRowLine(view());
            if (btnLine != -1 && line >= btnLine && line <= btnLine + 2) {
                int btn = MouseUtil.getClickedButtonIndex(col, "Save", "Cancel");
                if (btn == 0) {
                    return handleSave();
                } else if (btn == 1) {
                    return ScreenResult.navigate(new QuizListScreen(quizService, questionService, subjectService, authService, AssessmentType.SPEED));
                }
            }
            return ScreenResult.stay(this);
        }

        if (msg instanceof KeyPressMessage k) {
            if (focusedField == 0) {
                if (KeyUtil.isEsc(k)) {
                    if (subjectFilter.getQuery().length() > 0) {
                        subjectFilter.cancelSearch();
                        selectedSubjectIndex = subjectFilter.getSelectedOriginalIndex();
                        return ScreenResult.stay(this);
                    }
                    return ScreenResult.navigate(new QuizListScreen(quizService, questionService, subjectService, authService, AssessmentType.SPEED));
                }
            } else if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new QuizListScreen(quizService, questionService, subjectService, authService, AssessmentType.SPEED));
            }

            if (KeyUtil.isDown(k)) {
                subjectFilter.confirmSearch();
                focusedField = (focusedField + 1) % getFieldCount();
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isUp(k)) {
                subjectFilter.confirmSearch();
                focusedField = (focusedField - 1 + getFieldCount()) % getFieldCount();
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isEnter(k)) {
                if (focusedField == 7) {
                    return handleSave();
                } else if (focusedField == 8) {
                    return ScreenResult.navigate(new QuizListScreen(quizService, questionService, subjectService, authService, AssessmentType.SPEED));
                } else {
                    subjectFilter.confirmSearch();
                    focusedField = (focusedField + 1) % getFieldCount();
                    return ScreenResult.stay(this);
                }
            }

            if (focusedField == 7 || focusedField == 8) {
                if (KeyUtil.isLeft(k) || KeyUtil.isRight(k)) {
                    focusedField = (focusedField == 7) ? 8 : 7;
                    return ScreenResult.stay(this);
                }
            }

            handleFieldInput(k);
        }
        return ScreenResult.stay(this);
    }

    private void handleFieldInput(KeyPressMessage k) {
        switch (focusedField) {
            case 0 -> {
                if (KeyUtil.isTab(k) || KeyUtil.isRight(k)) {
                    subjectFilter.cycleNext();
                    selectedSubjectIndex = subjectFilter.getSelectedOriginalIndex();
                } else if (KeyUtil.isLeft(k)) {
                    subjectFilter.cyclePrev();
                    selectedSubjectIndex = subjectFilter.getSelectedOriginalIndex();
                } else if (KeyUtil.isBackspace(k)) {
                    subjectFilter.handleKey(k);
                    selectedSubjectIndex = subjectFilter.getSelectedOriginalIndex();
                } else {
                    boolean handled = subjectFilter.handleKey(k);
                    if (handled) {
                        selectedSubjectIndex = subjectFilter.getSelectedOriginalIndex();
                    }
                }
            }
            case 1 -> handleTextInput(title, k);
            case 2 -> handleTextInput(description, k);
            case 3 -> handleNumericInput(secondsPerQuestion, k);
            case 4 -> handleNumericInput(activeHours, k);
            case 5 -> {
                if (KeyUtil.isSpace(k) || KeyUtil.isRight(k) || KeyUtil.isLeft(k)) randomizeAnswers = !randomizeAnswers;
            }
            case 6 -> {
                if (KeyUtil.isSpace(k) || KeyUtil.isRight(k) || KeyUtil.isLeft(k)) showAnswersAfter = !showAnswersAfter;
            }
        }
    }

    private void handleTextInput(StringBuilder buffer, KeyPressMessage k) {
        if (KeyUtil.handleBackspace(buffer, k)) {
            errorMessage = "";
        } else if (KeyUtil.appendInput(buffer, k)) {
            errorMessage = "";
        }
    }

    private void handleNumericInput(StringBuilder buffer, KeyPressMessage k) {
        if (KeyUtil.isBackspace(k)) {
            if (!buffer.isEmpty()) buffer.deleteCharAt(buffer.length() - 1);
        } else if (k.key() != null && k.key().length() == 1 && Character.isDigit(k.key().charAt(0))) {
            buffer.append(k.key());
            errorMessage = "";
        }
    }

    private ScreenResult handleSave() {
        try {
            if (selectedSubjectIndex == 0) {
                throw new ValidationException("Subject is required.");
            }
            if (title.toString().trim().isBlank()) {
                throw new ValidationException("Speed Quiz title is required.");
            }

            Subject subj = subjects.get(selectedSubjectIndex - 1);

            int seconds = 15;
            String secStr = secondsPerQuestion.toString().trim();
            if (!secStr.isEmpty()) {
                try {
                    seconds = Integer.parseInt(secStr);
                } catch (NumberFormatException e) {
                    throw new ValidationException("Seconds per question must be a valid number.");
                }
            }
            if (seconds < 5) {
                throw new ValidationException("Seconds per question must be at least 5 seconds.");
            }

            int hours = 0;
            String hoursStr = activeHours.toString().trim();
            if (!hoursStr.isEmpty()) {
                try {
                    hours = Integer.parseInt(hoursStr);
                    if (hours < 0) {
                        throw new ValidationException("Active duration must be a non-negative number of hours.");
                    }
                } catch (NumberFormatException e) {
                    throw new ValidationException("Active duration must be a valid number of hours.");
                }
            }

            User currentTeacher = Session.getCurrentUser().orElse(null);
            Integer teacherId = currentTeacher != null ? currentTeacher.getId() : null;

            Quiz q = Quiz.builder()
                    .id(quizToEdit != null ? quizToEdit.getId() : null)
                    .subjectId(subj.getId())
                    .createdBy(teacherId)
                    .assessmentType(AssessmentType.SPEED)
                    .quizQuestionType(null)
                    .title(title.toString().trim())
                    .topic(title.toString().trim())
                    .description(description.toString().trim())
                    .timeLimitMins(null)
                    .speedSecondsPerQuestion(seconds)
                    .activeDurationHours(hours)
                    .passScore(50)
                    .randomizeQuestions(false)
                    .randomizeAnswers(randomizeAnswers)
                    .showAnswersAfter(showAnswersAfter)
                    .published(quizToEdit != null ? quizToEdit.isPublished() : false)
                    .expiresAt(quizToEdit != null ? quizToEdit.getExpiresAt() : null)
                    .build();

            if (quizToEdit != null) {
                quizService.updateQuiz(q);
                return ScreenResult.navigate(new QuizListScreen(quizService, questionService, subjectService, authService, AssessmentType.SPEED));
            } else {
                Quiz created = quizService.createQuiz(q);
                return ScreenResult.navigate(new QuizQuestionEditorScreen(created, quizService, questionService, subjectService, authService));
            }
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
        String subjectDisplay = subjectFilter.getFormDisplay("(No Subject)");
        return SpeedQuizViews.renderSpeedQuizForm(
                quizToEdit != null,
                subjectDisplay,
                title.toString(),
                description.toString(),
                secondsPerQuestion.toString(),
                activeHours.toString(),
                randomizeAnswers,
                showAnswersAfter,
                focusedField,
                errorMessage
        );
    }
}
