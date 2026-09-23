package com.proctor.controller;

import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.enums.AssessmentType;
import com.proctor.model.enums.QuestionType;
import com.proctor.exception.ValidationException;
import com.proctor.model.service.QuestionService;
import com.proctor.model.entity.Quiz;
import com.proctor.model.service.QuizService;
import com.proctor.model.entity.Subject;
import com.proctor.model.service.SubjectService;
import com.proctor.util.KeyUtil;
import com.proctor.util.MouseUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.QuizViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

import java.util.List;

public class QuizFormScreen implements Screen {
    private final QuizService quizService;
    private final QuestionService questionService;
    private final SubjectService subjectService;
    private final AuthService authService;
    private final Quiz quizToEdit;
    private final AssessmentType assessmentType;

    private final List<Subject> subjects;
    private int selectedSubjectIndex = 0;
    private final StringBuilder title = new StringBuilder();
    private QuestionType quizQuestionType = QuestionType.MCQ;
    private final StringBuilder description = new StringBuilder();
    private final StringBuilder timeLimit = new StringBuilder("30");
    private final StringBuilder activeHours = new StringBuilder("0");
    private final StringBuilder passScore = new StringBuilder("50");
    private boolean randomizeQuestions = true;
    private boolean randomizeAnswers = true;
    private boolean showAnswersAfter = true;

    private int focusedField = 0;
    private String errorMessage = "";
    private final InlineSubjectFilter<Subject> subjectFilter;

    public QuizFormScreen(QuizService quizService, QuestionService questionService, SubjectService subjectService, AuthService authService, Quiz quizToEdit) {
        this(quizService, questionService, subjectService, authService, quizToEdit,
                quizToEdit != null && quizToEdit.getAssessmentType() != null ? quizToEdit.getAssessmentType() : AssessmentType.QUIZ);
    }

    public QuizFormScreen(QuizService quizService, QuestionService questionService, SubjectService subjectService,
                          AuthService authService, Quiz quizToEdit, AssessmentType assessmentType) {
        this.quizService = quizService;
        this.questionService = questionService;
        this.subjectService = subjectService;
        this.authService = authService;
        this.quizToEdit = quizToEdit;
        this.assessmentType = assessmentType != null ? assessmentType : AssessmentType.QUIZ;
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
            if (quizToEdit.getQuizQuestionType() != null) {
                this.quizQuestionType = quizToEdit.getQuizQuestionType();
            }
            if (quizToEdit.getDescription() != null) this.description.append(quizToEdit.getDescription());
            this.timeLimit.setLength(0);
            this.timeLimit.append(quizToEdit.getTimeLimitMins() != null ? quizToEdit.getTimeLimitMins() : 0);
            this.passScore.setLength(0);
            this.passScore.append(quizToEdit.getPassScore());
            this.randomizeQuestions = quizToEdit.isRandomizeQuestions();
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
        return 12;
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
                int btn = MouseUtil.getClickedButtonIndex(col, "Submit", "Cancel");
                if (btn == 0) {
                    return handleSave();
                } else if (btn == 1) {
                    return ScreenResult.navigate(new QuizListScreen(quizService, questionService, subjectService, authService, assessmentType));
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
                    return ScreenResult.navigate(new QuizListScreen(quizService, questionService, subjectService, authService, assessmentType));
                }
            } else if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new QuizListScreen(quizService, questionService, subjectService, authService, assessmentType));
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
                if (focusedField == 10) {
                    return handleSave();
                } else if (focusedField == 11) {
                    return ScreenResult.navigate(new QuizListScreen(quizService, questionService, subjectService, authService, assessmentType));
                } else {
                    subjectFilter.confirmSearch();
                    focusedField = (focusedField + 1) % getFieldCount();
                    return ScreenResult.stay(this);
                }
            }

            if (focusedField == 10 || focusedField == 11) {
                if (KeyUtil.isLeft(k) || KeyUtil.isRight(k)) {
                    focusedField = (focusedField == 10) ? 11 : 10;
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
                if (KeyUtil.isRight(k)) {
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
            case 2 -> {
                if (assessmentType == AssessmentType.QUIZ) {
                    if (KeyUtil.isRight(k)) cycleQuizQuestionType(true);
                    else if (KeyUtil.isLeft(k)) cycleQuizQuestionType(false);
                }
            }
            case 3 -> handleTextInput(description, k);
            case 4 -> handleTextInput(timeLimit, k);
            case 5 -> handleTextInput(activeHours, k);
            case 6 -> handleTextInput(passScore, k);
            case 7 -> {
                if (KeyUtil.isSpace(k)) randomizeQuestions = !randomizeQuestions;
            }
            case 8 -> {
                if (KeyUtil.isSpace(k)) randomizeAnswers = !randomizeAnswers;
            }
            case 9 -> {
                if (KeyUtil.isSpace(k)) showAnswersAfter = !showAnswersAfter;
            }
        }
    }

    private void cycleQuizQuestionType(boolean forward) {
        QuestionType[] types = {QuestionType.MCQ, QuestionType.TRUE_FALSE, QuestionType.SHORT_ANSWER};
        int cur = 0;
        for (int i = 0; i < types.length; i++) {
            if (types[i] == quizQuestionType) cur = i;
        }
        int next = forward ? (cur + 1) % types.length : (cur - 1 + types.length) % types.length;
        quizQuestionType = types[next];
    }

    private void handleTextInput(StringBuilder buffer, KeyPressMessage k) {
        if (KeyUtil.handleBackspace(buffer, k)) {
            errorMessage = "";
        } else if (KeyUtil.appendInput(buffer, k)) {
            errorMessage = "";
        }
    }

    private ScreenResult handleSave() {
        try {
            if (selectedSubjectIndex == 0) {
                throw new ValidationException("Subject is required.");
            }
            if (title.toString().trim().isBlank()) {
                throw new ValidationException((assessmentType == AssessmentType.EXAM ? "Exam" : "Quiz") + " title is required.");
            }

            Subject subj = subjects.get(selectedSubjectIndex - 1);

            Integer mins = null;
            String minsStr = timeLimit.toString().trim();
            if (!minsStr.isEmpty()) {
                try {
                    mins = Integer.parseInt(minsStr);
                    if (mins <= 0) {
                        throw new ValidationException("Time limit must be a positive number of minutes.");
                    }
                    if (mins > 1440) {
                        throw new ValidationException("Time limit cannot exceed 1440 minutes (24 hours).");
                    }
                } catch (NumberFormatException e) {
                    throw new ValidationException("Time limit must be a valid number of minutes.");
                }
            }

            int hours = 0;
            String hoursStr = activeHours.toString().trim();
            if (!hoursStr.isEmpty()) {
                try {
                    hours = Integer.parseInt(hoursStr);
                    if (hours < 0) {
                        throw new ValidationException("Active duration must be a non-negative number of hours.");
                    }
                    if (hours > 8760) {
                        throw new ValidationException("Active duration cannot exceed 8760 hours (1 year).");
                    }
                } catch (NumberFormatException e) {
                    throw new ValidationException("Active duration must be a valid number of hours.");
                }
            }

            int score = 60;
            String scoreStr = passScore.toString().trim();
            if (!scoreStr.isEmpty()) {
                try {
                    score = Integer.parseInt(scoreStr);
                } catch (NumberFormatException e) {
                    throw new ValidationException("Passing score must be a valid number.");
                }
            }
            if (score < 1 || score > 100) {
                throw new ValidationException("Passing score must be between 1 and 100.");
            }

            User currentTeacher = Session.getCurrentUser().orElse(null);
            Integer teacherId = currentTeacher != null ? currentTeacher.getId() : null;

            Quiz q = Quiz.builder()
                    .id(quizToEdit != null ? quizToEdit.getId() : null)
                    .subjectId(subj.getId())
                    .createdBy(teacherId)
                    .assessmentType(assessmentType)
                    .quizQuestionType(assessmentType == AssessmentType.QUIZ ? quizQuestionType : null)
                    .title(title.toString().trim())
                    .topic(title.toString().trim())
                    .description(description.toString().trim())
                    .timeLimitMins(mins)
                    .activeDurationHours(hours)
                    .passScore(score)
                    .randomizeQuestions(randomizeQuestions)
                    .randomizeAnswers(randomizeAnswers)
                    .showAnswersAfter(showAnswersAfter)
                    .published(quizToEdit != null ? quizToEdit.isPublished() : false)
                    .expiresAt(quizToEdit != null ? quizToEdit.getExpiresAt() : null)
                    .build();

            if (quizToEdit != null) {
                quizService.updateQuiz(q);
                return ScreenResult.navigate(new QuizListScreen(quizService, questionService, subjectService, authService, assessmentType));
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
        return QuizViews.renderQuizForm(
                assessmentType,
                quizQuestionType,
                quizToEdit != null,
                subjectDisplay,
                title.toString(),
                description.toString(),
                timeLimit.toString(),
                activeHours.toString(),
                passScore.toString(),
                randomizeQuestions,
                randomizeAnswers,
                showAnswersAfter,
                focusedField,
                errorMessage
        );
    }
}