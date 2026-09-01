package model.service.impl;

import model.entity.Announcement;
import model.entity.User;
import model.entity.enums.AnnouncementType;
import model.entity.enums.Role;
import model.repository.AnnounceRepo;
import model.service.AnnouncementService;

import java.util.List;

public class AnnouncementServiceImpl implements AnnouncementService {
    private final AnnounceRepo announceRepo;

    public AnnouncementServiceImpl(AnnounceRepo announceRepo) {
        this.announceRepo = announceRepo;
    }
    @Override
    public Announcement notifyFailure(Long studentId, Long quizId, Long attemptId, String quizTitle, int attemptsUsed, int maxAttempts) {
        String message;
        if (maxAttempts <= 0) {
            message = "You failed \"" + quizTitle + "\". Please re-exam when you're ready — retakes are unlimited.";
        } else {
            int remaining = Math.max(0, maxAttempts - attemptsUsed);
            message = remaining > 0
                    ? "You failed \"" + quizTitle + "\". Please re-exam — you have " + remaining
                    + " of " + maxAttempts + " attempt(s) remaining."
                    : "You failed \"" + quizTitle + "\". You have used all " + maxAttempts
                    + " of your attempts; contact your teacher if you need another chance.";
        }
        Announcement a = Announcement.builder()
                .userId(studentId)
                .quizId(quizId)
                .attemptId(attemptId)
                .message(message)
                .type(AnnouncementType.FAIL_RETRY)
                .read(false)
                .build();
        return announceRepo.create(a);
    }

    @Override
    public Announcement post(User postedBy, Long targetUserId, Role targetRole, String message) {
        Announcement a = Announcement.builder()
                .userId(targetUserId)
                .roleTarget(targetUserId == null ? targetRole : null)
                .message(message)
                .type(AnnouncementType.INFO)
                .postedBy(postedBy.getId())
                .read(false)
                .build();
        return announceRepo.create(a);
    }

    @Override
    public List<Announcement> inboxFor(User user) {
        return announceRepo.findForUser(user.getId(), user.getRole());
    }




    @Override
    public List<Announcement> sentBy(User user) {
        return announceRepo.findPostedBy(user.getId());
    }

    @Override
    public void markRead(Long announcementId, User user) {
        announceRepo.markRead(announcementId, user.getId());
    }

    @Override
    public int unreadCount(User user) {
        return announceRepo.countUnreadForUser(user.getId(), user.getRole());
    }
}
