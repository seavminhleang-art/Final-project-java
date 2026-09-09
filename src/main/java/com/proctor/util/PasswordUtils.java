package com.proctor.util;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.proctor.exception.ValidationException;

public class PasswordUtils {
    public static String hash(String rawPassword) {
        return BCrypt.withDefaults().hashToString(10, rawPassword.toCharArray());
    }

    public static boolean verify(String rawPassword, String hashedPassword) {
        if (rawPassword == null || hashedPassword == null) {
            return false;
        }
        return BCrypt.verifyer().verify(rawPassword.toCharArray(), hashedPassword.toCharArray()).verified;
    }

    public static void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new ValidationException("Password is required.");
        }
        boolean hasSpace = false;
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isWhitespace(c)) hasSpace = true;
            else if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        if (hasSpace) {
            throw new ValidationException("Password must not contain spaces.");
        }
        if (password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters long.");
        }
        if (!hasUpper) {
            throw new ValidationException("Password must contain at least one uppercase letter.");
        }
        if (!hasLower) {
            throw new ValidationException("Password must contain at least one lowercase letter.");
        }
        if (!hasDigit) {
            throw new ValidationException("Password must contain at least one number.");
        }
    }
}