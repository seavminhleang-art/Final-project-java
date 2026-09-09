package com.proctor.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemOverviewDTO {
    private int totalUsers;
    private int totalTeachers;
    private int totalStudents;
    private int totalSubjects;
    private int totalQuizzes;
    private int totalAttempts;
    private double overallPassRate;
}