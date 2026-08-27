package model.entity;

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
public class Quiz {
    private Long id;
    private String title;
    private Long subjectId;
    private int durationMinutes;
    private BigDecimal passPercentage;
    private boolean published;
    /** Total attempts (initial + retakes) a student may take. 0 = unlimited. */
    @Builder.Default
    private int maxAttempts = 3;
    /** Optional exam schedule window. Both null = open any time it's published. */
    private LocalDateTime examOpenAt;
    private LocalDateTime examCloseAt;
    private Long createdBy;
    private LocalDateTime createdAt;
}
