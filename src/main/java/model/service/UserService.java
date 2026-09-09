package model.service;


import model.entity.User;
import model.entity.enums.Role;

import java.util.List;



public interface UserService {

    User update(User user);

    User setActive(Long id, boolean active);
    User get(Long id);
    List<User> listAll();
    List<User> search(String keyword, Role roleFilter);
    void resetPassword(Long userId, String newRawPassword);


    void disable(Long id);

    void enable(Long id);
}
