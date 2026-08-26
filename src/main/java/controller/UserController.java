package controller;

import model.entity.User;
import model.entity.enums.Role;
import model.service.UserService;
import view.ConsoleUI;

import java.util.List;

public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    public void menu() {
        boolean back = false;
        while (!back) {
            ConsoleUI.banner("User Management");
            ConsoleUI.println("1. List all users");
            ConsoleUI.println("2. Create user");
            ConsoleUI.println("3. Search users");
            ConsoleUI.println("4. Update user");
            ConsoleUI.println("5. Reset a user's password");
            ConsoleUI.println("6. Deactivate/Delete user");
            ConsoleUI.println("0. Back");
            switch (ConsoleUI.prompt("Choose")) {
                case "1" -> listAll();
                case "2" -> create();
                case "3" -> search();
                case "4" -> update();
                case "5" -> resetPassword();
                case "6" -> delete();
                case "0" -> back = true;
                default -> ConsoleUI.error("Invalid choice.");
            }
        }
    }

    private void listAll() {
        List<User> users = userService.listAll();
        print(users);
    }

    private void create() {
        ConsoleUI.banner("Create User");
        String username = ConsoleUI.prompt("Username");
        String password = ConsoleUI.promptPassword("Temporary password");
        String fullName = ConsoleUI.prompt("Full name");
        String email = ConsoleUI.prompt("Email");
        Role role = promptRole();
        try {
            User user = User.builder().username(username).fullName(fullName).email(email).role(role).build();
            userService.create(user, password);
            ConsoleUI.success("User created.");
        } catch (RuntimeException e) {
            ConsoleUI.error(e.getMessage());
        }
    }

    private void search() {
        String keyword = ConsoleUI.prompt("Search keyword (username/name/email, blank = all)");
        ConsoleUI.println("Filter by role? 1=Any 2=Admin 3=Teacher 4=Student");
        Role role = switch (ConsoleUI.prompt("Choose")) {
            case "2" -> Role.ADMIN;
            case "3" -> Role.TEACHER;
            case "4" -> Role.STUDENT;
            default -> null;
        };
        print(userService.search(keyword, role));
    }

    private void update() {
        Long id = ConsoleUI.promptLong("User ID to update");
        try {
            User user = userService.get(id);
            ConsoleUI.println("Leave blank to keep current value.");
            String fullName = ConsoleUI.prompt("Full name [" + user.getFullName() + "]");
            String email = ConsoleUI.prompt("Email [" + user.getEmail() + "]");
            if (!fullName.isBlank()) user.setFullName(fullName);
            if (!email.isBlank()) user.setEmail(email);
            boolean active = ConsoleUI.promptYesNo("Active?");
            user.setActive(active);
            userService.update(user);
            ConsoleUI.success("User updated.");
        } catch (RuntimeException e) {
            ConsoleUI.error(e.getMessage());
        }
    }

    private void resetPassword() {
        Long id = ConsoleUI.promptLong("User ID");
        String newPassword = ConsoleUI.promptPassword("New password");
        try {
            userService.resetPassword(id, newPassword);
            ConsoleUI.success("Password reset.");
        } catch (RuntimeException e) {
            ConsoleUI.error(e.getMessage());
        }
    }

    private void delete() {
        Long id = ConsoleUI.promptLong("User ID to delete");
        if (ConsoleUI.promptYesNo("Are you sure?")) {
            try {
                userService.delete(id);
                ConsoleUI.success("User deleted.");
            } catch (RuntimeException e) {
                ConsoleUI.error(e.getMessage());
            }
        }
    }

    private Role promptRole() {
        ConsoleUI.println("Role: 1=Admin 2=Teacher 3=Student");
        return switch (ConsoleUI.prompt("Choose")) {
            case "1" -> Role.ADMIN;
            case "2" -> Role.TEACHER;
            default -> Role.STUDENT;
        };
    }

    private void print(List<User> users) {
        if (users.isEmpty()) {
            ConsoleUI.println("(no users found)");
            return;
        }
        ConsoleUI.println(String.format("%-4s %-15s %-20s %-25s %-9s %-6s", "ID", "Username", "Full Name", "Email", "Role", "Active"));
        for (User u : users) {
            ConsoleUI.println(String.format("%-4d %-15s %-20s %-25s %-9s %-6s",
                    u.getId(), u.getUsername(), u.getFullName(), u.getEmail(), u.getRole(), u.isActive()));
        }
    }
}
