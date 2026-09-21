package com.taskboard.security;

import com.taskboard.domain.User;
import com.taskboard.domain.User.SystemRole;
import com.taskboard.repository.UserRepository;
import com.taskboard.security.JwtService.TokenType;
import com.taskboard.service.AuthService;
import com.taskboard.service.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** 액세스 토큰과 리프레시 토큰은 서로의 자리에 쓸 수 없어야 함 */
class TokenTypeTest {

    private static final String SECRET = "test-only-secret-key-please-32bytes-minimum";

    private JwtService jwtService;
    private TokenAuthenticator authenticator;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findById(1L)).thenReturn(Optional.of(
                User.builder().id(1L).username("user").password("x").name("사용자").role(SystemRole.USER).build()));
        jwtService = new JwtService(SECRET, 3600000, 604800000);
        authenticator = new TokenAuthenticator(jwtService, userRepository);
        authService = new AuthService(userRepository, mock(PasswordEncoder.class), jwtService, new LoginAttemptService());
    }

    @Test
    void 액세스_토큰으로는_API_인증이_된다() {
        String access = jwtService.generateAccessToken(1L, "user", "USER");

        assertThat(authenticator.resolve(access)).isPresent();
    }

    @Test
    void 리프레시_토큰으로는_API_인증이_안_된다() {
        String refresh = jwtService.generateRefreshToken(1L, "user", "USER");

        assertThat(authenticator.resolve(refresh)).isEmpty();
    }

    @Test
    void 액세스_토큰으로는_재발급이_안_된다() {
        String access = jwtService.generateAccessToken(1L, "user", "USER");

        assertThatThrownBy(() -> authService.refresh(access)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 리프레시_토큰으로는_재발급이_된다() {
        String refresh = jwtService.generateRefreshToken(1L, "user", "USER");

        assertThat(authService.refresh(refresh).accessToken()).isNotBlank();
    }

    @Test
    void 용도_클레임이_없는_예전_토큰은_거부된다() {
        String legacy = io.jsonwebtoken.Jwts.builder()
                .subject("user").claim("userId", 1L).claim("role", "USER")
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(SECRET.getBytes()))
                .compact();

        assertThat(jwtService.isValid(legacy, TokenType.ACCESS)).isFalse();
        assertThat(authenticator.resolve(legacy)).isEmpty();
    }
}
