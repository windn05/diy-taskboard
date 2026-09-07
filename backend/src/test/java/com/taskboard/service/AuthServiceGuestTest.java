package com.taskboard.service;

import com.taskboard.dto.AuthDtos.TokenResponse;
import com.taskboard.repository.UserRepository;
import com.taskboard.security.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

class AuthServiceGuestTest {

    private UserRepository userRepository;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        jwtService = new JwtService("test-only-secret-key-please-32bytes-minimum", 3600000, 604800000);
        authService = new AuthService(userRepository, mock(PasswordEncoder.class), jwtService);
    }

    @Test
    void 게스트_로그인은_DB에_사용자를_만들지_않는다() {
        authService.guestLogin();

        verifyNoInteractions(userRepository);
    }

    @Test
    void 게스트_토큰의_id는_음수라_실제_사용자와_겹치지_않는다() {
        Claims claims = jwtService.parseClaims(authService.guestLogin().accessToken());

        assertThat(claims.get("userId", Long.class)).isNegative();
        assertThat(claims.get("role", String.class)).isEqualTo("GUEST");
        assertThat(claims.getSubject()).startsWith("guest-");
    }

    @Test
    void 호출할_때마다_다른_신원이_발급된다() {
        TokenResponse first = authService.guestLogin();
        TokenResponse second = authService.guestLogin();

        Long firstId = jwtService.parseClaims(first.accessToken()).get("userId", Long.class);
        Long secondId = jwtService.parseClaims(second.accessToken()).get("userId", Long.class);

        assertThat(firstId).isNotEqualTo(secondId);
    }
}
