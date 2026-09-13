package com.proctor.model.entity;

import com.proctor.model.enums.AssessmentType;
import com.proctor.model.enums.QuestionType;
import com.proctor.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Quiz {
    private Integer id;
    private Integer subjectId;
    private String subjectCode;
    private Integer createdBy;
    private String creatorName;
    private String creatorGender;
    private Role creatorRole;

    public String getCreatorName() {
        if (creatorName == null || creatorName.isBlank()) {
            return creatorName;
        }
        if (creatorRole != null && creatorRole != Role.TEACHER) {
            return creatorName;
        }
        return User.formatTeacherName(creatorGender, creatorName);
    }
    @Builder.Default
    private AssessmentType assessmentType = AssessmentType.QUIZ;
    private QuestionType quizQuestionType;
    private String title;
    private String topic;
    private String description;
    private Integer timeLimitMins;
    private int passScore;
    private boolean randomizeQuestions;
    private boolean randomizeAnswers;
    private boolean showAnswersAfter;
    private boolean published;
    private Timestamp expiresAt;
    private Integer activeDurationHours;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    @Builder.Default
    private List<Question> questions = new ArrayList<>();
    private int questionCount;
    private double totalPoints;

    public boolean isExpired() {
        return expiresAt != null && expiresAt.before(new Timestamp(System.currentTimeMillis()));
    }
}