package com.taskboard.service;

import com.taskboard.domain.User;
import com.taskboard.domain.User.SystemRole;
import com.taskboard.dto.AuthDtos.*;
import com.taskboard.repository.UserRepository;
import com.taskboard.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginAttemptService loginAttemptService;

    /** 관리자만 호출한다(AdminUserController). 공개 회원가입 경로는 없다. */
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

    public TokenResponse login(LoginRequest request) {
        loginAttemptService.checkNotLocked(request.username());

        User user = userRepository.findByUsername(request.username()).orElse(null);
        // 아이디가 없을 때와 비밀번호가 틀렸을 때를 구분해서 알려주면 계정 존재 여부가 새어 나간다.
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            loginAttemptService.recordFailure(request.username());
            throw new IllegalArgumentException("아이디 또는 비밀번호가 올바르지 않습니다.");
        }

        loginAttemptService.recordSuccess(request.username());
        return issueTokens(user);
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
     * 게스트는 User 테이블에 저장하지 않고 매 요청마다 새 신원을 발급한다.
     * id는 음수로 발급해 실제 사용자(AUTO_INCREMENT 양수)와 절대 겹치지 않게 한다.
     */
    public TokenResponse guestLogin() {
        long guestId = -(RANDOM.nextLong(1, Long.MAX_VALUE));
        String username = "guest-" + String.format("%04x", RANDOM.nextInt(0x10000));
        String role = SystemRole.GUEST.name();
        return new TokenResponse(
                jwtService.generateAccessToken(guestId, username, role),
                jwtService.generateRefreshToken(guestId, username, role));
    }

    public TokenResponse refresh(String refreshToken) {
        if (!jwtService.isValid(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 refresh token 입니다.");
        }
        Long userId = jwtService.extractUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return issueTokens(user);
    }

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

    private TokenResponse issueTokens(User user) {
        return new TokenResponse(
                jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name()),
                jwtService.generateRefreshToken(user.getId(), user.getUsername(), user.getRole().name()));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getName(), user.getRole().name());
    }
}
