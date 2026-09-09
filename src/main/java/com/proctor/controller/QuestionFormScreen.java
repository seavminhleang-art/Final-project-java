package com.proctor.controller;

import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.enums.Difficulty;
import com.proctor.model.enums.QuestionType;
import com.proctor.exception.ValidationException;
import com.proctor.model.entity.Question;
import com.proctor.model.entity.QuestionOption;
import com.proctor.model.service.QuestionService;
import com.proctor.model.entity.Quiz;
import com.proctor.model.repository.QuizRepository;
import com.proctor.model.service.QuizService;
import com.proctor.model.entity.Subject;
import com.proctor.model.service.SubjectService;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.QuestionViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

import java.util.ArrayList;
import java.util.List;

public class QuestionFormScreen implements Screen {
    private final QuestionService questionService;
    private final SubjectService subjectService;
    private final AuthService authService;
    private final Question questionToEdit;
    private final Quiz quizContext;

    private final StringBuilder subjectName = new StringBuilder();
    private QuestionType selectedType = QuestionType.MCQ;
    private Difficulty selectedDifficulty = Difficulty.MEDIUM;
    private final StringBuilder pointsBuffer = new StringBuilder("1.0");
    private final StringBuilder questionText = new StringBuilder();
    private final StringBuilder explanation = new StringBuilder();

    private final StringBuilder optionA = new StringBuilder();
    private final StringBuilder optionB = new StringBuilder();
    private final StringBuilder optionC = new StringBuilder();
    private final StringBuilder optionD = new StringBuilder();
    private int correctMcqIndex = 0;

    private boolean tfCorrectIsTrue = true;

    private int focusedField = 0;
    private String errorMessage = "";

    public QuestionFormScreen(QuestionService questionService, SubjectService subjectService, AuthService authService, Question questionToEdit) {
        this(questionService, subjectService, authService, questionToEdit, null);
    }

    public QuestionFormScreen(QuestionService questionService, SubjectService subjectService, AuthService authService, Question questionToEdit, Quiz quizContext) {
        this.questionService = questionService;
        this.subjectService = subjectService;
        this.authService = authService;
        this.questionToEdit = questionToEdit;
        this.quizContext = quizContext;

        if (quizContext != null && quizContext.getSubjectId() != null) {
            subjectService.getSubjectById(quizContext.getSubjectId()).ifPresent(s -> {
                this.subjectName.append(s.getCode()).append(" - ").append(s.getName());
            });
        } else if (questionToEdit != null && questionToEdit.getSubjectId() != null) {
            subjectService.getSubjectById(questionToEdit.getSubjectId()).ifPresent(s -> {
                this.subjectName.append(s.getCode()).append(" - ").append(s.getName());
            });
        }

        if (questionToEdit != null) {
            this.selectedType = questionToEdit.getQuestionType();
            this.selectedDifficulty = questionToEdit.getDifficulty();
            this.pointsBuffer.setLength(0);
            this.pointsBuffer.append(questionToEdit.getPoints());
            this.questionText.append(questionToEdit.getQuestionText());
            if (questionToEdit.getExplanation() != null) {
                this.explanation.append(questionToEdit.getExplanation());
            }

            if (selectedType == QuestionType.MCQ && questionToEdit.getOptions() != null) {
                List<QuestionOption> opts = questionToEdit.getOptions();
                if (opts.size() > 0) optionA.append(opts.get(0).getOptionText());
                if (opts.size() > 1) optionB.append(opts.get(1).getOptionText());
                if (opts.size() > 2) optionC.append(opts.get(2).getOptionText());
                if (opts.size() > 3) optionD.append(opts.get(3).getOptionText());
                for (int i = 0; i < opts.size(); i++) {
                    if (opts.get(i).isCorrect()) correctMcqIndex = i;
                }
            } else if (selectedType == QuestionType.TRUE_FALSE && questionToEdit.getOptions() != null) {
                for (QuestionOption opt : questionToEdit.getOptions()) {
                    if ("True".equalsIgnoreCase(opt.getOptionText())) {
                        tfCorrectIsTrue = opt.isCorrect();
                    }
                }
            }
        } else if (isLockedQuizType()) {
            this.selectedType = quizContext.getQuizQuestionType();
        }
    }

    private boolean isLockedQuizType() {
        return quizContext != null && quizContext.getAssessmentType() == com.proctor.model.enums.AssessmentType.QUIZ
                && quizContext.getQuizQuestionType() != null;
    }

    private boolean isPinnedQuiz() {
        return quizContext != null;
    }

    private int getNumInputFields() {
        int base = isPinnedQuiz() ? 0 : 1;
        if (selectedType == QuestionType.MCQ) return base + 10;
        if (selectedType == QuestionType.TRUE_FALSE) return base + 6;
        return base + 5;
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

    private ScreenResult returnToPreviousScreen() {
        if (quizContext != null) {
            return ScreenResult.navigate(new QuizQuestionEditorScreen(quizContext, new QuizService(new QuizRepository()), questionService, subjectService, authService));
        }
        return ScreenResult.navigate(new QuestionListScreen(questionService, subjectService, authService));
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof KeyPressMessage k) {
            if (KeyUtil.isEsc(k)) {
                return returnToPreviousScreen();
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
                if (focusedField == getSaveButtonIndex()) {
                    return handleSave();
                } else if (focusedField == getCancelButtonIndex()) {
                    return returnToPreviousScreen();
                } else if (focusedField == getNumInputFields() - 1) {
                    focusedField = getSaveButtonIndex();
                    return ScreenResult.stay(this);
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

            handleFieldInput(k);
        }
        return ScreenResult.stay(this);
    }

    private void handleFieldInput(KeyPressMessage k) {
        int idx = focusedField;
        if (!isPinnedQuiz()) {
            if (idx == 0) {
                handleTextInput(subjectName, k);
                return;
            }
            idx -= 1;
        }

        switch (idx) {
            case 0 -> {
                if (isLockedQuizType()) {
                    errorMessage = "This quiz is strictly confined to " + quizContext.getQuizQuestionType() + " questions.";
                    return;
                }
                if (KeyUtil.isLeft(k)) cycleType(false);
                else if (KeyUtil.isRight(k) || KeyUtil.isSpace(k)) cycleType(true);
            }
            case 1 -> {
                if (KeyUtil.isLeft(k)) cycleDifficulty(false);
                else if (KeyUtil.isRight(k) || KeyUtil.isSpace(k)) cycleDifficulty(true);
            }
            case 2 -> handleTextInput(pointsBuffer, k);
            case 3 -> handleTextInput(questionText, k);
            default -> handleTypeSpecificInput(idx, k);
        }
    }

    private void handleTypeSpecificInput(int relativeIndex, KeyPressMessage k) {
        if (selectedType == QuestionType.MCQ) {
            switch (relativeIndex) {
                case 4 -> handleTextInput(optionA, k);
                case 5 -> handleTextInput(optionB, k);
                case 6 -> handleTextInput(optionC, k);
                case 7 -> handleTextInput(optionD, k);
                case 8 -> {
                    if (KeyUtil.isLeft(k)) correctMcqIndex = (correctMcqIndex - 1 + 4) % 4;
                    else if (KeyUtil.isRight(k) || KeyUtil.isSpace(k)) correctMcqIndex = (correctMcqIndex + 1) % 4;
                }
                case 9 -> handleTextInput(explanation, k);
            }
        } else if (selectedType == QuestionType.TRUE_FALSE) {
            switch (relativeIndex) {
                case 4 -> {
                    if (KeyUtil.isLeft(k) || KeyUtil.isRight(k) || KeyUtil.isSpace(k)) {
                        tfCorrectIsTrue = !tfCorrectIsTrue;
                    }
                }
                case 5 -> handleTextInput(explanation, k);
            }
        } else {
            if (relativeIndex == 4) handleTextInput(explanation, k);
        }
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

    private void cycleType(boolean forward) {
        if (forward) {
            if (selectedType == QuestionType.MCQ) selectedType = QuestionType.TRUE_FALSE;
            else if (selectedType == QuestionType.TRUE_FALSE) selectedType = QuestionType.SHORT_ANSWER;
            else selectedType = QuestionType.MCQ;
        } else {
            if (selectedType == QuestionType.MCQ) selectedType = QuestionType.SHORT_ANSWER;
            else if (selectedType == QuestionType.SHORT_ANSWER) selectedType = QuestionType.TRUE_FALSE;
            else selectedType = QuestionType.MCQ;
        }
        if (focusedField >= getFieldCount()) focusedField = getFieldCount() - 1;
    }

    private void cycleDifficulty(boolean forward) {
        if (forward) {
            if (selectedDifficulty == Difficulty.EASY) selectedDifficulty = Difficulty.MEDIUM;
            else if (selectedDifficulty == Difficulty.MEDIUM) selectedDifficulty = Difficulty.HARD;
            else selectedDifficulty = Difficulty.EASY;
        } else {
            if (selectedDifficulty == Difficulty.EASY) selectedDifficulty = Difficulty.HARD;
            else if (selectedDifficulty == Difficulty.HARD) selectedDifficulty = Difficulty.MEDIUM;
            else selectedDifficulty = Difficulty.EASY;
        }
    }

    private ScreenResult handleSave() {
        try {
            if (questionText.toString().trim().isBlank()) {
                throw new ValidationException("Question text cannot be blank.");
            }

            Integer subjId = null;
            if (quizContext != null) {
                subjId = quizContext.getSubjectId();
            } else {
                if (subjectName.toString().trim().isBlank()) {
                    throw new ValidationException("Subject is required.");
                }
                Subject s = subjectService.getOrCreateSubject(subjectName.toString());
                subjId = s.getId();
            }

            double pts = 1.0;
            try {
                pts = Double.parseDouble(pointsBuffer.toString().trim());
            } catch (Exception ignored) {}

            User currentTeacher = Session.getCurrentUser().orElse(null);
            Integer teacherId = currentTeacher != null ? currentTeacher.getId() : null;
            Integer quizId = quizContext != null ? quizContext.getId() : (questionToEdit != null ? questionToEdit.getQuizId() : null);

            List<QuestionOption> options = new ArrayList<>();
            if (selectedType == QuestionType.MCQ) {
                if (!optionA.isEmpty()) options.add(QuestionOption.builder().optionText(optionA.toString()).correct(correctMcqIndex == 0).optionOrder(1).build());
                if (!optionB.isEmpty()) options.add(QuestionOption.builder().optionText(optionB.toString()).correct(correctMcqIndex == 1).optionOrder(2).build());
                if (!optionC.isEmpty()) options.add(QuestionOption.builder().optionText(optionC.toString()).correct(correctMcqIndex == 2).optionOrder(3).build());
                if (!optionD.isEmpty()) options.add(QuestionOption.builder().optionText(optionD.toString()).correct(correctMcqIndex == 3).optionOrder(4).build());
            } else if (selectedType == QuestionType.TRUE_FALSE) {
                options.add(QuestionOption.builder().optionText("True").correct(tfCorrectIsTrue).optionOrder(1).build());
                options.add(QuestionOption.builder().optionText("False").correct(!tfCorrectIsTrue).optionOrder(2).build());
            }

            Question q = Question.builder()
                    .id(questionToEdit != null ? questionToEdit.getId() : null)
                    .quizId(quizId)
                    .subjectId(subjId)
                    .createdBy(teacherId)
                    .questionText(questionText.toString().trim())
                    .questionType(selectedType)
                    .difficulty(selectedDifficulty)
                    .points(pts)
                    .explanation(explanation.toString().trim())
                    .options(options)
                    .enabled(questionToEdit != null ? questionToEdit.isEnabled() : true)
                    .build();

            if (questionToEdit != null) {
                questionService.updateQuestion(q);
            } else {
                questionService.createQuestion(q);
            }

            return returnToPreviousScreen();
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
        List<StringBuilder> options = List.of(optionA, optionB, optionC, optionD);
        int correctIdx = (selectedType == QuestionType.TRUE_FALSE) ? (tfCorrectIsTrue ? 0 : 1) : correctMcqIndex;
        return QuestionViews.renderQuestionForm(
                questionToEdit != null,
                isPinnedQuiz(),
                isPinnedQuiz() ? quizContext.getTitle() : "",
                subjectName.toString(),
                questionText.toString(),
                selectedType,
                selectedDifficulty,
                pointsBuffer.toString(),
                options,
                correctIdx,
                explanation.toString(),
                focusedField,
                getFieldCount(),
                getSaveButtonIndex(),
                getCancelButtonIndex(),
                errorMessage
        );
    }
}