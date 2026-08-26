package service.impl;

import repository.QuestionDao;
import exception.NotFoundException;
import model.entity.enums.Difficulty;
import model.entity.Question;
import service.QuestionService;

import java.util.List;

public class QuestionServiceImpl implements QuestionService {

    private final QuestionDao questionDao;

    public QuestionServiceImpl(QuestionDao questionDao) {
        this.questionDao = questionDao;
    }

    @Override
    public Question create(Question question) {
        validate(question);
        return questionDao.create(question);
    }

    @Override
    public Question update(Question question) {
        get(question.getId());
        validate(question);
        return questionDao.update(question);
    }

    @Override
    public void delete(Long id) {
        get(id);
        questionDao.delete(id);
    }

    @Override
    public Question get(Long id) {
        return questionDao.findById(id).orElseThrow(() -> new NotFoundException("Question not found: " + id));
    }

    @Override
    public List<Question> bySubject(Long subjectId) {
        return questionDao.findBySubject(subjectId);
    }

    @Override
    public List<Question> filter(Long subjectId, Difficulty difficulty, String keyword) {
        return questionDao.filter(subjectId, difficulty, keyword);
    }

    private void validate(Question q) {
        if (q.getQuestionText() == null || q.getQuestionText().isBlank()) {
            throw new IllegalArgumentException("Question text cannot be empty.");
        }
        char correct = Character.toUpperCase(q.getCorrectOption());
        if ("ABCD".indexOf(correct) < 0) {
            throw new IllegalArgumentException("Correct option must be one of A, B, C, D.");
        }
        q.setCorrectOption(correct);
    }
}
