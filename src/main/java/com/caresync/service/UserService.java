package com.caresync.service;

import com.caresync.dto.UserResponse;

public interface UserService {

    UserResponse getUserById(Long id);

    UserResponse getUserByEmail(String email);

    UserResponse updateUser(String email, UserResponse userResponse);

    void deleteUser(String email);

    boolean userExists(String email);
}
