package model.entity.enums;

public enum AnnouncementType {
    /** System-generated "you failed, please re-exam" style notice. */
    FAIL_RETRY,
    /** General informational notice posted by a teacher/admin. */
    INFO,
    /** Platform-wide system notice. */
    SYSTEM
}