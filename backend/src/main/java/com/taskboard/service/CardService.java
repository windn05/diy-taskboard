package com.taskboard.service;

import com.taskboard.domain.ActivityLog;
import com.taskboard.domain.Card;
import com.taskboard.dto.CardDtos.*;
import com.taskboard.dto.RealtimeDtos.CardEvent;
import com.taskboard.exception.EntityNotFoundException;
import com.taskboard.repository.ActivityLogRepository;
import com.taskboard.repository.CardRepository;
import com.taskboard.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final StatusService statusService;
    private final ActivityLogRepository activityLogRepository;
    private final WorkspaceService workspaceService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    // labels가 LAZY라 조회에도 트랜잭션이 필요하다. 없으면 OSIV가 켜져 있을 때만 우연히 동작한다.
    @Transactional(readOnly = true)
    public List<CardResponse> list(CurrentUser user, Long workspaceId) {
        workspaceService.requireReadAccess(user, workspaceId);
        return cardRepository.findByWorkspaceIdOrderByCreatedAtAsc(workspaceId).stream().map(this::toResponse).toList();
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
        // 아래 셋은 null이 "지우기"를 뜻하므로 전달 여부로 판단한다.
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

    private CardResponse publish(String type, CardResponse card) {
        eventPublisher.publishEvent(new CardEvent(type, card));
        return card;
    }

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

    /** 같은 패키지의 ReleaseService도 쓴다. */
    CardResponse toResponse(Card card) {
        // labels는 LAZY 컬렉션이라 세션 밖(트랜잭션 커밋 후 브로드캐스트 등)에서 직렬화하면 깨진다. 여기서 복사해 분리한다.
        return new CardResponse(card.getId(), card.getWorkspaceId(), card.getStatusId(), card.getTitle(), card.getDescription(),
                card.getType(), card.getPriority().name(), card.getAssigneeId(), List.copyOf(card.getLabels()),
                card.getStartDate(), card.getDueDate(), card.getReleaseId(), card.getCreatedAt(), card.getUpdatedAt());
    }
}
