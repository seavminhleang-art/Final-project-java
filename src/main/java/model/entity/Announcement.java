package model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import model.entity.enums.AnnouncementType;
import model.entity.enums.Role;

import java.time.LocalDateTime;
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Announcement {
    private Long id;
    private Long userId;
    private Role roleTarget;
    private Long quizId;
    private Long attemptId;
    private String message;
    private AnnouncementType type;
    private Long postedBy;
    private boolean read;
    private LocalDateTime createdAt;
}
