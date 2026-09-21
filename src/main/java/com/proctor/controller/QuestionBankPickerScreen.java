package com.proctor.controller;

import com.proctor.model.entity.Question;
import com.proctor.model.entity.Quiz;
import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.repository.QuizRepository;
import com.proctor.model.service.AuthService;
import com.proctor.model.service.QuestionService;
import com.proctor.model.service.QuizService;
import com.proctor.model.service.SubjectService;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.QuestionBankViews;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QuestionBankPickerScreen implements Screen {
    private final Quiz quiz;
    private final QuizService quizService;
    private final QuestionService questionService;
    private final SubjectService subjectService;
    private final AuthService authService;

    private final List<Question> bankQuestions;
    private final Set<Integer> selectedIds = new HashSet<>();
    private final Set<Integer> alreadyAddedIds = new HashSet<>();
    private int selectedIndex = 0;
    private final String filterSummary;
    private String bannerMessage = "";

    public QuestionBankPickerScreen(Quiz quiz, QuizService quizService, QuestionService questionService,
                                    SubjectService subjectService, AuthService authService) {
        this.quiz = quiz;
        this.quizService = quizService;
        this.questionService = questionService;
        this.subjectService = subjectService;
        this.authService = authService;

        Integer currentUserId = Session.getCurrentUser().map(User::getId).orElse(null);
        Integer subjectId = quiz.getSubjectId();
        com.proctor.model.enums.QuestionType typeFilter = quiz.getQuizQuestionType();

        List<Question> rawQuestions = questionService.getBankQuestions(currentUserId, subjectId, typeFilter, null, null);
        if (quiz.getAssessmentType() == com.proctor.model.enums.AssessmentType.SPEED) {
            this.bankQuestions = rawQuestions.stream()
                    .filter(q -> q.getQuestionType() == com.proctor.model.enums.QuestionType.MCQ
                              || q.getQuestionType() == com.proctor.model.enums.QuestionType.TRUE_FALSE)
                    .toList();
        } else {
            this.bankQuestions = rawQuestions;
        }

        List<Question> existingQuizQuestions = questionService.getQuestionsByQuizId(quiz.getId());
        Set<String> existingTexts = new HashSet<>();
        for (Question eq : existingQuizQuestions) {
            if (eq.getQuestionText() != null) {
                existingTexts.add(eq.getQuestionText().trim().toLowerCase());
            }
        }
        for (Question bq : bankQuestions) {
            if (bq.getQuestionText() != null && existingTexts.contains(bq.getQuestionText().trim().toLowerCase())) {
                alreadyAddedIds.add(bq.getId());
            }
        }

        StringBuilder fs = new StringBuilder("Auto-filter: Your bank questions");
        if (subjectId != null) {
            subjectService.getSubjectById(subjectId).ifPresent(s ->
                    fs.append(" for subject [").append(s.getCode()).append(" - ").append(s.getName()).append("]"));
        }
        if (quiz.getAssessmentType() == com.proctor.model.enums.AssessmentType.SPEED) {
            fs.append(", MCQ & True/False only (Speed Quiz)");
        } else if (typeFilter != null) {
            fs.append(", type [").append(typeFilter.name()).append("]");
        }
        if (subjectId == null && typeFilter == null && quiz.getAssessmentType() != com.proctor.model.enums.AssessmentType.SPEED) {
            fs.setLength(0);
            fs.append("Showing all your bank questions (no subject/type filter on this quiz).");
        }
        this.filterSummary = fs.toString();
    }

    @Override
    public ScreenResult update(com.williamcallahan.tui4j.compat.bubbletea.Message msg) {
        if (msg instanceof com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage k) {

            if (KeyUtil.isEsc(k)) {
                return ScreenResult.navigate(new QuizQuestionEditorScreen(
                        quiz, new QuizService(new QuizRepository()), questionService, subjectService, authService));
            }

            if (KeyUtil.isUp(k)) {
                if (!bankQuestions.isEmpty()) selectedIndex = (selectedIndex - 1 + bankQuestions.size()) % bankQuestions.size();
            } else if (KeyUtil.isDown(k)) {
                if (!bankQuestions.isEmpty()) selectedIndex = (selectedIndex + 1) % bankQuestions.size();
            } else if (KeyUtil.isLeft(k)) {
                if (!bankQuestions.isEmpty()) {
                    int pageSize = TuiHelper.PAGE_SIZE;
                    int page = selectedIndex / pageSize;
                    if (page > 0) {
                        selectedIndex = (page - 1) * pageSize;
                    }
                }
            } else if (KeyUtil.isRight(k)) {
                if (!bankQuestions.isEmpty()) {
                    int pageSize = TuiHelper.PAGE_SIZE;
                    int page = selectedIndex / pageSize;
                    int totalPages = Math.max(1, (int) Math.ceil((double) bankQuestions.size() / pageSize));
                    if (page < totalPages - 1) {
                        selectedIndex = Math.min(bankQuestions.size() - 1, (page + 1) * pageSize);
                    }
                }
            } else if ((KeyUtil.isSpace(k) || KeyUtil.isEnter(k)) && !bankQuestions.isEmpty()) {
                int qid = bankQuestions.get(selectedIndex).getId();
                if (alreadyAddedIds.contains(qid)) {
                    bannerMessage = TuiHelper.yellow("● This question is already in the quiz.");
                    return ScreenResult.stay(this);
                }
                if (selectedIds.contains(qid)) {
                    selectedIds.remove(qid);
                } else {
                    selectedIds.add(qid);
                }
            } else if ("c".equalsIgnoreCase(k.key())) {
                if (selectedIds.isEmpty()) {
                    bannerMessage = TuiHelper.yellow("⚠ No questions selected. Use Space/Enter to toggle.");
                    return ScreenResult.stay(this);
                }
                int imported = 0;
                List<String> errors = new ArrayList<>();
                for (int qid : selectedIds) {
                    try {
                        questionService.copyBankQuestionToQuiz(qid, quiz.getId());
                        imported++;
                    } catch (Exception e) {
                        errors.add("Q" + qid + ": " + e.getMessage());
                    }
                }
                String msg2 = TuiHelper.green("✔ Imported " + imported + " question(s) from your bank.");
                if (!errors.isEmpty()) {
                    msg2 += " " + TuiHelper.red("Errors: " + String.join(", ", errors));
                }
                QuizQuestionEditorScreen editor = new QuizQuestionEditorScreen(
                        quiz, new QuizService(new QuizRepository()), questionService, subjectService, authService);
                editor.setBannerMessage(msg2);
                return ScreenResult.navigate(editor);
            }
        }
        return ScreenResult.stay(this);
    }

    @Override
    public String view() {
        return QuestionBankViews.renderBankPicker(quiz, bankQuestions, selectedIds, alreadyAddedIds, selectedIndex, filterSummary, bannerMessage);
    }
}
