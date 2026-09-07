package com.taskboard.service;

import com.taskboard.exception.TooManyAttemptsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptServiceTest {

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        service = new LoginAttemptService();
    }

    @Test
    void 실패가_임계치에_닿기_전에는_막지_않는다() {
        for (int i = 0; i < 4; i++) {
            service.recordFailure("admin");
        }

        assertThatCode(() -> service.checkNotLocked("admin")).doesNotThrowAnyException();
    }

    @Test
    void 실패가_임계치에_닿으면_막는다() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("admin");
        }

        assertThatThrownBy(() -> service.checkNotLocked("admin"))
                .isInstanceOf(TooManyAttemptsException.class);
    }

    @Test
    void 로그인에_성공하면_실패_기록이_사라진다() {
        for (int i = 0; i < 4; i++) {
            service.recordFailure("admin");
        }
        service.recordSuccess("admin");
        service.recordFailure("admin");

        assertThatCode(() -> service.checkNotLocked("admin")).doesNotThrowAnyException();
    }

    /** 대소문자를 따로 세면 Admin/ADMIN을 번갈아 시도해 잠금을 우회할 수 있다. */
    @Test
    void 대소문자가_달라도_같은_계정으로_센다() {
        service.recordFailure("admin");
        service.recordFailure("Admin");
        service.recordFailure("ADMIN");
        service.recordFailure("aDmIn");
        service.recordFailure("admiN");

        assertThatThrownBy(() -> service.checkNotLocked("admin"))
                .isInstanceOf(TooManyAttemptsException.class);
    }

    @Test
    void 다른_계정의_실패는_서로_영향을_주지_않는다() {
        for (int i = 0; i < 5; i++) {
            service.recordFailure("admin");
        }

        assertThatCode(() -> service.checkNotLocked("user")).doesNotThrowAnyException();
    }
}
