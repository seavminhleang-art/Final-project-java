package com.proctor.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamSession {
    private Attempt attempt;
    private Quiz quiz;

    @Builder.Default
    private List<Question> questions = new ArrayList<>();

    @Builder.Default
    private Map<Integer, Integer> selectedOptions = new HashMap<>();

    @Builder.Default
    private Map<Integer, String> textAnswers = new HashMap<>();

    private int remainingSeconds;
    private boolean isTimed;
}