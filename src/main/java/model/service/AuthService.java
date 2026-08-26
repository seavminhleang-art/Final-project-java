package model.service;

import model.entity.User;
import model.entity.enums.Role;

public interface AuthService {
    User login(String username, String password);

    User register(String username, String rawPassword, String fullName, String email, Role role);

    void resetPassword(Long userId, String newRawPassword);
    void requireRole(User user, Role... allowed);
}
