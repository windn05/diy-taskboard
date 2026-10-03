package com.taskboard.card;

import com.taskboard.card.CardDtos.*;
import com.taskboard.comment.CommentDtos.*;
import com.taskboard.comment.CommentService;
import com.taskboard.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 작업과 댓글 API */
@RestController
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;
    private final CommentService commentService;

    @GetMapping("/workspaces/{workspaceId}/cards")
    public ResponseEntity<List<CardResponse>> list(@AuthenticationPrincipal CurrentUser user,
                                                   @PathVariable Long workspaceId) {
        return ResponseEntity.ok(cardService.list(user, workspaceId));
    }

    @PostMapping("/workspaces/{workspaceId}/cards")
    public ResponseEntity<CardResponse> create(@AuthenticationPrincipal CurrentUser user,
                                               @PathVariable Long workspaceId,
                                               @Valid @RequestBody CreateCardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cardService.create(user.getId(), workspaceId, request));
    }

    /*************************************************************************
     * 목적 : 작업 부분 수정 (보내지 않은 필드는 유지, 날짜는 null로 비움)
     * 이유 : -
     * 파라미터
     * - user : 로그인 사용자
     * - cardId : 작업 id
     * - request : 바꿀 필드만 담은 요청
     * 반환
     * - 200 + 수정된 작업
     *************************************************************************/
    @PatchMapping("/cards/{cardId}")
    public ResponseEntity<CardResponse> update(@AuthenticationPrincipal CurrentUser user,
                                               @PathVariable Long cardId,
                                               @RequestBody UpdateCardRequest request) {
        return ResponseEntity.ok(cardService.update(user.getId(), cardId, request));
    }

    @DeleteMapping("/cards/{cardId}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal CurrentUser user, @PathVariable Long cardId) {
        cardService.delete(user.getId(), cardId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cards/{cardId}/comments")
    public ResponseEntity<List<CommentResponse>> listComments(@AuthenticationPrincipal CurrentUser user,
                                                              @PathVariable Long cardId) {
        return ResponseEntity.ok(commentService.list(user, cardId));
    }

    @PostMapping("/cards/{cardId}/comments")
    public ResponseEntity<CommentResponse> addComment(@AuthenticationPrincipal CurrentUser user,
                                                      @PathVariable Long cardId,
                                                      @Valid @RequestBody CreateCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.create(user.getId(), cardId, request));
    }

    @DeleteMapping("/cards/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@AuthenticationPrincipal CurrentUser user, @PathVariable Long commentId) {
        commentService.delete(user.getId(), commentId);
        return ResponseEntity.noContent().build();
    }
}
