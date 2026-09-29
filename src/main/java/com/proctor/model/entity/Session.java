package com.proctor.model.entity;

import java.util.Optional;

public class Session {
    private static volatile User currentUser;

    // Store currently authenticated user in thread-safe memory
    public static synchronized void setLoggedInUser(User user) {
        currentUser = user;
    }

    // Retrieve active logged-in user if present
    public static synchronized Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    // Clear active user session on logout
    public static synchronized void clear() {
        currentUser = null;
    }
}
