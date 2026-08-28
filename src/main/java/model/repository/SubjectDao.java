package repository;

import model.entity.Subject;

import java.util.List;
import java.util.Optional;

public interface SubjectDao {
    Subject create(Subject subject);
    Optional<Subject> findById(Long id);
    List<Subject> findAll();
    Subject update(Subject subject);
    boolean delete(Long id);
}
