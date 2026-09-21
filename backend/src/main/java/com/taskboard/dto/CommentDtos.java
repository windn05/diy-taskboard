package com.taskboard.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/** 댓글 요청/응답 */
public class CommentDtos {

    public record CreateCommentRequest(@NotBlank String content) {
    }

    /** authorName은 저장하지 않고 조회 시 첨부 — 사용자가 이름을 바꾸면 댓글에도 반영되도록 */
    public record CommentResponse(
            Long id, Long cardId, Long userId, String authorName, String content, LocalDateTime createdAt) {
    }
}
