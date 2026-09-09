package com.proctor.model.entity;

import java.util.Optional;

public class Session {
    private static User currentUser;

    public static void setLoggedInUser(User user) {
        currentUser = user;
    }

    public static Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void clear() {
        currentUser = null;
    }
}