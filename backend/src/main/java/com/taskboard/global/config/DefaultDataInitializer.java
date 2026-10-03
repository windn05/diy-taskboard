package com.taskboard.global.config;

import com.taskboard.cardtype.CardType;
import com.taskboard.cardtype.CardTypeRepository;
import com.taskboard.status.Status;
import com.taskboard.status.StatusRepository;
import com.taskboard.user.User;
import com.taskboard.user.User.SystemRole;
import com.taskboard.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 기동 시 기본 상태·유형과 최초 관리자 생성 */
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

    /*************************************************************************
     * 목적 : 계정이 하나도 없을 때 환경변수 값으로 최초 관리자 생성
     * 이유 : 공개 회원가입이 없어 새 DB에는 계정을 만들 관리자도 없음 — 그 순환을 끊는 유일한 통로
     * 파라미터
     * -
     * 반환
     * -
     *************************************************************************/
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
