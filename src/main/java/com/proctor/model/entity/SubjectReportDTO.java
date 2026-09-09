package com.proctor.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectReportDTO {
    private int subjectId;
    private String subjectCode;
    private String subjectName;
    private int teacherCount;
    private int quizCount;
    private int totalAttempts;
    private double avgScore;
}