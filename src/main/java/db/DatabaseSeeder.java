package db;

import model.entity.User;
import model.entity.enums.Role;
import model.repository.UserRepo;
import org.postgresql.util.PasswordUtil;
import view.ConsoleUI;

import java.util.Optional;

public class DatabaseSeeder {
    private DatabaseSeeder() {
    }

    public static void ensureAdminExists(UserRepo userRepo) {
        Optional<User> existing = userRepo.findByUsername("admin");
        if (existing.isPresent()) {
            return;
        }
        String salt = PasswordUtils.generateSalt();
        String hash = PasswordUtils.hash("Admin@123", salt);
        User admin = User.builder()
                .username("admin")
                .passwordHash(hash)
                .salt(salt)
                .fullName("System Administrator")
                .email("admin@proctor.local")
                .role(Role.ADMIN)
                .active(true)
                .build();
        userRepo.create(admin);
        ConsoleUI.println("First run detected: created default admin account (username: admin / password: Admin@123).");
        ConsoleUI.println("Please log in and change this password immediately.");
    }
}
