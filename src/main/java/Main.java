import db.DatabaseSeeder;
import model.repository.UserRepo;
import model.service.AuthService;
import model.service.ReportService;
import model.service.UserService;
import model.service.impl.AuthServiceImpl;
import model.service.impl.ReportServiceImpl;
import model.service.impl.UserServiceImpl;
import view.ConsoleUI;

public class Main {
    public static void main(String[] args) {
        // DAOs
        UserRepo userRepo = new UserDaoImpl();
        SubjectDao subjectDao = new SubjectDaoImpl();
        QuestionDao questionDao = new QuestionDaoImpl();
        QuizDao quizDao = new QuizDaoImpl();
        AttemptDao attemptDao = new AttemptDaoImpl();
        AnnouncementDao announcementDao = new AnnouncementDaoImpl();

        try {
            DatabaseSeeder.ensureAdminExists(userRepo);
        } catch (RuntimeException e) {
            ConsoleUI.error("Could not connect to the database. Check src/main/resources/db.properties "
                    + "and make sure PostgreSQL is running with the schema loaded (see schema.sql).");
            ConsoleUI.error(e.getMessage());
            return;
        }

        // Services
        AuthService authService = new AuthServiceImpl(userRepo);
        UserService userService = new UserServiceImpl(userRepo);
        SubjectService subjectService = new SubjectServiceImpl(subjectDao);
        QuestionService questionService = new QuestionServiceImpl(questionDao);
        QuizService quizService = new QuizServiceImpl(quizDao);
        AnnouncementService announcementService = new AnnouncementServiceImpl(announcementDao);
        ExamService examService = new ExamServiceImpl(attemptDao, quizDao, announcementService);
        ReportService reportService = new ReportServiceImpl(attemptDao, userDao, subjectDao, questionDao, quizDao);

        // Controllers
        AuthController authController = new AuthController(authService);
        UserController userController = new UserController(userService);
        SubjectController subjectController = new SubjectController(subjectService);
        QuestionController questionController = new QuestionController(questionService, subjectService);
        QuizController quizController = new QuizController(quizService, subjectService, questionService);
        ExamController examController = new ExamController(examService, quizService);
        StudentPortalController studentPortalController =
                new StudentPortalController(examService, quizService, reportService);
        ReportController reportController = new ReportController(reportService);
        AnnouncementController announcementController = new AnnouncementController(announcementService);

        MainMenuController mainMenu = new MainMenuController(
                userController, subjectController, questionController, quizController,
                examController, studentPortalController, reportController, announcementController);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> ConnectionPool.getInstance().shutdown()));

        ConsoleUI.println("Welcome to Proctor - Terminal-Based Examination Platform");
        while (true) {
            if (!Session.isLoggedIn()) {
                boolean loggedIn = authController.loginMenu();
                if (!loggedIn) continue;
            }
            mainMenu.run();
        }
    }
}
