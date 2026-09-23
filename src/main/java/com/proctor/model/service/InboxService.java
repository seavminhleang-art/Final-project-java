package com.proctor.model.service;

import com.proctor.model.entity.User;
import com.proctor.model.repository.UserRepository;
import com.proctor.model.enums.Role;
import com.proctor.exception.ValidationException;
import com.proctor.model.repository.AttemptRepository;
import com.proctor.model.entity.Attempt;
import com.proctor.model.entity.InboxMessage;
import com.proctor.model.entity.Quiz;
import com.proctor.model.entity.Result;
import com.proctor.model.enums.InboxMessageType;
import com.proctor.model.enums.InboxStatus;
import com.proctor.model.repository.InboxRepository;
import com.proctor.model.repository.QuizRepository;
import com.proctor.model.repository.ResultRepository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class InboxService {
    public static final long THREE_DAYS_MILLIS = 3L * 24 * 60 * 60 * 1000;

    private final InboxRepository inboxRepository;
    private final UserRepository userRepository;
    private final AttemptRepository attemptRepository;
    private final ResultRepository resultRepository;
    private final QuizRepository quizRepository;
    private final EmailService emailService;

    public InboxService(InboxRepository inboxRepository, UserRepository userRepository) {
        this(inboxRepository, userRepository, new AttemptRepository(), new ResultRepository(), new QuizRepository(), new EmailService());
    }

    public InboxService(InboxRepository inboxRepository, UserRepository userRepository, AttemptRepository attemptRepository) {
        this(inboxRepository, userRepository, attemptRepository, new ResultRepository(), new QuizRepository(), new EmailService());
    }

    public InboxService(InboxRepository inboxRepository, UserRepository userRepository, AttemptRepository attemptRepository, ResultRepository resultRepository) {
        this(inboxRepository, userRepository, attemptRepository, resultRepository, new QuizRepository(), new EmailService());
    }

    public InboxService(InboxRepository inboxRepository, UserRepository userRepository, AttemptRepository attemptRepository, ResultRepository resultRepository, QuizRepository quizRepository) {
        this(inboxRepository, userRepository, attemptRepository, resultRepository, quizRepository, new EmailService());
    }

    public InboxService(InboxRepository inboxRepository, UserRepository userRepository, AttemptRepository attemptRepository, ResultRepository resultRepository, QuizRepository quizRepository, EmailService emailService) {
        this.inboxRepository = inboxRepository;
        this.userRepository = userRepository;
        this.attemptRepository = attemptRepository;
        this.resultRepository = resultRepository;
        this.quizRepository = quizRepository;
        this.emailService = emailService != null ? emailService : new EmailService();
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
        if (title == null || title.trim().isBlank()) {
            throw new ValidationException("Notification title cannot be blank.");
        }
        String safeTitle = title.trim();
        if (safeTitle.length() > 200) {
            safeTitle = safeTitle.substring(0, 197) + "...";
        }
        InboxMessage msg = InboxMessage.builder()
                .recipientId(recipientId)
                .type(InboxMessageType.NOTIFICATION)
                .title(safeTitle)
                .body(body != null ? body : "")
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

        if (attemptRepository != null && resultRepository != null) {
            Optional<Attempt> attOpt = attemptRepository.findLatestAttempt(quizId, studentId);
            if (attOpt.isPresent()) {
                Optional<Result> resOpt = resultRepository.findByAttemptId(attOpt.get().getId());
                if (resOpt.isPresent() && resOpt.get().isPassed()) {
                    throw new ValidationException("Retakes cannot be requested for assessments that have been passed.");
                }
            }
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

        if (attemptRepository != null && resultRepository != null) {
            Optional<Attempt> attOpt = attemptRepository.findLatestAttempt(quizId, studentId);
            if (attOpt.isPresent()) {
                Optional<Result> resOpt = resultRepository.findByAttemptId(attOpt.get().getId());
                if (resOpt.isPresent() && resOpt.get().isPassed()) {
                    throw new ValidationException("Retakes cannot be requested for assessments that have been passed.");
                }
            }
        }

        if (reason == null || reason.trim().isBlank()) {
            throw new ValidationException("A reason is required for exam makeup requests.");
        }
        if (reason.trim().length() > 500) {
            throw new ValidationException("Reason must not exceed 500 characters.");
        }

        if (examTimestamp != null) {
            if (System.currentTimeMillis() - examTimestamp.getTime() > THREE_DAYS_MILLIS) {
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
                .body(String.format("Student %s %s and requested a makeup for exam \"%s\".\n\nStudent's Reason:\n\"%s\"",
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

    public boolean approveQuizRetake(int messageId) {
        Optional<InboxMessage> opt = inboxRepository.findById(messageId);
        if (opt.isEmpty()) return false;
        InboxMessage msg = opt.get();
        if (msg.getStatus() != InboxStatus.PENDING) {
            throw new ValidationException("This request has already been processed (status: " + msg.getStatus() + ").");
        }
        if (msg.getTargetId() == null || msg.getSenderId() == null) return false;
        if (quizRepository != null && quizRepository.findById(msg.getTargetId()).isEmpty()) {
            throw new ValidationException("The assessment associated with this request has been deleted and cannot be retaken.");
        }

        if (attemptRepository != null) {
            attemptRepository.deleteAttemptsForStudent(msg.getTargetId(), msg.getSenderId());
        }
        boolean updated = inboxRepository.updateStatus(messageId, InboxStatus.APPROVED, new Timestamp(System.currentTimeMillis()));
        if (updated) {
            sendNotification(msg.getSenderId(), "Quiz Retake Approved",
                    "Your teacher has approved your quiz retake request. You may now retake the quiz from Quizzes.");
            if (emailService != null) {
                userRepository.findById(msg.getSenderId()).ifPresent(student -> {
                    String assessmentTitle = "Quiz";
                    if (quizRepository != null && msg.getTargetId() != null) {
                        assessmentTitle = quizRepository.findById(msg.getTargetId())
                                .map(Quiz::getTitle)
                                .orElseGet(() -> msg.getTitle() != null ? msg.getTitle().replaceFirst("^Quiz Retake Request:\\s*", "") : "Quiz");
                    } else if (msg.getTitle() != null) {
                        assessmentTitle = msg.getTitle().replaceFirst("^Quiz Retake Request:\\s*", "");
                    }
                    emailService.sendRetakeDecision(student.getEmail(), student.getFullName(), assessmentTitle, true, null);
                });
            }
        }
        return updated;
    }

    public boolean approveExamRetake(int messageId) {
        Optional<InboxMessage> opt = inboxRepository.findById(messageId);
        if (opt.isEmpty()) return false;
        InboxMessage msg = opt.get();
        if (msg.getStatus() != InboxStatus.PENDING) {
            throw new ValidationException("This request has already been processed (status: " + msg.getStatus() + ").");
        }
        if (msg.getTargetId() == null || msg.getSenderId() == null) return false;
        if (quizRepository != null && quizRepository.findById(msg.getTargetId()).isEmpty()) {
            throw new ValidationException("The assessment associated with this request has been deleted and cannot be retaken.");
        }

        if (attemptRepository != null) {
            attemptRepository.deleteAttemptsForStudent(msg.getTargetId(), msg.getSenderId());
        }
        boolean updated = inboxRepository.updateStatus(messageId, InboxStatus.APPROVED, new Timestamp(System.currentTimeMillis()));
        if (updated) {
            sendNotification(msg.getSenderId(), "Exam Makeup Approved",
                    "Your teacher has approved your exam makeup request. You may now take the exam from Exams.");
            if (emailService != null) {
                userRepository.findById(msg.getSenderId()).ifPresent(student -> {
                    String assessmentTitle = "Exam";
                    if (quizRepository != null && msg.getTargetId() != null) {
                        assessmentTitle = quizRepository.findById(msg.getTargetId())
                                .map(Quiz::getTitle)
                                .orElseGet(() -> msg.getTitle() != null ? msg.getTitle().replaceFirst("^Exam Makeup Request:\\s*", "") : "Exam");
                    } else if (msg.getTitle() != null) {
                        assessmentTitle = msg.getTitle().replaceFirst("^Exam Makeup Request:\\s*", "");
                    }
                    emailService.sendRetakeDecision(student.getEmail(), student.getFullName(), assessmentTitle, true, null);
                });
            }
        }
        return updated;
    }

    public boolean rejectRequest(int messageId, String responseNote) {
        Optional<InboxMessage> opt = inboxRepository.findById(messageId);
        if (opt.isEmpty()) return false;

        InboxMessage msg = opt.get();
        if (msg.getStatus() != InboxStatus.PENDING) {
            throw new ValidationException("This request has already been processed (status: " + msg.getStatus() + ").");
        }
        if (responseNote != null && responseNote.trim().length() > 300) {
            throw new ValidationException("Rejection note must not exceed 300 characters.");
        }
        boolean updated = inboxRepository.updateStatus(messageId, InboxStatus.REJECTED, new Timestamp(System.currentTimeMillis()));

        if (updated && msg.getSenderId() != null) {
            String note = (responseNote != null && !responseNote.isBlank()) ? "\nNote: " + responseNote.trim() : "";
            sendNotification(msg.getSenderId(), "Request Rejected: " + msg.getTitle(),
                    "Your request was reviewed and rejected by the instructor." + note);
            if (emailService != null && (msg.getType() == InboxMessageType.QUIZ_RETAKE || msg.getType() == InboxMessageType.EXAM_RETAKE)) {
                userRepository.findById(msg.getSenderId()).ifPresent(student -> {
                    String assessmentTitle = "Assessment";
                    if (quizRepository != null && msg.getTargetId() != null) {
                        assessmentTitle = quizRepository.findById(msg.getTargetId())
                                .map(Quiz::getTitle)
                                .orElseGet(() -> msg.getTitle() != null 
                                        ? msg.getTitle().replaceFirst("^(Quiz Retake Request|Exam Makeup Request):\\s*", "") 
                                        : "Assessment");
                    } else if (msg.getTitle() != null) {
                        assessmentTitle = msg.getTitle().replaceFirst("^(Quiz Retake Request|Exam Makeup Request):\\s*", "");
                    }
                    emailService.sendRetakeDecision(student.getEmail(), student.getFullName(), assessmentTitle, false, responseNote);
                });
            }
        }
        return updated;
    }

    public boolean updateStatus(int messageId, InboxStatus status) {
        return inboxRepository.updateStatus(messageId, status, new Timestamp(System.currentTimeMillis()));
    }
}