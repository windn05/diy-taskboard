package com.taskboard.card;

import com.taskboard.card.CardDtos.*;
import com.taskboard.comment.CommentDtos.*;
import com.taskboard.comment.CommentService;
import com.taskboard.global.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 작업(카드)과 댓글. 프로젝트 멤버 여부 등 권한 검사는 서비스에서 처리.
 * 변경 사항은 커밋 후 WebSocket으로 같은 프로젝트 접속자에게 전파
 */
@RestController
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;
    private final CommentService commentService;

    @GetMapping("/workspaces/{workspaceId}/cards")
    public List<CardResponse> list(@AuthenticationPrincipal CurrentUser user, @PathVariable Long workspaceId) {
        return cardService.list(user, workspaceId);
    }

    @PostMapping("/workspaces/{workspaceId}/cards")
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponse create(@AuthenticationPrincipal CurrentUser user, @PathVariable Long workspaceId,
                                @Valid @RequestBody CreateCardRequest request) {
        return cardService.create(user.getId(), workspaceId, request);
    }

    /** 부분 수정. 보내지 않은 필드는 유지 (날짜는 null로 비우기 가능) */
    @PatchMapping("/cards/{cardId}")
    public CardResponse update(@AuthenticationPrincipal CurrentUser user, @PathVariable Long cardId,
                                @RequestBody UpdateCardRequest request) {
        return cardService.update(user.getId(), cardId, request);
    }

    @DeleteMapping("/cards/{cardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal CurrentUser user, @PathVariable Long cardId) {
        cardService.delete(user.getId(), cardId);
    }

    @GetMapping("/cards/{cardId}/comments")
    public List<CommentResponse> listComments(@AuthenticationPrincipal CurrentUser user, @PathVariable Long cardId) {
        return commentService.list(user, cardId);
    }

    @PostMapping("/cards/{cardId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(@AuthenticationPrincipal CurrentUser user, @PathVariable Long cardId,
                                       @Valid @RequestBody CreateCommentRequest request) {
        return commentService.create(user.getId(), cardId, request);
    }

    @DeleteMapping("/cards/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@AuthenticationPrincipal CurrentUser user, @PathVariable Long commentId) {
        commentService.delete(user.getId(), commentId);
    }
}
