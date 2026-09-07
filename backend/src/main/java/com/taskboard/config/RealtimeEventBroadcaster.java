package com.taskboard.config;

import com.taskboard.dto.RealtimeDtos.CardEvent;
import com.taskboard.dto.RealtimeDtos.CommentEvent;
import com.taskboard.service.NotificationService.NotificationCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 트랜잭션이 커밋된 뒤에만 브로드캐스트한다.
 * 서비스 안에서 바로 전송하면 이후 롤백된 변경까지 다른 접속자에게 전파될 수 있다.
 */
@Component
@RequiredArgsConstructor
public class RealtimeEventBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCardEvent(CardEvent event) {
        messagingTemplate.convertAndSend(topic(event.card().workspaceId(), "cards"), event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentEvent(CommentEvent event) {
        messagingTemplate.convertAndSend(topic(event.workspaceId(), "comments"), event);
    }

    /** 알림은 받는 사람 개인 채널로만 보낸다. 구독 권한은 StompAuthChannelInterceptor에서 검사한다. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        messagingTemplate.convertAndSend("/topic/users/" + event.userId() + "/notifications", event.notification());
    }

    private String topic(Long workspaceId, String channel) {
        return "/topic/workspaces/" + workspaceId + "/" + channel;
    }
}
