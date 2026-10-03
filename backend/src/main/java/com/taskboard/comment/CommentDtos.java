package com.taskboard.comment;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/** 댓글 요청/응답 */
public class CommentDtos {

    public record CreateCommentRequest(@NotBlank String content) {
    }

    /** 댓글 응답 (작성자 이름은 조회 시점 값) */
    public record CommentResponse(
            Long id, Long cardId, Long userId, String authorName, String content, LocalDateTime createdAt) {
    }
}
