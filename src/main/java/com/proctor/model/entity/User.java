package com.proctor.model.entity;

import com.proctor.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Integer id;
    private String email;
    private String username;
    private String passwordHash;
    private String fullName;
    private LocalDate dateOfBirth;
    private String gender;
    private Role role;
    private boolean enabled;
    private String academicDegree;
    private String educationBackground;
    private String specialization;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public String getDisplayIdentifier() {
        if (username != null && !username.isBlank()) {
            return "@" + username;
        }
        return email != null ? email : "";
    }

    public String getDisplayNameWithHonorific() {
        if (role == Role.TEACHER) {
            return formatTeacherName(gender, fullName);
        }
        return fullName != null ? fullName : "";
    }

    public static String formatTeacherName(String gender, String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "Teacher";
        }
        String clean = fullName.trim();
        if (clean.startsWith("Mr. ") || clean.startsWith("Mrs. ") || clean.startsWith("Ms. ")
                || clean.startsWith("Miss ") || clean.startsWith("Dr. ") || clean.startsWith("Prof. ")) {
            return clean;
        }
        if ("Female".equalsIgnoreCase(gender)) {
            return "Mrs. " + clean;
        } else {
            return "Mr. " + clean;
        }
    }
}