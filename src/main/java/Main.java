import controller.*;
import db.DatabaseSeeder;
import db.DbConnection;
import db.Session;
import model.repository.AnnounceRepo;
import model.repository.AttemptDao;
import model.repository.UserRepo;
import model.repository.impl.*;
import model.service.*;
import model.service.impl.*;

import view.ConsoleUI;

public class Main {
    public static void main(String[] args) {
        // DAOs
        UserRepo userRepo = new UserRepoImpl();
        repository.SubjectDao subjectDao = new SubjectDaoImpl();
        QuestionRepo questionDao = new QuestionRepo();
        repository.QuizDao quizDao = new QuizDaoImpl();
        AttemptDao attemptDao = new AttemptDaoImpl();
        AnnounceRepo announcementDao = new AnnounceRepoImpl();

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
        SubjectServiceImpl subjectService = new SubjectServiceImpl(subjectDao);
        model.service.QuestionService questionService = new QuestionServiceImpl(questionDao);
        QuizServiceImpl quizService = new QuizServiceImpl(quizDao);
        model.service.AnnouncementService announcementService = new AnnouncementServiceImpl(announcementDao);
        ExamServiceImpl examService = new ExamServiceImpl(attemptDao, quizDao, announcementService);
        model.service.ReportService reportService = new ReportServiceImpl(attemptDao, userRepo, subjectDao, questionDao, quizDao);

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

        Runtime.getRuntime().addShutdownHook(new Thread(() -> DbConnection.getInstance().shutdown()));

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

