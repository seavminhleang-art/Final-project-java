package com.proctor.controller;

import com.proctor.model.entity.AIQuestionDraft;
import com.proctor.model.service.AIService;
import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.enums.Difficulty;
import com.proctor.model.enums.QuestionType;
import com.proctor.model.entity.Question;
import com.proctor.model.service.QuestionService;
import com.proctor.model.entity.Quiz;
import com.proctor.model.repository.QuizRepository;
import com.proctor.model.service.QuizService;
import com.proctor.model.entity.Subject;
import com.proctor.model.service.SubjectService;
import com.proctor.util.KeyUtil;
import com.proctor.util.ListNavigationHelper;
import com.proctor.util.MouseUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.QuestionViews;
import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AIQuestionGeneratorScreen implements Screen {
    private final AIService aiService;
    private final QuestionService questionService;
    private final SubjectService subjectService;
    private final AuthService authService;
    private final Quiz quizContext;
    private final List<Subject> subjects;

    private int selectedSubjectIndex = 0;
    private final StringBuilder topicBuffer = new StringBuilder();
    private final StringBuilder customPromptBuffer = new StringBuilder();
    private final StringBuilder countBuffer = new StringBuilder("3");
    private QuestionType selectedType = QuestionType.MCQ;
    private Difficulty selectedDifficulty = Difficulty.MEDIUM;
    private int mcqOptionCount = 4;

    private int focusedField = 0;
    private boolean isGenerating = false;
    private boolean reviewingDrafts = false;
    private List<AIQuestionDraft> generatedDrafts = new ArrayList<>();
    private int selectedDraftIndex = 0;
    private String bannerMessage = "";
    private int spinnerTick = 0;
    private long generationStartTime = 0;
    private int activeGenerationId = 0;
    private AtomicBoolean activeCancellation = null;
    private final InlineSubjectFilter<Subject> subjectFilter;

    public record LoadingTickMessage(int generationId) implements Message {}
    public record AIQuestionsGeneratedMessage(int generationId, List<AIQuestionDraft> drafts, String errorMessage) implements Message {
        public AIQuestionsGeneratedMessage(List<AIQuestionDraft> drafts, String errorMessage) {
            this(0, drafts, errorMessage);
        }
    }

    public AIQuestionGeneratorScreen(AIService aiService, QuestionService questionService, SubjectService subjectService, AuthService authService) {
        this(aiService, questionService, subjectService, authService, null);
    }

    public AIQuestionGeneratorScreen(AIService aiService, QuestionService questionService, SubjectService subjectService, AuthService authService, Quiz quizContext) {
        this.aiService = aiService;
        this.questionService = questionService;
        this.subjectService = subjectService;
        this.authService = authService;
        this.quizContext = quizContext;
        this.subjects = subjectService.getSubjects(null);

        if (quizContext != null && quizContext.getSubjectId() != null) {
            final Integer sid = quizContext.getSubjectId();
            for (int i = 0; i < this.subjects.size(); i++) {
                if (this.subjects.get(i).getId().equals(sid)) {
                    this.selectedSubjectIndex = i + 1;
                    break;
                }
            }
        }

        if (isLockedQuizType()) {
            this.selectedType = quizContext.getQuizQuestionType();
        }

        List<InlineSubjectFilter.Item<Subject>> items = new java.util.ArrayList<>();
        items.add(new InlineSubjectFilter.Item<>(null, "", "(No Subject)"));
        for (Subject s : this.subjects) {
            items.add(new InlineSubjectFilter.Item<>(s, s.getCode(), s.getCode() + " - " + s.getName()));
        }
        this.subjectFilter = new InlineSubjectFilter<>(items);
        this.subjectFilter.setSelectedOriginalIndex(this.selectedSubjectIndex);
    }

    private boolean isLockedQuizType() {
        return quizContext != null && quizContext.getAssessmentType() == com.proctor.model.enums.AssessmentType.QUIZ
                && quizContext.getQuizQuestionType() != null;
    }

    private boolean isSpeedQuiz() {
        return quizContext != null && quizContext.getAssessmentType() == com.proctor.model.enums.AssessmentType.SPEED;
    }

    private boolean isPinnedQuiz() {
        return quizContext != null;
    }

    private int getNumInputFields() {
        int base = isPinnedQuiz() ? 5 : 6;
        return selectedType == QuestionType.MCQ ? base + 1 : base;
    }

    private int getFieldCount() {
        return getNumInputFields() + 2;
    }

    private int getGenerateButtonIndex() {
        return getNumInputFields();
    }

    private int getCancelButtonIndex() {
        return getNumInputFields() + 1;
    }

    private ScreenResult returnToPreviousScreen() {
        if (quizContext != null) {
            return ScreenResult.navigate(new QuizQuestionEditorScreen(quizContext, new QuizService(new QuizRepository()), questionService, subjectService, authService));
        }
        return ScreenResult.navigate(new QuestionBankScreen(questionService, subjectService, authService));
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof LoadingTickMessage t) {
            if (isGenerating && t.generationId() == this.activeGenerationId) {
                spinnerTick++;
                return ScreenResult.stay(this, Command.tick(Duration.ofMillis(80), time -> new LoadingTickMessage(this.activeGenerationId)));
            }
            return ScreenResult.stay(this);
        }

        if (msg instanceof AIQuestionsGeneratedMessage m) {
            if (!isGenerating || (m.generationId() != 0 && m.generationId() != this.activeGenerationId)) {
                return ScreenResult.stay(this);
            }
            isGenerating = false;
            if (m.errorMessage() != null) {
                bannerMessage = TuiHelper.red("✖ " + m.errorMessage());
                return ScreenResult.stay(this);
            }
            if (m.drafts() != null && !m.drafts().isEmpty()) {
                this.generatedDrafts = m.drafts();
                this.reviewingDrafts = true;
                this.selectedDraftIndex = 0;
                this.bannerMessage = "";
            } else {
                this.bannerMessage = TuiHelper.red("✖ No questions returned by AI model.");
            }
            return ScreenResult.stay(this);
        }

        if (isGenerating) {
            if (msg instanceof KeyPressMessage k && KeyUtil.isEsc(k)) {
                isGenerating = false;
                activeGenerationId++;
                if (activeCancellation != null) {
                    activeCancellation.set(true);
                }
                bannerMessage = TuiHelper.yellow("Generation cancelled.");
            } else if (MouseUtil.isLeftClick(msg)) {
                int line = MouseUtil.getLineIndex(msg);
                int btnLine = MouseUtil.findButtonRowLine(view());
                if (btnLine != -1 && line >= btnLine && line <= btnLine + 2) {
                    isGenerating = false;
                    activeGenerationId++;
                    if (activeCancellation != null) {
                        activeCancellation.set(true);
                    }
                    bannerMessage = TuiHelper.yellow("Generation cancelled.");
                }
            }
            return ScreenResult.stay(this);
        }

        if (reviewingDrafts) {
            if (MouseUtil.isWheelUp(msg)) {
                if (!generatedDrafts.isEmpty()) {
                    selectedDraftIndex = (selectedDraftIndex - 1 + generatedDrafts.size()) % generatedDrafts.size();
                }
                return ScreenResult.stay(this);
            }
            if (MouseUtil.isWheelDown(msg)) {
                if (!generatedDrafts.isEmpty()) {
                    selectedDraftIndex = (selectedDraftIndex + 1) % generatedDrafts.size();
                }
                return ScreenResult.stay(this);
            }
            if (MouseUtil.isLeftClick(msg)) {
                int line = MouseUtil.getLineIndex(msg);
                int col = MouseUtil.getColInLine(msg);
                int btnLine = MouseUtil.findButtonRowLine(view());
                if (btnLine != -1 && line >= btnLine && line <= btnLine + 2) {
                    int btn = MouseUtil.getClickedButtonIndex(col, "Save Drafts", "Cancel");
                    if (btn == 0) {
                        try {
                            saveAllDrafts();
                            return returnToPreviousScreen();
                        } catch (Exception ex) {
                            bannerMessage = TuiHelper.red("✖ Failed to save questions: " + ex.getMessage());
                            return ScreenResult.stay(this);
                        }
                    } else if (btn == 1) {
                        reviewingDrafts = false;
                        return ScreenResult.stay(this);
                    }
                }
                int pagLine = MouseUtil.findPaginationLine(view());
                if (pagLine != -1 && line == pagLine && !generatedDrafts.isEmpty()) {
                    selectedDraftIndex = ListNavigationHelper.handlePaginationClick(col, selectedDraftIndex, generatedDrafts.size(), 3);
                }
                return ScreenResult.stay(this);
            }
        } else {
            if (MouseUtil.isWheelUp(msg)) {
                if (!isPinnedQuiz()) subjectFilter.confirmSearch();
                focusedField = (focusedField - 1 + getFieldCount()) % getFieldCount();
                return ScreenResult.stay(this);
            }
            if (MouseUtil.isWheelDown(msg)) {
                if (!isPinnedQuiz()) subjectFilter.confirmSearch();
                focusedField = (focusedField + 1) % getFieldCount();
                return ScreenResult.stay(this);
            }
            if (MouseUtil.isLeftClick(msg)) {
                int line = MouseUtil.getLineIndex(msg);
                int col = MouseUtil.getColInLine(msg);
                int btnLine = MouseUtil.findButtonRowLine(view());
                if (btnLine != -1 && line >= btnLine && line <= btnLine + 2) {
                    int btn = MouseUtil.getClickedButtonIndex(col, "Generate Questions", "Cancel");
                    if (btn == 0) {
                        return startAsyncGeneration();
                    } else if (btn == 1) {
                        return returnToPreviousScreen();
                    }
                }
                String hintAction = MouseUtil.getClickedHintAction(view(), line, col);
                if (hintAction != null && "Esc".equals(hintAction)) {
                    return returnToPreviousScreen();
                }
                return ScreenResult.stay(this);
            }
        }

        if (msg instanceof KeyPressMessage k) {
            if (reviewingDrafts) {
                if (KeyUtil.isEsc(k)) {
                    reviewingDrafts = false;
                    return ScreenResult.stay(this);
                } else if (KeyUtil.isUp(k)) {
                    if (!generatedDrafts.isEmpty()) {
                        selectedDraftIndex = (selectedDraftIndex - 1 + generatedDrafts.size()) % generatedDrafts.size();
                    }
                } else if (KeyUtil.isDown(k)) {
                    if (!generatedDrafts.isEmpty()) {
                        selectedDraftIndex = (selectedDraftIndex + 1) % generatedDrafts.size();
                    }
                } else if (KeyUtil.isLeft(k)) {
                    selectedDraftIndex = ListNavigationHelper.prevPage(selectedDraftIndex, 3);
                } else if (KeyUtil.isRight(k)) {
                    selectedDraftIndex = ListNavigationHelper.nextPage(selectedDraftIndex, generatedDrafts.size(), 3);
                } else if (KeyUtil.isEnter(k)) {
                    try {
                        saveAllDrafts();
                        return returnToPreviousScreen();
                    } catch (Exception ex) {
                        bannerMessage = TuiHelper.red("✖ Failed to save questions: " + ex.getMessage());
                        return ScreenResult.stay(this);
                    }
                }
                return ScreenResult.stay(this);
            }

            if (!isPinnedQuiz() && focusedField == 0) {
                if (KeyUtil.isEsc(k)) {
                    if (subjectFilter.getQuery().length() > 0) {
                        subjectFilter.cancelSearch();
                        selectedSubjectIndex = subjectFilter.getSelectedOriginalIndex();
                        return ScreenResult.stay(this);
                    }
                    return returnToPreviousScreen();
                }
            } else if (KeyUtil.isEsc(k)) {
                return returnToPreviousScreen();
            }

            if (KeyUtil.isDown(k)) {
                if (!isPinnedQuiz()) subjectFilter.confirmSearch();
                focusedField = (focusedField + 1) % getFieldCount();
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isUp(k)) {
                if (!isPinnedQuiz()) subjectFilter.confirmSearch();
                focusedField = (focusedField - 1 + getFieldCount()) % getFieldCount();
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isEnter(k)) {
                if (focusedField == getGenerateButtonIndex()) {
                    return startAsyncGeneration();
                } else if (focusedField == getCancelButtonIndex()) {
                    return returnToPreviousScreen();
                } else {
                    if (!isPinnedQuiz()) subjectFilter.confirmSearch();
                    focusedField = (focusedField + 1) % getFieldCount();
                    return ScreenResult.stay(this);
                }
            }

            if (focusedField == getGenerateButtonIndex() || focusedField == getCancelButtonIndex()) {
                if (KeyUtil.isLeft(k) || KeyUtil.isRight(k)) {
                    focusedField = (focusedField == getGenerateButtonIndex()) ? getCancelButtonIndex() : getGenerateButtonIndex();
                    return ScreenResult.stay(this);
                }
            }

            handleFormInput(k);
        }
        return ScreenResult.stay(this);
    }

    private void handleFormInput(KeyPressMessage k) {
        int idx = focusedField;
        if (!isPinnedQuiz()) {
            if (idx == 0) {
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
                return;
            }
            idx -= 1;
        }

        switch (idx) {
            case 0 -> handleTextInput(topicBuffer, k);
            case 1 -> handleTextInput(customPromptBuffer, k);
            case 2 -> handleTextInput(countBuffer, k);
            case 3 -> {
                if (isLockedQuizType()) {
                    bannerMessage = TuiHelper.yellow("This quiz is strictly confined to " + quizContext.getQuizQuestionType() + " questions.");
                    return;
                }
                if (isSpeedQuiz()) {
                    if (KeyUtil.isLeft(k) || KeyUtil.isRight(k)) {
                        selectedType = (selectedType == QuestionType.MCQ) ? QuestionType.TRUE_FALSE : QuestionType.MCQ;
                    }
                    if (focusedField >= getFieldCount()) focusedField = getFieldCount() - 1;
                    return;
                }
                if (KeyUtil.isLeft(k)) {
                    if (selectedType == QuestionType.MCQ) selectedType = QuestionType.SHORT_ANSWER;
                    else if (selectedType == QuestionType.SHORT_ANSWER) selectedType = QuestionType.TRUE_FALSE;
                    else selectedType = QuestionType.MCQ;
                } else if (KeyUtil.isRight(k)) {
                    if (selectedType == QuestionType.MCQ) selectedType = QuestionType.TRUE_FALSE;
                    else if (selectedType == QuestionType.TRUE_FALSE) selectedType = QuestionType.SHORT_ANSWER;
                    else selectedType = QuestionType.MCQ;
                }
                if (focusedField >= getFieldCount()) focusedField = getFieldCount() - 1;
            }
            case 4 -> {
                if (KeyUtil.isLeft(k)) {
                    if (selectedDifficulty == Difficulty.EASY) selectedDifficulty = Difficulty.HARD;
                    else if (selectedDifficulty == Difficulty.HARD) selectedDifficulty = Difficulty.MEDIUM;
                    else selectedDifficulty = Difficulty.EASY;
                } else if (KeyUtil.isRight(k)) {
                    if (selectedDifficulty == Difficulty.EASY) selectedDifficulty = Difficulty.MEDIUM;
                    else if (selectedDifficulty == Difficulty.MEDIUM) selectedDifficulty = Difficulty.HARD;
                    else selectedDifficulty = Difficulty.EASY;
                }
            }
            case 5 -> {
                if (selectedType == QuestionType.MCQ) {
                    if (KeyUtil.isLeft(k)) {
                        if (mcqOptionCount == 2) mcqOptionCount = 4;
                        else if (mcqOptionCount == 4) mcqOptionCount = 3;
                        else mcqOptionCount = 2;
                    } else if (KeyUtil.isRight(k)) {
                        if (mcqOptionCount == 2) mcqOptionCount = 3;
                        else if (mcqOptionCount == 3) mcqOptionCount = 4;
                        else mcqOptionCount = 2;
                    }
                }
            }
        }
    }

    private void handleTextInput(StringBuilder buffer, KeyPressMessage k) {
        if (!KeyUtil.handleBackspace(buffer, k)) {
            KeyUtil.appendInput(buffer, k);
        }
    }

    private ScreenResult startAsyncGeneration() {
        if (topicBuffer.toString().trim().isBlank()) {
            bannerMessage = TuiHelper.red("✖ Topic cannot be blank.");
            return ScreenResult.stay(this);
        }
        if (topicBuffer.toString().trim().length() > 200) {
            bannerMessage = TuiHelper.red("✖ Topic must not exceed 200 characters.");
            return ScreenResult.stay(this);
        }
        if (customPromptBuffer.toString().trim().length() > 500) {
            bannerMessage = TuiHelper.red("✖ Additional instructions must not exceed 500 characters.");
            return ScreenResult.stay(this);
        }
        if (!isPinnedQuiz() && selectedSubjectIndex == 0) {
            bannerMessage = TuiHelper.red("✖ Subject is required.");
            return ScreenResult.stay(this);
        }

        int count = 3;
        String countStr = countBuffer.toString().trim();
        if (!countStr.isEmpty()) {
            try {
                count = Integer.parseInt(countStr);
            } catch (NumberFormatException e) {
                bannerMessage = TuiHelper.red("✖ Question count must be a valid number (1-10).");
                return ScreenResult.stay(this);
            }
        }
        if (count < 1 || count > 10) {
            bannerMessage = TuiHelper.red("✖ Question count must be between 1 and 10.");
            return ScreenResult.stay(this);
        }

        isGenerating = true;
        spinnerTick = 0;
        generationStartTime = System.currentTimeMillis();
        final int genId = ++activeGenerationId;
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        this.activeCancellation = cancelled;
        bannerMessage = "";

        final int finalCount = count;
        final String topic = topicBuffer.toString().trim();
        final String customPrompt = customPromptBuffer.toString().trim();
        final QuestionType type = selectedType;
        final Difficulty diff = selectedDifficulty;
        final int optsPerMcq = mcqOptionCount;
        final String subj = (selectedSubjectIndex > 0 && selectedSubjectIndex <= subjects.size())
                ? subjects.get(selectedSubjectIndex - 1).getCode() + " - " + subjects.get(selectedSubjectIndex - 1).getName()
                : "";
        final String fullTopic = !subj.isBlank() ? (subj + ": " + topic) : topic;

        Command genCmd = () -> {
            try {
                List<AIQuestionDraft> drafts = aiService.generateQuestions(fullTopic, finalCount, type, diff, optsPerMcq, customPrompt);
                if (cancelled.get()) {
                    return new AIQuestionsGeneratedMessage(genId, null, "Cancelled");
                }
                return new AIQuestionsGeneratedMessage(genId, drafts, null);
            } catch (Exception e) {
                if (cancelled.get()) {
                    return new AIQuestionsGeneratedMessage(genId, null, "Cancelled");
                }
                String err = e.getMessage();
                if (err == null || err.isBlank()) {
                    Throwable cause = e.getCause();
                    if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
                        err = cause.getMessage();
                    } else {
                        err = "Failed to generate questions with AI model: " + e.getClass().getSimpleName();
                    }
                }
                return new AIQuestionsGeneratedMessage(genId, null, err);
            }
        };

        Command tickCmd = Command.tick(Duration.ofMillis(80), time -> new LoadingTickMessage(genId));

        return ScreenResult.stay(this, Command.batch(genCmd, tickCmd));
    }

    private void saveAllDrafts() {
        User teacher = Session.getCurrentUser().orElse(null);
        Integer teacherId = teacher != null ? teacher.getId() : null;

        Integer subjId;
        if (quizContext != null) {
            subjId = quizContext.getSubjectId();
        } else {
            subjId = (selectedSubjectIndex > 0 && selectedSubjectIndex <= subjects.size())
                    ? subjects.get(selectedSubjectIndex - 1).getId() : null;
        }

        Integer quizId = quizContext != null ? quizContext.getId() : null;

        for (AIQuestionDraft draft : generatedDrafts) {
            Question q = Question.builder()
                    .quizId(quizId)
                    .subjectId(subjId)
                    .createdBy(teacherId)
                    .questionText(draft.getQuestionText())
                    .questionType(draft.getQuestionType())
                    .difficulty(draft.getDifficulty())
                    .points(draft.getPoints())
                    .explanation(draft.getExplanation())
                    .aiGenerated(true)
                    .enabled(true)
                    .options(draft.getOptions())
                    .build();
            questionService.createQuestion(q);
        }
    }

    @Override
    public String view() {
        if (isGenerating) {
            int elapsedSeconds = (int) Math.max(0, (System.currentTimeMillis() - generationStartTime) / 1000);
            return QuestionViews.renderAIQuestionLoading(topicBuffer.toString(), spinnerTick, elapsedSeconds);
        }
        if (reviewingDrafts) {
            String targetStr = quizContext != null ? "Quiz: " + quizContext.getTitle() : "Question Bank";
            return QuestionViews.renderAIQuestionReview(generatedDrafts, selectedDraftIndex, targetStr, bannerMessage);
        }
        String subjectDisplay = !isPinnedQuiz()
                ? subjectFilter.getFormDisplay("(No Subject)")
                : (subjects.isEmpty() || selectedSubjectIndex == 0
                    ? "(No Subject)"
                    : subjects.get(selectedSubjectIndex - 1).getCode() + " - " + subjects.get(selectedSubjectIndex - 1).getName());
        return QuestionViews.renderAIQuestionForm(
                isPinnedQuiz(),
                isPinnedQuiz() ? quizContext.getTitle() : "",
                subjectDisplay,
                topicBuffer.toString(),
                customPromptBuffer.toString(),
                countBuffer.toString(),
                selectedType,
                selectedDifficulty,
                mcqOptionCount,
                focusedField,
                getGenerateButtonIndex(),
                getCancelButtonIndex(),
                bannerMessage
        );
    }
}