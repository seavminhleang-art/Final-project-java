package com.proctor.model.service;

import com.proctor.model.entity.User;
import com.proctor.model.repository.UserRepository;
import com.proctor.model.enums.Role;
import com.proctor.exception.ValidationException;
import com.proctor.util.PasswordUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class UserService {
    private final UserRepository userRepository;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9._-]+$");

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getUsers(String search, Role role) {
        return userRepository.findAll(search, role);
    }

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
        return createUser(email, username, rawPassword, fullName, role, null, "Other");
    }

    public User createUser(String email, String username, String rawPassword, String fullName, Role role) {
        return createUser(email, username, rawPassword, fullName, role, null, "Other");
    }

    public User createUser(String email, String username, String rawPassword, String fullName, Role role, LocalDate dateOfBirth) {
        return createUser(email, username, rawPassword, fullName, role, dateOfBirth, "Other");
    }

    public User createUser(String email, String username, String rawPassword, String fullName, Role role, LocalDate dateOfBirth, String gender) {
        return createUser(email, username, rawPassword, fullName, role, dateOfBirth, gender, null, null, null);
    }

    public User createUser(String email, String username, String rawPassword, String fullName, Role role, LocalDate dateOfBirth, String gender, String academicDegree, String educationBackground, String specialization) {
        if (email == null || email.isBlank() || username == null || username.isBlank() ||
                rawPassword == null || rawPassword.isBlank() || fullName == null || fullName.isBlank()) {
            throw new ValidationException("All fields are required.");
        }
        if (gender == null || gender.trim().isBlank()) {
            throw new ValidationException("Gender is required.");
        }
        if (fullName.trim().length() > 100) {
            throw new ValidationException("Full name cannot exceed 100 characters.");
        }
        if (role == Role.TEACHER && (academicDegree != null || educationBackground != null || specialization != null)) {
            if (academicDegree == null || academicDegree.trim().isBlank()) {
                throw new ValidationException("Academic degree / qualification is required.");
            }
            if (educationBackground == null || educationBackground.trim().isBlank()) {
                throw new ValidationException("Education background (university) is required.");
            }
            if (specialization == null || specialization.trim().isBlank()) {
                throw new ValidationException("Primary subject / specialization is required.");
            }
        }
        if (dateOfBirth != null) {
            validateDateOfBirth(dateOfBirth, role != null ? role : Role.STUDENT);
        }

        PasswordUtils.validatePassword(rawPassword);

        String cleanEmail = email.trim().toLowerCase();
        String cleanUsername = username.trim().toLowerCase();

        if (cleanEmail.length() > 100) {
            throw new ValidationException("Email cannot exceed 100 characters.");
        }

        if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            throw new ValidationException("Invalid email format (e.g. user@proctor.edu).");
        }

        if (cleanUsername.length() < 3) {
            throw new ValidationException("Username must be at least 3 characters long.");
        }

        if (cleanUsername.length() > 50) {
            throw new ValidationException("Username cannot exceed 50 characters.");
        }

        if (cleanUsername.contains(" ")) {
            throw new ValidationException("Username cannot contain spaces.");
        }

        if (!USERNAME_PATTERN.matcher(cleanUsername).matches()) {
            throw new ValidationException("Username can only contain letters, numbers, dots, underscores, and hyphens.");
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
                .gender(gender.trim())
                .role(role != null ? role : Role.STUDENT)
                .enabled(true)
                .academicDegree(academicDegree != null ? academicDegree.trim() : null)
                .educationBackground(educationBackground != null ? educationBackground.trim() : null)
                .specialization(specialization != null ? specialization.trim() : null)
                .build();

        boolean created = userRepository.create(user);
        if (!created) {
            throw new ValidationException("Failed to create user.");
        }
        return user;
    }

    public User updateUser(int id, String fullName, Role role, boolean enabled, LocalDate dateOfBirth) {
        Optional<User> existing = userRepository.findById(id);
        String gender = existing.map(User::getGender).orElse("Other");
        return updateUser(id, fullName, role, enabled, dateOfBirth, gender);
    }

    public User updateUser(int id, String fullName, Role role, boolean enabled, LocalDate dateOfBirth, String gender) {
        Optional<User> existing = userRepository.findById(id);
        String deg = existing.map(User::getAcademicDegree).orElse(null);
        String edu = existing.map(User::getEducationBackground).orElse(null);
        String spec = existing.map(User::getSpecialization).orElse(null);
        return updateUser(id, fullName, role, enabled, dateOfBirth, gender, deg, edu, spec);
    }

    public User updateUser(int id, String fullName, Role role, boolean enabled, LocalDate dateOfBirth, String gender, String academicDegree, String educationBackground, String specialization) {
        if (fullName == null || fullName.isBlank()) {
            throw new ValidationException("Full name cannot be blank.");
        }
        if (fullName.trim().length() > 100) {
            throw new ValidationException("Full name cannot exceed 100 characters.");
        }
        if (gender == null || gender.trim().isBlank()) {
            throw new ValidationException("Gender is required.");
        }

        if (dateOfBirth != null) {
            validateDateOfBirth(dateOfBirth, role != null ? role : Role.STUDENT);
        }

        Optional<User> existing = userRepository.findById(id);
        if (existing.isEmpty()) {
            throw new ValidationException("User not found.");
        }

        User user = existing.get();
        user.setFullName(fullName.trim());
        user.setDateOfBirth(dateOfBirth);
        user.setGender(gender.trim());
        user.setAcademicDegree(academicDegree != null && !academicDegree.isBlank() ? academicDegree.trim() : null);
        user.setEducationBackground(educationBackground != null && !educationBackground.isBlank() ? educationBackground.trim() : null);
        user.setSpecialization(specialization != null && !specialization.isBlank() ? specialization.trim() : null);

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

    public boolean changePassword(int id, String currentPassword, String newPassword) {
        if (currentPassword == null || currentPassword.isBlank()) {
            throw new ValidationException("Current password cannot be blank.");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new ValidationException("New password cannot be blank.");
        }
        if (currentPassword.equals(newPassword.trim())) {
            throw new ValidationException("New password must be different from your current password.");
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

    private void validateDateOfBirth(LocalDate dateOfBirth, Role role) {
        if (dateOfBirth == null) return;
        if (!dateOfBirth.isBefore(LocalDate.now())) {
            throw new ValidationException("Date of birth must be in the past.");
        }
        if (dateOfBirth.isBefore(LocalDate.now().minusYears(120))) {
            throw new ValidationException("Invalid date of birth — year is too far in the past.");
        }
        if (role == Role.TEACHER || role == Role.ADMIN) {
            if (dateOfBirth.isAfter(LocalDate.now().minusYears(18))) {
                throw new ValidationException("Teachers and administrators must be at least 18 years old.");
            }
        } else if (role == Role.STUDENT) {
            if (dateOfBirth.isAfter(LocalDate.now().minusYears(5))) {
                throw new ValidationException("Students must be at least 5 years old.");
            }
        }
    }
}