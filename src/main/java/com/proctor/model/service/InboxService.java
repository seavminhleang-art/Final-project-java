package com.proctor.model.service;

import com.proctor.model.entity.User;
import com.proctor.model.repository.UserRepository;
import com.proctor.model.enums.Role;
import com.proctor.exception.ValidationException;
import com.proctor.model.repository.AttemptRepository;
import com.proctor.model.entity.InboxMessage;
import com.proctor.model.enums.InboxMessageType;
import com.proctor.model.enums.InboxStatus;
import com.proctor.util.PasswordUtils;
import com.proctor.model.repository.InboxRepository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class InboxService {
    private final InboxRepository inboxRepository;
    private final UserRepository userRepository;
    private final AttemptRepository attemptRepository;

    public InboxService(InboxRepository inboxRepository, UserRepository userRepository) {
        this(inboxRepository, userRepository, new AttemptRepository());
    }

    public InboxService(InboxRepository inboxRepository, UserRepository userRepository, AttemptRepository attemptRepository) {
        this.inboxRepository = inboxRepository;
        this.userRepository = userRepository;
        this.attemptRepository = attemptRepository;
    }

    public List<InboxMessage> getInbox(int userId) {
        return inboxRepository.findByRecipientId(userId);
    }

    public int getUnreadCount(int userId) {
        return inboxRepository.countUnreadByRecipientId(userId);
    }

    public Optional<InboxMessage> getMessage(int messageId) {
        return inboxRepository.findById(messageId);
    }

    public boolean markAsRead(int messageId) {
        return inboxRepository.markAsRead(messageId);
    }

    public boolean markAllAsRead(int userId) {
        return inboxRepository.markAllAsRead(userId);
    }

    public boolean deleteMessage(int messageId) {
        return inboxRepository.delete(messageId);
    }

    public InboxMessage sendNotification(int recipientId, String title, String body) {
        InboxMessage msg = InboxMessage.builder()
                .recipientId(recipientId)
                .type(InboxMessageType.NOTIFICATION)
                .title(title)
                .body(body)
                .status(InboxStatus.READ)
                .read(false)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build();
        inboxRepository.create(msg);
        return msg;
    }

    public InboxMessage sendQuizRetakeRequest(int studentId, int teacherId, int quizId, String quizTitle) {
        if (inboxRepository.hasPendingRequest(studentId, InboxMessageType.QUIZ_RETAKE, quizId)) {
            throw new ValidationException("You already have a pending retake request for this quiz.");
        }

        Optional<User> student = userRepository.findById(studentId);
        String studentName = student.map(User::getFullName).orElse("A student");

        InboxMessage msg = InboxMessage.builder()
                .senderId(studentId)
                .recipientId(teacherId)
                .type(InboxMessageType.QUIZ_RETAKE)
                .title("Quiz Retake Request: " + quizTitle)
                .body(String.format("Student %s has requested a retake for quiz \"%s\" after the timer expired.", studentName, quizTitle))
                .targetId(quizId)
                .status(InboxStatus.PENDING)
                .read(false)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build();

        boolean created = inboxRepository.create(msg);
        if (!created) {
            throw new ValidationException("Failed to send retake request.");
        }
        return msg;
    }

    public InboxMessage sendExamRetakeRequest(int studentId, int teacherId, int quizId, String examTitle, String reason, boolean missed) {
        return sendExamRetakeRequest(studentId, teacherId, quizId, examTitle, reason, missed, null);
    }

    public InboxMessage sendExamRetakeRequest(int studentId, int teacherId, int quizId, String examTitle, String reason, boolean missed, Timestamp examTimestamp) {
        if (inboxRepository.hasPendingRequest(studentId, InboxMessageType.EXAM_RETAKE, quizId)) {
            throw new ValidationException("You already have a pending makeup request for this exam.");
        }

        if (reason == null || reason.trim().isBlank()) {
            throw new ValidationException("A justification reason is required for exam makeup requests.");
        }

        if (examTimestamp != null) {
            long threeDaysMillis = 3L * 24 * 60 * 60 * 1000;
            if (System.currentTimeMillis() - examTimestamp.getTime() > threeDaysMillis) {
                throw new ValidationException("Exam retake requests must be submitted within 3 days (72 hours) of the exam.");
            }
        }

        Optional<User> student = userRepository.findById(studentId);
        String studentName = student.map(User::getFullName).orElse("A student");
        String cause = missed ? "missed the scheduled exam window" : "did not achieve a passing score";

        InboxMessage msg = InboxMessage.builder()
                .senderId(studentId)
                .recipientId(teacherId)
                .type(InboxMessageType.EXAM_RETAKE)
                .title("Exam Makeup Request: " + examTitle)
                .body(String.format("Student %s %s and requested a makeup for exam \"%s\".\n\nStudent's Justification:\n\"%s\"",
                        studentName, cause, examTitle, reason.trim()))
                .targetId(quizId)
                .status(InboxStatus.PENDING)
                .read(false)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build();

        boolean created = inboxRepository.create(msg);
        if (!created) {
            throw new ValidationException("Failed to send makeup request.");
        }
        return msg;
    }

    public int sendPasswordResetRequest(String identifier, String newPassword) {
        if (identifier == null || identifier.trim().isBlank()) {
            throw new ValidationException("Email or username is required.");
        }
        if (newPassword == null || newPassword.isBlank()) {
            throw new ValidationException("New password is required.");
        }

        Optional<User> userOpt = userRepository.findByEmailOrUsername(identifier.trim());
        if (userOpt.isEmpty()) {
            throw new ValidationException("No account found matching '" + identifier.trim() + "'.");
        }

        User requester = userOpt.get();
        if (requester.getRole() == Role.ADMIN || SeedService.ADMIN_USERNAME.equalsIgnoreCase(requester.getUsername())) {
            throw new ValidationException("The administrator account is hardcoded and cannot be reset.");
        }

        PasswordUtils.validatePassword(newPassword);
        String hash = PasswordUtils.hash(newPassword.trim());

        List<User> admins = userRepository.findAll(null, Role.ADMIN);
        if (admins.isEmpty()) {
            throw new ValidationException("No administrator found to receive the password reset request.");
        }

        int sent = 0;
        for (User admin : admins) {
            String bodyText = String.format("User @%s (%s, %s) has forgotten their password and provided their desired new password.\n[HASH:%s]",
                    requester.getUsername(), requester.getFullName(), requester.getEmail(), hash);

            InboxMessage msg = InboxMessage.builder()
                    .senderId(requester.getId())
                    .recipientId(admin.getId())
                    .type(InboxMessageType.PASSWORD_RESET)
                    .title("Password Reset Request: @" + requester.getUsername())
                    .body(bodyText)
                    .targetId(requester.getId())
                    .proposedPasswordHash(hash)
                    .status(InboxStatus.PENDING)
                    .read(false)
                    .createdAt(new Timestamp(System.currentTimeMillis()))
                    .build();

            if (inboxRepository.create(msg)) {
                sent++;
            }
        }
        return sent;
    }

    public boolean approvePasswordReset(int messageId) {
        Optional<InboxMessage> opt = inboxRepository.findById(messageId);
        if (opt.isEmpty()) return false;
        InboxMessage msg = opt.get();
        if (msg.getTargetId() == null) return false;

        String hash = msg.getEffectivePasswordHash();
        if (hash == null) return false;

        userRepository.updatePassword(msg.getTargetId(), hash);
        boolean updated = inboxRepository.updateStatus(messageId, InboxStatus.RESOLVED, new Timestamp(System.currentTimeMillis()));
        if (updated && msg.getSenderId() != null) {
            sendNotification(msg.getSenderId(), "Password Reset Approved",
                    "Your password change request has been approved by an administrator. You may now log in using the new password you specified.");
        }
        return updated;
    }

    public boolean approveQuizRetake(int messageId) {
        Optional<InboxMessage> opt = inboxRepository.findById(messageId);
        if (opt.isEmpty()) return false;
        InboxMessage msg = opt.get();
        if (msg.getTargetId() == null || msg.getSenderId() == null) return false;

        if (attemptRepository != null) {
            attemptRepository.deleteAttemptsForStudent(msg.getTargetId(), msg.getSenderId());
        }
        boolean updated = inboxRepository.updateStatus(messageId, InboxStatus.APPROVED, new Timestamp(System.currentTimeMillis()));
        if (updated) {
            sendNotification(msg.getSenderId(), "Quiz Retake Approved",
                    "Your teacher has approved your quiz retake request. You may now retake the quiz from Available Quizzes.");
        }
        return updated;
    }

    public boolean approveExamRetake(int messageId) {
        Optional<InboxMessage> opt = inboxRepository.findById(messageId);
        if (opt.isEmpty()) return false;
        InboxMessage msg = opt.get();
        if (msg.getTargetId() == null || msg.getSenderId() == null) return false;

        if (attemptRepository != null) {
            attemptRepository.deleteAttemptsForStudent(msg.getTargetId(), msg.getSenderId());
        }
        boolean updated = inboxRepository.updateStatus(messageId, InboxStatus.APPROVED, new Timestamp(System.currentTimeMillis()));
        if (updated) {
            sendNotification(msg.getSenderId(), "Exam Makeup Approved",
                    "Your teacher has approved your exam makeup request. You may now take the exam from Available Exams.");
        }
        return updated;
    }

    public boolean rejectRequest(int messageId, String responseNote) {
        Optional<InboxMessage> opt = inboxRepository.findById(messageId);
        if (opt.isEmpty()) return false;

        InboxMessage msg = opt.get();
        boolean updated = inboxRepository.updateStatus(messageId, InboxStatus.REJECTED, new Timestamp(System.currentTimeMillis()));

        if (updated && msg.getSenderId() != null) {
            String note = (responseNote != null && !responseNote.isBlank()) ? "\nNote: " + responseNote.trim() : "";
            sendNotification(msg.getSenderId(), "Request Rejected: " + msg.getTitle(),
                    "Your request was reviewed and rejected by the instructor/administrator." + note);
        }
        return updated;
    }

    public boolean updateStatus(int messageId, InboxStatus status) {
        return inboxRepository.updateStatus(messageId, status, new Timestamp(System.currentTimeMillis()));
    }
}