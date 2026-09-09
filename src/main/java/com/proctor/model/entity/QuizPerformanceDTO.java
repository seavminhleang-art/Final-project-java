package com.proctor.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizPerformanceDTO {
    private int quizId;
    private String quizTitle;
    private String subjectCode;
    private int totalAttempts;
    private double avgScore;
    private double passRate;
    private double topScore;
}