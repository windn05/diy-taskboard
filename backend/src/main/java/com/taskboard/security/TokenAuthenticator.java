package com.taskboard.security;

import com.taskboard.domain.User.SystemRole;
import com.taskboard.repository.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TokenAuthenticator {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public Optional<CurrentUser> resolve(String token) {
        if (token == null || !jwtService.isValid(token)) return Optional.empty();
        Claims claims = jwtService.parseClaims(token);
        Long userId = claims.get("userId", Long.class);
        String role = claims.get("role", String.class);

        // 게스트는 DB에 저장하지 않으므로 토큰 클레임만으로 신원을 구성한다.
        if (SystemRole.GUEST.name().equals(role)) {
            return Optional.of(new CurrentUser(userId, claims.getSubject(), role));
        }
        return userRepository.findById(userId)
                .map(user -> new CurrentUser(user.getId(), user.getUsername(), user.getRole().name()));
    }

    public Optional<CurrentUser> resolveBearer(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) return Optional.empty();
        return resolve(authorizationHeader.substring(7));
    }
}
