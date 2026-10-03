package com.taskboard.auth;

import com.taskboard.auth.AuthDtos.*;
import com.taskboard.global.security.CurrentUser;
import com.taskboard.user.User;
import com.taskboard.user.User.SystemRole;
import com.taskboard.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

/** 로그인 인증과 계정 생성·비밀번호 변경. 세션에 담는 일은 컨트롤러(웹 계층) 책임 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    /** 관리자만 호출(AdminUserController). 공개 회원가입 경로 없음 */
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        User user = User.builder()
                .username(request.username())
                .password(passwordEncoder.encode(request.password()))
                .name(request.name())
                .role(parseRole(request.role()))
                .build();
        userRepository.save(user);
        return toResponse(user);
    }

    public CurrentUser login(LoginRequest request) {
        loginAttemptService.checkNotLocked(request.username());

        User user = userRepository.findByUsername(request.username()).orElse(null);
        // 아이디 없음과 비밀번호 틀림을 구분해서 알려주면 계정 존재 여부가 노출됨
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            loginAttemptService.recordFailure(request.username());
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        loginAttemptService.recordSuccess(request.username());
        return new CurrentUser(user.getId(), user.getUsername(), user.getRole().name());
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
    }

    /**
     * 게스트는 User 테이블에 저장하지 않고 요청마다 새 신원 발급.
     * id는 음수로 발급해 실제 사용자(AUTO_INCREMENT 양수)와 겹치지 않게 함.
     * 이 신원은 세션에만 담기고 DB 어디에도 남지 않으며, 세션이 끝나면 그냥 사라진다.
     */
    public CurrentUser guestLogin() {
        long guestId = -(RANDOM.nextLong(1, Long.MAX_VALUE));
        String username = "guest-" + String.format("%04x", RANDOM.nextInt(0x10000));
        return new CurrentUser(guestId, username, SystemRole.GUEST.name());
    }

    /** 비어 있으면 USER. GUEST는 거부 */
    private SystemRole parseRole(String role) {
        if (role == null || role.isBlank()) return SystemRole.USER;
        SystemRole parsed;
        try {
            parsed = SystemRole.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("알 수 없는 역할입니다: " + role);
        }
        if (parsed == SystemRole.GUEST) {
            throw new IllegalArgumentException("게스트는 저장하지 않는 신원이라 계정으로 만들 수 없습니다.");
        }
        return parsed;
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getName(), user.getRole().name());
    }
}
