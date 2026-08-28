package service;

import model.entity.Subject;

import java.util.List;

public interface SubjectService {
    Subject create(Subject subject);
    Subject update(Subject subject);
    void delete(Long id);
    Subject get(Long id);
    List<Subject> listAll();
}
