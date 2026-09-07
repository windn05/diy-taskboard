package com.taskboard.config;

import com.taskboard.domain.CardType;
import com.taskboard.domain.Status;
import com.taskboard.domain.User;
import com.taskboard.domain.User.SystemRole;
import com.taskboard.repository.CardTypeRepository;
import com.taskboard.repository.StatusRepository;
import com.taskboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전역 상태·타입의 초기값을 기동 시점에 한 번만 넣는다.
 * 조회 메서드에서 만들면 GET 요청이 쓰기를 유발해, 읽기 전용이어야 할 게스트도 데이터를 만들게 된다.
 *
 * <p>최초 관리자도 여기서 만든다. 공개 회원가입을 없앤 뒤로는 계정을 만들려면 관리자여야 하는데,
 * 새 DB에는 그 관리자가 없다. 그 순환을 끊는 유일한 통로다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultDataInitializer implements ApplicationRunner {

    private static final String[] DEFAULT_STATUSES = {"To Do", "In Progress", "Review", "Done"};
    private static final String[] DEFAULT_CARD_TYPES = {"Task", "Bug", "Story"};

    private final StatusRepository statusRepository;
    private final CardTypeRepository cardTypeRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${taskboard.bootstrap.admin-username:}")
    private String bootstrapAdminUsername;

    @Value("${taskboard.bootstrap.admin-password:}")
    private String bootstrapAdminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (statusRepository.count() == 0) {
            for (int i = 0; i < DEFAULT_STATUSES.length; i++) {
                statusRepository.save(Status.builder().name(DEFAULT_STATUSES[i]).order(i).build());
            }
        }
        if (cardTypeRepository.count() == 0) {
            for (String name : DEFAULT_CARD_TYPES) {
                cardTypeRepository.save(CardType.builder().name(name).build());
            }
        }
        createBootstrapAdminIfNeeded();
    }

    /** 계정이 하나도 없을 때만 만든다. 이미 계정이 있으면 환경변수가 남아 있어도 아무 일도 하지 않는다. */
    private void createBootstrapAdminIfNeeded() {
        if (userRepository.count() > 0) return;

        if (bootstrapAdminUsername.isBlank() || bootstrapAdminPassword.isBlank()) {
            log.warn("계정이 하나도 없는데 BOOTSTRAP_ADMIN_USERNAME/PASSWORD가 없어 최초 관리자를 만들지 못했습니다. "
                    + "이 값을 넣고 다시 기동하세요.");
            return;
        }

        userRepository.save(User.builder()
                .username(bootstrapAdminUsername)
                .password(passwordEncoder.encode(bootstrapAdminPassword))
                .name(bootstrapAdminUsername)
                .role(SystemRole.ADMIN)
                .build());
        log.warn("최초 관리자 '{}'을(를) 만들었습니다. 로그인 후 비밀번호를 바꾸고 "
                + "BOOTSTRAP_ADMIN_PASSWORD를 환경에서 지우세요.", bootstrapAdminUsername);
    }
}
