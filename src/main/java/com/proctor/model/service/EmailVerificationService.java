package com.proctor.model.service;

import com.proctor.exception.ValidationException;
import com.proctor.model.entity.EmailVerificationToken;
import com.proctor.model.entity.User;
import com.proctor.model.enums.Role;
import com.proctor.model.repository.EmailVerificationRepository;
import com.proctor.model.repository.UserRepository;
import com.proctor.util.PasswordUtils;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.util.Optional;

public class EmailVerificationService {

    public static final long TOKEN_EXPIRY_MILLIS = 10L * 60 * 1000;

    private final EmailVerificationRepository verificationRepo;
    private final UserRepository userRepo;
    private final EmailService emailService;
    private final SecureRandom random = new SecureRandom();

    public EmailVerificationService() {
        this(new EmailVerificationRepository(), new UserRepository(), new EmailService());
    }

    public EmailVerificationService(EmailVerificationRepository verificationRepo, UserRepository userRepo, EmailService emailService) {
        this.verificationRepo = verificationRepo;
        this.userRepo = userRepo;
        this.emailService = emailService;
    }

    public String sendRegistrationCode(String email, String fullName) {
        if (email == null || email.isBlank()) {
            throw new ValidationException("Email is required.");
        }
        String cleanEmail = email.trim().toLowerCase();

        if (userRepo.findByEmail(cleanEmail).isPresent()) {
            throw new ValidationException("An account with this email address already exists.");
        }

        verificationRepo.invalidatePendingTokens(cleanEmail, "REGISTRATION");

        String code = generateCode();
        Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + TOKEN_EXPIRY_MILLIS);
        verificationRepo.saveToken(cleanEmail, code, "REGISTRATION", expiresAt);

        emailService.sendRegistrationCode(cleanEmail, fullName, code);
        return code;
    }

    public String sendRegistrationCode(String email) {
        return sendRegistrationCode(email, null);
    }

    public boolean verifyRegistrationCode(String email, String code) {
        if (email == null || code == null) {
            throw new ValidationException("Email and verification code are required.");
        }
        String cleanEmail = email.trim().toLowerCase();
        String cleanCode = code.trim();

        Optional<EmailVerificationToken> tokenOpt = verificationRepo.findLatestValidToken(cleanEmail, cleanCode, "REGISTRATION");
        if (tokenOpt.isEmpty()) {
            throw new ValidationException("Invalid or expired verification code. Please check your email or click Resend.");
        }

        verificationRepo.markTokenUsed(tokenOpt.get().getId());
        return true;
    }

    public String sendPasswordResetCode(String emailOrUsername) {
        if (emailOrUsername == null || emailOrUsername.isBlank()) {
            throw new ValidationException("Please enter your email or username.");
        }

        Optional<User> userOpt = userRepo.findByEmailOrUsername(emailOrUsername.trim());
        if (userOpt.isEmpty()) {
            throw new ValidationException("No user found with that email or username.");
        }

        User user = userOpt.get();
        if (!user.isEnabled()) {
            throw new ValidationException("This account has been disabled. Please contact an administrator.");
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ValidationException("User does not have a registered email address.");
        }

        String userEmail = user.getEmail().trim().toLowerCase();
        verificationRepo.invalidatePendingTokens(userEmail, "PASSWORD_RESET");

        String code = generateCode();
        Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + TOKEN_EXPIRY_MILLIS);
        verificationRepo.saveToken(userEmail, code, "PASSWORD_RESET", expiresAt);

        emailService.sendPasswordResetCode(userEmail, user.getFullName(), code);
        return maskEmail(userEmail);
    }

    public boolean verifyPasswordResetCode(String emailOrUsername, String code) {
        if (emailOrUsername == null || emailOrUsername.isBlank()) {
            throw new ValidationException("Email or username is required.");
        }
        if (code == null || code.isBlank()) {
            throw new ValidationException("Verification code is required.");
        }

        Optional<User> userOpt = userRepo.findByEmailOrUsername(emailOrUsername.trim());
        if (userOpt.isEmpty()) {
            throw new ValidationException("No user found with that email or username.");
        }

        User user = userOpt.get();
        if (!user.isEnabled()) {
            throw new ValidationException("This account has been disabled. Please contact an administrator.");
        }

        String userEmail = user.getEmail().trim().toLowerCase();
        String cleanCode = code.trim();

        Optional<EmailVerificationToken> tokenOpt = verificationRepo.findLatestValidToken(userEmail, cleanCode, "PASSWORD_RESET");
        if (tokenOpt.isEmpty()) {
            throw new ValidationException("Invalid or expired verification code. Please check your email or request a new code.");
        }

        return true;
    }

    public boolean verifyAndResetPassword(String emailOrUsername, String code, String newPassword, String confirmPassword) {
        if (emailOrUsername == null || emailOrUsername.isBlank()) {
            throw new ValidationException("Email or username is required.");
        }
        if (code == null || code.isBlank()) {
            throw new ValidationException("Verification code is required.");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new ValidationException("New password is required.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new ValidationException("Passwords do not match.");
        }

        PasswordUtils.validatePassword(newPassword);

        Optional<User> userOpt = userRepo.findByEmailOrUsername(emailOrUsername.trim());
        if (userOpt.isEmpty()) {
            throw new ValidationException("No user found with that email or username.");
        }

        User user = userOpt.get();
        if (!user.isEnabled()) {
            throw new ValidationException("This account has been disabled. Please contact an administrator.");
        }
        if (PasswordUtils.verify(newPassword.trim(), user.getPasswordHash())) {
            throw new ValidationException("New password must be different from your current password.");
        }

        String userEmail = user.getEmail().trim().toLowerCase();
        String cleanCode = code.trim();

        Optional<EmailVerificationToken> tokenOpt = verificationRepo.findLatestValidToken(userEmail, cleanCode, "PASSWORD_RESET");
        if (tokenOpt.isEmpty()) {
            throw new ValidationException("Invalid or expired verification code. Please check your email or request a new code.");
        }

        String newHash = PasswordUtils.hash(newPassword);
        boolean updated = userRepo.updatePassword(user.getId(), newHash);
        if (!updated) {
            throw new ValidationException("Failed to update password in database.");
        }

        verificationRepo.markTokenUsed(tokenOpt.get().getId());
        return true;
    }

    public boolean verifyPasswordResetAndChangePassword(String emailOrUsername, String code, String newPassword) {
        return verifyAndResetPassword(emailOrUsername, code, newPassword, newPassword);
    }

    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int atIndex = email.indexOf('@');
        String name = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (name.length() <= 2) {
            return name.charAt(0) + "***" + domain;
        }
        return name.charAt(0) + "***" + name.charAt(name.length() - 1) + domain;
    }

    private String generateCode() {
        int number = random.nextInt(1_000_000);
        return String.format("%06d", number);
    }

    public EmailService getEmailService() {
        return emailService;
    }
}
