package com.financelog.backend.dto;

import com.financelog.backend.entity.User;

import java.util.UUID;

public record UserDto(
        UUID id,
        String email,
        String profileImageUrl,
        String defaultCurrency,
        int firstDayOfWeek
){
    public static UserDto from(User user){
        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getProfileImageUrl(),
                user.getDefaultCurrency(),
                user.getFirstDayOfWeek()
        );
    }
}
