package com.proctor.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntry {
    private int rank;
    private int studentId;
    private String studentName;
    private String username;
    private int totalQuizzes;
    private double totalPoints;
    private double avgPercentage;
}