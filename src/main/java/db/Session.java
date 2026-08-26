package db;

import model.entity.User;

public class Session {
    private static User currentUser;

    private Session() {
    }

    public static void login(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    public static User current() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}
