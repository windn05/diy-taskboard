package com.taskboard.realtime;

import com.taskboard.realtime.RealtimeDtos.CardEvent;
import com.taskboard.realtime.RealtimeDtos.CommentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 트랜잭션 커밋 후에만 브로드캐스트.
 * 서비스 안에서 바로 보내면 롤백된 변경까지 다른 접속자에게 전파될 수 있음
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

    private String topic(Long workspaceId, String channel) {
        return "/topic/workspaces/" + workspaceId + "/" + channel;
    }
}
