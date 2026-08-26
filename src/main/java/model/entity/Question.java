package model.entity;

import model.entity.enums.Difficulty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    private Long id;
    private Long subjectId;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private char correctOption; // 'A'..'D'
    private Difficulty difficulty;
    private Long createdBy;
    private LocalDateTime createdAt;

    public String optionFor(char label) {
        return switch (Character.toUpperCase(label)) {
            case 'A' -> optionA;
            case 'B' -> optionB;
            case 'C' -> optionC;
            case 'D' -> optionD;
            default -> throw new IllegalArgumentException("Invalid option: " + label);
        };
    }
}
