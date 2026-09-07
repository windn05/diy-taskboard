package com.taskboard.service;

import com.taskboard.domain.Card;
import com.taskboard.domain.Status;
import com.taskboard.domain.Workspace;
import com.taskboard.domain.WorkspaceMember;
import com.taskboard.domain.WorkspaceMember.WorkspaceRole;
import com.taskboard.dto.AuthDtos.SignupRequest;
import com.taskboard.dto.CommentDtos.CreateCommentRequest;
import com.taskboard.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class NotificationServiceTest {

    @Autowired NotificationService notificationService;
    @Autowired CommentService commentService;
    @Autowired CardService cardService;
    @Autowired AuthService authService;
    @Autowired NotificationRepository notificationRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired WorkspaceMemberRepository memberRepository;
    @Autowired StatusRepository statusRepository;
    @Autowired CardRepository cardRepository;
    @Autowired CommentRepository commentRepository;
    @Autowired ActivityLogRepository activityLogRepository;
    @Autowired UserRepository userRepository;

    private Long assigneeId;
    private Long actorId;
    private Long cardId;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        activityLogRepository.deleteAll();
        commentRepository.deleteAll();
        cardRepository.deleteAll();
        memberRepository.deleteAll();
        workspaceRepository.deleteAll();
        statusRepository.deleteAll();
        userRepository.deleteAll();

        assigneeId = authService.signup(new SignupRequest("assignee", "pw12345678", "담당자")).id();
        actorId = authService.signup(new SignupRequest("actor", "pw12345678", "댓글쓴이")).id();

        Long workspaceId = workspaceRepository.save(
                Workspace.builder().name("프로젝트").ownerId(assigneeId).build()).getId();
        memberRepository.save(WorkspaceMember.builder()
                .workspaceId(workspaceId).userId(assigneeId).role(WorkspaceRole.OWNER).build());
        memberRepository.save(WorkspaceMember.builder()
                .workspaceId(workspaceId).userId(actorId).role(WorkspaceRole.MEMBER).build());
        Long statusId = statusRepository.save(Status.builder().name("할 일").order(1).build()).getId();

        cardId = cardRepository.save(Card.builder()
                .workspaceId(workspaceId).statusId(statusId).title("작업 제목").type("Task")
                .assigneeId(assigneeId).build()).getId();
    }

    private void comment(Long userId, String content) {
        commentService.create(userId, cardId, new CreateCommentRequest(content));
    }

    @Test
    void 댓글이_달리면_담당자에게_알림이_간다() {
        comment(actorId, "확인 부탁드립니다");

        var result = notificationService.list(assigneeId);
        assertThat(result.unreadCount()).isEqualTo(1);
        assertThat(result.notifications()).singleElement().satisfies(n -> {
            assertThat(n.actorName()).isEqualTo("댓글쓴이");
            assertThat(n.cardTitle()).isEqualTo("작업 제목");
            assertThat(n.cardId()).isEqualTo(cardId);
            assertThat(n.read()).isFalse();
        });
    }

    @Test
    void 본인이_단_댓글은_자신에게_알리지_않는다() {
        comment(assigneeId, "내가 내 작업에 남기는 메모");

        assertThat(notificationService.list(assigneeId).notifications()).isEmpty();
    }

    @Test
    void 댓글을_단_사람에게는_알림이_가지_않는다() {
        comment(actorId, "확인 부탁드립니다");

        assertThat(notificationService.list(actorId).notifications()).isEmpty();
    }

    @Test
    void 담당자가_없으면_알림을_만들지_않는다() {
        Card card = cardRepository.findById(cardId).orElseThrow();
        card.setAssigneeId(null);
        cardRepository.save(card);

        comment(actorId, "담당자 없는 작업");

        assertThat(notificationRepository.findAll()).isEmpty();
    }

    @Test
    void 읽음_처리하면_안읽은_개수가_줄어든다() {
        comment(actorId, "첫 댓글");
        comment(actorId, "둘째 댓글");

        var before = notificationService.list(assigneeId);
        assertThat(before.unreadCount()).isEqualTo(2);

        notificationService.markRead(assigneeId, before.notifications().get(0).id());
        assertThat(notificationService.list(assigneeId).unreadCount()).isEqualTo(1);

        notificationService.markAllRead(assigneeId);
        assertThat(notificationService.list(assigneeId).unreadCount()).isZero();
    }

    @Test
    void 작업이_삭제되면_그_작업의_알림도_사라진다() {
        comment(actorId, "확인 부탁드립니다");
        assertThat(notificationRepository.findAll()).hasSize(1);

        cardService.delete(assigneeId, cardId);

        assertThat(notificationRepository.findAll()).isEmpty();
    }

    @Test
    void 남의_알림은_읽음_처리할_수_없다() {
        comment(actorId, "확인 부탁드립니다");
        Long notificationId = notificationService.list(assigneeId).notifications().get(0).id();

        // 수신자가 아닌 사람이 요청하면 찾지 못해야 한다.
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> notificationService.markRead(actorId, notificationId));
        assertThat(notificationService.list(assigneeId).unreadCount()).isEqualTo(1);
    }

    @Test
    void 알림은_최신순으로_돌려준다() {
        comment(actorId, "첫 댓글");
        comment(actorId, "둘째 댓글");

        List<Long> ids = notificationService.list(assigneeId).notifications().stream()
                .map(n -> n.id())
                .toList();
        assertThat(ids).isSortedAccordingTo((a, b) -> Long.compare(b, a));
    }
}
