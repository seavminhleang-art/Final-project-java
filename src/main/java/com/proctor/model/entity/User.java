package com.proctor.model.entity;

import com.proctor.model.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

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
    private Role role;
    private boolean enabled;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public String getDisplayIdentifier() {
        if (username != null && !username.isBlank()) {
            return "@" + username;
        }
        return email != null ? email : "";
    }
}