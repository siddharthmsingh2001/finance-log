package com.financelog.backend.dto;

public record SignUpRequestDto(
        String email,
        String password,
        String givenName,
        String familyName,
        String profileImageUrl
) {
}
