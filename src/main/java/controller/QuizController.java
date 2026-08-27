package controller;

import model.entity.Question;
import model.entity.Quiz;
import service.QuestionService;
import service.QuizService;
import service.SubjectService;
import view.ConsoleUI;
import db.Session;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class QuizController {

    private final QuizService quizService;
    private final SubjectService subjectService;
    private final QuestionService questionService;

    public QuizController(QuizService quizService, SubjectService subjectService, QuestionService questionService) {
        this.quizService = quizService;
        this.subjectService = subjectService;
        this.questionService = questionService;
    }

    public void menu() {
        boolean back = false;
        while (!back) {
            ConsoleUI.banner("Quiz Management");
            ConsoleUI.println("1. List quizzes");
            ConsoleUI.println("2. Create quiz");
            ConsoleUI.println("3. Configure quiz (edit duration/pass %)");
            ConsoleUI.println("4. Assign questions to quiz");
            ConsoleUI.println("5. Remove question from quiz");
            ConsoleUI.println("6. Publish / Unpublish quiz");
            ConsoleUI.println("7. Delete quiz");
            ConsoleUI.println("0. Back");
            switch (ConsoleUI.prompt("Choose")) {
                case "1" -> list();
                case "2" -> create();
                case "3" -> configure();
                case "4" -> assignQuestions();
                case "5" -> removeQuestion();
                case "6" -> togglePublish();
                case "7" -> delete();
                case "0" -> back = true;
                default -> ConsoleUI.error("Invalid choice.");
            }
        }
    }

    private void list() {
        List<Quiz> quizzes = quizService.listAll();
        if (quizzes.isEmpty()) {
            ConsoleUI.println("(no quizzes yet)");
            return;
        }
        for (Quiz q : quizzes) {
            BigDecimal total = quizService.totalMarks(q.getId());
            ConsoleUI.println(q.getId() + ". " + q.getTitle()
                    + " | subject=" + q.getSubjectId()
                    + " | " + q.getDurationMinutes() + " min"
                    + " | pass=" + q.getPassPercentage() + "%"
                    + " | totalMarks=" + total
                    + " | maxAttempts=" + (q.getMaxAttempts() == 0 ? "unlimited" : q.getMaxAttempts())
                    + " | window=" + ConsoleUI.formatDateTime(q.getExamOpenAt()) + "→" + ConsoleUI.formatDateTime(q.getExamCloseAt())
                    + " | " + (q.isPublished() ? "PUBLISHED" : "draft"));
        }
    }

    private void create() {
        ConsoleUI.banner("Create Quiz");
        String title = ConsoleUI.prompt("Title");
        Long subjectId = ConsoleUI.promptLong("Subject ID");
        try {
            subjectService.get(subjectId);
        } catch (RuntimeException e) {
            ConsoleUI.error(e.getMessage());
            return;
        }
        int duration = ConsoleUI.promptInt("Duration (minutes)");
        BigDecimal passPct = ConsoleUI.promptDecimal("Pass percentage (e.g. 40)");
        int maxAttempts = promptMaxAttempts(3);
        ConsoleUI.println("Exam schedule window (leave both blank for 'open any time while published'):");
        LocalDateTime openAt = ConsoleUI.promptDateTimeOptional("Opens at");
        LocalDateTime closeAt = ConsoleUI.promptDateTimeOptional("Closes at");
        try {
            Quiz quiz = Quiz.builder()
                    .title(title)
                    .subjectId(subjectId)
                    .durationMinutes(duration)
                    .passPercentage(passPct)
                    .published(false)
                    .maxAttempts(maxAttempts)
                    .examOpenAt(openAt)
                    .examCloseAt(closeAt)
                    .createdBy(Session.current().getId())
                    .build();
            quizService.create(quiz);
            ConsoleUI.success("Quiz created (draft). Now assign questions before publishing.");
        } catch (RuntimeException e) {
            ConsoleUI.error(e.getMessage());
        }
    }

    private void configure() {
        Long id = ConsoleUI.promptLong("Quiz ID");
        try {
            Quiz quiz = quizService.get(id);
            ConsoleUI.println("Leave blank to keep current value.");
            String titleRaw = ConsoleUI.prompt("Title [" + quiz.getTitle() + "]");
            if (!titleRaw.isBlank()) quiz.setTitle(titleRaw);
            String durRaw = ConsoleUI.prompt("Duration minutes [" + quiz.getDurationMinutes() + "]");
            if (!durRaw.isBlank()) quiz.setDurationMinutes(Integer.parseInt(durRaw));
            String passRaw = ConsoleUI.prompt("Pass percentage [" + quiz.getPassPercentage() + "]");
            if (!passRaw.isBlank()) quiz.setPassPercentage(new BigDecimal(passRaw));
            String attemptsRaw = ConsoleUI.prompt(
                    "Max attempts, 0 = unlimited [" + quiz.getMaxAttempts() + "]");
            if (!attemptsRaw.isBlank()) quiz.setMaxAttempts(Integer.parseInt(attemptsRaw));
            if (ConsoleUI.promptYesNo("Change the exam schedule window (currently "
                    + ConsoleUI.formatDateTime(quiz.getExamOpenAt()) + " to "
                    + ConsoleUI.formatDateTime(quiz.getExamCloseAt()) + ")?")) {
                quiz.setExamOpenAt(ConsoleUI.promptDateTimeOptional("Opens at"));
                quiz.setExamCloseAt(ConsoleUI.promptDateTimeOptional("Closes at"));
            }
            quizService.update(quiz);
            ConsoleUI.success("Quiz updated.");
        } catch (RuntimeException e) {
            ConsoleUI.error(e.getMessage());
        }
    }

    private int promptMaxAttempts(int defaultValue) {
        String raw = ConsoleUI.prompt("Max attempts allowed (retakes included), 0 = unlimited ["
                + defaultValue + "]");
        if (raw.isBlank()) return defaultValue;
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            ConsoleUI.error("Not a number, using default of " + defaultValue + ".");
            return defaultValue;
        }
    }

    private void assignQuestions() {
        Long quizId = ConsoleUI.promptLong("Quiz ID");
        Quiz quiz;
        try {
            quiz = quizService.get(quizId);
        } catch (RuntimeException e) {
            ConsoleUI.error(e.getMessage());
            return;
        }
        List<Question> available = questionService.bySubject(quiz.getSubjectId());
        if (available.isEmpty()) {
            ConsoleUI.println("No questions exist yet for this quiz's subject. Add questions first.");
            return;
        }
        ConsoleUI.println("Available questions for subject " + quiz.getSubjectId() + ":");
        for (Question q : available) {
            ConsoleUI.println("  " + q.getId() + ". [" + q.getDifficulty() + "] " + q.getQuestionText());
        }
        boolean more = true;
        while (more) {
            Long questionId = ConsoleUI.promptLong("Question ID to add (0 to stop)");
            if (questionId == 0) break;
            BigDecimal marks = ConsoleUI.promptDecimal("Marks for this question");
            try {
                quizService.assignQuestion(quizId, questionId, marks);
                ConsoleUI.success("Assigned.");
            } catch (RuntimeException e) {
                ConsoleUI.error(e.getMessage());
            }
            more = ConsoleUI.promptYesNo("Add another question?");
        }
    }

    private void removeQuestion() {
        Long quizId = ConsoleUI.promptLong("Quiz ID");
        Long questionId = ConsoleUI.promptLong("Question ID to remove");
        try {
            quizService.removeQuestion(quizId, questionId);
            ConsoleUI.success("Removed.");
        } catch (RuntimeException e) {
            ConsoleUI.error(e.getMessage());
        }
    }

    private void togglePublish() {
        Long id = ConsoleUI.promptLong("Quiz ID");
        try {
            Quiz quiz = quizService.get(id);
            if (quiz.isPublished()) {
                quizService.unpublish(id);
                ConsoleUI.success("Quiz unpublished.");
            } else {
                quizService.publish(id);
                ConsoleUI.success("Quiz published. Students can now attempt it.");
            }
        } catch (RuntimeException e) {
            ConsoleUI.error(e.getMessage());
        }
    }

    private void delete() {
        Long id = ConsoleUI.promptLong("Quiz ID to delete");
        if (ConsoleUI.promptYesNo("Delete this quiz and all its attempts?")) {
            try {
                quizService.delete(id);
                ConsoleUI.success("Quiz deleted.");
            } catch (RuntimeException e) {
                ConsoleUI.error(e.getMessage());
            }
        }
    }
}
