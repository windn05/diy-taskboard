package com.taskboard.card;

import com.taskboard.card.CardDtos.*;
import com.taskboard.comment.Comment;
import com.taskboard.comment.CommentRepository;
import com.taskboard.global.exception.EntityNotFoundException;
import com.taskboard.global.security.CurrentUser;
import com.taskboard.realtime.RealtimeDtos.CardEvent;
import com.taskboard.status.StatusService;
import com.taskboard.workspace.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/** 작업 생성·조회·수정·삭제 */
@Service
@RequiredArgsConstructor
public class CardService {

    private final CardRepository cardRepository;
    private final StatusService statusService;
    private final CommentRepository commentRepository;
    private final WorkspaceService workspaceService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<CardResponse> list(CurrentUser user, Long workspaceId) {
        workspaceService.requireReadAccess(user, workspaceId);
        List<Card> cards = cardRepository.findByWorkspaceIdOrderByCreatedAtAsc(workspaceId);

        // 댓글 수를 카드마다 세면 N+1이라, 한 번에 모아서 카드별로 집계
        Map<Long, Long> commentCounts = commentRepository.findByCardIdIn(cards.stream().map(Card::getId).toList())
                .stream().collect(Collectors.groupingBy(Comment::getCardId, Collectors.counting()));

        return cards.stream().map(card -> toResponse(card, commentCounts.getOrDefault(card.getId(), 0L))).toList();
    }

    @Transactional
    public CardResponse create(Long userId, Long workspaceId, CreateCardRequest request) {
        workspaceService.requireMember(userId, workspaceId);
        Long statusId = request.statusId() != null ? request.statusId() : defaultStatusId();
        Card card = Card.builder()
                .workspaceId(workspaceId)
                .statusId(statusId)
                .title(request.title())
                .description(request.description())
                .type(request.type() != null ? request.type() : "Task")
                .startDate(request.startDate())
                .dueDate(request.dueDate())
                .build();
        cardRepository.save(card);
        return publish("CREATED", toResponse(card));
    }

    @Transactional
    public CardResponse update(Long userId, Long cardId, UpdateCardRequest request) {
        Card card = getCard(cardId);
        workspaceService.requireMember(userId, card.getWorkspaceId());
        if (request.getTitle() != null) card.setTitle(request.getTitle());
        if (request.getDescription() != null) card.setDescription(request.getDescription());
        if (request.getType() != null) card.setType(request.getType());
        // 날짜는 null이 "지우기"를 뜻하므로 전달 여부로 판단
        if (request.isStartDatePresent()) card.setStartDate(request.getStartDate());
        if (request.isDueDatePresent()) card.setDueDate(request.getDueDate());
        boolean moved = request.getStatusId() != null && !Objects.equals(request.getStatusId(), card.getStatusId());
        if (moved) card.setStatusId(request.getStatusId());
        return publish(moved ? "MOVED" : "UPDATED", toResponse(card));
    }

    @Transactional
    public void delete(Long userId, Long cardId) {
        Card card = getCard(cardId);
        workspaceService.requireMember(userId, card.getWorkspaceId());
        CardResponse deleted = toResponse(card);
        cardRepository.delete(card);
        publish("DELETED", deleted);
    }

    /*************************************************************************
     * 목적 : 작업 변경 이벤트 발행 (실제 전송은 커밋 이후 RealtimeEventBroadcaster가 처리)
     * 이유 : 커밋 전에 보내면 롤백된 변경이 다른 사람 화면에 퍼질 수 있음
     * 파라미터
     * - type : CREATED | UPDATED | MOVED | DELETED
     * - card : 변경된 작업
     * 반환
     * - 전달받은 작업 응답 그대로
     *************************************************************************/
    private CardResponse publish(String type, CardResponse card) {
        eventPublisher.publishEvent(new CardEvent(type, card));
        return card;
    }

    /*************************************************************************
     * 목적 : 새 작업의 기본 상태(첫 번째 컬럼) 조회
     * 이유 : -
     * 파라미터
     * -
     * 반환
     * - 첫 번째 상태 id
     *************************************************************************/
    private Long defaultStatusId() {
        return statusService.list().stream().findFirst()
                .orElseThrow(() -> new EntityNotFoundException("상태가 존재하지 않습니다."))
                .id();
    }

    private Card getCard(Long cardId) {
        return cardRepository.findById(cardId).orElseThrow(() -> new EntityNotFoundException("카드를 찾을 수 없습니다."));
    }

    /*************************************************************************
     * 목적 : 작업 엔티티를 응답 형태로 변환 (ReleaseService도 사용)
     * 이유 : 단건 변환이라 댓글 수를 그때그때 세도 N+1 부담 없음
     * 파라미터
     * - card : 작업 엔티티
     * 반환
     * - 작업 응답
     *************************************************************************/
    public CardResponse toResponse(Card card) {
        return toResponse(card, commentRepository.countByCardId(card.getId()));
    }

    private CardResponse toResponse(Card card, long commentCount) {
        return new CardResponse(card.getId(), card.getWorkspaceId(), card.getStatusId(), card.getTitle(), card.getDescription(),
                card.getType(),
                card.getStartDate(), card.getDueDate(), card.getReleaseId(), commentCount,
                card.getCreatedAt(), card.getUpdatedAt());
    }
}
