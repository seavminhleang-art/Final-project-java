package controller;


import db.Session;
import model.entity.enums.Role;
import view.ConsoleUI;

public class MainMenuController {
    private final UserController userController;
    private final SubjectController subjectController;
    private final QuestionController questionController;
    private final QuizController quizController;
    private final ExamController examController;
    private final StudentPortalController studentPortalController;
    private final ReportController reportController;
    private final AnnouncementController announcementController;

    public MainMenuController(UserController userController, SubjectController subjectController,
                              QuestionController questionController, QuizController quizController,
                              ExamController examController, StudentPortalController studentPortalController,
                              ReportController reportController, AnnouncementController announcementController) {
        this.userController = userController;
        this.subjectController = subjectController;
        this.questionController = questionController;
        this.quizController = quizController;
        this.examController = examController;
        this.studentPortalController = studentPortalController;
        this.reportController = reportController;
        this.announcementController = announcementController;
    }

    public void run() {
        Role role = Session.current().getRole();
        boolean loggedOut = false;
        while (!loggedOut) {
            ConsoleUI.banner("Proctor - " + role + " Dashboard (" + Session.current().getFullName() + ")");
            switch (role) {
                case ADMIN -> loggedOut = adminMenu();
                case TEACHER -> loggedOut = teacherMenu();
                case STUDENT -> loggedOut = studentMenu();
            }
        }
    }

    private boolean adminMenu() {
        ConsoleUI.println("1. User Management");
        ConsoleUI.println("2. Subject Management");
        ConsoleUI.println("3. Question Management");
        ConsoleUI.println("4. Quiz Management");
        ConsoleUI.println("5. Reporting");
        ConsoleUI.println("6. " + announcementController.menuLabel());
        ConsoleUI.println("9. Logout");
        return switch (ConsoleUI.prompt("Choose")) {
            case "1" -> { userController.menu(); yield false; }
            case "2" -> { subjectController.menu(); yield false; }
            case "3" -> { questionController.menu(); yield false; }
            case "4" -> { quizController.menu(); yield false; }
            case "5" -> { reportController.menu(); yield false; }
            case "6" -> { announcementController.menu(); yield false; }
            case "9" -> { Session.logout(); yield true; }
            default -> { ConsoleUI.error("Invalid choice."); yield false; }
        };
    }

    private boolean teacherMenu() {
        ConsoleUI.println("1. Subject Management");
        ConsoleUI.println("2. Question Management");
        ConsoleUI.println("3. Quiz Management");
        ConsoleUI.println("4. Reporting");
        ConsoleUI.println("5. " + announcementController.menuLabel());
        ConsoleUI.println("9. Logout");
        return switch (ConsoleUI.prompt("Choose")) {
            case "1" -> { subjectController.menu(); yield false; }
            case "2" -> { questionController.menu(); yield false; }
            case "3" -> { quizController.menu(); yield false; }
            case "4" -> { reportController.menu(); yield false; }
            case "5" -> { announcementController.menu(); yield false; }
            case "9" -> { Session.logout(); yield true; }
            default -> { ConsoleUI.error("Invalid choice."); yield false; }
        };
    }

    private boolean studentMenu() {
        ConsoleUI.println("1. Take an exam");
        ConsoleUI.println("2. Student portal (history / leaderboard)");
        ConsoleUI.println("3. " + announcementController.menuLabel());
        ConsoleUI.println("9. Logout");
        return switch (ConsoleUI.prompt("Choose")) {
            case "1" -> { examController.takeExamMenu(); yield false; }
            case "2" -> { studentPortalController.menu(); yield false; }
            case "3" -> { announcementController.menu(); yield false; }
            case "9" -> { Session.logout(); yield true; }
            default -> { ConsoleUI.error("Invalid choice."); yield false; }
        };
    }
}
