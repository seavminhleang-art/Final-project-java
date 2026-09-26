package com.proctor.model.entity;

import java.util.Optional;

public class Session {
    private static volatile User currentUser;

    public static synchronized void setLoggedInUser(User user) {
        currentUser = user;
    }

    public static synchronized Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public static synchronized void clear() {
        currentUser = null;
    }
}
