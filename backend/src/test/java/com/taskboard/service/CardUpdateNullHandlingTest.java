package com.taskboard.service;

import com.taskboard.domain.Card;
import com.taskboard.domain.Status;
import com.taskboard.domain.Workspace;
import com.taskboard.domain.WorkspaceMember;
import com.taskboard.domain.WorkspaceMember.WorkspaceRole;
import com.taskboard.dto.CardDtos.CardResponse;
import com.taskboard.dto.CardDtos.UpdateCardRequest;
import com.taskboard.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 회귀 테스트: 부분 수정에서 "값을 전달하지 않음"과 "null로 지움"은 다르게 동작해야 함.
 * 예전에는 둘 다 무시해서, 한 번 지정한 날짜를 화면에서 되돌릴 수 없었음
 */
@SpringBootTest
@ActiveProfiles("test")
class CardUpdateNullHandlingTest {

    @Autowired CardService cardService;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository memberRepository;
    @Autowired StatusRepository statusRepository;
    @Autowired CardRepository cardRepository;

    private static final Long USER_ID = 1L;
    private Long cardId;

    @BeforeEach
    void setUp() {
        cardRepository.deleteAll();
        memberRepository.deleteAll();
        workspaceRepository.deleteAll();
        statusRepository.deleteAll();

        Long workspaceId = workspaceRepository.save(
                Workspace.builder().name("프로젝트").ownerId(USER_ID).build()).getId();
        memberRepository.save(WorkspaceMember.builder()
                .workspaceId(workspaceId).userId(USER_ID).role(WorkspaceRole.OWNER).build());
        Long statusId = statusRepository.save(Status.builder().name("할 일").order(1).build()).getId();

        cardId = cardRepository.save(Card.builder()
                .workspaceId(workspaceId)
                .statusId(statusId)
                .title("작업")
                .type("Task")
                .startDate(LocalDate.of(2026, 9, 10))
                .dueDate(LocalDate.of(2026, 9, 20))
                .build()).getId();
    }

    @Test
    void null을_보내면_날짜가_지워진다() {
        UpdateCardRequest request = new UpdateCardRequest();
        request.setStartDate(null);
        request.setDueDate(null);

        CardResponse updated = cardService.update(USER_ID, cardId, request);

        assertThat(updated.startDate()).isNull();
        assertThat(updated.dueDate()).isNull();
    }

    @Test
    void 전달하지_않은_필드는_그대로_유지된다() {
        UpdateCardRequest request = new UpdateCardRequest();
        request.setTitle("제목만 변경");

        CardResponse updated = cardService.update(USER_ID, cardId, request);

        assertThat(updated.title()).isEqualTo("제목만 변경");
        assertThat(updated.startDate()).isEqualTo(LocalDate.of(2026, 9, 10));
        assertThat(updated.dueDate()).isEqualTo(LocalDate.of(2026, 9, 20));
    }

    @Test
    void 값을_보내면_그대로_반영된다() {
        UpdateCardRequest request = new UpdateCardRequest();
        request.setDueDate(LocalDate.of(2026, 12, 25));

        CardResponse updated = cardService.update(USER_ID, cardId, request);

        assertThat(updated.dueDate()).isEqualTo(LocalDate.of(2026, 12, 25));
        assertThat(updated.startDate()).isEqualTo(LocalDate.of(2026, 9, 10));
    }
}
