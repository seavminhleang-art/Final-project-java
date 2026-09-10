package com.proctor.controller;

import com.proctor.model.entity.AIQuestionDraft;
import com.proctor.model.service.AIService;
import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.service.AuthService;
import com.proctor.model.enums.AssessmentType;
import com.proctor.model.enums.Difficulty;
import com.proctor.model.enums.QuestionType;
import com.proctor.model.entity.Question;
import com.proctor.model.service.QuestionService;
import com.proctor.model.entity.Quiz;
import com.proctor.model.service.QuizService;
import com.proctor.model.entity.Subject;
import com.proctor.model.service.SubjectService;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.QuizViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

import java.util.List;

public class AIQuizGeneratorScreen implements Screen {
    private final AIService aiService;
    private final QuizService quizService;
    private final QuestionService questionService;
    private final SubjectService subjectService;
    private final AuthService authService;
    private final AssessmentType assessmentType;

    private final StringBuilder subjectName = new StringBuilder();
    private final StringBuilder titleBuffer = new StringBuilder();
    private final StringBuilder countBuffer = new StringBuilder("5");
    private final StringBuilder mcqCountBuffer = new StringBuilder("2");
    private final StringBuilder tfCountBuffer = new StringBuilder("2");
    private final StringBuilder saCountBuffer = new StringBuilder("1");
    private QuestionType selectedType = QuestionType.MCQ;
    private boolean isExamMixed;
    private Difficulty selectedDifficulty = Difficulty.MEDIUM;
    private int mcqOptionCount = 4;

    private final StringBuilder timeLimitBuffer = new StringBuilder("30");
    private final StringBuilder activeHours = new StringBuilder("0");
    private final StringBuilder passScore = new StringBuilder("50");
    private boolean randomizeQuestions = true;
    private boolean randomizeAnswers = true;
    private boolean showAnswersAfter = true;

    private int focusedField = 0;
    private boolean isGenerating = false;
    private String bannerMessage = "";
    private int spinnerTick = 0;

    public record AIQuizGeneratedMessage(Quiz createdQuiz, List<AIQuestionDraft> drafts, String errorMessage) implements Message {}

    public AIQuizGeneratorScreen(AIService aiService, QuizService quizService, QuestionService questionService, SubjectService subjectService, AuthService authService) {
        this(aiService, quizService, questionService, subjectService, authService, AssessmentType.QUIZ);
    }

    public AIQuizGeneratorScreen(AIService aiService, QuizService quizService, QuestionService questionService, SubjectService subjectService, AuthService authService, AssessmentType assessmentType) {
        this.aiService = aiService;
        this.quizService = quizService;
        this.questionService = questionService;
        this.subjectService = subjectService;
        this.authService = authService;
        this.assessmentType = assessmentType != null ? assessmentType : AssessmentType.QUIZ;
        this.isExamMixed = (this.assessmentType == AssessmentType.EXAM);
    }

    private boolean isMcqApplicable() {
        if (assessmentType == AssessmentType.EXAM && isExamMixed) {
            return !"0".equals(mcqCountBuffer.toString().trim());
        }
        return selectedType == QuestionType.MCQ;
    }

    private int getNumInputFields() {
        if (assessmentType == AssessmentType.EXAM && isExamMixed) {
            return isMcqApplicable() ? 14 : 13;
        }
        return isMcqApplicable() ? 12 : 11;
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

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof AIQuizGeneratedMessage m) {
            isGenerating = false;
            if (m.errorMessage() != null) {
                bannerMessage = TuiHelper.red("✖ " + m.errorMessage());
                return ScreenResult.stay(this);
            }
            return ScreenResult.navigate(new QuizQuestionEditorScreen(m.createdQuiz(), quizService, questionService, subjectService, authService));
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
            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new QuizListScreen(quizService, questionService, subjectService, authService, assessmentType));
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
                    return startAsyncQuizGeneration();
                } else if (focusedField == getCancelButtonIndex()) {
                    return ScreenResult.navigate(new QuizListScreen(quizService, questionService, subjectService, authService, assessmentType));
                } else {
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
        if (focusedField == 0) {
            handleTextInput(subjectName, k);
            return;
        }
        if (focusedField == 1) {
            handleTextInput(titleBuffer, k);
            return;
        }
        if (focusedField == 2) {
            if (assessmentType == AssessmentType.EXAM) {
                int cur = isExamMixed ? 0 : (selectedType == QuestionType.MCQ ? 1 : (selectedType == QuestionType.TRUE_FALSE ? 2 : 3));
                int next = (KeyUtil.isLeft(k)) ? (cur - 1 + 4) % 4 : (cur + 1) % 4;
                if (next == 0) {
                    isExamMixed = true;
                } else if (next == 1) {
                    isExamMixed = false;
                    selectedType = QuestionType.MCQ;
                } else if (next == 2) {
                    isExamMixed = false;
                    selectedType = QuestionType.TRUE_FALSE;
                } else {
                    isExamMixed = false;
                    selectedType = QuestionType.SHORT_ANSWER;
                }
            } else {
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
            return;
        }

        int current = 3;
        if (assessmentType == AssessmentType.EXAM && isExamMixed) {
            if (focusedField == current++) {
                handleTextInput(mcqCountBuffer, k);
                return;
            }
            if (focusedField == current++) {
                handleTextInput(tfCountBuffer, k);
                return;
            }
            if (focusedField == current++) {
                handleTextInput(saCountBuffer, k);
                return;
            }
        } else {
            if (focusedField == current++) {
                handleTextInput(countBuffer, k);
                return;
            }
        }

        if (focusedField == current++) {
            if (KeyUtil.isLeft(k)) {
                if (selectedDifficulty == Difficulty.EASY) selectedDifficulty = Difficulty.HARD;
                else if (selectedDifficulty == Difficulty.HARD) selectedDifficulty = Difficulty.MEDIUM;
                else selectedDifficulty = Difficulty.EASY;
            } else if (KeyUtil.isRight(k) || KeyUtil.isSpace(k)) {
                if (selectedDifficulty == Difficulty.EASY) selectedDifficulty = Difficulty.MEDIUM;
                else if (selectedDifficulty == Difficulty.MEDIUM) selectedDifficulty = Difficulty.HARD;
                else selectedDifficulty = Difficulty.EASY;
            }
            return;
        }

        if (isMcqApplicable()) {
            if (focusedField == current) {
                if (KeyUtil.isLeft(k)) {
                    if (mcqOptionCount == 2) mcqOptionCount = 4;
                    else if (mcqOptionCount == 4) mcqOptionCount = 3;
                    else mcqOptionCount = 2;
                } else if (KeyUtil.isRight(k) || KeyUtil.isSpace(k)) {
                    if (mcqOptionCount == 2) mcqOptionCount = 3;
                    else if (mcqOptionCount == 3) mcqOptionCount = 4;
                    else mcqOptionCount = 2;
                }
                return;
            }
            current++;
        }

        if (focusedField == current++) {
            handleTextInput(timeLimitBuffer, k);
            return;
        }
        if (focusedField == current++) {
            handleTextInput(activeHours, k);
            return;
        }
        if (focusedField == current++) {
            handleTextInput(passScore, k);
            return;
        }
        if (focusedField == current++) {
            if (KeyUtil.isSpace(k) || KeyUtil.isRight(k) || KeyUtil.isLeft(k)) randomizeQuestions = !randomizeQuestions;
            return;
        }
        if (focusedField == current++) {
            if (KeyUtil.isSpace(k) || KeyUtil.isRight(k) || KeyUtil.isLeft(k)) randomizeAnswers = !randomizeAnswers;
            return;
        }
        if (focusedField == current) {
            if (KeyUtil.isSpace(k) || KeyUtil.isRight(k) || KeyUtil.isLeft(k)) showAnswersAfter = !showAnswersAfter;
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

    private ScreenResult startAsyncQuizGeneration() {
        if (subjectName.toString().trim().isBlank()) {
            bannerMessage = TuiHelper.red("✖ Subject is required.");
            return ScreenResult.stay(this);
        }
        if (titleBuffer.toString().trim().isBlank()) {
            bannerMessage = TuiHelper.red("✖ " + (assessmentType == AssessmentType.EXAM ? "Exam" : "Quiz") + " Title / Topic is required.");
            return ScreenResult.stay(this);
        }

        int mcqC = 0, tfC = 0, saC = 0, singleC = 5;
        if (assessmentType == AssessmentType.EXAM && isExamMixed) {
            try { mcqC = Integer.parseInt(mcqCountBuffer.toString().trim()); } catch (Exception ignored) {}
            try { tfC = Integer.parseInt(tfCountBuffer.toString().trim()); } catch (Exception ignored) {}
            try { saC = Integer.parseInt(saCountBuffer.toString().trim()); } catch (Exception ignored) {}
            mcqC = Math.max(0, Math.min(10, mcqC));
            tfC = Math.max(0, Math.min(10, tfC));
            saC = Math.max(0, Math.min(10, saC));
            if (mcqC + tfC + saC == 0) {
                bannerMessage = TuiHelper.red("✖ Please specify at least 1 question across types.");
                return ScreenResult.stay(this);
            }
        } else {
            try { singleC = Integer.parseInt(countBuffer.toString().trim()); } catch (Exception ignored) {}
            singleC = Math.max(1, Math.min(10, singleC));
        }

        int timeLimit = 30;
        try { timeLimit = Integer.parseInt(timeLimitBuffer.toString().trim()); } catch (Exception ignored) {}

        int hours = 0;
        try { hours = Integer.parseInt(activeHours.toString().trim()); } catch (Exception ignored) {}

        int score = 60;
        try { score = Integer.parseInt(passScore.toString().trim()); } catch (Exception ignored) {}

        isGenerating = true;
        bannerMessage = "";

        final int finalMcqCount = mcqC;
        final int finalTfCount = tfC;
        final int finalSaCount = saC;
        final int finalSingleCount = singleC;
        final int finalMins = timeLimit;
        final int finalHours = hours;
        final int finalScore = score;
        final String subjStr = subjectName.toString().trim();
        final String titleTopic = titleBuffer.toString().trim();
        final QuestionType type = selectedType;
        final boolean mixed = isExamMixed;
        final Difficulty diff = selectedDifficulty;
        final int optsPerMcq = mcqOptionCount;

        return ScreenResult.stay(this, () -> {
            Quiz createdQuiz = null;
            User teacher = Session.getCurrentUser().orElse(null);
            try {
                Subject subj = subjectService.getOrCreateSubject(subjStr);
                Integer teacherId = teacher != null ? teacher.getId() : null;

                Quiz quiz = Quiz.builder()
                        .subjectId(subj.getId())
                        .createdBy(teacherId)
                        .title(titleTopic)
                        .topic(titleTopic)
                        .description("AI Generated " + (assessmentType == AssessmentType.EXAM ? "Exam" : "Quiz") + " on " + titleTopic)
                        .assessmentType(assessmentType)
                        .quizQuestionType(assessmentType == AssessmentType.QUIZ ? type : null)
                        .timeLimitMins(finalMins > 0 ? finalMins : null)
                        .activeDurationHours(finalHours)
                        .passScore(finalScore)
                        .maxAttempts(1)
                        .randomizeQuestions(randomizeQuestions)
                        .randomizeAnswers(randomizeAnswers)
                        .showAnswersAfter(showAnswersAfter)
                        .published(false)
                        .build();

                createdQuiz = quizService.createQuiz(quiz);

                String fullPromptTopic = subjStr + ": " + titleTopic;
                List<AIQuestionDraft> drafts;
                if (assessmentType == AssessmentType.EXAM && mixed) {
                    drafts = aiService.generateMixedQuestions(fullPromptTopic, finalMcqCount, finalTfCount, finalSaCount, diff, optsPerMcq);
                } else {
                    drafts = aiService.generateQuestions(fullPromptTopic, finalSingleCount, type, diff, optsPerMcq);
                }

                if (drafts.isEmpty()) {
                    quizService.deleteQuiz(createdQuiz.getId(), teacher);
                    return new AIQuizGeneratedMessage(null, null, "AI model returned 0 questions. Please try again with a clearer topic.");
                }

                for (AIQuestionDraft draft : drafts) {
                    Question q = Question.builder()
                            .quizId(createdQuiz.getId())
                            .subjectId(subj.getId())
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

                return new AIQuizGeneratedMessage(createdQuiz, drafts, null);
            } catch (Exception e) {
                if (createdQuiz != null) {
                    try { quizService.deleteQuiz(createdQuiz.getId(), teacher); } catch (Exception ignored) {}
                }
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                return new AIQuizGeneratedMessage(null, null, cause.getMessage());
            }
        });
    }

    @Override
    public String view() {
        if (isGenerating) {
            return QuizViews.renderAIQuizLoading(assessmentType, titleBuffer.toString(), spinnerTick);
        }
        String typeLabel = (assessmentType == AssessmentType.EXAM && isExamMixed)
                ? "MIXED (Custom Counts)" : selectedType.name();
        return QuizViews.renderAIQuizForm(
                assessmentType,
                subjectName.toString(),
                titleBuffer.toString(),
                countBuffer.toString(),
                mcqCountBuffer.toString(),
                tfCountBuffer.toString(),
                saCountBuffer.toString(),
                typeLabel,
                selectedDifficulty,
                mcqOptionCount,
                timeLimitBuffer.toString(),
                activeHours.toString(),
                passScore.toString(),
                randomizeQuestions,
                randomizeAnswers,
                showAnswersAfter,
                focusedField,
                bannerMessage
        );
    }
}