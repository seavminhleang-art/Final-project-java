package model.entity;

import model.entity.enums.AttemptStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attempt {
    private Long id;
    private Long quizId;
    private Long studentId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AttemptStatus status;
    private BigDecimal score;
    private BigDecimal totalMarks;
    private BigDecimal percentage;
    private Boolean passed;
}