package com.caresync.service;

import com.caresync.dto.UserResponse;
import com.caresync.dto.UserUpdateRequest;

public interface UserService {

    UserResponse getUserById(Long id);

    UserResponse getUserById(Long id, String authenticatedEmail);

    UserResponse getUserByEmail(String email);

    UserResponse updateUser(String email, UserUpdateRequest updateRequest);

    void deleteUser(String email);

    boolean userExists(String email);
}
