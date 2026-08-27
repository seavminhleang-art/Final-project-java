package controller;

import db.Session;
import model.entity.User;
import model.entity.enums.Role;
import model.service.AuthService;
import view.ConsoleUI;

public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Returns true once a user is logged in (loops on bad credentials).
     * There is no self-registration: STUDENT and TEACHER accounts are created only by an ADMIN
     * via User Management, and ADMIN accounts likewise (or the bootstrapped seed admin).
     */
    public boolean loginMenu() {
        ConsoleUI.banner("PROCTOR - Sign In");
        ConsoleUI.println("1. Login");
        ConsoleUI.println("2. Register");
        ConsoleUI.println("0. Exit");

        String choice = ConsoleUI.prompt("Choose");

        switch (choice) {
            case "1" -> {
                return login();
            }
            case "2" -> {
                 registerStudent();
                 return false;
            }
            case "0" -> {
                System.exit(0);
                return false;
            }
            default -> {
                ConsoleUI.error("Invalid choice.");
                return false;
            }
        }
    }

    private boolean login() {
        String username = ConsoleUI.prompt("Username");
        String password = ConsoleUI.promptPassword("Password");
        try {
            User user = authService.login(username, password);
            Session.login(user);
            ConsoleUI.success("Welcome, " + user.getFullName() + " (" + user.getRole() + ")");
            return true;
        } catch (RuntimeException e) {
            ConsoleUI.error(e.getMessage());
            return false;
        }
    }
    private void registerStudent() {
        ConsoleUI.banner("Student Registration");
        String username = ConsoleUI.prompt("Choose a username");
        String password = ConsoleUI.promptPassword("Choose a password");
        String confirm = ConsoleUI.promptPassword("Confirm password");
        String fullName = ConsoleUI.prompt("Full name");
        String email = ConsoleUI.prompt("Email");

        if (!password.equals(confirm)) {
            ConsoleUI.error("Passwords do not match.");
            return;
        }
        try {
            authService.register(username, password, fullName, email, Role.STUDENT);
            ConsoleUI.success("Account created. You can now log in.");
        } catch (RuntimeException e) {
            ConsoleUI.error(e.getMessage());
        }
    }
}
