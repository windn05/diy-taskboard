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

/** 로그인·로그아웃(세션)과 본인 비밀번호 변경 API */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SecurityContextRepository securityContextRepository;

    // 공개 회원가입 없음. 계정 생성은 POST /admin/users (관리자 전용)

    /*************************************************************************
     * 목적 : 아이디·비밀번호 확인 후 세션 발급 (연속 실패 시 일정 시간 잠금, 429)
     * 이유 : -
     * 파라미터
     * - request : 아이디·비밀번호
     * - httpRequest : 세션을 만들 HTTP 요청
     * - httpResponse : 세션 쿠키를 실을 HTTP 응답
     * 반환
     * - 200 + 로그인한 사용자 정보
     *************************************************************************/
    @PostMapping("/login")
    public ResponseEntity<SessionUser> login(@Valid @RequestBody LoginRequest request,
                                             HttpServletRequest httpRequest,
                                             HttpServletResponse httpResponse) {
        return ResponseEntity.ok(startSession(authService.login(request), httpRequest, httpResponse));
    }

    /*************************************************************************
     * 목적 : 계정 없이 둘러볼 수 있는 읽기 전용 게스트 세션 발급
     * 이유 : -
     * 파라미터
     * - httpRequest : 세션을 만들 HTTP 요청
     * - httpResponse : 세션 쿠키를 실을 HTTP 응답
     * 반환
     * - 200 + 게스트 사용자 정보
     *************************************************************************/
    @PostMapping("/guest")
    public ResponseEntity<SessionUser> guest(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        return ResponseEntity.ok(startSession(authService.guestLogin(), httpRequest, httpResponse));
    }

    /*************************************************************************
     * 목적 : 현재 세션의 사용자 정보 조회 (새로고침 시 로그인 상태 복원용)
     * 이유 : 세션 쿠키는 JS가 읽을 수 없어 프론트가 로그인 여부를 서버에 확인해야 함
     * 파라미터
     * - user : 로그인 사용자
     * 반환
     * - 200 + 로그인한 사용자 정보 (비로그인이면 401)
     *************************************************************************/
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

    /*************************************************************************
     * 목적 : 로그인한 본인의 비밀번호 변경 (게스트는 SecurityConfig에서 차단)
     * 이유 : -
     * 파라미터
     * - user : 로그인 사용자
     * - request : 현재·새 비밀번호
     * 반환
     * - 204 (본문 없음)
     *************************************************************************/
    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal CurrentUser user,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user.getId(), request);
        return ResponseEntity.noContent().build();
    }

    /*************************************************************************
     * 목적 : 로그인한 사용자 정보를 현재 요청과 HTTP 세션에 저장
     * 이유 : 세션에 저장해 두어야 이후 요청에서 로그인 상태가 자동 복원됨
     * 파라미터
     * - user : 로그인 처리된 사용자
     * - request : HTTP 요청
     * - response : HTTP 응답
     * 반환
     * - 응답용 사용자 정보
     *************************************************************************/
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
