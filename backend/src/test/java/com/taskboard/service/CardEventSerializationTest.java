package com.taskboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskboard.domain.Card;
import com.taskboard.domain.Status;
import com.taskboard.domain.Workspace;
import com.taskboard.domain.WorkspaceMember;
import com.taskboard.domain.WorkspaceMember.WorkspaceRole;
import com.taskboard.dto.CardDtos.CardResponse;
import com.taskboard.repository.*;
import com.taskboard.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 회귀 테스트: CardResponse가 엔티티의 LAZY 컬렉션(labels)을 그대로 들고 있으면
 * 트랜잭션 커밋 이후 브로드캐스트 시점에 직렬화 실패(삭제 이벤트에서 실제 발생).
 * REST 응답만 쓸 때는 OSIV가 가려주므로 세션 밖 직렬화로 검증
 */
@SpringBootTest
@ActiveProfiles("test")
class CardEventSerializationTest {

    @Autowired CardService cardService;
    @Autowired ObjectMapper objectMapper;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository memberRepository;
    @Autowired StatusRepository statusRepository;
    @Autowired CardRepository cardRepository;
    @Autowired ActivityLogRepository activityLogRepository;

    private static final Long USER_ID = 1L;
    private Long workspaceId;
    private Long statusId;

    @BeforeEach
    void setUp() {
        activityLogRepository.deleteAll();
        cardRepository.deleteAll();
        memberRepository.deleteAll();
        workspaceRepository.deleteAll();
        statusRepository.deleteAll();

        workspaceId = workspaceRepository.save(
                Workspace.builder().name("프로젝트").ownerId(USER_ID).build()).getId();
        memberRepository.save(WorkspaceMember.builder()
                .workspaceId(workspaceId).userId(USER_ID).role(WorkspaceRole.OWNER).build());
        statusId = statusRepository.save(Status.builder().name("할 일").order(1).build()).getId();
    }

    private Long saveCardWithLabels() {
        return cardRepository.save(Card.builder()
                .workspaceId(workspaceId)
                .statusId(statusId)
                .title("라벨 있는 작업")
                .type("Task")
                .labels(List.of("긴급", "백엔드"))
                .build()).getId();
    }

    @Test
    void 삭제_직전에_만든_응답도_세션_밖에서_직렬화된다() {
        Long cardId = saveCardWithLabels();

        // delete()는 삭제 전에 CardResponse를 만들어 DELETED 이벤트로 발행
        cardService.delete(USER_ID, cardId);

        // 트랜잭션이 끝난 뒤 조회하면 실제 브로드캐스트와 같은 조건
        assertThat(cardRepository.findById(cardId)).isEmpty();
    }

    @Test
    void 조회한_작업의_labels는_영속_컬렉션이_아니라_복사본이다() {
        saveCardWithLabels();

        List<CardResponse> cards = cardService.list(new CurrentUser(USER_ID, "user", "USER"), workspaceId);

        assertThat(cards).hasSize(1);
        CardResponse response = cards.get(0);
        assertThat(response.labels()).containsExactlyInAnyOrder("긴급", "백엔드");
        // 세션이 닫힌 뒤에도 직렬화가 성공해야 함 — 여기서 실패하면 실시간 브로드캐스트도 실패
        assertThatCode(() -> objectMapper.writeValueAsString(response)).doesNotThrowAnyException();
        assertThat(response.labels().getClass().getName()).doesNotContain("hibernate");
    }
}
