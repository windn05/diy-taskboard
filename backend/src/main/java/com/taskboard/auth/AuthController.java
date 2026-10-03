package com.taskboard.auth;

import com.taskboard.auth.AuthDtos.*;
import com.taskboard.global.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

/** 로그인·로그아웃(세션)과 본인 비밀번호 변경 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SecurityContextRepository securityContextRepository;

    // 공개 회원가입 없음. 계정 생성은 POST /admin/users (관리자 전용)

    /** 연속 실패 시 일정 시간 잠금 (LoginAttemptService, 429) */
    @PostMapping("/login")
    public ResponseEntity<SessionUser> login(@Valid @RequestBody LoginRequest request,
                                             HttpServletRequest httpRequest,
                                             HttpServletResponse httpResponse) {
        return ResponseEntity.ok(startSession(authService.login(request), httpRequest, httpResponse));
    }

    /** 계정 없이 둘러보기. 읽기 전용 GUEST 세션 발급 */
    @PostMapping("/guest")
    public ResponseEntity<SessionUser> guest(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return ResponseEntity.ok(startSession(authService.guestLogin(), httpRequest, httpResponse));
    }

    /** 새로고침 시 로그인 상태 복원용. 세션 쿠키는 JS가 못 읽으므로 프론트가 이 응답으로 신원을 파악한다 */
    @GetMapping("/me")
    public ResponseEntity<SessionUser> me(@AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.ok(toSessionUser(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    /** 로그인한 본인의 비밀번호 변경. 게스트는 SecurityConfig에서 차단 */
    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal CurrentUser user,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user.getId(), request);
        return ResponseEntity.noContent().build();
    }

    /** SecurityContext를 만들어 SecurityContextHolder와 세션 양쪽에 반영 — 이후 요청부턴 세션에서 자동 복원됨 */
    private SessionUser startSession(CurrentUser user, HttpServletRequest request, HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(user.toAuthentication());
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);
        return toSessionUser(user);
    }

    private SessionUser toSessionUser(CurrentUser user) {
        return new SessionUser(user.getId(), user.getUsername(), user.getRole());
    }
}
