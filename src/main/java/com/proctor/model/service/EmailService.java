package com.proctor.model.service;

import com.proctor.config.Config;
import com.proctor.exception.ValidationException;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class EmailService {

    public record EmailRecord(String toAddress, String subject, String bodyText) {}

    public static Consumer<EmailRecord> testEmailInterceptor = null;

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "proctor-mailer");
        t.setDaemon(true);
        return t;
    });

    private final String host;
    private final int port;
    private final boolean auth;
    private final boolean starttls;
    private final String username;
    private final String password;
    private final String fromAddress;

    public EmailService() {
        this(
                Config.get("mail.smtp.host", "smtp.gmail.com"),
                Config.getInt("mail.smtp.port", 587),
                Boolean.parseBoolean(Config.get("mail.smtp.auth", "true")),
                Boolean.parseBoolean(Config.get("mail.smtp.starttls.enable", "true")),
                Config.get("mail.smtp.username", ""),
                Config.get("mail.smtp.password", ""),
                Config.get("mail.from", "Proctor System <noreply@proctor.edu>")
        );
    }

    public EmailService(String host, int port, boolean auth, boolean starttls, String username, String password, String fromAddress) {
        this.host = host;
        this.port = port;
        this.auth = auth;
        this.starttls = starttls;
        this.username = username;
        this.password = password != null ? password.replace(" ", "") : "";
        this.fromAddress = fromAddress;
    }

    public void sendEmail(String toAddress, String subject, String bodyText) {
        if (toAddress == null || toAddress.isBlank()) {
            throw new ValidationException("Recipient email address cannot be empty.");
        }

        if (testEmailInterceptor != null) {
            testEmailInterceptor.accept(new EmailRecord(toAddress, subject, bodyText));
            return;
        }

        String lower = toAddress.toLowerCase().trim();
        if (lower.endsWith("@proctor.edu") || lower.endsWith("@example.com") || lower.endsWith("@test.edu") || lower.endsWith("@test.com") || lower.endsWith(".invalid") || lower.endsWith(".local")) {
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.auth", String.valueOf(auth));
        props.put("mail.smtp.starttls.enable", String.valueOf(starttls));
        props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
        props.put("mail.smtp.connectiontimeout", "8000");
        props.put("mail.smtp.timeout", "8000");

        Session session;
        if (auth) {
            session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
        } else {
            session = Session.getInstance(props);
        }

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(fromAddress));
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(toAddress.trim()));
            msg.setSubject(subject);
            msg.setText(bodyText);
            Transport.send(msg);
        } catch (MessagingException e) {
            System.err.println("[PROCTOR EMAIL ERROR] Failed to send email to " + toAddress + ": " + e.getMessage());
            throw new ValidationException("Failed to send email to " + toAddress + ": " + e.getMessage(), e);
        }
    }

    public CompletableFuture<Void> sendEmailAsync(String toAddress, String subject, String bodyText) {
        return CompletableFuture.runAsync(() -> sendEmail(toAddress, subject, bodyText), EXECUTOR);
    }

    public void sendRegistrationCode(String toEmail, String fullName, String code) {
        String displayName = (fullName != null && !fullName.isBlank()) ? fullName.trim() : "Future User";
        String subject = "Proctor - Confirm Your Email Registration";
        String body = String.format(
                "Hello %s,\n\n" +
                "Thank you for signing up for Proctor Automated Examination System.\n" +
                "Your 6-digit verification code is:\n\n" +
                "    %s\n\n" +
                "This code is valid for 10 minutes.\n" +
                "Please enter this code on the screen to verify your email and activate your account.\n\n" +
                "If you did not request this registration, you can safely disregard this email.\n\n" +
                "— Proctor Examination System",
                displayName, code
        );
        sendEmail(toEmail, subject, body);
    }

    public void sendPasswordResetCode(String toEmail, String fullName, String code) {
        String displayName = (fullName != null && !fullName.isBlank()) ? fullName.trim() : "User";
        String subject = "Proctor - Password Reset Verification Code";
        String body = String.format(
                "Hello %s,\n\n" +
                "We received a request to reset the password for your Proctor account.\n" +
                "Your 6-digit verification code is:\n\n" +
                "    %s\n\n" +
                "This code is valid for 10 minutes.\n" +
                "Please enter this code along with your new password to complete the reset.\n\n" +
                "If you did not request this password reset, please contact your administrator immediately.\n\n" +
                "— Proctor Examination System",
                displayName, code
        );
        sendEmail(toEmail, subject, body);
    }

    public CompletableFuture<Void> sendRetakeDecision(String toEmail, String studentName, String assessmentTitle, boolean approved, String note) {
        String displayName = (studentName != null && !studentName.isBlank()) ? studentName.trim() : "Student";
        String statusText = approved ? "APPROVED" : "REJECTED";
        String subject = "Proctor - Retake Request " + statusText + ": " + assessmentTitle;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Hello %s,\n\n", displayName));
        if (approved) {
            sb.append(String.format("Your instructor has APPROVED your retake request for \"%s\".\n", assessmentTitle));
            sb.append("You may now log in to the Proctor terminal and take your assessment.\n");
        } else {
            sb.append(String.format("Your instructor has REJECTED your retake request for \"%s\".\n", assessmentTitle));
        }

        if (note != null && !note.isBlank()) {
            sb.append(String.format("\nInstructor's Note:\n%s\n", note.trim()));
        }

        sb.append("\n— Proctor Examination System\n");
        return sendEmailAsync(toEmail, subject, sb.toString());
    }

    public String getUsername() {
        return username;
    }
}
