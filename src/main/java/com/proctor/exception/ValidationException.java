package com.proctor.exception;

import java.io.Serial;

// Runtime exception representing validation failures in user inputs and business rules
public class ValidationException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public ValidationException(String message) {
        super(message);
    }

    public ValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
