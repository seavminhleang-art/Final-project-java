package com.proctor.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssessmentOverviewDTO {
    private Quiz quiz;
    private String subjectCode;
    private String subjectName;
    private String teacherName;
    private int questionCount;
    private double totalPoints;
    private Integer timeLimitMins;
    private Integer speedSecondsPerQuestion;
    private int passScorePercent;
    private String questionTypesSummary;
    private String overallDifficulty;
    private int easyQuestions;
    private int mediumQuestions;
    private int hardQuestions;

    private int totalTakers;
    private int passedCount;
    private double passRate;
    private double avgScore;
    private double topScore;

    private Attempt studentAttempt;
    private Result studentResult;
    private boolean canStart;
    private boolean canRetake;
    private boolean canViewResult;
}
