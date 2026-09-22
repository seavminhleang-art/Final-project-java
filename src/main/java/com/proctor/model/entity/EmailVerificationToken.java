package com.proctor.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailVerificationToken {
    private Integer id;
    private String email;
    private String code;
    private String purpose;
    private Timestamp expiresAt;
    @Builder.Default
    private boolean used = false;
    private Timestamp createdAt;

    public boolean isExpired() {
        return expiresAt != null && expiresAt.before(new Timestamp(System.currentTimeMillis()));
    }

    public boolean isValid() {
        return !used && !isExpired();
    }
}
