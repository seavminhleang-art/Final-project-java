package com.proctor.model.entity;

import com.proctor.model.enums.InboxMessageType;
import com.proctor.model.enums.InboxStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InboxMessage {
    private Integer id;
    private Integer senderId;
    private String senderName;
    private Integer recipientId;
    private InboxMessageType type;
    private String title;
    private String body;
    private Integer targetId;
    private String proposedPasswordHash;
    @Builder.Default
    private InboxStatus status = InboxStatus.PENDING;
    private boolean read;
    private Timestamp createdAt;
    private Timestamp resolvedAt;

    public boolean isActionable() {
        return status == InboxStatus.PENDING &&
                (type == InboxMessageType.QUIZ_RETAKE ||
                 type == InboxMessageType.EXAM_RETAKE);
    }

    public String getEffectivePasswordHash() {
        if (proposedPasswordHash != null && !proposedPasswordHash.isBlank()) {
            return proposedPasswordHash;
        }
        if (body != null && body.contains("[HASH:")) {
            int start = body.indexOf("[HASH:") + 6;
            int end = body.indexOf("]", start);
            if (end > start) {
                return body.substring(start, end);
            }
        }
        return null;
    }
}