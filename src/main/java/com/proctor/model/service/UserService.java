package com.proctor.model.service;

import com.proctor.model.entity.User;
import com.proctor.model.repository.UserRepository;
import com.proctor.model.enums.Role;
import com.proctor.exception.ValidationException;
import com.proctor.util.PasswordUtils;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class UserService {
    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getUsers(String search, Role role) {
        return userRepository.findAll(search, role);
    }

    public Optional<User> getUserById(int id) {
        return userRepository.findById(id);
    }

    /** Convenience overload used by SeedService and admin quick-create (no birthday). */
    public User createUser(String emailOrUsername, String rawPassword, String fullName, Role role) {
        if (emailOrUsername == null || emailOrUsername.isBlank()) {
            throw new ValidationException("Email or username is required.");
        }
        String clean = emailOrUsername.trim();
        String email;
        String username;
        if (clean.contains("@")) {
            email = clean;
            username = clean.split("@")[0];
        } else {
            username = clean;
            email = clean + "@proctor.edu";
        }
        return createUser(email, username, rawPassword, fullName, role, null);
    }

    /** Overload without birthday — delegates to the main overload with null birthday. */
    public User createUser(String email, String username, String rawPassword, String fullName, Role role) {
        return createUser(email, username, rawPassword, fullName, role, null);
    }

    public User createUser(String email, String username, String rawPassword, String fullName, Role role, LocalDate dateOfBirth) {
        if (email == null || email.isBlank() || username == null || username.isBlank() ||
                rawPassword == null || rawPassword.isBlank() || fullName == null || fullName.isBlank()) {
            throw new ValidationException("All fields are required.");
        }

        PasswordUtils.validatePassword(rawPassword);

        String cleanEmail = email.trim().toLowerCase();
        String cleanUsername = username.trim().toLowerCase();

        if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            throw new ValidationException("Invalid email format (e.g. user@proctor.edu).");
        }

        if (cleanUsername.length() < 3) {
            throw new ValidationException("Username must be at least 3 characters long.");
        }

        if (userRepository.findByEmail(cleanEmail).isPresent()) {
            throw new ValidationException("An account with email '" + cleanEmail + "' already exists.");
        }

        if (userRepository.findByUsername(cleanUsername).isPresent()) {
            throw new ValidationException("Username '" + cleanUsername + "' is already taken.");
        }

        User user = User.builder()
                .email(cleanEmail)
                .username(cleanUsername)
                .passwordHash(PasswordUtils.hash(rawPassword))
                .fullName(fullName.trim())
                .dateOfBirth(dateOfBirth)
                .role(role != null ? role : Role.STUDENT)
                .enabled(true)
                .build();

        boolean created = userRepository.create(user);
        if (!created) {
            throw new ValidationException("Failed to create user.");
        }
        return user;
    }

    public User updateUser(int id, String fullName, Role role, boolean enabled, LocalDate dateOfBirth) {
        if (fullName == null || fullName.isBlank()) {
            throw new ValidationException("Full name cannot be blank.");
        }

        Optional<User> existing = userRepository.findById(id);
        if (existing.isEmpty()) {
            throw new ValidationException("User not found.");
        }

        User user = existing.get();
        user.setFullName(fullName.trim());
        user.setDateOfBirth(dateOfBirth);
        if (SeedService.ADMIN_USERNAME.equalsIgnoreCase(user.getUsername())) {
            role = Role.ADMIN;
            enabled = true;
        }
        if (role != null) user.setRole(role);
        user.setEnabled(enabled);

        boolean updated = userRepository.update(user);
        if (!updated) {
            throw new ValidationException("Failed to update user.");
        }
        return user;
    }

    public boolean toggleUserStatus(int id) {
        Optional<User> existing = userRepository.findById(id);
        if (existing.isPresent()) {
            User user = existing.get();
            if (user.getRole() == Role.ADMIN || SeedService.ADMIN_USERNAME.equalsIgnoreCase(user.getUsername())) {
                throw new ValidationException("The administrator account cannot be disabled.");
            }
        }
        return userRepository.toggleEnabled(id);
    }

    public String resetPassword(int id) {
        Optional<User> existing = userRepository.findById(id);
        if (existing.isEmpty()) {
            throw new ValidationException("User not found.");
        }

        User user = existing.get();
        if (user.getRole() == Role.ADMIN || SeedService.ADMIN_USERNAME.equalsIgnoreCase(user.getUsername())) {
            throw new ValidationException("The administrator account is hardcoded and cannot be reset.");
        }

        String tempPassword = "Temp@" + (1000 + random.nextInt(9000));
        String hash = PasswordUtils.hash(tempPassword);
        boolean updated = userRepository.updatePassword(id, hash);
        if (!updated) {
            throw new ValidationException("Failed to reset password.");
        }
        return tempPassword;
    }

    public boolean resetPasswordWithAdminPassword(int id, String newRawPassword) {
        PasswordUtils.validatePassword(newRawPassword);
        Optional<User> existing = userRepository.findById(id);
        if (existing.isEmpty()) {
            throw new ValidationException("User not found.");
        }
        User user = existing.get();
        if (user.getRole() == Role.ADMIN || SeedService.ADMIN_USERNAME.equalsIgnoreCase(user.getUsername())) {
            throw new ValidationException("The administrator account is hardcoded and cannot be modified.");
        }
        String hash = PasswordUtils.hash(newRawPassword.trim());
        boolean updated = userRepository.updatePassword(id, hash);
        if (!updated) {
            throw new ValidationException("Failed to reset password.");
        }
        return true;
    }

    public boolean changePassword(int id, String currentPassword, String newPassword) {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new ValidationException("Current password cannot be blank.");
        }
        PasswordUtils.validatePassword(newPassword);
        Optional<User> existing = userRepository.findById(id);
        if (existing.isEmpty()) {
            throw new ValidationException("User not found.");
        }
        User user = existing.get();
        if (user.getRole() == Role.ADMIN || SeedService.ADMIN_USERNAME.equalsIgnoreCase(user.getUsername())) {
            throw new ValidationException("The administrator account is hardcoded and cannot be modified.");
        }
        if (!PasswordUtils.verify(currentPassword, user.getPasswordHash())) {
            throw new ValidationException("Current password does not match.");
        }
        String hash = PasswordUtils.hash(newPassword.trim());
        boolean updated = userRepository.updatePassword(id, hash);
        if (!updated) {
            throw new ValidationException("Failed to update password.");
        }
        return true;
    }

    public boolean applyPasswordHash(int id, String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new ValidationException("Password hash cannot be blank.");
        }
        Optional<User> existing = userRepository.findById(id);
        if (existing.isEmpty()) {
            throw new ValidationException("User not found.");
        }
        User user = existing.get();
        if (user.getRole() == Role.ADMIN || SeedService.ADMIN_USERNAME.equalsIgnoreCase(user.getUsername())) {
            throw new ValidationException("The administrator account is hardcoded and cannot be modified.");
        }
        boolean updated = userRepository.updatePassword(id, passwordHash);
        if (!updated) {
            throw new ValidationException("Failed to update password.");
        }
        return true;
    }
}