package model.service.impl;

import db.PasswordUtils;
import exception.AuthenticationException;
import exception.AuthorizationException;
import model.entity.User;
import model.entity.enums.Role;
import model.repository.UserRepo;
import model.service.AuthService;
import org.postgresql.util.PasswordUtil;

import java.util.Arrays;
import java.util.Optional;

public class AuthServiceImpl implements AuthService {

    private final UserRepo userRepo;

    public AuthServiceImpl(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public User login(String username, String password) {
        Optional<User> found = userRepo.findByUsername(username);
        if (found.isEmpty()) {
            throw new AuthenticationException("Invalid username or password.");
        }
        User user = found.get();
        if (!user.isActive()) {
            throw new AuthenticationException("This account has been deactivated.");
        }
        if (!PasswordUtils.verify(password, user.getSalt(), user.getPasswordHash())) {
            throw new AuthenticationException("Invalid username or password.");
        }
        return user;
    }

    @Override
    public User register(String username, String rawPassword, String fullName, String email, Role role) {
        userRepo.findByUsername(username).ifPresent(u -> {
            throw new IllegalArgumentException("Username already taken.");
        });
        String salt = PasswordUtils.generateSalt();
        String hash = PasswordUtils.hash(rawPassword, salt);
        User user = User.builder()
                .username(username)
                .passwordHash(hash)
                .salt(salt)
                .fullName(fullName)
                .email(email)
                .role(role)
                .active(true)
                .build();
        return userRepo.create(user);
    }

    @Override
    public void resetPassword(Long userId, String newRawPassword) {
        String salt = PasswordUtils.generateSalt();
        String hash = PasswordUtils.hash(newRawPassword, salt);
        boolean ok = userRepo.updatePassword(userId, hash, salt);
        if (!ok) {
            throw new IllegalStateException("Failed to reset password: user not found.");
        }
    }

    @Override
    public void requireRole(User user, Role... allowed) {
        if (user == null || Arrays.stream(allowed).noneMatch(r -> r == user.getRole())) {
            throw new AuthorizationException("You do not have permission to perform this action.");
        }
    }
}
