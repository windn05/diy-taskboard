package com.taskboard.controller;

import com.taskboard.dto.AuthDtos.*;
import com.taskboard.security.CurrentUser;
import com.taskboard.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 공개 회원가입은 없다. 계정 생성은 POST /admin/users (관리자 전용).

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/guest")
    public TokenResponse guest() {
        return authService.guestLogin();
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@RequestBody String refreshToken) {
        return authService.refresh(refreshToken);
    }

    /** 로그인한 본인만 자기 비밀번호를 바꾼다. 게스트는 SecurityConfig에서 걸러진다. */
    @PostMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal CurrentUser user,
                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user.getId(), request);
    }
}
