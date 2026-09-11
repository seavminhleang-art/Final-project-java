package com.proctor.model.entity;

import com.proctor.model.enums.AttemptStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attempt {
    private Integer id;
    private Integer quizId;
    private String quizTitle;
    private Integer studentId;
    private String studentName;
    private Timestamp startedAt;
    private Timestamp submittedAt;
    private AttemptStatus status;
}