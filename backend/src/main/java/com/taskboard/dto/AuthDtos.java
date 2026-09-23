package com.taskboard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 인증·계정 요청/응답 */
public class AuthDtos {

    static final int MIN_PASSWORD_LENGTH = 8;

    /** 계정 생성은 관리자 전용. 공개 회원가입 없음 */
    public record CreateUserRequest(
            @NotBlank String username,
            @NotBlank @Size(min = MIN_PASSWORD_LENGTH, message = "비밀번호는 " + MIN_PASSWORD_LENGTH + "자 이상이어야 합니다.")
            String password,
            @NotBlank String name,
            /** null이면 USER. GUEST는 저장하지 않는 신원이라 생성 불가 */
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

    public record UserResponse(Long id, String username, String name, String role) {
    }

    /**
     * 로그인 성공·세션 조회 응답. 토큰 시절엔 프론트가 JWT를 직접 디코드해 이 정보를 얻었지만
     * 세션 쿠키는 JS가 못 읽으므로 서버가 이 형태로 내려준다({@code GET /auth/me}도 동일)
     */
    public record SessionUser(Long userId, String username, String role) {
    }
}
