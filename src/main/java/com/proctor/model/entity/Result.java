package com.proctor.model.entity;

import com.proctor.model.enums.AssessmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Result {
    private Integer id;
    private Integer attemptId;
    private Integer studentId;
    private String studentName;
    private Integer quizId;
    private String quizTitle;
    private AssessmentType assessmentType;
    private double totalPoints;
    private double maxPoints;
    private double percentage;
    private boolean passed;
    private boolean pendingReview;
    private Timestamp gradedAt;
}