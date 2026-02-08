package com.financelog.backend.dto;

public record LoginRequestDto(
        String email,
        String password
) {
}
