package com.proctor.controller;

import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.enums.Role;
import com.proctor.model.service.AuthService;
import com.proctor.exception.ValidationException;
import com.proctor.model.entity.Attempt;
import com.proctor.model.entity.AttemptAnswer;
import com.proctor.model.service.ExamService;
import com.proctor.model.entity.Question;
import com.proctor.model.service.QuestionService;
import com.proctor.model.entity.Quiz;
import com.proctor.model.service.QuizService;
import com.proctor.model.entity.Result;
import com.proctor.model.service.SubjectService;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.TeacherSubmissionViews;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

public class TeacherSubmissionScreen implements Screen {
    private final Quiz specificQuiz;
    private final ExamService examService;
    private final QuizService quizService;
    private final QuestionService questionService;
    private final SubjectService subjectService;
    private final AuthService authService;

    private List<Attempt> allSubmissions = new java.util.ArrayList<>();
    private List<Attempt> submissions = new java.util.ArrayList<>();
    private int selectedIndex = 0;
    private boolean inspectingAnswerSheet = false;
    private int inspectingAnswerIndex = 0;
    private String bannerMessage = "";
    private final StringBuilder searchBuffer = new StringBuilder();
    private boolean searchMode = false;
    private int statusFilterIndex = 0;
    private static final String[] STATUS_FILTERS = {"ALL", "PENDING REVIEW", "GRADED"};
    private boolean isGrading = false;
    private int spinnerTick = 0;

    public record AIGradingCompletedMessage(boolean success, String errorMessage) implements Message {}

    public TeacherSubmissionScreen(Quiz specificQuiz, ExamService examService, QuizService quizService, QuestionService questionService, SubjectService subjectService, AuthService authService) {
        this.specificQuiz = specificQuiz;
        this.examService = examService;
        this.quizService = quizService;
        this.questionService = questionService;
        this.subjectService = subjectService;
        this.authService = authService;
        refreshList();
    }

    private void refreshList() {
        if (specificQuiz != null) {
            this.allSubmissions = examService.getSubmissionsForQuiz(specificQuiz.getId());
        } else {
            User teacher = Session.getCurrentUser().orElse(null);
            Integer teacherId = teacher != null ? teacher.getId() : null;
            this.allSubmissions = examService.getAllSubmissions(teacherId);
        }
        applyFilters();
    }

    private void applyFilters() {
        String filter = STATUS_FILTERS[statusFilterIndex];
        String search = searchBuffer.toString().trim().toLowerCase();

        this.submissions = allSubmissions.stream().filter(a -> {
            if ("PENDING REVIEW".equals(filter)) {
                if (a.getStatus() != com.proctor.model.enums.AttemptStatus.AUTO_SUBMITTED
                        && a.getStatus() != com.proctor.model.enums.AttemptStatus.TURNED_IN) {
                    return false;
                }
            } else if ("GRADED".equals(filter)) {
                if (a.getStatus() != com.proctor.model.enums.AttemptStatus.GRADED) {
                    return false;
                }
            }
            if (!search.isEmpty()) {
                boolean matchStudent = a.getStudentName() != null && a.getStudentName().toLowerCase().contains(search);
                boolean matchQuiz = a.getQuizTitle() != null && a.getQuizTitle().toLowerCase().contains(search);
                if (!matchStudent && !matchQuiz) {
                    return false;
                }
            }
            return true;
        }).toList();

        if (submissions.isEmpty()) {
            selectedIndex = 0;
        } else if (selectedIndex >= submissions.size()) {
            selectedIndex = submissions.size() - 1;
        }
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof AIGradingCompletedMessage m) {
            isGrading = false;
            if (m.success()) {
                bannerMessage = inspectingAnswerSheet
                        ? TuiHelper.green("✔ AI evaluation complete! Press [r] to finalize and return grade.")
                        : TuiHelper.green("✔ Auto-graded with AI! Press [r] to return grade.");
                refreshList();
            } else {
                String err = (m.errorMessage() != null && !m.errorMessage().isBlank())
                        ? m.errorMessage()
                        : "AI grading failed. Please check Ollama.";
                bannerMessage = TuiHelper.red("✖ " + err);
            }
            return ScreenResult.stay(this);
        }

        if (isGrading) {
            spinnerTick++;
            if (msg instanceof KeyPressMessage k && KeyUtil.isEsc(k)) {
                isGrading = false;
                bannerMessage = TuiHelper.yellow("AI grading cancelled.");
            }
            return ScreenResult.stay(this);
        }

        if (msg instanceof KeyPressMessage k) {
            if (inspectingAnswerSheet) {
                Attempt currentAttempt = !submissions.isEmpty() ? submissions.get(selectedIndex) : null;
                Quiz currentQuiz = specificQuiz;
                if (currentQuiz == null && currentAttempt != null) {
                    currentQuiz = quizService.getQuizById(currentAttempt.getQuizId()).orElse(null);
                }
                List<Question> questions = (currentQuiz != null) ? currentQuiz.getQuestions() : List.of();
                if (KeyUtil.isEsc(k)) {
                    inspectingAnswerSheet = false;
                    return ScreenResult.stay(this);
                } else if (KeyUtil.isUp(k) || KeyUtil.isLeft(k)) {
                    if (!questions.isEmpty()) {
                        inspectingAnswerIndex = (inspectingAnswerIndex - 1 + questions.size()) % questions.size();
                    }
                } else if (KeyUtil.isDown(k) || KeyUtil.isRight(k)) {
                    if (!questions.isEmpty()) {
                        inspectingAnswerIndex = (inspectingAnswerIndex + 1) % questions.size();
                    }
                } else if ("g".equalsIgnoreCase(k.key())) {
                    return startAsyncGrading();
                } else if ("r".equalsIgnoreCase(k.key())) {
                    if (!submissions.isEmpty()) {
                        Attempt att = submissions.get(selectedIndex);
                        try {
                            Result res = examService.returnGrade(att.getId());
                            bannerMessage = TuiHelper.green(String.format("✔ Grade returned: %.1f/%.1f points (%.1f%%) - %s",
                                    res.getTotalPoints(), res.getMaxPoints(), res.getPercentage(),
                                    res.isPassed() ? "PASSED" : "FAILED"));
                            refreshList();
                        } catch (ValidationException e) {
                            bannerMessage = TuiHelper.red("✖ " + e.getMessage());
                        }
                    }
                }
                return ScreenResult.stay(this);
            }

            if (searchMode) {
                if (KeyUtil.isEsc(k) || KeyUtil.isEnter(k)) {
                    searchMode = false;
                    applyFilters();
                } else if (KeyUtil.isBackspace(k)) {
                    if (!searchBuffer.isEmpty()) {
                        searchBuffer.deleteCharAt(searchBuffer.length() - 1);
                        applyFilters();
                    }
                } else if (k.type() == KeyType.KeyRunes && k.runes() != null) {
                    for (char c : k.runes()) {
                        if (!Character.isISOControl(c)) searchBuffer.append(c);
                    }
                    applyFilters();
                } else if (k.key() != null && k.key().length() == 1 && !Character.isISOControl(k.key().charAt(0))) {
                    searchBuffer.append(k.key());
                    applyFilters();
                }
                return ScreenResult.stay(this);
            }

            bannerMessage = "";

            if (KeyUtil.isEsc(k)) {
                if (specificQuiz != null) {
                    return ScreenResult.navigate(new QuizListScreen(quizService, questionService, subjectService, authService, specificQuiz.getAssessmentType()));
                }
                User user = Session.getCurrentUser().orElse(null);
                if (user != null && user.getRole() == Role.ADMIN) {
                    return ScreenResult.navigate(new AdminDashboardScreen(authService));
                }
                return ScreenResult.navigate(new TeacherDashboardScreen(authService, questionService, subjectService, quizService));
            }

            if (KeyUtil.isUp(k)) {
                if (!submissions.isEmpty()) {
                    selectedIndex = (selectedIndex - 1 + submissions.size()) % submissions.size();
                }
            } else if (KeyUtil.isDown(k)) {
                if (!submissions.isEmpty()) {
                    selectedIndex = (selectedIndex + 1) % submissions.size();
                }
            } else if (KeyUtil.isLeft(k)) {
                if (!submissions.isEmpty()) {
                    int pageSize = TuiHelper.PAGE_SIZE;
                    int currentPage = selectedIndex / pageSize;
                    if (currentPage > 0) {
                        selectedIndex = (currentPage - 1) * pageSize;
                    }
                }
            } else if (KeyUtil.isRight(k)) {
                if (!submissions.isEmpty()) {
                    int pageSize = TuiHelper.PAGE_SIZE;
                    int totalPages = Math.max(1, (int) Math.ceil((double) submissions.size() / pageSize));
                    int currentPage = selectedIndex / pageSize;
                    if (currentPage < totalPages - 1) {
                        selectedIndex = Math.min(submissions.size() - 1, (currentPage + 1) * pageSize);
                    }
                }
            } else if ("f".equalsIgnoreCase(k.key())) {
                statusFilterIndex = (statusFilterIndex + 1) % STATUS_FILTERS.length;
                selectedIndex = 0;
                applyFilters();
            } else if ("/".equals(k.key())) {
                searchMode = true;
                searchBuffer.setLength(0);
                applyFilters();
            } else if (KeyUtil.isEnter(k)) {
                if (!submissions.isEmpty()) {
                    inspectingAnswerSheet = true;
                    inspectingAnswerIndex = 0;
                    bannerMessage = "";
                }
            } else if ("g".equalsIgnoreCase(k.key())) {
                return startAsyncGrading();
            } else if ("r".equalsIgnoreCase(k.key())) {
                if (!submissions.isEmpty()) {
                    Attempt att = submissions.get(selectedIndex);
                    try {
                        Result res = examService.returnGrade(att.getId());
                        bannerMessage = TuiHelper.green(String.format("✔ Grade returned: %.1f/%.1f (%.1f%%) - %s",
                                res.getTotalPoints(), res.getMaxPoints(), res.getPercentage(),
                                res.isPassed() ? "PASSED" : "FAILED"));
                        refreshList();
                    } catch (ValidationException e) {
                        bannerMessage = TuiHelper.red("✖ " + e.getMessage());
                    }
                }
            }
        }
        return ScreenResult.stay(this);
    }

    private ScreenResult startAsyncGrading() {
        if (submissions.isEmpty()) {
            return ScreenResult.stay(this);
        }
        Attempt att = submissions.get(selectedIndex);
        isGrading = true;
        bannerMessage = "";
        return ScreenResult.stay(this, () -> {
            try {
                boolean ok = examService.gradeWithAI(att.getId());
                return new AIGradingCompletedMessage(ok, ok ? null : "AI grading failed. Please check Ollama.");
            } catch (Exception e) {
                String err = e.getMessage();
                if (err == null || err.isBlank()) {
                    Throwable cause = e.getCause();
                    if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
                        err = cause.getMessage();
                    } else {
                        err = "Failed to grade with AI: " + e.getClass().getSimpleName();
                    }
                }
                return new AIGradingCompletedMessage(false, err);
            }
        });
    }

    @Override
    public String view() {
        if (isGrading) {
            Attempt attempt = !submissions.isEmpty() ? submissions.get(selectedIndex) : null;
            String studentName = null;
            String quizTitle = null;
            if (attempt != null) {
                studentName = (attempt.getStudentName() != null && !attempt.getStudentName().isBlank())
                        ? attempt.getStudentName() : "Student #" + attempt.getStudentId();
                quizTitle = (specificQuiz != null) ? specificQuiz.getTitle()
                        : (attempt.getQuizTitle() != null ? attempt.getQuizTitle() : "Quiz #" + attempt.getQuizId());
            }
            return TeacherSubmissionViews.renderAIGradingLoading(studentName, quizTitle, spinnerTick);
        }
        if (inspectingAnswerSheet && !submissions.isEmpty()) {
            Attempt attempt = submissions.get(selectedIndex);
            Quiz currentQuiz = specificQuiz;
            if (currentQuiz == null) {
                currentQuiz = quizService.getQuizById(attempt.getQuizId()).orElse(null);
            }
            List<AttemptAnswer> answers = examService.getAttemptAnswers(attempt.getId());
            Map<Integer, AttemptAnswer> answerMap = new java.util.HashMap<>();
            for (AttemptAnswer a : answers) {
                answerMap.put(a.getQuestionId(), a);
            }
            return TeacherSubmissionViews.renderAnswerSheet(currentQuiz, attempt, answerMap, inspectingAnswerIndex, bannerMessage);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        return TeacherSubmissionViews.renderSubmissionList(specificQuiz, submissions, java.util.Collections.emptyMap(),
                selectedIndex, sdf, STATUS_FILTERS[statusFilterIndex], searchBuffer.toString(), searchMode, bannerMessage);
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}