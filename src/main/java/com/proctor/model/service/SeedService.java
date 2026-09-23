package com.proctor.model.service;

import com.proctor.model.entity.Subject;
import com.proctor.model.entity.User;
import com.proctor.model.enums.PredefinedSubject;
import com.proctor.model.enums.Role;
import com.proctor.model.repository.SubjectRepository;
import com.proctor.model.repository.UserRepository;
import com.proctor.util.PasswordUtils;

import java.util.Optional;

public class SeedService {
    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_EMAIL = "admin@proctor.edu";
    public static final String ADMIN_PASSWORD = "admin123";
    public static final String ADMIN_FULLNAME = "System Administrator";

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;

    public SeedService(UserRepository userRepository) {
        this(userRepository, new SubjectRepository());
    }

    public SeedService(UserRepository userRepository, SubjectRepository subjectRepository) {
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
    }

    public void seedDefaultSubjects() {
        if (subjectRepository == null) return;
        for (PredefinedSubject ps : PredefinedSubject.values()) {
            Optional<Subject> byCode = subjectRepository.findByCode(ps.getCode());
            if (byCode.isPresent()) {
                Subject s = byCode.get();
                if (!s.getName().equals(ps.getDisplayName()) ||
                        !ps.getDescription().equals(s.getDescription()) ||
                        !s.isEnabled()) {
                    s.setName(ps.getDisplayName());
                    s.setDescription(ps.getDescription());
                    s.setEnabled(true);
                    subjectRepository.update(s);
                }
                continue;
            }

            Optional<Subject> byName = subjectRepository.findByName(ps.getDisplayName());
            if (byName.isPresent()) {
                Subject s = byName.get();
                s.setCode(ps.getCode());
                s.setDescription(ps.getDescription());
                s.setEnabled(true);
                subjectRepository.update(s);
                continue;
            }

            Subject newSubject = Subject.builder()
                    .code(ps.getCode())
                    .name(ps.getDisplayName())
                    .description(ps.getDescription())
                    .enabled(true)
                    .build();
            subjectRepository.create(newSubject);
        }
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
            userRepository.updatePassword(admin.getId(), PasswordUtils.hash(ADMIN_PASSWORD));
        }
    }
}