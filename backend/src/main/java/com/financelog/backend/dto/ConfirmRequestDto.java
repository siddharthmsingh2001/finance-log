package com.financelog.backend.dto;

public record ConfirmRequestDto(
        String email,
        String code
) {
}
