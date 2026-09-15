package com.proctor.controller;

import com.proctor.model.service.AIService;
import com.proctor.model.service.AuthService;
import com.proctor.exception.ValidationException;
import com.proctor.model.entity.Question;
import com.proctor.model.service.QuestionService;
import com.proctor.model.entity.Quiz;
import com.proctor.model.service.QuizService;
import com.proctor.model.service.SubjectService;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.QuizViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;

import java.util.List;

public class QuizQuestionEditorScreen implements Screen {
    private final Quiz quiz;
    private final QuizService quizService;
    private final QuestionService questionService;
    private final SubjectService subjectService;
    private final AuthService authService;

    private List<Question> questions;
    private int selectedIndex = 0;
    private boolean confirmingDelete = false;
    private boolean confirmDeleteFocused = false;
    private Question pendingDeleteQuestion = null;
    private String bannerMessage = "";

    public QuizQuestionEditorScreen(Quiz quiz, QuizService quizService, QuestionService questionService, SubjectService subjectService, AuthService authService) {
        this.quiz = quiz;
        this.quizService = quizService;
        this.questionService = questionService;
        this.subjectService = subjectService;
        this.authService = authService;
        if (quiz != null && quiz.getId() != null) {
            refreshList();
        } else {
            this.questions = new java.util.ArrayList<>();
            this.bannerMessage = TuiHelper.red("✖ Assessment not found or failed to load.");
        }
    }

    private void refreshList() {
        this.questions = questionService.getQuestionsByQuizId(quiz.getId());
        if (questions.isEmpty()) {
            selectedIndex = 0;
        } else if (selectedIndex >= questions.size()) {
            selectedIndex = questions.size() - 1;
        }
    }

    public void setBannerMessage(String message) {
        this.bannerMessage = message;
    }

    private double calculateTotalPoints() {
        return questions.stream().mapToDouble(Question::getPoints).sum();
    }

    @Override
    public ScreenResult update(Message msg) {
        if (quiz == null) {
            if (msg instanceof KeyPressMessage k && KeyUtil.isEsc(k)) {
                if (quizService != null) {
                    return ScreenResult.navigate(new QuizListScreen(quizService, questionService, subjectService, authService, com.proctor.model.enums.AssessmentType.QUIZ));
                }
            }
            return ScreenResult.stay(this);
        }

        if (msg instanceof KeyPressMessage k) {
            if (confirmingDelete) {
                if (KeyUtil.isLeft(k) || KeyUtil.isRight(k)) {
                    confirmDeleteFocused = !confirmDeleteFocused;
                    return ScreenResult.stay(this);
                } else if (KeyUtil.isEnter(k)) {
                    if (confirmDeleteFocused && pendingDeleteQuestion != null) {
                        questionService.deleteQuestion(pendingDeleteQuestion.getId());
                        bannerMessage = TuiHelper.green("✔ Deleted question #" + pendingDeleteQuestion.getId());
                        refreshList();
                    } else {
                        bannerMessage = TuiHelper.yellow("Deletion cancelled.");
                    }
                    confirmingDelete = false;
                    pendingDeleteQuestion = null;
                    return ScreenResult.stay(this);
                } else if ("y".equalsIgnoreCase(k.key())) {
                    if (pendingDeleteQuestion != null) {
                        questionService.deleteQuestion(pendingDeleteQuestion.getId());
                        bannerMessage = TuiHelper.green("✔ Deleted question #" + pendingDeleteQuestion.getId());
                        refreshList();
                    }
                    confirmingDelete = false;
                    pendingDeleteQuestion = null;
                    return ScreenResult.stay(this);
                } else if ("n".equalsIgnoreCase(k.key()) || KeyUtil.isEsc(k)) {
                    confirmingDelete = false;
                    pendingDeleteQuestion = null;
                    bannerMessage = TuiHelper.yellow("Deletion cancelled.");
                    return ScreenResult.stay(this);
                }
                return ScreenResult.stay(this);
            }

            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new QuizListScreen(quizService, questionService, subjectService, authService, quiz.getAssessmentType()));
            }

            if (KeyUtil.isUp(k)) {
                if (!questions.isEmpty()) {
                    selectedIndex = (selectedIndex - 1 + questions.size()) % questions.size();
                }
            } else if (KeyUtil.isDown(k)) {
                if (!questions.isEmpty()) {
                    selectedIndex = (selectedIndex + 1) % questions.size();
                }
            } else if (KeyUtil.isLeft(k)) {
                if (!questions.isEmpty()) {
                    int pageSize = TuiHelper.PAGE_SIZE;
                    int currentPage = selectedIndex / pageSize;
                    if (currentPage > 0) {
                        selectedIndex = (currentPage - 1) * pageSize;
                    }
                }
            } else if (KeyUtil.isRight(k)) {
                if (!questions.isEmpty()) {
                    int pageSize = TuiHelper.PAGE_SIZE;
                    int totalPages = Math.max(1, (int) Math.ceil((double) questions.size() / pageSize));
                    int currentPage = selectedIndex / pageSize;
                    if (currentPage < totalPages - 1) {
                        selectedIndex = Math.min(questions.size() - 1, (currentPage + 1) * pageSize);
                    }
                }
            } else if ("n".equalsIgnoreCase(k.key()) || "a".equalsIgnoreCase(k.key())) {
                return ScreenResult.navigate(new QuestionFormScreen(questionService, subjectService, authService, null, quiz));
            } else if ("b".equalsIgnoreCase(k.key())) {
                return ScreenResult.navigate(new QuestionBankPickerScreen(quiz, quizService, questionService, subjectService, authService));
            } else if ("e".equalsIgnoreCase(k.key()) || KeyUtil.isEnter(k)) {
                if (!questions.isEmpty()) {
                    return ScreenResult.navigate(new QuestionFormScreen(questionService, subjectService, authService, questions.get(selectedIndex), quiz));
                }
            } else if ("d".equalsIgnoreCase(k.key()) || KeyUtil.isDelete(k)) {
                if (!questions.isEmpty()) {
                    confirmingDelete = true;
                    confirmDeleteFocused = false;
                    pendingDeleteQuestion = questions.get(selectedIndex);
                }
            } else if ("g".equalsIgnoreCase(k.key())) {
                return ScreenResult.navigate(new AIQuestionGeneratorScreen(new AIService(), questionService, subjectService, authService, quiz));
            } else if ("p".equalsIgnoreCase(k.key()) || KeyUtil.isSpace(k)) {
                try {
                    quizService.togglePublishStatus(quiz.getId());
                    quiz.setPublished(!quiz.isPublished());
                    String status = quiz.isPublished() ? TuiHelper.green("PUBLISHED") : TuiHelper.dim("DRAFT");
                    bannerMessage = "✔ Quiz status updated to " + status;
                } catch (ValidationException e) {
                    bannerMessage = TuiHelper.red("✖ " + e.getMessage());
                }
            }
        }
        return ScreenResult.stay(this);
    }

    @Override
    public String view() {
        if (quiz == null) {
            return TuiHelper.header("QUIZZES") + "\n"
                    + TuiHelper.boxTitle("Assessment Error") + "\n\n"
                    + (bannerMessage.isEmpty() ? TuiHelper.red("✖ Assessment not found or failed to load.") : bannerMessage)
                    + "\n\n" + TuiHelper.wrapHints(List.of("[Esc] Back to list"));
        }

        if (confirmingDelete && pendingDeleteQuestion != null) {
            return TuiHelper.confirmationModal(
                    "Question #" + pendingDeleteQuestion.getId(),
                    "Are you sure you want to delete this question?",
                    truncate(pendingDeleteQuestion.getQuestionText(), 60),
                    "Delete Question",
                    "Cancel",
                    confirmDeleteFocused
            );
        }

        String subjectText = "General";
        if (quiz.getSubjectId() != null) {
            subjectText = subjectService.getSubjectById(quiz.getSubjectId())
                    .map(s -> s.getCode() + " - " + s.getName())
                    .orElse("Subject #" + quiz.getSubjectId());
        }

        return QuizViews.renderQuizQuestionEditor(quiz, subjectText, questions, calculateTotalPoints(), selectedIndex, bannerMessage);
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, Math.max(0, max - 3)) + "...";
    }
}