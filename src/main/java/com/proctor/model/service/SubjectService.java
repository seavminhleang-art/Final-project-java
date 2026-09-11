package com.proctor.model.service;

import com.proctor.model.entity.User;
import com.proctor.exception.ValidationException;
import com.proctor.model.entity.Subject;
import com.proctor.model.repository.SubjectRepository;

import com.proctor.model.enums.PredefinedSubject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SubjectService {
    private static final Map<String, Integer> PREDEFINED_ORDER = new HashMap<>();
    static {
        PredefinedSubject[] values = PredefinedSubject.values();
        for (int i = 0; i < values.length; i++) {
            PREDEFINED_ORDER.put(values[i].getCode().toUpperCase(), i);
        }
    }

    private final SubjectRepository subjectRepository;

    public SubjectService(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    public List<Subject> getSubjects(String search) {
        List<Subject> list = subjectRepository.findAll(search);
        list.sort((a, b) -> {
            Integer orderA = PREDEFINED_ORDER.get(a.getCode().toUpperCase());
            Integer orderB = PREDEFINED_ORDER.get(b.getCode().toUpperCase());
            if (orderA != null && orderB != null) {
                return Integer.compare(orderA, orderB);
            }
            if (orderA != null) return -1;
            if (orderB != null) return 1;
            return a.getName().compareToIgnoreCase(b.getName());
        });
        return list;
    }

    public Optional<Subject> getSubjectById(int id) {
        return subjectRepository.findById(id);
    }

    public Optional<Subject> getSubjectByCode(String code) {
        return subjectRepository.findByCode(code);
    }

    public Optional<Subject> getSubjectByName(String name) {
        return subjectRepository.findByName(name);
    }



    public Subject createSubject(String code, String name, String description) {
        if (code == null || code.isBlank() || name == null || name.isBlank()) {
            throw new ValidationException("Subject code and name are required.");
        }

        String cleanCode = code.trim().toUpperCase();
        if (subjectRepository.findByCode(cleanCode).isPresent()) {
            throw new ValidationException("Subject code '" + cleanCode + "' already exists.");
        }

        Subject subject = Subject.builder()
                .code(cleanCode)
                .name(name.trim())
                .description(description != null ? description.trim() : "")
                .enabled(true)
                .build();

        boolean created = subjectRepository.create(subject);
        if (!created) {
            throw new ValidationException("Failed to create subject.");
        }
        return subject;
    }

    public Subject updateSubject(int id, String name, String description, boolean enabled) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Subject name cannot be blank.");
        }

        Optional<Subject> existing = subjectRepository.findById(id);
        if (existing.isEmpty()) {
            throw new ValidationException("Subject not found.");
        }

        Subject subject = existing.get();
        subject.setName(name.trim());
        subject.setDescription(description != null ? description.trim() : "");
        subject.setEnabled(enabled);

        boolean updated = subjectRepository.update(subject);
        if (!updated) {
            throw new ValidationException("Failed to update subject.");
        }
        return subject;
    }

    public boolean toggleSubjectStatus(int id) {
        return subjectRepository.toggleEnabled(id);
    }

    public List<User> getAssignedTeachers(int subjectId) {
        return subjectRepository.getAssignedTeachers(subjectId);
    }

    public List<Subject> getSubjectsForTeacher(int teacherId) {
        return subjectRepository.getSubjectsForTeacher(teacherId);
    }

    public boolean assignTeacher(int userId, int subjectId) {
        return subjectRepository.assignTeacher(userId, subjectId);
    }

    public boolean unassignTeacher(int userId, int subjectId) {
        return subjectRepository.unassignTeacher(userId, subjectId);
    }
}