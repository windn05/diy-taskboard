package com.taskboard.service;

import com.taskboard.domain.Card;
import com.taskboard.domain.Status;
import com.taskboard.domain.Workspace;
import com.taskboard.domain.WorkspaceMember;
import com.taskboard.domain.WorkspaceMember.WorkspaceRole;
import com.taskboard.dto.ReleaseDtos.CreateReleaseRequest;
import com.taskboard.dto.ReleaseDtos.UpdateReleaseRequest;
import com.taskboard.repository.*;
import com.taskboard.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ReleaseServiceTest {

    @Autowired ReleaseService releaseService;
    @Autowired ReleaseRepository releaseRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository memberRepository;
    @Autowired StatusRepository statusRepository;
    @Autowired CardRepository cardRepository;
    @Autowired ActivityLogRepository activityLogRepository;

    private static final Long USER_ID = 1L;
    private static final CurrentUser USER = new CurrentUser(USER_ID, "user", "USER");

    private Long workspaceId;
    private Long doneStatusId;
    private Long releasedStatusId;
    private Long cardA;
    private Long cardB;

    @BeforeEach
    void setUp() {
        releaseRepository.deleteAll();
        activityLogRepository.deleteAll();
        cardRepository.deleteAll();
        memberRepository.deleteAll();
        workspaceRepository.deleteAll();
        statusRepository.deleteAll();

        workspaceId = workspaceRepository.save(
                Workspace.builder().name("프로젝트").ownerId(USER_ID).build()).getId();
        memberRepository.save(WorkspaceMember.builder()
                .workspaceId(workspaceId).userId(USER_ID).role(WorkspaceRole.OWNER).build());

        doneStatusId = statusRepository.save(Status.builder().name("개발 완료").order(1).build()).getId();
        releasedStatusId = statusRepository.save(Status.builder().name("배포 완료").order(2).build()).getId();

        cardA = saveCard("작업 A", doneStatusId);
        cardB = saveCard("작업 B", doneStatusId);
    }

    private Long saveCard(String title, Long statusId) {
        return cardRepository.save(Card.builder()
                .workspaceId(workspaceId).statusId(statusId).title(title).type("Task").build()).getId();
    }

    private CreateReleaseRequest request(String version, List<Long> cardIds, Long completedStatusId) {
        return new CreateReleaseRequest(version, "패치노트", cardIds, completedStatusId);
    }

    @Test
    void 배포하면_포함된_작업이_지정한_상태로_옮겨진다() {
        releaseService.create(USER_ID, workspaceId, request("v1.0.0", List.of(cardA, cardB), releasedStatusId));

        assertThat(cardRepository.findById(cardA).orElseThrow().getStatusId()).isEqualTo(releasedStatusId);
        assertThat(cardRepository.findById(cardB).orElseThrow().getStatusId()).isEqualTo(releasedStatusId);
    }

    @Test
    void 전환할_상태를_주지_않으면_작업_상태는_그대로다() {
        releaseService.create(USER_ID, workspaceId, request("v1.0.0", List.of(cardA), null));

        assertThat(cardRepository.findById(cardA).orElseThrow().getStatusId()).isEqualTo(doneStatusId);
    }

    @Test
    void 배포된_작업은_다음_배포_후보에서_빠진다() {
        assertThat(releaseService.candidates(USER, workspaceId, null)).hasSize(2);

        releaseService.create(USER_ID, workspaceId, request("v1.0.0", List.of(cardA), releasedStatusId));

        assertThat(releaseService.candidates(USER, workspaceId, null))
                .singleElement()
                .satisfies(card -> assertThat(card.id()).isEqualTo(cardB));
    }

    @Test
    void 후보는_상태로_걸러진다() {
        assertThat(releaseService.candidates(USER, workspaceId, doneStatusId)).hasSize(2);
        assertThat(releaseService.candidates(USER, workspaceId, releasedStatusId)).isEmpty();
    }

    @Test
    void 같은_프로젝트에_같은_버전을_두_번_만들_수_없다() {
        releaseService.create(USER_ID, workspaceId, request("v1.0.0", List.of(cardA), releasedStatusId));

        assertThatThrownBy(() ->
                releaseService.create(USER_ID, workspaceId, request("v1.0.0", List.of(cardB), releasedStatusId)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 이미_배포된_작업은_다시_넣을_수_없다() {
        releaseService.create(USER_ID, workspaceId, request("v1.0.0", List.of(cardA), releasedStatusId));

        assertThatThrownBy(() ->
                releaseService.create(USER_ID, workspaceId, request("v1.0.1", List.of(cardA), releasedStatusId)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 다른_프로젝트의_작업은_포함할_수_없다() {
        Long otherWorkspaceId = workspaceRepository.save(
                Workspace.builder().name("다른 프로젝트").ownerId(USER_ID).build()).getId();
        Long foreignCard = cardRepository.save(Card.builder()
                .workspaceId(otherWorkspaceId).statusId(doneStatusId).title("남의 작업").type("Task").build()).getId();

        assertThatThrownBy(() ->
                releaseService.create(USER_ID, workspaceId, request("v1.0.0", List.of(foreignCard), releasedStatusId)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 배포를_취소하면_작업이_다시_후보가_된다() {
        var release = releaseService.create(USER_ID, workspaceId, request("v1.0.0", List.of(cardA), releasedStatusId));
        assertThat(releaseService.candidates(USER, workspaceId, null)).hasSize(1);

        releaseService.delete(USER_ID, release.id());

        assertThat(releaseService.candidates(USER, workspaceId, null)).hasSize(2);
        // 상태까지 되돌리지는 않음
        assertThat(cardRepository.findById(cardA).orElseThrow().getStatusId()).isEqualTo(releasedStatusId);
    }

    @Test
    void 패치노트를_수정할_수_있다() {
        var release = releaseService.create(USER_ID, workspaceId, request("v1.0.0", List.of(cardA), releasedStatusId));

        var updated = releaseService.update(USER_ID, release.id(),
                new UpdateReleaseRequest(null, "수정된 패치노트", null, null));

        assertThat(updated.notes()).isEqualTo("수정된 패치노트");
        assertThat(updated.version()).isEqualTo("v1.0.0");
    }

    @Test
    void 배포한_뒤에도_작업을_추가할_수_있다() {
        var release = releaseService.create(USER_ID, workspaceId, request("v1.0.0", List.of(cardA), releasedStatusId));

        var updated = releaseService.update(USER_ID, release.id(),
                new UpdateReleaseRequest(null, null, List.of(cardB), releasedStatusId));

        assertThat(updated.cards()).extracting(c -> c.id()).containsExactlyInAnyOrder(cardA, cardB);
        assertThat(cardRepository.findById(cardB).orElseThrow().getStatusId()).isEqualTo(releasedStatusId);
    }

    @Test
    void 이미_배포된_작업은_다른_배포에_추가할_수_없다() {
        releaseService.create(USER_ID, workspaceId, request("v1.0.0", List.of(cardA), releasedStatusId));
        var second = releaseService.create(USER_ID, workspaceId, request("v1.0.1", List.of(cardB), releasedStatusId));

        assertThatThrownBy(() -> releaseService.update(USER_ID, second.id(),
                new UpdateReleaseRequest(null, null, List.of(cardA), releasedStatusId)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 배포_이력은_최신순이며_포함_작업을_함께_준다() {
        releaseService.create(USER_ID, workspaceId, request("v1.0.0", List.of(cardA), releasedStatusId));
        releaseService.create(USER_ID, workspaceId, request("v1.0.1", List.of(cardB), releasedStatusId));

        var releases = releaseService.list(USER, workspaceId);

        assertThat(releases).extracting(r -> r.version()).containsExactly("v1.0.1", "v1.0.0");
        assertThat(releases.get(0).cards()).singleElement()
                .satisfies(card -> assertThat(card.title()).isEqualTo("작업 B"));
    }
}
