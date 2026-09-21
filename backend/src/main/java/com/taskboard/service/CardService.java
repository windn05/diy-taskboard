package com.taskboard.service;

import com.taskboard.domain.ActivityLog;
import com.taskboard.domain.Card;
import com.taskboard.domain.Comment;
import com.taskboard.dto.CardDtos.*;
import com.taskboard.dto.RealtimeDtos.CardEvent;
import com.taskboard.exception.EntityNotFoundException;
import com.taskboard.repository.ActivityLogRepository;
import com.taskboard.repository.CardRepository;
import com.taskboard.repository.CommentRepository;
import com.taskboard.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 작업(카드) CRUD. 조회는 읽기 권한, 변경은 프로젝트 멤버만 가능.
 * 변경마다 활동 이력을 남기고, 커밋 후 같은 프로젝트 접속자에게 이벤트 전송
 */
@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final StatusService statusService;
    private final ActivityLogRepository activityLogRepository;
    private final CommentRepository commentRepository;
    private final WorkspaceService workspaceService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    // labels가 LAZY라 조회에도 트랜잭션 필요. 없으면 OSIV가 켜져 있을 때만 우연히 동작
    @Transactional(readOnly = true)
    public List<CardResponse> list(CurrentUser user, Long workspaceId) {
        workspaceService.requireReadAccess(user, workspaceId);
        List<Card> cards = cardRepository.findByWorkspaceIdOrderByCreatedAtAsc(workspaceId);

        // 댓글 수를 카드마다 세면 N+1이라, 한 번에 모아서 카드별로 집계
        Map<Long, Long> commentCounts = commentRepository.findByCardIdIn(cards.stream().map(Card::getId).toList())
                .stream().collect(Collectors.groupingBy(Comment::getCardId, Collectors.counting()));

        return cards.stream().map(card -> toResponse(card, commentCounts.getOrDefault(card.getId(), 0L))).toList();
    }

    @Transactional
    public CardResponse create(Long userId, Long workspaceId, CreateCardRequest request) {
        workspaceService.requireMember(userId, workspaceId);
        Long statusId = request.statusId() != null ? request.statusId() : defaultStatusId();
        Card card = Card.builder()
                .workspaceId(workspaceId)
                .statusId(statusId)
                .title(request.title())
                .description(request.description())
                .type(request.type() != null ? request.type() : "Task")
                .priority(request.priority() != null ? Card.Priority.valueOf(request.priority().toUpperCase()) : Card.Priority.MEDIUM)
                .assigneeId(request.assigneeId())
                .labels(request.labels() != null ? request.labels() : List.of())
                .startDate(request.startDate())
                .dueDate(request.dueDate())
                .build();
        cardRepository.save(card);
        logActivity(card.getId(), userId, "CREATED");
        return publish("CREATED", toResponse(card));
    }

    @Transactional
    public CardResponse update(Long userId, Long cardId, UpdateCardRequest request) {
        Card card = getCard(cardId);
        workspaceService.requireMember(userId, card.getWorkspaceId());
        if (request.getTitle() != null) card.setTitle(request.getTitle());
        if (request.getDescription() != null) card.setDescription(request.getDescription());
        if (request.getType() != null) card.setType(request.getType());
        if (request.getPriority() != null) card.setPriority(Card.Priority.valueOf(request.getPriority().toUpperCase()));
        if (request.getLabels() != null) card.setLabels(request.getLabels());
        // 아래 셋은 null이 "지우기"를 뜻하므로 전달 여부로 판단
        if (request.isAssigneeIdPresent()) card.setAssigneeId(request.getAssigneeId());
        if (request.isStartDatePresent()) card.setStartDate(request.getStartDate());
        if (request.isDueDatePresent()) card.setDueDate(request.getDueDate());
        boolean moved = request.getStatusId() != null && !Objects.equals(request.getStatusId(), card.getStatusId());
        if (moved) card.setStatusId(request.getStatusId());
        logActivity(card.getId(), userId, moved ? "MOVED" : "UPDATED");
        return publish(moved ? "MOVED" : "UPDATED", toResponse(card));
    }

    @Transactional
    public void delete(Long userId, Long cardId) {
        Card card = getCard(cardId);
        workspaceService.requireMember(userId, card.getWorkspaceId());
        CardResponse deleted = toResponse(card);
        notificationService.deleteByCardIds(List.of(cardId));
        cardRepository.delete(card);
        publish("DELETED", deleted);
    }

    /** 실제 전송은 커밋 이후 RealtimeEventBroadcaster가 처리 */
    private CardResponse publish(String type, CardResponse card) {
        eventPublisher.publishEvent(new CardEvent(type, card));
        return card;
    }

    /** 상태를 지정하지 않은 새 작업은 첫 번째 컬럼에 배치 */
    private Long defaultStatusId() {
        return statusService.list().stream().findFirst()
                .orElseThrow(() -> new EntityNotFoundException("상태가 존재하지 않습니다."))
                .id();
    }

    private void logActivity(Long cardId, Long userId, String action) {
        activityLogRepository.save(ActivityLog.builder().cardId(cardId).userId(userId).action(action).build());
    }

    private Card getCard(Long cardId) {
        return cardRepository.findById(cardId).orElseThrow(() -> new EntityNotFoundException("카드를 찾을 수 없습니다."));
    }

    /** 같은 패키지의 ReleaseService도 사용. 단건이라 댓글 수를 그때그때 조회(N+1 걱정 없는 범위) */
    CardResponse toResponse(Card card) {
        return toResponse(card, commentRepository.countByCardId(card.getId()));
    }

    private CardResponse toResponse(Card card, long commentCount) {
        // labels는 LAZY 컬렉션이라 세션 밖(커밋 후 브로드캐스트 등)에서 직렬화하면 실패. 여기서 복사해 분리
        return new CardResponse(card.getId(), card.getWorkspaceId(), card.getStatusId(), card.getTitle(), card.getDescription(),
                card.getType(), card.getPriority().name(), card.getAssigneeId(), List.copyOf(card.getLabels()),
                card.getStartDate(), card.getDueDate(), card.getReleaseId(), commentCount,
                card.getCreatedAt(), card.getUpdatedAt());
    }
}
