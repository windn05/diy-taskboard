package com.taskboard.controller;

import com.taskboard.dto.CardDtos.*;
import com.taskboard.dto.CommentDtos.*;
import com.taskboard.security.CurrentUser;
import com.taskboard.service.CardService;
import com.taskboard.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
