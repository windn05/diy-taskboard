package com.taskboard.service;

import com.taskboard.domain.Card;
import com.taskboard.domain.Notification;
import com.taskboard.dto.NotificationDtos.NotificationListResponse;
import com.taskboard.dto.NotificationDtos.NotificationResponse;
import com.taskboard.exception.EntityNotFoundException;
import com.taskboard.repository.NotificationRepository;
import com.taskboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int DEFAULT_LIMIT = 30;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 작업에 댓글이 달리면 담당자에게 알린다.
     * 담당자가 없거나 본인이 단 댓글이면 알리지 않는다 — 자기 행동을 자기에게 알릴 이유가 없다.
     */
    @Transactional
    public void notifyComment(Card card, Long actorId) {
        Long assigneeId = card.getAssigneeId();
        if (assigneeId == null || assigneeId.equals(actorId)) return;

        String actorName = userRepository.findById(actorId).map(u -> u.getName()).orElse("알 수 없음");
        Notification saved = notificationRepository.save(Notification.builder()
                .userId(assigneeId)
                .type(Notification.Type.COMMENT)
                .workspaceId(card.getWorkspaceId())
                .cardId(card.getId())
                .cardTitle(card.getTitle())
                .actorName(actorName)
                .build());

        eventPublisher.publishEvent(new NotificationCreatedEvent(assigneeId, toResponse(saved)));
    }

    /** 커밋 이후에 푸시하기 위한 내부 이벤트. */
    public record NotificationCreatedEvent(Long userId, NotificationResponse notification) {
    }

    @Transactional(readOnly = true)
    public NotificationListResponse list(Long userId) {
        List<NotificationResponse> notifications = notificationRepository
                .findByUserIdOrderByIdDesc(userId, PageRequest.of(0, DEFAULT_LIMIT)).stream()
                .map(this::toResponse)
                .toList();
        return new NotificationListResponse(notifications, notificationRepository.countByUserIdAndReadFalse(userId));
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new EntityNotFoundException("알림을 찾을 수 없습니다."));
        notification.setRead(true);
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.findByUserIdAndReadFalse(userId).forEach(n -> n.setRead(true));
    }

    /** 작업이 사라지면 그 작업을 가리키는 알림도 의미가 없다. */
    @Transactional
    public void deleteByCardIds(List<Long> cardIds) {
        if (!cardIds.isEmpty()) notificationRepository.deleteByCardIdIn(cardIds);
    }

    private NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(n.getId(), n.getType().name(), n.getWorkspaceId(), n.getCardId(),
                n.getCardTitle(), n.getActorName(), n.isRead(), n.getCreatedAt());
    }
}
