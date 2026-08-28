package repository;

import model.entity.Announcement;
import model.entity.enums.Role;

import java.util.List;

public interface AnnouncementDao {
    Announcement create(Announcement announcement);

    /** Announcements visible to this user: addressed to them directly, or broadcast to their role / everyone. */
    List<Announcement> findForUser(Long userId, Role role);

    /** Announcements a teacher/admin posted themselves (for their own "sent" view). */
    List<Announcement> findPostedBy(Long postedBy);

    boolean markRead(Long announcementId, Long userId);

    int countUnreadForUser(Long userId, Role role);
}