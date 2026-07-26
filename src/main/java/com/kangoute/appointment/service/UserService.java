package com.kangoute.appointment.service;

import com.kangoute.appointment.entity.User;

public interface UserService {

    User createUser(User user);

    User getUserByEmail(String email);

    User getUserById(Long id);
}
