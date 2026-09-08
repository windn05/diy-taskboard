package com.taskboard.dto;

import com.taskboard.dto.CardDtos.CardResponse;
import com.taskboard.dto.CommentDtos.CommentResponse;

/**
 * 서비스 계층이 발행하는 Spring 이벤트이자, 그대로 STOMP 페이로드로 전송되는 타입.
 * 두 용도가 같은 모양이라 별도 변환 없이 하나로 쓴다.
 */
public class RealtimeDtos {

    /** type: CREATED | UPDATED | MOVED | DELETED. 목적지 결정에 쓰는 workspaceId는 card 안에 있다. */
    public record CardEvent(String type, CardResponse card) {
    }

    /** type: CREATED | DELETED. */
    public record CommentEvent(String type, Long workspaceId, Long cardId, CommentResponse comment) {
    }
}
