package com.proctor.controller;

import com.proctor.model.entity.AIQuestionDraft;
import com.proctor.model.service.AIService;
import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.enums.Difficulty;
import com.proctor.model.enums.QuestionType;
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

public class AIQuestionGeneratorScreen implements Screen {
    private final AIService aiService;
    private final QuestionService questionService;
    private final SubjectService subjectService;
    private final AuthService authService;
    private final Quiz quizContext;

    private final StringBuilder subjectName = new StringBuilder();
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

    public record AIQuestionsGeneratedMessage(List<AIQuestionDraft> drafts, String errorMessage) implements Message {}

    public AIQuestionGeneratorScreen(AIService aiService, QuestionService questionService, SubjectService subjectService, AuthService authService) {
        this(aiService, questionService, subjectService, authService, null);
    }

    public AIQuestionGeneratorScreen(AIService aiService, QuestionService questionService, SubjectService subjectService, AuthService authService, Quiz quizContext) {
        this.aiService = aiService;
        this.questionService = questionService;
        this.subjectService = subjectService;
        this.authService = authService;
        this.quizContext = quizContext;

        if (quizContext != null) {
            String initialTopic = (quizContext.getTopic() != null && !quizContext.getTopic().isBlank())
                    ? quizContext.getTopic() : quizContext.getTitle();
            this.topicBuffer.append(initialTopic);

            if (quizContext.getSubjectId() != null) {
                subjectService.getSubjectById(quizContext.getSubjectId()).ifPresent(s -> {
                    this.subjectName.append(s.getCode()).append(" - ").append(s.getName());
                });
            }
        } else {
            this.subjectName.append("General");
            this.topicBuffer.append("General Assessment");
        }
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
        return ScreenResult.navigate(new TeacherDashboardScreen(authService, questionService, subjectService));
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof AIQuestionsGeneratedMessage m) {
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
            spinnerTick++;
            if (msg instanceof KeyPressMessage k && KeyUtil.isEsc(k)) {
                isGenerating = false;
                bannerMessage = TuiHelper.yellow("Generation cancelled.");
            }
            return ScreenResult.stay(this);
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
                } else if ("s".equalsIgnoreCase(k.key()) || KeyUtil.isEnter(k)) {
                    saveAllDrafts();
                    return returnToPreviousScreen();
                }
                return ScreenResult.stay(this);
            }

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
                if (focusedField == getGenerateButtonIndex() || focusedField == getNumInputFields() - 1) {
                    return startAsyncGeneration();
                } else if (focusedField == getCancelButtonIndex()) {
                    return returnToPreviousScreen();
                } else {
                    focusedField = (focusedField + 1) % getFieldCount();
                    return ScreenResult.stay(this);
                }
            }

            if (focusedField == getGenerateButtonIndex() || focusedField == getCancelButtonIndex()) {
                if ("left".equals(k.key()) || "right".equals(k.key())) {
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
                handleTextInput(subjectName, k);
                return;
            }
            idx -= 1;
        }

        switch (idx) {
            case 0 -> handleTextInput(topicBuffer, k);
            case 1 -> handleTextInput(customPromptBuffer, k);
            case 2 -> handleTextInput(countBuffer, k);
            case 3 -> {
                if (KeyUtil.isLeft(k)) {
                    if (selectedType == QuestionType.MCQ) selectedType = QuestionType.SHORT_ANSWER;
                    else if (selectedType == QuestionType.SHORT_ANSWER) selectedType = QuestionType.TRUE_FALSE;
                    else selectedType = QuestionType.MCQ;
                } else if (KeyUtil.isRight(k) || KeyUtil.isSpace(k)) {
                    if (selectedType == QuestionType.MCQ) selectedType = QuestionType.TRUE_FALSE;
                    else if (selectedType == QuestionType.TRUE_FALSE) selectedType = QuestionType.SHORT_ANSWER;
                    else selectedType = QuestionType.MCQ;
                }
            }
            case 4 -> {
                if (KeyUtil.isLeft(k)) {
                    if (selectedDifficulty == Difficulty.EASY) selectedDifficulty = Difficulty.HARD;
                    else if (selectedDifficulty == Difficulty.HARD) selectedDifficulty = Difficulty.MEDIUM;
                    else selectedDifficulty = Difficulty.EASY;
                } else if (KeyUtil.isRight(k) || KeyUtil.isSpace(k)) {
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
                    } else if (KeyUtil.isRight(k) || KeyUtil.isSpace(k)) {
                        if (mcqOptionCount == 2) mcqOptionCount = 3;
                        else if (mcqOptionCount == 3) mcqOptionCount = 4;
                        else mcqOptionCount = 2;
                    }
                }
            }
        }
    }

    private void handleTextInput(StringBuilder buffer, KeyPressMessage k) {
        if (KeyUtil.isBackspace(k)) {
            if (!buffer.isEmpty()) buffer.deleteCharAt(buffer.length() - 1);
        } else if (k.type() == KeyType.KeyRunes && k.runes() != null) {
            for (char c : k.runes()) {
                if (!Character.isISOControl(c)) buffer.append(c);
            }
        } else if (k.key() != null && k.key().length() == 1 && !Character.isISOControl(k.key().charAt(0))) {
            buffer.append(k.key());
        }
    }

    public ScreenResult startAsyncGeneration() {
        if (topicBuffer.toString().trim().isBlank()) {
            bannerMessage = TuiHelper.red("✖ Topic cannot be blank.");
            return ScreenResult.stay(this);
        }
        if (!isPinnedQuiz() && subjectName.toString().trim().isBlank()) {
            bannerMessage = TuiHelper.red("✖ Subject is required.");
            return ScreenResult.stay(this);
        }

        int count = 3;
        try { count = Integer.parseInt(countBuffer.toString().trim()); } catch (Exception ignored) {}
        count = Math.max(1, Math.min(10, count));

        isGenerating = true;
        bannerMessage = "";

        final int finalCount = count;
        final String topic = topicBuffer.toString().trim();
        final String customPrompt = customPromptBuffer.toString().trim();
        final QuestionType type = selectedType;
        final Difficulty diff = selectedDifficulty;
        final int optsPerMcq = mcqOptionCount;
        final String subj = subjectName.toString().trim();
        final String fullTopic = !subj.isBlank() ? (subj + ": " + topic) : topic;

        return ScreenResult.stay(this, () -> {
            try {
                List<AIQuestionDraft> drafts = aiService.generateQuestions(fullTopic, finalCount, type, diff, optsPerMcq, customPrompt);
                return new AIQuestionsGeneratedMessage(drafts, null);
            } catch (Exception e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                return new AIQuestionsGeneratedMessage(null, cause.getMessage());
            }
        });
    }

    private void saveAllDrafts() {
        User teacher = Session.getCurrentUser().orElse(null);
        Integer teacherId = teacher != null ? teacher.getId() : null;

        Integer subjId;
        if (quizContext != null) {
            subjId = quizContext.getSubjectId();
        } else {
            Subject s = subjectService.getOrCreateSubject(subjectName.toString());
            subjId = s.getId();
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
            return QuestionViews.renderAIQuestionLoading(topicBuffer.toString(), spinnerTick);
        }
        if (reviewingDrafts) {
            String targetStr = quizContext != null ? "Quiz: " + quizContext.getTitle() : "Question Bank";
            return QuestionViews.renderAIQuestionReview(generatedDrafts, selectedDraftIndex, targetStr);
        }
        return QuestionViews.renderAIQuestionForm(
                isPinnedQuiz(),
                isPinnedQuiz() ? quizContext.getTitle() : "",
                subjectName.toString(),
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