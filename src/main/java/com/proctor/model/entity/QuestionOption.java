package com.proctor.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionOption {
    private Integer id;
    private Integer questionId;
    private String optionText;
    private boolean correct;
    private int optionOrder;
}