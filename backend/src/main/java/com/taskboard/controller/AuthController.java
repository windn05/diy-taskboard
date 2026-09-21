package com.taskboard.controller;

import com.taskboard.dto.AuthDtos.*;
import com.taskboard.security.CurrentUser;
import com.taskboard.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** 로그인·토큰 발급과 본인 비밀번호 변경 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 공개 회원가입 없음. 계정 생성은 POST /admin/users (관리자 전용)

    /** 연속 실패 시 일정 시간 잠금 (LoginAttemptService, 429) */
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** 계정 없이 둘러보기. 읽기 전용 GUEST 토큰 발급 */
    @PostMapping("/guest")
    public TokenResponse guest() {
        return authService.guestLogin();
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@RequestBody String refreshToken) {
        return authService.refresh(refreshToken);
    }

    /** 로그인한 본인의 비밀번호 변경. 게스트는 SecurityConfig에서 차단 */
    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal CurrentUser user,
                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user.getId(), request);
    }
}
