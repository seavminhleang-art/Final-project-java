package com.proctor.model.entity;

import com.proctor.model.enums.Difficulty;
import com.proctor.model.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    private Integer id;
    private Integer quizId;
    private Integer subjectId;
    private String subjectCode;
    private Integer createdBy;
    private String questionText;
    private QuestionType questionType;
    private Difficulty difficulty;
    @Builder.Default
    private double points = 1.0;
    private String explanation;
    private boolean aiGenerated;
    private boolean enabled;
    private Timestamp createdAt;

    @Builder.Default
    private List<QuestionOption> options = new ArrayList<>();
}