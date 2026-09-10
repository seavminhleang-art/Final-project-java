package com.proctor.model.service;

import com.proctor.model.entity.User;
import com.proctor.model.repository.UserRepository;
import com.proctor.model.enums.Role;
import com.proctor.util.PasswordUtils;

import java.util.Optional;

public class SeedService {
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_EMAIL = "admin@proctor.edu";
    public static final String ADMIN_PASSWORD = "admin123";
    public static final String ADMIN_FULLNAME = "System Administrator";

    private final UserRepository userRepository;

    public SeedService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public void seedDefaultAdmin() {
        Optional<User> existing = userRepository.findByUsername(ADMIN_USERNAME);
        if (existing.isEmpty()) {
            User admin = User.builder()
                    .email(ADMIN_EMAIL)
                    .username(ADMIN_USERNAME)
                    .passwordHash(PasswordUtils.hash(ADMIN_PASSWORD))
                    .fullName(ADMIN_FULLNAME)
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();
            userRepository.create(admin);
        } else {
            User admin = existing.get();
            admin.setPasswordHash(PasswordUtils.hash(ADMIN_PASSWORD));
            admin.setEnabled(true);
            admin.setRole(Role.ADMIN);
            userRepository.update(admin);
        }
    }
}