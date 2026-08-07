package com.kangoute.appointment.service;

import com.kangoute.appointment.entity.User;
import com.kangoute.appointment.enums.RoleName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    User createUser(User user);

    User getUserByEmail(String email);

    User getUserById(Long id);

    Page<User> getAllUsers(Pageable pageable, String query, RoleName role);

    java.util.List<User> getAllUsers();

    User updateUser(Long id, User user);

    void deleteUser(Long id);
}
