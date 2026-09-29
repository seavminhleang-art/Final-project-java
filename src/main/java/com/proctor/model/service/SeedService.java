package com.proctor.model.service;

import com.proctor.config.Config;
import com.proctor.model.entity.Subject;
import com.proctor.model.entity.User;
import com.proctor.model.enums.PredefinedSubject;
import com.proctor.model.enums.Role;
import com.proctor.model.repository.SubjectRepository;
import com.proctor.model.repository.UserRepository;
import com.proctor.util.PasswordUtils;

import java.util.Optional;

public class SeedService {

    public static final String ADMIN_USERNAME = Config.get("admin.username", "admin");
    public static final String ADMIN_EMAIL    = Config.get("admin.email",    "admin@proctor.edu");
    public static final String ADMIN_PASSWORD = Config.get("admin.password", "admin123");
    public static final String ADMIN_FULLNAME = Config.get("admin.fullname", "System Administrator");

    private final UserRepository userRepository;
    private final SubjectRepository subjectRepository;

    public SeedService(UserRepository userRepository) {
        this(userRepository, new SubjectRepository());
    }

    public SeedService(UserRepository userRepository, SubjectRepository subjectRepository) {
        this.userRepository = userRepository;
        this.subjectRepository = subjectRepository;
    }

    // Seed or update the 15 predefined curriculum subjects
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
                    .code(ps.getCode()).name(ps.getDisplayName())
                    .description(ps.getDescription()).enabled(true).build();
            subjectRepository.create(newSubject);
        }
    }

    // Seed default system administrator account if not present
    public void seedDefaultAdmin() {
        Optional<User> existing = userRepository.findByUsername(ADMIN_USERNAME);
        if (existing.isEmpty()) {
            User admin = User.builder()
                    .email(ADMIN_EMAIL).username(ADMIN_USERNAME)
                    .passwordHash(PasswordUtils.hash(ADMIN_PASSWORD))
                    .fullName(ADMIN_FULLNAME).role(Role.ADMIN).enabled(true).build();
            userRepository.create(admin);
        } else {
            User admin = existing.get();
            boolean needUpdate = false;
            if (admin.getRole() != Role.ADMIN || !admin.isEnabled()) {
                admin.setRole(Role.ADMIN); admin.setEnabled(true); needUpdate = true;
            }
            if (admin.getEmail() == null || !admin.getEmail().contains("@")) {
                admin.setEmail(ADMIN_EMAIL); needUpdate = true;
            }
            if (needUpdate) userRepository.update(admin);
        }
    }
}
