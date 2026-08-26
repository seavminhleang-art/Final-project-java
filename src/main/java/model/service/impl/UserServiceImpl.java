package model.service.impl;

import exception.NotFoundException;
import model.entity.User;
import model.entity.enums.Role;
import model.repository.UserRepo;
import model.service.UserService;

import db.PasswordUtils;

import java.util.List;

public class UserServiceImpl implements UserService {
    private final UserRepo userRepo;

    public UserServiceImpl(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @Override
    public User create(User user, String rawPassword) {
        userRepo.findByUsername(user.getUsername()).ifPresent(u -> {
            throw new IllegalArgumentException("Username already taken.");
        });
        String salt = PasswordUtils.generateSalt();
        user.setSalt(salt);
        user.setPasswordHash(PasswordUtils.hash(rawPassword, salt));
        user.setActive(true);
        return userRepo.create(user);
    }

    @Override
    public User update(User user) {
        get(user.getId());
        return userRepo.update(user);
    }

    @Override
    public void delete(Long id) {
        get(id);
        userRepo.delete(id);
    }

    @Override
    public User get(Long id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    @Override
    public List<User> listAll() {
        return userRepo.findAll();
    }

    @Override
    public List<User> search(String keyword, Role roleFilter) {
        return userRepo.search(keyword == null ? "" : keyword, roleFilter);
    }

    @Override
    public void resetPassword(Long userId, String newRawPassword) {
        get(userId);
        String salt = PasswordUtils.generateSalt();
        String hash = PasswordUtils.hash(newRawPassword, salt);
        userRepo.updatePassword(userId, hash, salt);
    }


}
