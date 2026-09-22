package com.proctor.model.service;

import com.proctor.model.enums.Difficulty;
import com.proctor.model.enums.QuestionType;
import com.proctor.exception.ValidationException;
import com.proctor.model.entity.Question;
import com.proctor.model.entity.QuestionOption;
import com.proctor.model.repository.QuestionRepository;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class QuestionService {
    private final QuestionRepository questionRepository;

    public QuestionService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public List<Question> getQuestionsByQuizId(int quizId) {
        return questionRepository.findByQuizId(quizId);
    }


    public boolean deleteQuestion(int id) {
        return questionRepository.delete(id);
    }

    public Question createQuestion(Question q) {
        validateQuestion(q);
        normalizeQuestion(q);

        boolean created = questionRepository.create(q);
        if (!created) {
            throw new ValidationException("Failed to create question.");
        }
        return q;
    }

    public Question updateQuestion(Question q) {
        if (q.getId() == null) {
            throw new ValidationException("Question ID is required for update.");
        }
        validateQuestion(q);
        normalizeQuestion(q);

        boolean updated = questionRepository.update(q);
        if (!updated) {
            throw new ValidationException("Failed to update question.");
        }
        return q;
    }

    public List<Question> getBankQuestions(Integer createdBy, Integer subjectId,
                                           QuestionType type, Difficulty difficulty,
                                           String search) {
        return questionRepository.findBankQuestions(createdBy, subjectId, type, difficulty, search);
    }

    public int copyBankQuestionToQuiz(int bankQuestionId, int quizId) {
        if (bankQuestionId <= 0 || quizId <= 0) {
            throw new ValidationException("Invalid question or quiz ID.");
        }
        Optional<Question> bankQOpt = questionRepository.findById(bankQuestionId);
        if (bankQOpt.isPresent()) {
            Question bankQ = bankQOpt.get();
            List<Question> existing = questionRepository.findByQuizId(quizId);
            if (existing.stream().anyMatch(q -> q.getQuestionText() != null && q.getQuestionText().trim().equalsIgnoreCase(bankQ.getQuestionText().trim()))) {
                throw new ValidationException("This question is already in the quiz.");
            }
        }
        return questionRepository.copyToQuiz(bankQuestionId, quizId);
    }

    private void validateQuestion(Question q) {
        if (q.getQuestionText() == null || q.getQuestionText().isBlank()) {
            throw new ValidationException("Question text is required.");
        }
        if (q.getQuestionText().trim().length() > 500) {
            throw new ValidationException("Question text cannot exceed 500 characters.");
        }
        if (q.getExplanation() != null && q.getExplanation().trim().length() > 300) {
            throw new ValidationException("Explanation cannot exceed 300 characters.");
        }
        if (q.getPoints() <= 0) {
            throw new ValidationException("Question points must be greater than 0.");
        }
        if (q.getPoints() > 999.0) {
            throw new ValidationException("Question points cannot exceed 999.");
        }
        if (q.getDifficulty() == null) {
            q.setDifficulty(Difficulty.MEDIUM);
        }
        if (q.getQuestionType() == null) {
            q.setQuestionType(QuestionType.MCQ);
        }

        if (q.getQuestionType() == QuestionType.MCQ) {
            if (q.getOptions() == null || q.getOptions().size() < 2) {
                throw new ValidationException("MCQ questions require at least 2 options.");
            }
            Set<String> seenOptions = new HashSet<>();
            for (QuestionOption opt : q.getOptions()) {
                if (opt.getOptionText() == null || opt.getOptionText().isBlank()) {
                    throw new ValidationException("MCQ option text cannot be blank.");
                }
                if (opt.getOptionText().trim().length() > 200) {
                    throw new ValidationException("MCQ option text cannot exceed 200 characters.");
                }
                String normalized = opt.getOptionText().trim().toLowerCase();
                if (!seenOptions.add(normalized)) {
                    throw new ValidationException("Duplicate options are not allowed: \"" + opt.getOptionText().trim() + "\".");
                }
            }
            boolean hasCorrect = q.getOptions().stream().anyMatch(opt -> opt.isCorrect() && opt.getOptionText() != null && !opt.getOptionText().isBlank());
            if (!hasCorrect) {
                throw new ValidationException("At least one MCQ option must be marked as correct.");
            }
        }
    }

    private void normalizeQuestion(Question q) {
        if (q.getQuestionType() == QuestionType.SHORT_ANSWER) {
            q.setOptions(new ArrayList<>());
        } else if (q.getQuestionType() == QuestionType.TRUE_FALSE) {
            boolean trueIsCorrect = true;
            if (q.getOptions() != null && !q.getOptions().isEmpty()) {
                for (QuestionOption opt : q.getOptions()) {
                    if ("True".equalsIgnoreCase(opt.getOptionText())) {
                        trueIsCorrect = opt.isCorrect();
                    }
                }
            }
            List<QuestionOption> tfOptions = new ArrayList<>();
            tfOptions.add(QuestionOption.builder().optionText("True").correct(trueIsCorrect).optionOrder(1).build());
            tfOptions.add(QuestionOption.builder().optionText("False").correct(!trueIsCorrect).optionOrder(2).build());
            q.setOptions(tfOptions);
        }
    }
}