package model.repository;

import model.entity.User;
import model.entity.enums.Role;

import java.util.List;
import java.util.Optional;

public interface UserRepo {
    User create(User user);
    Optional<User> findById(Long id);
    Optional<User> findByUsername(String username);
    List<User> findAll();
    List<User> search(String keyword, Role roleFilter);
    User update(User user);
    boolean updatePassword(Long userId, String newHash, String newSalt);
    boolean delete(Long id);
}
