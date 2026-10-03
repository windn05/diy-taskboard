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

/** 로그인 인증과 계정 생성·비밀번호 변경 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;

    /*************************************************************************
     * 목적 : 새 계정 생성 (AdminUserController에서만 호출)
     * 이유 : 공개 회원가입이 없어 계정은 관리자만 생성
     * 파라미터
     * - request : 아이디·비밀번호·이름·역할
     * 반환
     * - 생성된 계정 정보
     *************************************************************************/
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

    /*************************************************************************
     * 목적 : 게스트 신원 발급 (DB에 저장하지 않음)
     * 이유 : id를 음수로 발급해 실제 사용자(양수 id)와 절대 겹치지 않게 함
     * 파라미터
     * -
     * 반환
     * - 게스트 사용자
     *************************************************************************/
    public CurrentUser guestLogin() {
        long guestId = -(RANDOM.nextLong(1, Long.MAX_VALUE));
        String username = "guest-" + String.format("%04x", RANDOM.nextInt(0x10000));
        return new CurrentUser(guestId, username, SystemRole.GUEST.name());
    }

    /*************************************************************************
     * 목적 : 역할 문자열을 SystemRole로 변환 (비어 있으면 USER)
     * 이유 : GUEST는 저장하지 않는 신원이라 계정 역할로 허용하지 않음
     * 파라미터
     * - role : 요청된 역할 이름
     * 반환
     * - 변환된 역할
     *************************************************************************/
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
