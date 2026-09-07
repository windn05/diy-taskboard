package com.taskboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    static final int MIN_PASSWORD_LENGTH = 8;

    /** 계정 생성은 관리자만 한다. 공개 회원가입은 없앴다. */
    public record CreateUserRequest(
            @NotBlank String username,
            @NotBlank @Size(min = MIN_PASSWORD_LENGTH, message = "비밀번호는 " + MIN_PASSWORD_LENGTH + "자 이상이어야 합니다.")
            String password,
            @NotBlank String name,
            /** null이면 USER. GUEST는 저장하지 않는 신원이라 만들 수 없다. */
            String role) {
    }

    public record LoginRequest(
            @NotBlank String username,
            @NotBlank String password) {
    }

    public record ChangePasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = MIN_PASSWORD_LENGTH, message = "비밀번호는 " + MIN_PASSWORD_LENGTH + "자 이상이어야 합니다.")
            String newPassword) {
    }

    public record TokenResponse(String accessToken, String refreshToken) {
    }

    public record UserResponse(Long id, String username, String name, String role) {
    }
}
