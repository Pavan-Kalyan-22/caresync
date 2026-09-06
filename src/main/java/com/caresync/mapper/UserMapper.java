package com.caresync.mapper;

import com.caresync.entity.User;
import com.caresync.dto.UserResponse;
import com.caresync.dto.UserUpdateRequest;
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

    public void updateUserFromDto(UserUpdateRequest dto, User user) {
        if (dto == null || user == null) {
            return;
        }
        if (dto.getFullName() != null) {
            user.setFullName(dto.getFullName());
        }
        if (dto.getDateOfBirth() != null) {
            user.setDateOfBirth(dto.getDateOfBirth());
        }
        if (dto.getGender() != null) {
            user.setGender(User.Gender.valueOf(dto.getGender().toUpperCase()));
        }
        if (dto.getHeight() != null) {
            user.setHeight(dto.getHeight());
        }
        if (dto.getWeight() != null) {
            user.setWeight(dto.getWeight());
        }
        if (dto.getCountry() != null) {
            user.setCountry(dto.getCountry());
        }
        if (dto.getOccupation() != null) {
            user.setOccupation(dto.getOccupation());
        }
        if (dto.getPhoneNumber() != null) {
            user.setPhoneNumber(dto.getPhoneNumber());
        }
        if (dto.getProfileImageUrl() != null) {
            user.setProfileImageUrl(dto.getProfileImageUrl());
        }
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
