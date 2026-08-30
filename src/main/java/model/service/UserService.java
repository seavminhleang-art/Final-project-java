package model.service;


import model.entity.User;
import model.entity.enums.Role;

import java.util.List;



public interface UserService {
    User create(User user, String rawPassword);
    User update(User user);
    void delete(Long id);
    User get(Long id);
    List<User> listAll();
    List<User> search(String keyword, Role roleFilter);
    void resetPassword(Long userId, String newRawPassword);

}
