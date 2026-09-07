package com.taskboard.dto;

import jakarta.validation.constraints.NotBlank;

public class AuthDtos {

    public record SignupRequest(
            @NotBlank String username,
            @NotBlank String password,
            @NotBlank String name) {
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password) {
    }

    public record TokenResponse(String accessToken, String refreshToken) {
    }

    public record UserResponse(Long id, String username, String name, String role) {
    }
}
