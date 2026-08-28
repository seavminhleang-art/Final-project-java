package controller;

import model.entity.Subject;
import service.SubjectService;
import view.ConsoleUI;
import db.Session;

import java.util.List;

public class SubjectController {

    private final SubjectService subjectService;

    public SubjectController(SubjectService subjectService) {
        this.subjectService = subjectService;
    }

    public void menu() {
        boolean back = false;
        while (!back) {
            ConsoleUI.banner("Subject Management");
            ConsoleUI.println("1. List subjects");
            ConsoleUI.println("2. Create subject");
            ConsoleUI.println("3. Update subject");
            ConsoleUI.println("4. Delete subject");
            ConsoleUI.println("0. Back");
            switch (ConsoleUI.prompt("Choose")) {
                case "1" -> list();
                case "2" -> create();
                case "3" -> update();
                case "4" -> delete();
                case "0" -> back = true;
                default -> ConsoleUI.error("Invalid choice.");
            }
        }
    }

    private void list() {
        List<Subject> subjects = subjectService.listAll();
        if (subjects.isEmpty()) {
            ConsoleUI.println("(no subjects yet)");
            return;
        }
        for (Subject s : subjects) {
            ConsoleUI.println(s.getId() + ". " + s.getName() + " - " + (s.getDescription() == null ? "" : s.getDescription()));
        }
    }

    private void create() {
        String name = ConsoleUI.prompt("Subject name");
        String desc = ConsoleUI.prompt("Description");
        try {
            Subject subject = Subject.builder().name(name).description(desc).createdBy(Session.current().getId()).build();
            subjectService.create(subject);
            ConsoleUI.success("Subject created.");
        } catch (RuntimeException e) {
            ConsoleUI.error(e.getMessage());
        }
    }

    private void update() {
        Long id = ConsoleUI.promptLong("Subject ID");
        try {
            Subject subject = subjectService.get(id);
            String name = ConsoleUI.prompt("Name [" + subject.getName() + "]");
            String desc = ConsoleUI.prompt("Description [" + subject.getDescription() + "]");
            if (!name.isBlank()) subject.setName(name);
            if (!desc.isBlank()) subject.setDescription(desc);
            subjectService.update(subject);
            ConsoleUI.success("Subject updated.");
        } catch (RuntimeException e) {
            ConsoleUI.error(e.getMessage());
        }
    }

    private void delete() {
        Long id = ConsoleUI.promptLong("Subject ID to delete");
        if (ConsoleUI.promptYesNo("Delete this subject and all its questions/quizzes?")) {
            try {
                subjectService.delete(id);
                ConsoleUI.success("Subject deleted.");
            } catch (RuntimeException e) {
                ConsoleUI.error(e.getMessage());
            }
        }
    }
}
