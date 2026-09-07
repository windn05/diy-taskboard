package com.taskboard.service;

import com.taskboard.domain.User;
import com.taskboard.domain.User.SystemRole;
import com.taskboard.dto.AuthDtos.ChangePasswordRequest;
import com.taskboard.dto.AuthDtos.CreateUserRequest;
import com.taskboard.dto.AuthDtos.LoginRequest;
import com.taskboard.exception.TooManyAttemptsException;
import com.taskboard.repository.UserRepository;
import com.taskboard.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceSecurityTest {

    private static final String PASSWORD = "correct-horse";

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;
    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        JwtService jwtService = new JwtService("test-only-secret-key-please-32bytes-minimum", 3600000, 604800000);
        authService = new AuthService(userRepository, passwordEncoder, jwtService, new LoginAttemptService());

        user = User.builder()
                .id(1L)
                .username("admin")
                .password(passwordEncoder.encode(PASSWORD))
                .name("관리자")
                .role(SystemRole.ADMIN)
                .build();
    }

    @Test
    void 비밀번호를_반복해서_틀리면_옳은_비밀번호도_막힌다() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", PASSWORD)))
                .isInstanceOf(TooManyAttemptsException.class);
    }

    /** 없는 아이디와 틀린 비밀번호를 다르게 알려주면 계정 존재 여부가 새어 나간다. */
    @Test
    void 없는_아이디와_틀린_비밀번호는_같은_메시지를_준다() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(userRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        String wrongPassword = catchMessage(() -> authService.login(new LoginRequest("admin", "wrong")));
        String unknownUser = catchMessage(() -> authService.login(new LoginRequest("nobody", "wrong")));

        assertThat(wrongPassword).isEqualTo(unknownUser);
    }

    @Test
    void 비밀번호_변경은_현재_비밀번호가_맞아야_한다() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.changePassword(1L, new ChangePasswordRequest("wrong", "new-password")))
                .isInstanceOf(IllegalArgumentException.class);

        authService.changePassword(1L, new ChangePasswordRequest(PASSWORD, "new-password"));

        assertThat(passwordEncoder.matches("new-password", user.getPassword())).isTrue();
    }

    @Test
    void 게스트는_계정으로_만들_수_없다() {
        when(userRepository.existsByUsername(any())).thenReturn(false);

        assertThatThrownBy(() -> authService.createUser(
                new CreateUserRequest("someone", "password1", "누군가", "GUEST")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 역할을_지정하지_않으면_USER로_만든다() {
        when(userRepository.existsByUsername(any())).thenReturn(false);

        assertThat(authService.createUser(new CreateUserRequest("someone", "password1", "누군가", null)).role())
                .isEqualTo("USER");
    }

    private String catchMessage(Runnable action) {
        try {
            action.run();
            throw new AssertionError("예외가 발생해야 한다");
        } catch (IllegalArgumentException e) {
            return e.getMessage();
        }
    }
}
