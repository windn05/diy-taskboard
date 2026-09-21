package com.taskboard.service;

import com.taskboard.domain.Card;
import com.taskboard.domain.Comment;
import com.taskboard.domain.User;
import com.taskboard.dto.CommentDtos.*;
import com.taskboard.dto.RealtimeDtos.CommentEvent;
import com.taskboard.exception.AccessDeniedException;
import com.taskboard.exception.EntityNotFoundException;
import com.taskboard.repository.CardRepository;
import com.taskboard.repository.CommentRepository;
import com.taskboard.repository.UserRepository;
import com.taskboard.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 댓글. 작성은 프로젝트 멤버, 삭제는 작성자 본인만 가능. 작성 시 담당자에게 알림 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final WorkspaceService workspaceService;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<CommentResponse> list(CurrentUser user, Long cardId) {
        Card card = getCard(cardId);
        workspaceService.requireReadAccess(user, card.getWorkspaceId());
        List<Comment> comments = commentRepository.findByCardIdOrderByCreatedAtAsc(cardId);

        // 작성자 이름은 한 번에 조회 (댓글마다 findById 하면 N+1)
        Map<Long, String> authorNames = userRepository
                .findAllById(comments.stream().map(Comment::getUserId).distinct().toList()).stream()
                .collect(Collectors.toMap(User::getId, User::getName));

        return comments.stream().map(c -> toResponse(c, authorNames.get(c.getUserId()))).toList();
    }

    @Transactional
    public CommentResponse create(Long userId, Long cardId, CreateCommentRequest request) {
        Card card = getCard(cardId);
        workspaceService.requireMember(userId, card.getWorkspaceId());
        Comment comment = Comment.builder().cardId(cardId).userId(userId).content(request.content()).build();
        commentRepository.save(comment);
        CommentResponse response = toResponse(comment, userRepository.findById(userId).map(User::getName).orElse(null));
        eventPublisher.publishEvent(new CommentEvent("CREATED", card.getWorkspaceId(), cardId, response));
        notificationService.notifyComment(card, userId);
        return response;
    }

    /** 본인이 쓴 댓글만 삭제 가능 */
    @Transactional
    public void delete(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("댓글을 찾을 수 없습니다."));
        if (!comment.getUserId().equals(userId)) {
            throw new AccessDeniedException("본인 댓글만 삭제할 수 있습니다.");
        }
        Card card = getCard(comment.getCardId());
        commentRepository.delete(comment);
        CommentResponse response = toResponse(comment, null);
        eventPublisher.publishEvent(new CommentEvent("DELETED", card.getWorkspaceId(), comment.getCardId(), response));
    }

    private Card getCard(Long cardId) {
        return cardRepository.findById(cardId).orElseThrow(() -> new EntityNotFoundException("카드를 찾을 수 없습니다."));
    }

    private CommentResponse toResponse(Comment comment, String authorName) {
        return new CommentResponse(comment.getId(), comment.getCardId(), comment.getUserId(),
                authorName != null ? authorName : "알 수 없음", comment.getContent(), comment.getCreatedAt());
    }
}
