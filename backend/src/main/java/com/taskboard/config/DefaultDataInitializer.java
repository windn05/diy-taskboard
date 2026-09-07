package com.taskboard.config;

import com.taskboard.domain.CardType;
import com.taskboard.domain.Status;
import com.taskboard.repository.CardTypeRepository;
import com.taskboard.repository.StatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전역 상태·타입의 초기값을 기동 시점에 한 번만 넣는다.
 * 조회 메서드에서 만들면 GET 요청이 쓰기를 유발해, 읽기 전용이어야 할 게스트도 데이터를 만들게 된다.
 */
@Component
@RequiredArgsConstructor
public class DefaultDataInitializer implements ApplicationRunner {

    private static final String[] DEFAULT_STATUSES = {"To Do", "In Progress", "Review", "Done"};
    private static final String[] DEFAULT_CARD_TYPES = {"Task", "Bug", "Story"};

    private final StatusRepository statusRepository;
    private final CardTypeRepository cardTypeRepository;

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
    }
}
