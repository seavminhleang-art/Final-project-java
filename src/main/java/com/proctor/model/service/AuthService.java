package com.proctor.model.service;

import com.proctor.model.entity.Session;
import com.proctor.model.entity.User;
import com.proctor.model.repository.UserRepository;
import com.proctor.exception.AuthException;
import com.proctor.util.PasswordUtils;

import java.util.Optional;

public class AuthService {
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Authenticate user with password verification and start active session
    public User login(String emailOrUsername, String rawPassword) {
        // Validate presence of credentials
        if (emailOrUsername == null || emailOrUsername.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            throw new AuthException("Email and password are required.");
        }

        // Look up user by email or username
        Optional<User> userOpt = userRepository.findByEmailOrUsername(emailOrUsername.trim());

        if (userOpt.isEmpty()) {
            throw new AuthException("Invalid email or password.");
        }

        User user = userOpt.get();
        // Verify account is enabled
        if (!user.isEnabled()) {
            throw new AuthException("Account is disabled. Contact an administrator.");
        }

        // Verify password hash against raw password
        if (!PasswordUtils.verify(rawPassword, user.getPasswordHash())) {
            throw new AuthException("Invalid email or password.");
        }

        // Establish global authenticated session
        Session.setLoggedInUser(user);
        return user;
    }

    // Clear authenticated user session
    public void logout() {
        Session.clear();
    }
}
