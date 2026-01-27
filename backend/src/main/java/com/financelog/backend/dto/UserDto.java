package com.financelog.backend.dto;

import com.financelog.backend.entity.User;

import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String fullName,
        String profileImageUrl,
        String defaultCurrency,
        int firstDayOfWeek
){
    public static UserDto from(User user, String fullName){
        return new UserDto(
                user.getId(),
                user.getEmail(),
                fullName,
                user.getProfileImageUrl(),
                user.getDefaultCurrency(),
                user.getFirstDayOfWeek()
        );
    }
}
