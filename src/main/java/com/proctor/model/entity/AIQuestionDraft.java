package com.proctor.model.entity;

import com.proctor.model.enums.Difficulty;
import com.proctor.model.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIQuestionDraft {
    private String questionText;
    private QuestionType questionType;
    private Difficulty difficulty;
    private double points;
    private String explanation;

    @Builder.Default
    private List<QuestionOption> options = new ArrayList<>();
}