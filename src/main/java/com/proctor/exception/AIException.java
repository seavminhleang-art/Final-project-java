package com.proctor.exception;

import java.io.Serial;

// Runtime exception representing AI generation or grading failures
public class AIException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public AIException(String message) {
        super(message);
    }

    public AIException(String message, Throwable cause) {
        super(message, cause);
    }
}
