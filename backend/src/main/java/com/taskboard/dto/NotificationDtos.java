package com.taskboard.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 알림 응답 */
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

    /** 알림 목록과 안 읽은 개수를 함께 응답 — 배지와 목록이 항상 같은 시점을 보도록 */
    public record NotificationListResponse(List<NotificationResponse> notifications, long unreadCount) {
    }
}
