package com.taskboard.realtime;

import com.taskboard.card.CardDtos.CardResponse;
import com.taskboard.comment.CommentDtos.CommentResponse;

/** 실시간 이벤트 (Spring 이벤트이자 STOMP 메시지) */
public class RealtimeDtos {

    /** 작업 변경 이벤트 (CREATED·UPDATED·MOVED·DELETED) */
    public record CardEvent(String type, CardResponse card) {
    }

    /** 댓글 변경 이벤트 (CREATED·DELETED) */
    public record CommentEvent(String type, Long workspaceId, Long cardId, CommentResponse comment) {
    }
}
