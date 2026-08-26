package service;

import model.entity.enums.Difficulty;
import model.entity.Question;

import java.util.List;

public interface QuestionService {
    Question create(Question question);
    Question update(Question question);
    void delete(Long id);
    Question get(Long id);
    List<Question> bySubject(Long subjectId);
    List<Question> filter(Long subjectId, Difficulty difficulty, String keyword);
}
