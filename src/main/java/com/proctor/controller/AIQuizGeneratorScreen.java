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

    private final List<Subject> subjects;
    private int selectedSubjectIndex = 0;
    private final StringBuilder titleBuffer = new StringBuilder();
    private final StringBuilder descriptionBuffer = new StringBuilder();
    private final StringBuilder customPromptBuffer = new StringBuilder();
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
        if (this.assessmentType == AssessmentType.SPEED) {
            this.mcqCountBuffer.setLength(0);
            this.mcqCountBuffer.append("3");
            this.tfCountBuffer.setLength(0);
            this.tfCountBuffer.append("3");
            this.timeLimitBuffer.setLength(0);
            this.timeLimitBuffer.append("15");
        }
        this.subjects = subjectService.getSubjects(null);
    }

    private boolean isMcqApplicable() {
        if (assessmentType == AssessmentType.SPEED) {
            return !"0".equals(mcqCountBuffer.toString().trim());
        }
        if (assessmentType == AssessmentType.EXAM && isExamMixed) {
            return !"0".equals(mcqCountBuffer.toString().trim());
        }
        return selectedType == QuestionType.MCQ;
    }

    private int getNumInputFields() {
        if (assessmentType == AssessmentType.SPEED) {
            return isMcqApplicable() ? 12 : 11;
        }
        if (assessmentType == AssessmentType.EXAM && isExamMixed) {
            return isMcqApplicable() ? 16 : 15;
        }
        return isMcqApplicable() ? 14 : 13;
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
            if (m.errorMessage() != null || m.createdQuiz() == null) {
                String err = (m.errorMessage() != null && !m.errorMessage().isBlank())
                        ? m.errorMessage()
                        : "Failed to generate assessment. Please ensure Ollama is running ('ollama serve') and try again.";
                bannerMessage = TuiHelper.red("✖ " + err);
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

            if (KeyUtil.isDown(k)) {
                focusedField = (focusedField + 1) % getFieldCount();
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isUp(k)) {
                focusedField = (focusedField - 1 + getFieldCount()) % getFieldCount();
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isEnter(k)) {
                if (focusedField == getGenerateButtonIndex()) {
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
        if (assessmentType == AssessmentType.SPEED) {
            handleSpeedFormInput(k);
            return;
        }
        if (focusedField == 0) {
            int size = subjects.size() + 1;
            if (KeyUtil.isLeft(k)) selectedSubjectIndex = (selectedSubjectIndex - 1 + size) % size;
            else if (KeyUtil.isRight(k) || KeyUtil.isSpace(k)) selectedSubjectIndex = (selectedSubjectIndex + 1) % size;
            return;
        }
        if (focusedField == 1) {
            handleTextInput(titleBuffer, k);
            return;
        }
        if (focusedField == 2) {
            handleTextInput(descriptionBuffer, k);
            return;
        }
        if (focusedField == 3) {
            handleTextInput(customPromptBuffer, k);
            return;
        }
        if (focusedField == 4) {
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

        int current = 5;
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

    private void handleSpeedFormInput(KeyPressMessage k) {
        if (focusedField == 0) {
            int size = subjects.size() + 1;
            if (KeyUtil.isLeft(k)) selectedSubjectIndex = (selectedSubjectIndex - 1 + size) % size;
            else if (KeyUtil.isRight(k) || KeyUtil.isSpace(k)) selectedSubjectIndex = (selectedSubjectIndex + 1) % size;
            return;
        }
        if (focusedField == 1) {
            handleTextInput(titleBuffer, k);
            return;
        }
        if (focusedField == 2) {
            handleTextInput(descriptionBuffer, k);
            return;
        }
        if (focusedField == 3) {
            handleTextInput(customPromptBuffer, k);
            return;
        }
        if (focusedField == 4) {
            handleTextInput(mcqCountBuffer, k);
            return;
        }
        if (focusedField == 5) {
            handleTextInput(tfCountBuffer, k);
            return;
        }
        if (focusedField == 6) {
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

        int current = 7;
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
            if (KeyUtil.isSpace(k) || KeyUtil.isRight(k) || KeyUtil.isLeft(k)) randomizeAnswers = !randomizeAnswers;
            return;
        }
        if (focusedField == current) {
            if (KeyUtil.isSpace(k) || KeyUtil.isRight(k) || KeyUtil.isLeft(k)) showAnswersAfter = !showAnswersAfter;
        }
    }

    private void handleTextInput(StringBuilder buffer, KeyPressMessage k) {
        if (!KeyUtil.handleBackspace(buffer, k)) {
            KeyUtil.appendInput(buffer, k);
        }
    }

    private ScreenResult startAsyncQuizGeneration() {
        if (selectedSubjectIndex == 0) {
            bannerMessage = TuiHelper.red("✖ Subject is required.");
            return ScreenResult.stay(this);
        }
        if (titleBuffer.toString().trim().isBlank()) {
            bannerMessage = TuiHelper.red("✖ " + (assessmentType == AssessmentType.EXAM ? "Exam" : (assessmentType == AssessmentType.SPEED ? "Speed Quiz" : "Quiz")) + " Title / Topic is required.");
            return ScreenResult.stay(this);
        }

        int mcqC = 0, tfC = 0, saC = 0, singleC = 5;
        int timeLimit = 30;
        if (assessmentType == AssessmentType.SPEED) {
            try { mcqC = Integer.parseInt(mcqCountBuffer.toString().trim()); } catch (Exception ignored) {}
            try { tfC = Integer.parseInt(tfCountBuffer.toString().trim()); } catch (Exception ignored) {}
            mcqC = Math.max(0, Math.min(10, mcqC));
            tfC = Math.max(0, Math.min(10, tfC));
            if (mcqC + tfC == 0) {
                bannerMessage = TuiHelper.red("✖ Please specify at least 1 question across MCQ / True-False.");
                return ScreenResult.stay(this);
            }
            int speedSecs = 15;
            try { speedSecs = Integer.parseInt(timeLimitBuffer.toString().trim()); } catch (Exception ignored) {}
            if (speedSecs < 5) speedSecs = 5;
            timeLimit = speedSecs;
        } else if (assessmentType == AssessmentType.EXAM && isExamMixed) {
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
            try { timeLimit = Integer.parseInt(timeLimitBuffer.toString().trim()); } catch (Exception ignored) {}
        } else {
            try { singleC = Integer.parseInt(countBuffer.toString().trim()); } catch (Exception ignored) {}
            singleC = Math.max(1, Math.min(10, singleC));
            try { timeLimit = Integer.parseInt(timeLimitBuffer.toString().trim()); } catch (Exception ignored) {}
        }

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
        final Subject selectedSubj = subjects.get(selectedSubjectIndex - 1);
        final String subjStr = selectedSubj.getCode() + " - " + selectedSubj.getName();
        final String titleTopic = titleBuffer.toString().trim();
        final String studentInstructions = descriptionBuffer.toString().trim();
        final String customPrompt = customPromptBuffer.toString().trim();
        final QuestionType type = selectedType;
        final boolean mixed = isExamMixed;
        final Difficulty diff = selectedDifficulty;
        final int optsPerMcq = mcqOptionCount;

        return ScreenResult.stay(this, () -> {
            Quiz createdQuiz = null;
            User teacher = Session.getCurrentUser().orElse(null);
            try {
                Subject subj = selectedSubj;
                Integer teacherId = teacher != null ? teacher.getId() : null;

                String defaultDesc = "AI Generated " + (assessmentType == AssessmentType.EXAM ? "Exam" : (assessmentType == AssessmentType.SPEED ? "Speed Quiz" : "Quiz")) + " on " + titleTopic;
                String quizDesc = studentInstructions.isBlank() ? defaultDesc : studentInstructions;

                Quiz quiz = Quiz.builder()
                        .subjectId(subj.getId())
                        .createdBy(teacherId)
                        .title(titleTopic)
                        .topic(titleTopic)
                        .description(quizDesc)
                        .assessmentType(assessmentType)
                        .quizQuestionType(assessmentType == AssessmentType.QUIZ ? type : null)
                        .timeLimitMins(assessmentType == AssessmentType.SPEED ? null : (finalMins > 0 ? finalMins : null))
                        .speedSecondsPerQuestion(assessmentType == AssessmentType.SPEED ? finalMins : null)
                        .activeDurationHours(finalHours)
                        .passScore(finalScore)
                        .randomizeQuestions(assessmentType != AssessmentType.SPEED && randomizeQuestions)
                        .randomizeAnswers(randomizeAnswers)
                        .showAnswersAfter(showAnswersAfter)
                        .published(false)
                        .build();

                createdQuiz = quizService.createQuiz(quiz);

                String fullPromptTopic = subjStr + ": " + titleTopic;
                List<AIQuestionDraft> drafts;
                if (assessmentType == AssessmentType.SPEED) {
                    drafts = aiService.generateMixedQuestions(fullPromptTopic, finalMcqCount, finalTfCount, 0, diff, optsPerMcq, customPrompt);
                } else if (assessmentType == AssessmentType.EXAM && mixed) {
                    drafts = aiService.generateMixedQuestions(fullPromptTopic, finalMcqCount, finalTfCount, finalSaCount, diff, optsPerMcq, customPrompt);
                } else {
                    drafts = aiService.generateQuestions(fullPromptTopic, finalSingleCount, type, diff, optsPerMcq, customPrompt);
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
                String err = e.getMessage();
                if (err == null || err.isBlank()) {
                    Throwable cause = e.getCause();
                    if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
                        err = cause.getMessage();
                    } else {
                        err = "Failed to generate assessment with AI model: " + e.getClass().getSimpleName();
                    }
                }
                return new AIQuizGeneratedMessage(null, null, err);
            }
        });
    }

    @Override
    public String view() {
        if (isGenerating) {
            return QuizViews.renderAIQuizLoading(assessmentType, titleBuffer.toString(), spinnerTick);
        }
        String typeLabel;
        if (assessmentType == AssessmentType.SPEED) {
            typeLabel = "SPEED (MCQ + T/F)";
        } else if (assessmentType == AssessmentType.EXAM && isExamMixed) {
            typeLabel = "MIXED (Custom Counts)";
        } else {
            typeLabel = selectedType.name();
        }
        String subjectDisplay = (subjects.isEmpty() || selectedSubjectIndex == 0)
                ? "(No Subject)"
                : subjects.get(selectedSubjectIndex - 1).getCode() + " - " + subjects.get(selectedSubjectIndex - 1).getName();
        return QuizViews.renderAIQuizForm(
                assessmentType,
                subjectDisplay,
                titleBuffer.toString(),
                descriptionBuffer.toString(),
                customPromptBuffer.toString(),
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