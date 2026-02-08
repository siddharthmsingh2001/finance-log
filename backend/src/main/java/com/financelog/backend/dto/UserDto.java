package com.financelog.backend.dto;

import com.financelog.backend.entity.AuthenticatedUser;
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
    public static UserDto from(User user, AuthenticatedUser auth) {
        return new UserDto(
                user.getId(),
                user.getEmail(),
                auth.getGivenName() + " " + auth.getFamilyName(),
                user.getProfileImageUrl(),
                user.getDefaultCurrency(),
                user.getFirstDayOfWeek()
        );
    }
}
