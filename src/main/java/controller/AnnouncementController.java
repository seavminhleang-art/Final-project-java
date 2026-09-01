package controller;

import db.Session;
import model.entity.Announcement;
import model.entity.User;
import model.entity.enums.Role;
import model.service.AnnouncementService;
import view.ConsoleUI;

import java.util.List;

public class AnnouncementController {
    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    /** Menu label with an unread badge, for the dashboard listing. */
    public String menuLabel() {
        int unread = announcementService.unreadCount(Session.current());
        return "Announcements" + (unread > 0 ? " (" + unread + " unread)" : "");
    }

    public void menu() {
        User user = Session.current();
        boolean canPost = user.getRole() == Role.ADMIN || user.getRole() == Role.TEACHER;
        boolean back = false;
        while (!back) {
            ConsoleUI.banner("Announcements");
            ConsoleUI.println("1. View my announcements");
            if (canPost) {
                ConsoleUI.println("2. Post an announcement");
                ConsoleUI.println("3. View announcements I've posted");
            }
            ConsoleUI.println("0. Back");
            switch (ConsoleUI.prompt("Choose")) {
                case "1" -> viewInbox();
                case "2" -> { if (canPost) post(); else invalid(); }
                case "3" -> { if (canPost) viewSent(); else invalid(); }
                case "0" -> back = true;
                default -> invalid();
            }
        }
    }

    private void invalid() {
        ConsoleUI.error("Invalid choice.");
    }

    private void viewInbox() {
        List<Announcement> announcements = announcementService.inboxFor(Session.current());
        if (announcements.isEmpty()) {
            ConsoleUI.println("(no announcements)");
            return;
        }
        ConsoleUI.banner("My Announcements");
        for (Announcement a : announcements) {
            String flag = a.getUserId() != null && !a.isRead() ? "[NEW] " : "";
            ConsoleUI.println(a.getId() + ". " + flag + "[" + a.getType() + "] " + a.getMessage()
                    + "  (" + a.getCreatedAt() + ")");
        }
        Long id = ConsoleUI.promptLong("Announcement ID to mark as read (0 to skip)");
        if (id != 0) {
            announcementService.markRead(id, Session.current());
            ConsoleUI.success("Marked read.");
        }
    }

    private void post() {
        ConsoleUI.banner("Post Announcement");
        ConsoleUI.println("Send to: 1=Everyone 2=All students 3=All teachers 4=Specific user (by ID)");
        String choice = ConsoleUI.prompt("Choose");
        Long targetUserId = null;
        Role targetRole = null;
        switch (choice) {
            case "2" -> targetRole = Role.STUDENT;
            case "3" -> targetRole = Role.TEACHER;
            case "4" -> targetUserId = ConsoleUI.promptLong("User ID");
            default -> { /* everyone: both null */ }
        }
        String message = ConsoleUI.prompt("Message");
        if (message.isBlank()) {
            ConsoleUI.error("Message cannot be blank.");
            return;
        }
        try {
            announcementService.post(Session.current(), targetUserId, targetRole, message);
            ConsoleUI.success("Announcement posted.");
        } catch (RuntimeException e) {
            ConsoleUI.error(e.getMessage());
        }
    }

    private void viewSent() {
        List<Announcement> sent = announcementService.sentBy(Session.current());
        if (sent.isEmpty()) {
            ConsoleUI.println("(you haven't posted any announcements)");
            return;
        }
        ConsoleUI.banner("Announcements I've Posted");
        for (Announcement a : sent) {
            String target = a.getUserId() != null ? ("user #" + a.getUserId())
                    : (a.getRoleTarget() != null ? "all " + a.getRoleTarget() + "s" : "everyone");
            ConsoleUI.println(a.getId() + ". To " + target + ": " + a.getMessage() + "  (" + a.getCreatedAt() + ")");
        }
        ConsoleUI.pause();
    }
}
