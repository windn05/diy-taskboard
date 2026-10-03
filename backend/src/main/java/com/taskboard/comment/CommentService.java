package com.taskboard.comment;

import com.taskboard.card.Card;
import com.taskboard.card.CardRepository;
import com.taskboard.comment.CommentDtos.*;
import com.taskboard.global.exception.AccessDeniedException;
import com.taskboard.global.exception.EntityNotFoundException;
import com.taskboard.global.security.CurrentUser;
import com.taskboard.realtime.RealtimeDtos.CommentEvent;
import com.taskboard.user.User;
import com.taskboard.user.UserRepository;
import com.taskboard.workspace.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 댓글 작성·조회·삭제 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final WorkspaceService workspaceService;
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
        return response;
    }

    /*************************************************************************
     * 목적 : 댓글 삭제 (본인 댓글만)
     * 이유 : -
     * 파라미터
     * - userId : 요청한 사용자 id
     * - commentId : 댓글 id
     * 반환
     * -
     *************************************************************************/
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
