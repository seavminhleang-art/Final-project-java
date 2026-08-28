package model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttemptAnswer {
    private Long attemptId;
    private Long questionId;
    private Character selectedOption; // nullable = unanswered
    private Boolean isCorrect;
}