package com.taskboard.dto;

import java.time.LocalDateTime;
import java.util.List;

public class NotificationDtos {

    public record NotificationResponse(
            Long id,
            String type,
            Long workspaceId,
            Long cardId,
            String cardTitle,
            String actorName,
            boolean read,
            LocalDateTime createdAt) {
    }

    /** 알림 목록과 안 읽은 개수를 함께 준다 — 배지와 목록이 항상 같은 시점을 보게 하기 위함. */
    public record NotificationListResponse(List<NotificationResponse> notifications, long unreadCount) {
    }
}
