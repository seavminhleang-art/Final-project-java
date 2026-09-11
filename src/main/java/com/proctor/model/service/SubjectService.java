package com.proctor.model.service;

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




    public Subject createSubject(String code, String name, String description) {
        if (code == null || code.isBlank() || name == null || name.isBlank()) {
            throw new ValidationException("Subject code and name are required.");
        }

        String cleanCode = code.trim().toUpperCase();
        if (cleanCode.length() > 20) {
            throw new ValidationException("Subject code cannot exceed 20 characters.");
        }
        if (name.trim().length() > 100) {
            throw new ValidationException("Subject name cannot exceed 100 characters.");
        }
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

    public Subject updateSubject(int id, String code, String name, String description, boolean enabled) {
        if (code == null || code.isBlank()) {
            throw new ValidationException("Subject code cannot be blank.");
        }
        if (name == null || name.isBlank()) {
            throw new ValidationException("Subject name cannot be blank.");
        }

        String cleanCode = code.trim().toUpperCase();
        if (cleanCode.length() > 20) {
            throw new ValidationException("Subject code cannot exceed 20 characters.");
        }
        if (name.trim().length() > 100) {
            throw new ValidationException("Subject name cannot exceed 100 characters.");
        }

        Optional<Subject> withCode = subjectRepository.findByCode(cleanCode);
        if (withCode.isPresent() && !withCode.get().getId().equals(id)) {
            throw new ValidationException("Subject code '" + cleanCode + "' is already in use by another subject.");
        }

        Optional<Subject> existing = subjectRepository.findById(id);
        if (existing.isEmpty()) {
            throw new ValidationException("Subject not found.");
        }

        Subject subject = existing.get();
        subject.setCode(cleanCode);
        subject.setName(name.trim());
        subject.setDescription(description != null ? description.trim() : "");
        subject.setEnabled(enabled);

        boolean updated = subjectRepository.update(subject);
        if (!updated) {
            throw new ValidationException("Failed to update subject.");
        }
        return subject;
    }

    public Subject updateSubject(int id, String name, String description, boolean enabled) {
        Subject s = subjectRepository.findById(id).orElseThrow(() -> new ValidationException("Subject not found."));
        return updateSubject(id, s.getCode(), name, description, enabled);
    }

    public boolean deleteSubject(int id) {
        Optional<Subject> existing = subjectRepository.findById(id);
        if (existing.isEmpty()) {
            throw new ValidationException("Subject not found.");
        }
        boolean deleted = subjectRepository.delete(id);
        if (!deleted) {
            throw new ValidationException("Failed to delete subject.");
        }
        return true;
    }

    public boolean toggleSubjectStatus(int id) {
        return subjectRepository.toggleEnabled(id);
    }
}