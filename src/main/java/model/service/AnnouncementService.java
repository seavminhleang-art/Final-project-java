package model.service;

import model.entity.Announcement;
import model.entity.User;
import model.entity.enums.Role;

import java.util.List;

public interface AnnouncementService {

    /** System notice sent straight to the student: "you failed <quiz>, please re-exam", with retake info. */
    Announcement notifyFailure(Long studentId, Long quizId, Long attemptId, String quizTitle,
                               int attemptsUsed, int maxAttempts);

    /** Teacher/admin posts a notice: to one user, to a whole role, or to everyone (target/role null = everyone). */
    Announcement post(User postedBy, Long targetUserId, Role targetRole, String message);

    List<Announcement> inboxFor(User user);

    List<Announcement> sentBy(User user);

    void markRead(Long announcementId, User user);

    int unreadCount(User user);
}
