package com.taskboard.realtime;

import com.taskboard.realtime.RealtimeDtos.CardEvent;
import com.taskboard.realtime.RealtimeDtos.CommentEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 작업·댓글 변경을 커밋 후 같은 프로젝트 접속자에게 전송 */
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
