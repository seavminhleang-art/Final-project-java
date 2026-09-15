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
    }

    private int getFieldCount() {
        return 12;
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new QuizListScreen(quizService, questionService, subjectService, authService, assessmentType));
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
                if (focusedField == 10) {
                    return handleSave();
                } else if (focusedField == 11) {
                    return ScreenResult.navigate(new QuizListScreen(quizService, questionService, subjectService, authService, assessmentType));
                } else {
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
                int size = subjects.size() + 1;
                if (KeyUtil.isLeft(k)) selectedSubjectIndex = (selectedSubjectIndex - 1 + size) % size;
                else if (KeyUtil.isRight(k) || KeyUtil.isSpace(k)) selectedSubjectIndex = (selectedSubjectIndex + 1) % size;
            }
            case 1 -> handleTextInput(title, k);
            case 2 -> {
                if (assessmentType == AssessmentType.QUIZ) {
                    if (KeyUtil.isSpace(k) || KeyUtil.isRight(k)) cycleQuizQuestionType(true);
                    else if (KeyUtil.isLeft(k)) cycleQuizQuestionType(false);
                }
            }
            case 3 -> handleTextInput(description, k);
            case 4 -> handleTextInput(timeLimit, k);
            case 5 -> handleTextInput(activeHours, k);
            case 6 -> handleTextInput(passScore, k);
            case 7 -> {
                if (KeyUtil.isSpace(k) || KeyUtil.isRight(k) || KeyUtil.isLeft(k)) randomizeQuestions = !randomizeQuestions;
            }
            case 8 -> {
                if (KeyUtil.isSpace(k) || KeyUtil.isRight(k) || KeyUtil.isLeft(k)) randomizeAnswers = !randomizeAnswers;
            }
            case 9 -> {
                if (KeyUtil.isSpace(k) || KeyUtil.isRight(k) || KeyUtil.isLeft(k)) showAnswersAfter = !showAnswersAfter;
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
        if (KeyUtil.isBackspace(k)) {
            if (!buffer.isEmpty()) buffer.deleteCharAt(buffer.length() - 1);
        } else if (k.type() == KeyType.KeyRunes && k.runes() != null) {
            for (char c : k.runes()) {
                if (!Character.isISOControl(c)) buffer.append(c);
            }
            errorMessage = "";
        } else if (k.key() != null && k.key().length() == 1 && !Character.isISOControl(k.key().charAt(0))) {
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
                throw new ValidationException((assessmentType == AssessmentType.EXAM ? "Exam" : "Quiz") + " title is required.");
            }

            Subject subj = subjects.get(selectedSubjectIndex - 1);

            int mins = 0;
            try { mins = Integer.parseInt(timeLimit.toString().trim()); } catch (Exception ignored) {}
            int hours = 0;
            try { hours = Integer.parseInt(activeHours.toString().trim()); } catch (Exception ignored) {}
            int score = 60;
            try { score = Integer.parseInt(passScore.toString().trim()); } catch (Exception ignored) {}

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
                    .timeLimitMins(mins > 0 ? mins : null)
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
        String subjectDisplay = (subjects.isEmpty() || selectedSubjectIndex == 0)
                ? "(No Subject)"
                : subjects.get(selectedSubjectIndex - 1).getCode() + " - " + subjects.get(selectedSubjectIndex - 1).getName();
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