package com.caresync.util;

import com.caresync.entity.User;
import com.caresync.dto.UserResponse;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        if (user == null) {
            return null;
        }
        
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .dateOfBirth(user.getDateOfBirth())
                .age(user.getAge())
                .gender(user.getGender() != null ? user.getGender().toString() : null)
                .height(user.getHeight())
                .weight(user.getWeight())
                .country(user.getCountry())
                .occupation(user.getOccupation())
                .phoneNumber(user.getPhoneNumber())
                .profileImageUrl(user.getProfileImageUrl())
                .isEmailVerified(user.getIsEmailVerified())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public User toUser(User user, UserResponse response) {
        if (response == null) {
            return user;
        }
        
        user.setFullName(response.getFullName());
        user.setDateOfBirth(response.getDateOfBirth());
        user.setAge(response.getAge());
        user.setHeight(response.getHeight());
        user.setWeight(response.getWeight());
        user.setCountry(response.getCountry());
        user.setOccupation(response.getOccupation());
        user.setPhoneNumber(response.getPhoneNumber());
        user.setProfileImageUrl(response.getProfileImageUrl());
        
        return user;
    }
}
