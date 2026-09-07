package com.taskboard.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

public class CommentDtos {

    public record CreateCommentRequest(@NotBlank String content) {
    }

    /** authorName은 저장하지 않고 조회할 때 붙인다 — 사용자가 이름을 바꾸면 댓글에도 반영되어야 하므로. */
    public record CommentResponse(
            Long id, Long cardId, Long userId, String authorName, String content, LocalDateTime createdAt) {
    }
}
