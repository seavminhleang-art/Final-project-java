package com.proctor.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttemptAnswer {
    private Integer id;
    private Integer attemptId;
    private Integer questionId;
    private Integer selectedOptionId;
    private String textAnswer;
    private Integer aiScore;
    private String aiFeedback;
    private String teacherFeedback;
    private Boolean correct;
    private double pointsAwarded;
}