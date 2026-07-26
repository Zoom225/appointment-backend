package com.kangoute.appointment.service;

import com.kangoute.appointment.entity.User;

public interface UserService {

    User createUser(User user);

    User getUserByEmail(String email);

    User getUserById(Long id);

    java.util.List<User> getAllUsers();

    User updateUser(Long id, User user);

    void deleteUser(Long id);
}
