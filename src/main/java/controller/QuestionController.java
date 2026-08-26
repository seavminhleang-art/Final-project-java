package controller;

import model.entity.enums.Difficulty;
import model.entity.Question;
import service.QuestionService;
import service.SubjectService;
import view.ConsoleUI;
import db.Session;

import java.util.List;

public class QuestionController {

    private final QuestionService questionService;
    private final SubjectService subjectService;

    public QuestionController(QuestionService questionService, SubjectService subjectService) {
        this.questionService = questionService;
        this.subjectService = subjectService;
    }

    public void menu() {
        boolean back = false;
        while (!back) {
            ConsoleUI.banner("Question Management");
            ConsoleUI.println("1. List questions by subject");
            ConsoleUI.println("2. Create question");
            ConsoleUI.println("3. Search/Filter questions");
            ConsoleUI.println("4. Update question");
            ConsoleUI.println("5. Delete question");
            ConsoleUI.println("0. Back");
            switch (ConsoleUI.prompt("Choose")) {
                case "1" -> listBySubject();
                case "2" -> create();
                case "3" -> filter();
                case "4" -> update();
                case "5" -> delete();
                case "0" -> back = true;
                default -> ConsoleUI.error("Invalid choice.");
            }
        }
    }

    private void listBySubject() {
        Long subjectId = ConsoleUI.promptLong("Subject ID");
        print(questionService.bySubject(subjectId));
    }

    private void create() {
        ConsoleUI.banner("Create Question");
        Long subjectId = ConsoleUI.promptLong("Subject ID");
        try {
            subjectService.get(subjectId);
        } catch (RuntimeException e) {
            ConsoleUI.error(e.getMessage());
            return;
        }
        String text = ConsoleUI.prompt("Question text");
        String a = ConsoleUI.prompt("Option A");
        String b = ConsoleUI.prompt("Option B");
        String c = ConsoleUI.prompt("Option C");
        String d = ConsoleUI.prompt("Option D");
        char correct = ConsoleUI.promptChar("Correct option (A/B/C/D)", "ABCD");
        Difficulty difficulty = promptDifficulty();
        try {
            Question q = Question.builder()
                    .subjectId(subjectId)
                    .questionText(text)
                    .optionA(a).optionB(b).optionC(c).optionD(d)
                    .correctOption(correct)
                    .difficulty(difficulty)
                    .createdBy(Session.current().getId())
                    .build();
            questionService.create(q);
            ConsoleUI.success("Question created.");
        } catch (RuntimeException e) {
            ConsoleUI.error(e.getMessage());
        }
    }

    private void filter() {
        String subjectRaw = ConsoleUI.prompt("Subject ID (blank = any)");
        Long subjectId = subjectRaw.isBlank() ? null : Long.parseLong(subjectRaw);
        ConsoleUI.println("Difficulty: 1=Any 2=Easy 3=Medium 4=Hard");
        Difficulty difficulty = switch (ConsoleUI.prompt("Choose")) {
            case "2" -> Difficulty.EASY;
            case "3" -> Difficulty.MEDIUM;
            case "4" -> Difficulty.HARD;
            default -> null;
        };
        String keyword = ConsoleUI.prompt("Keyword in question text (blank = any)");
        print(questionService.filter(subjectId, difficulty, keyword));
    }

    private void update() {
        Long id = ConsoleUI.promptLong("Question ID");
        try {
            Question q = questionService.get(id);
            ConsoleUI.println("Leave blank to keep current value.");
            String text = ConsoleUI.prompt("Question text [" + q.getQuestionText() + "]");
            if (!text.isBlank()) q.setQuestionText(text);
            String a = ConsoleUI.prompt("Option A [" + q.getOptionA() + "]");
            if (!a.isBlank()) q.setOptionA(a);
            String b = ConsoleUI.prompt("Option B [" + q.getOptionB() + "]");
            if (!b.isBlank()) q.setOptionB(b);
            String c = ConsoleUI.prompt("Option C [" + q.getOptionC() + "]");
            if (!c.isBlank()) q.setOptionC(c);
            String d = ConsoleUI.prompt("Option D [" + q.getOptionD() + "]");
            if (!d.isBlank()) q.setOptionD(d);
            String correctRaw = ConsoleUI.prompt("Correct option [" + q.getCorrectOption() + "]");
            if (!correctRaw.isBlank()) q.setCorrectOption(correctRaw.trim().toUpperCase().charAt(0));
            questionService.update(q);
            ConsoleUI.success("Question updated.");
        } catch (RuntimeException e) {
            ConsoleUI.error(e.getMessage());
        }
    }

    private void delete() {
        Long id = ConsoleUI.promptLong("Question ID to delete");
        if (ConsoleUI.promptYesNo("Are you sure?")) {
            try {
                questionService.delete(id);
                ConsoleUI.success("Question deleted.");
            } catch (RuntimeException e) {
                ConsoleUI.error(e.getMessage());
            }
        }
    }

    private Difficulty promptDifficulty() {
        ConsoleUI.println("Difficulty: 1=Easy 2=Medium 3=Hard");
        return switch (ConsoleUI.prompt("Choose")) {
            case "2" -> Difficulty.MEDIUM;
            case "3" -> Difficulty.HARD;
            default -> Difficulty.EASY;
        };
    }

    private void print(List<Question> questions) {
        if (questions.isEmpty()) {
            ConsoleUI.println("(no questions found)");
            return;
        }
        for (Question q : questions) {
            ConsoleUI.println(q.getId() + ". [" + q.getDifficulty() + "] " + q.getQuestionText());
            ConsoleUI.println("     A) " + q.getOptionA() + "   B) " + q.getOptionB());
            ConsoleUI.println("     C) " + q.getOptionC() + "   D) " + q.getOptionD());
            ConsoleUI.println("     Correct: " + q.getCorrectOption());
        }
    }
}
