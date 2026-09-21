package com.taskboard.security;

import com.taskboard.domain.User.SystemRole;
import com.taskboard.repository.UserRepository;
import com.taskboard.security.JwtService.TokenType;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/** 토큰 → CurrentUser 변환. REST 필터와 STOMP 인터셉터가 같은 규칙을 쓰도록 한곳에 모음 */
@Component
@RequiredArgsConstructor
public class TokenAuthenticator {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public Optional<CurrentUser> resolve(String token) {
        // API 인증에는 액세스 토큰만 허용
        if (token == null || !jwtService.isValid(token, TokenType.ACCESS)) return Optional.empty();
        Claims claims = jwtService.parseClaims(token);
        Long userId = claims.get("userId", Long.class);
        String role = claims.get("role", String.class);

        // 게스트는 DB에 저장하지 않으므로 토큰 클레임만으로 신원 구성
        if (SystemRole.GUEST.name().equals(role)) {
            return Optional.of(new CurrentUser(userId, claims.getSubject(), role));
        }
        // 역할은 토큰이 아닌 DB 값 사용 — 권한 변경·계정 삭제가 토큰 만료를 기다리지 않고 즉시 반영
        return userRepository.findById(userId)
                .map(user -> new CurrentUser(user.getId(), user.getUsername(), user.getRole().name()));
    }

    public Optional<CurrentUser> resolveBearer(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) return Optional.empty();
        return resolve(authorizationHeader.substring(7));
    }
}
