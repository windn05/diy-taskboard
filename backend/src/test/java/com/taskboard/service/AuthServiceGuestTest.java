package com.taskboard.service;

import com.taskboard.repository.UserRepository;
import com.taskboard.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AuthServiceGuestTest {

    private UserRepository userRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        authService = new AuthService(userRepository, mock(PasswordEncoder.class), new LoginAttemptService());
    }

    @Test
    void 게스트_로그인은_DB에_사용자를_만들지_않는다() {
        authService.guestLogin();

        verifyNoInteractions(userRepository);
    }

    @Test
    void 게스트_신원의_id는_음수라_실제_사용자와_겹치지_않는다() {
        CurrentUser guest = authService.guestLogin();

        assertThat(guest.getId()).isNegative();
        assertThat(guest.getRole()).isEqualTo("GUEST");
        assertThat(guest.getUsername()).startsWith("guest-");
    }

    @Test
    void 호출할_때마다_다른_신원이_발급된다() {
        CurrentUser first = authService.guestLogin();
        CurrentUser second = authService.guestLogin();

        assertThat(first.getId()).isNotEqualTo(second.getId());
    }
}
