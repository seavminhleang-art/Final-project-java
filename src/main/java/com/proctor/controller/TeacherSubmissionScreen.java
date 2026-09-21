package com.proctor.controller;

import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.enums.Role;
import com.proctor.model.service.AuthService;
import com.proctor.exception.ValidationException;
import com.proctor.model.entity.Attempt;
import com.proctor.model.enums.AssessmentType;
import com.proctor.model.enums.AttemptStatus;
import com.proctor.model.entity.AttemptAnswer;
import com.proctor.model.service.ExamService;
import com.proctor.model.entity.Question;
import com.proctor.model.service.QuestionService;
import com.proctor.model.entity.Quiz;
import com.proctor.model.service.QuizService;
import com.proctor.model.entity.Result;
import com.proctor.model.service.SubjectService;
import com.proctor.model.enums.QuestionType;
import com.proctor.util.KeyUtil;
import com.proctor.util.TuiHelper;
import com.proctor.view.TeacherSubmissionViews;
import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.input.key.KeyType;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    private boolean isGrading = false;
    private int spinnerTick = 0;
    private long gradingStartTime = 0;
    private int activeGradingId = 0;
    private AtomicBoolean activeCancellation = null;

    public record GradingTickMessage(int gradingId) implements Message {}
    public record AIGradingCompletedMessage(int gradingId, boolean success, String errorMessage) implements Message {
        public AIGradingCompletedMessage(boolean success, String errorMessage) {
            this(0, success, errorMessage);
        }
    }

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
            Integer teacherId = (teacher != null && teacher.getRole() != Role.ADMIN) ? teacher.getId() : null;
            this.allSubmissions = examService.getAllSubmissions(teacherId);
        }
        applyFilters();
    }

    private void applyFilters() {
        String filter = STATUS_FILTERS[statusFilterIndex];
        String search = searchBuffer.toString().trim().toLowerCase();

        this.submissions = allSubmissions.stream().filter(a -> {
            if ("PENDING REVIEW".equals(filter)) {
                if (a.isGraded() || a.getStatus() == AttemptStatus.GRADED) {
                    return false;
                }
                if (a.getStatus() != AttemptStatus.AUTO_SUBMITTED && a.getStatus() != AttemptStatus.TURNED_IN) {
                    return false;
                }
            } else if ("GRADED".equals(filter)) {
                if (!a.isGraded() && a.getStatus() != AttemptStatus.GRADED) {
                    return false;
                }
            }
            if (!search.isEmpty()) {
                boolean matchStudent = a.getStudentName() != null && a.getStudentName().toLowerCase().contains(search);
                boolean matchQuiz = a.getQuizTitle() != null && a.getQuizTitle().toLowerCase().contains(search);
                boolean matchType = a.getAssessmentType() != null && a.getAssessmentType().name().toLowerCase().contains(search);
                if (!matchStudent && !matchQuiz && !matchType) {
                    return false;
                }
            }
            return true;
        }).toList();

        if (submissions.isEmpty()) {
            selectedIndex = 0;
            inspectingAnswerSheet = false;
            inspectingAnswerIndex = 0;
        } else if (selectedIndex >= submissions.size()) {
            selectedIndex = submissions.size() - 1;
        }
    }

    @Override
    public ScreenResult update(Message msg) {
        if (msg instanceof GradingTickMessage t) {
            if (isGrading && t.gradingId() == this.activeGradingId) {
                spinnerTick++;
                return ScreenResult.stay(this, Command.tick(Duration.ofMillis(80), time -> new GradingTickMessage(this.activeGradingId)));
            }
            return ScreenResult.stay(this);
        }

        if (msg instanceof AIGradingCompletedMessage m) {
            if (!isGrading || (m.gradingId() != 0 && m.gradingId() != this.activeGradingId)) {
                return ScreenResult.stay(this);
            }
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
            if (msg instanceof KeyPressMessage k && KeyUtil.isEsc(k)) {
                isGrading = false;
                activeGradingId++;
                if (activeCancellation != null) {
                    activeCancellation.set(true);
                }
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
                } else if (currentQuiz != null && (currentQuiz.getQuestions() == null || currentQuiz.getQuestions().isEmpty())) {
                    currentQuiz = quizService.getQuizById(currentQuiz.getId()).orElse(currentQuiz);
                }
                List<Question> questions = (currentQuiz != null && currentQuiz.getQuestions() != null) ? currentQuiz.getQuestions() : List.of();
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
                    executeReturnGrade();
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
            } else if (KeyUtil.isTab(k) || "f".equalsIgnoreCase(k.key())) {
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
                executeReturnGrade();
            }
        }
        return ScreenResult.stay(this);
    }

    private void executeReturnGrade() {
        if (submissions.isEmpty()) {
            return;
        }
        Attempt att = submissions.get(selectedIndex);
        if (att.getAssessmentType() == AssessmentType.SPEED) {
            bannerMessage = TuiHelper.yellow("● Speed Quizzes are auto-scored objective assessments. Grade is already finalized.");
            return;
        }
        if (att.getStatus() == AttemptStatus.GRADED || att.isGraded()) {
            bannerMessage = TuiHelper.yellow("● Grade has already been returned for this submission.");
            return;
        }
        if (att.getStatus() == AttemptStatus.IN_PROGRESS) {
            bannerMessage = TuiHelper.yellow("● Cannot return grade for an assessment that is still in progress.");
            return;
        }

        Quiz quiz = specificQuiz;
        if (quiz == null) {
            quiz = quizService.getQuizById(att.getQuizId()).orElse(null);
        } else if (quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
            quiz = quizService.getQuizById(quiz.getId()).orElse(quiz);
        }
        if (quiz != null && quiz.getQuestions() != null) {
            List<AttemptAnswer> answers = examService.getAttemptAnswers(att.getId());
            Map<Integer, AttemptAnswer> ansMap = new HashMap<>();
            for (AttemptAnswer a : answers) {
                ansMap.put(a.getQuestionId(), a);
            }
            boolean hasUnevaluatedShortAnswer = false;
            for (Question q : quiz.getQuestions()) {
                if (q.getQuestionType() == QuestionType.SHORT_ANSWER) {
                    AttemptAnswer ans = ansMap.get(q.getId());
                    if (ans != null && ans.getTextAnswer() != null && !ans.getTextAnswer().isBlank()) {
                        if (ans.getAiScore() == null && ans.getTeacherFeedback() == null && ans.getPointsAwarded() == 0.0) {
                            hasUnevaluatedShortAnswer = true;
                            break;
                        }
                    }
                }
            }
            if (hasUnevaluatedShortAnswer) {
                bannerMessage = TuiHelper.yellow("● Written answers have not been evaluated. Press [g] to grade with AI first.");
                return;
            }
        }

        try {
            Result res = examService.returnGrade(att.getId());
            bannerMessage = TuiHelper.green(String.format("✔ Grade returned: %.1f/%.1f points (%.1f%%) - %s",
                    res.getTotalPoints(), res.getMaxPoints(), res.getPercentage(),
                    res.isPassed() ? "PASSED" : "FAILED"));
            inspectingAnswerSheet = false;
            inspectingAnswerIndex = 0;
            refreshList();
        } catch (ValidationException e) {
            bannerMessage = TuiHelper.red("✖ " + e.getMessage());
        }
    }

    private ScreenResult startAsyncGrading() {
        if (submissions.isEmpty()) {
            return ScreenResult.stay(this);
        }
        Attempt att = submissions.get(selectedIndex);
        if (att.getAssessmentType() == AssessmentType.SPEED) {
            bannerMessage = TuiHelper.yellow("● Speed Quizzes are objective (MCQ / T&F) only. AI grading is not applicable.");
            return ScreenResult.stay(this);
        }
        if (att.getStatus() == AttemptStatus.GRADED) {
            bannerMessage = TuiHelper.yellow("● This submission is already graded. AI grading cannot be re-run.");
            return ScreenResult.stay(this);
        }
        if (att.getStatus() == AttemptStatus.IN_PROGRESS) {
            bannerMessage = TuiHelper.yellow("● Cannot grade an assessment that is still in progress by the student.");
            return ScreenResult.stay(this);
        }
        isGrading = true;
        spinnerTick = 0;
        gradingStartTime = System.currentTimeMillis();
        final int gId = ++activeGradingId;
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        this.activeCancellation = cancelled;
        bannerMessage = "";

        Command gradeCmd = () -> {
            try {
                boolean ok = examService.gradeWithAI(att.getId());
                if (cancelled.get()) {
                    return new AIGradingCompletedMessage(gId, false, "Cancelled");
                }
                return new AIGradingCompletedMessage(gId, ok, ok ? null : "AI grading failed. Please check Ollama.");
            } catch (Exception e) {
                if (cancelled.get()) {
                    return new AIGradingCompletedMessage(gId, false, "Cancelled");
                }
                String err = e.getMessage();
                if (err == null || err.isBlank()) {
                    Throwable cause = e.getCause();
                    if (cause != null && cause.getMessage() != null && !cause.getMessage().isBlank()) {
                        err = cause.getMessage();
                    } else {
                        err = "Failed to grade with AI: " + e.getClass().getSimpleName();
                    }
                }
                return new AIGradingCompletedMessage(gId, false, err);
            }
        };

        Command tickCmd = Command.tick(Duration.ofMillis(80), time -> new GradingTickMessage(gId));

        return ScreenResult.stay(this, Command.batch(gradeCmd, tickCmd));
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
            int elapsedSeconds = (int) Math.max(0, (System.currentTimeMillis() - gradingStartTime) / 1000);
            return TeacherSubmissionViews.renderAIGradingLoading(studentName, quizTitle, spinnerTick, elapsedSeconds);
        }
        if (inspectingAnswerSheet && !submissions.isEmpty()) {
            Attempt attempt = submissions.get(selectedIndex);
            Quiz currentQuiz = specificQuiz;
            if (currentQuiz == null) {
                currentQuiz = quizService.getQuizById(attempt.getQuizId()).orElse(null);
            } else if (currentQuiz.getQuestions() == null || currentQuiz.getQuestions().isEmpty()) {
                currentQuiz = quizService.getQuizById(currentQuiz.getId()).orElse(currentQuiz);
            }
            List<AttemptAnswer> answers = examService.getAttemptAnswers(attempt.getId());
            Map<Integer, AttemptAnswer> answerMap = new java.util.HashMap<>();
            for (AttemptAnswer a : answers) {
                answerMap.put(a.getQuestionId(), a);
            }
            return TeacherSubmissionViews.renderAnswerSheet(currentQuiz, attempt, answerMap, inspectingAnswerIndex, bannerMessage);
        }
        return TeacherSubmissionViews.renderSubmissionList(specificQuiz, submissions, java.util.Collections.emptyMap(),
                selectedIndex, dateFormat, STATUS_FILTERS[statusFilterIndex], searchBuffer.toString(), searchMode, bannerMessage);
    }

    private String truncate(String text, int max) {
        return TuiHelper.truncate(text, max);
    }
}