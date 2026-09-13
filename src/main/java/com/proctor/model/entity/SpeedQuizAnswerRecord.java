package com.proctor.model.entity;

import com.proctor.model.enums.Difficulty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpeedQuizAnswerRecord {
    private Question question;
    private Difficulty tierShown;
    private Integer selectedOptionId;
    private boolean correct;
    private int secondsRemaining;
    private double basePoints;
    private double speedBonus;
    private double streakBonus;
    private double totalPointsAwarded;
}
