package model.entity;

import model.entity.enums.Role;
import model.entity.enums.AnnouncementType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Announcement {
    private Long id;
    /** Direct recipient. Null = broadcast (see roleTarget). */
    private Long userId;
    /** Broadcast filter when userId is null: a specific role, or null for everyone. */
    private Role roleTarget;
    private Long quizId;
    private Long attemptId;
    private String message;
    private AnnouncementType type;
    private Long postedBy;
    private boolean read;
    private LocalDateTime createdAt;
}