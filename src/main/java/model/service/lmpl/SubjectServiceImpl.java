package service.impl;

import repository.SubjectDao;
import exception.NotFoundException;
import model.entity.Subject;
import service.SubjectService;

import java.util.List;

public class SubjectServiceImpl implements SubjectService {

    private final SubjectDao subjectDao;

    public SubjectServiceImpl(SubjectDao subjectDao) {
        this.subjectDao = subjectDao;
    }

    @Override
    public Subject create(Subject subject) {
        return subjectDao.create(subject);
    }

    @Override
    public Subject update(Subject subject) {
        get(subject.getId());
        return subjectDao.update(subject);
    }

    @Override
    public void delete(Long id) {
        get(id);
        subjectDao.delete(id);
    }

    @Override
    public Subject get(Long id) {
        return subjectDao.findById(id).orElseThrow(() -> new NotFoundException("Subject not found: " + id));
    }

    @Override
    public List<Subject> listAll() {
        return subjectDao.findAll();
    }
}
