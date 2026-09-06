package com.caresync.service.impl;

import com.caresync.dto.UserResponse;
import com.caresync.dto.UserUpdateRequest;
import com.caresync.entity.User;
import com.caresync.exception.ResourceNotFoundException;
import com.caresync.mapper.UserMapper;
import com.caresync.repository.UserRepository;
import com.caresync.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;

@Service
@Slf4j
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.info("Fetching user with id: {}", id);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id, String authenticatedEmail) {
        log.info("Fetching user with id: {} by authenticated user: {}", id, authenticatedEmail);
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (authenticatedEmail == null || !user.getEmail().equalsIgnoreCase(authenticatedEmail)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Access denied: You are not authorized to view another user's profile");
        }
        return userMapper.toUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByEmail(String email) {
        log.info("Fetching user with email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        return userMapper.toUserResponse(user);
    }

    @Override
    public UserResponse updateUser(String email, UserUpdateRequest updateRequest) {
        log.info("Updating user with email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        userMapper.updateUserFromDto(updateRequest, user);

        // DOB is the single source of truth: recalculate age if dateOfBirth was provided
        if (updateRequest.getDateOfBirth() != null) {
            user.setAge(Period.between(user.getDateOfBirth(), LocalDate.now()).getYears());
        }

        User updatedUser = userRepository.save(user);
        
        log.info("User updated successfully: {}", email);
        return userMapper.toUserResponse(updatedUser);
    }

    @Override
    public void deleteUser(String email) {
        log.info("Deleting user with email: {}", email);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
        
        // Soft delete by marking inactive
        user.setIsActive(false);
        userRepository.save(user);
        
        log.info("User deleted successfully: {}", email);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean userExists(String email) {
        return userRepository.existsByEmail(email);
    }
}
