package com.taskboard.release;

import com.taskboard.card.Card;
import com.taskboard.card.CardDtos.CardResponse;
import com.taskboard.card.CardRepository;
import com.taskboard.card.CardService;
import com.taskboard.global.exception.EntityNotFoundException;
import com.taskboard.global.security.CurrentUser;
import com.taskboard.release.ReleaseDtos.*;
import com.taskboard.workspace.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 배포(버전) 관리. 작업을 배포에 묶으면 releaseId가 채워지고, 이후로는 완료된 작업으로 취급.
 * 한 작업은 한 배포에만 속할 수 있음
 */
@Service
@RequiredArgsConstructor
public class ReleaseService {

    private final ReleaseRepository releaseRepository;
    private final CardRepository cardRepository;
    private final WorkspaceService workspaceService;
    private final CardService cardService;

    @Transactional(readOnly = true)
    public List<ReleaseResponse> list(CurrentUser user, Long workspaceId) {
        workspaceService.requireReadAccess(user, workspaceId);
        List<Release> releases = releaseRepository.findByWorkspaceIdOrderByIdDesc(workspaceId);
        if (releases.isEmpty()) return List.of();

        // 배포별 작업을 한 번에 조회 (배포마다 조회하면 N+1)
        Map<Long, List<ReleasedCard>> cardsByRelease = cardRepository
                .findByReleaseIdInOrderByCreatedAtAsc(releases.stream().map(Release::getId).toList()).stream()
                .collect(Collectors.groupingBy(Card::getReleaseId,
                        Collectors.mapping(this::toReleasedCard, Collectors.toList())));

        return releases.stream()
                .map(r -> toResponse(r, cardsByRelease.getOrDefault(r.getId(), List.of())))
                .toList();
    }

    /** 아직 배포되지 않은 작업. 상태 필터는 화면에서 전달 */
    @Transactional(readOnly = true)
    public List<CardResponse> candidates(CurrentUser user, Long workspaceId, Long statusId) {
        workspaceService.requireReadAccess(user, workspaceId);
        return cardRepository.findByWorkspaceIdAndReleaseIdIsNullOrderByCreatedAtAsc(workspaceId).stream()
                .filter(card -> statusId == null || statusId.equals(card.getStatusId()))
                .map(cardService::toResponse)
                .toList();
    }

    /** 지정한 작업을 새 배포에 연결. completedStatusId가 있으면 작업 상태도 함께 변경 */
    @Transactional
    public ReleaseResponse create(Long userId, Long workspaceId, CreateReleaseRequest request) {
        workspaceService.requireMember(userId, workspaceId);
        releaseRepository.findByWorkspaceIdAndVersion(workspaceId, request.version()).ifPresent(r -> {
            throw new IllegalArgumentException("이미 존재하는 버전입니다.");
        });

        // 전부 이 프로젝트의 미배포 작업이어야 함
        List<Card> cards = cardRepository.findAllById(request.cardIds());
        if (cards.size() != request.cardIds().size()) {
            throw new EntityNotFoundException("존재하지 않는 작업이 포함되어 있습니다.");
        }
        for (Card card : cards) {
            if (!workspaceId.equals(card.getWorkspaceId())) {
                throw new IllegalArgumentException("다른 프로젝트의 작업은 포함할 수 없습니다.");
            }
            if (card.getReleaseId() != null) {
                throw new IllegalArgumentException("이미 배포된 작업이 포함되어 있습니다.");
            }
        }

        Release release = releaseRepository.save(Release.builder()
                .workspaceId(workspaceId)
                .version(request.version())
                .notes(request.notes())
                .build());

        cards.forEach(card -> {
            card.setReleaseId(release.getId());
            if (request.completedStatusId() != null) card.setStatusId(request.completedStatusId());
        });

        return toResponse(release, cards.stream().map(this::toReleasedCard).toList());
    }

    /** 버전·패치노트 수정과 작업 추가. 이미 묶인 작업을 빼는 기능은 없음 */
    @Transactional
    public ReleaseResponse update(Long userId, Long releaseId, UpdateReleaseRequest request) {
        Release release = getRelease(releaseId);
        workspaceService.requireMember(userId, release.getWorkspaceId());

        if (request.version() != null && !request.version().isBlank()
                && !request.version().equals(release.getVersion())) {
            releaseRepository.findByWorkspaceIdAndVersion(release.getWorkspaceId(), request.version())
                    .ifPresent(r -> {
                        throw new IllegalArgumentException("이미 존재하는 버전입니다.");
                    });
            release.setVersion(request.version());
        }
        if (request.notes() != null) release.setNotes(request.notes());

        if (request.addCardIds() != null && !request.addCardIds().isEmpty()) {
            List<Card> toAdd = cardRepository.findAllById(request.addCardIds());
            if (toAdd.size() != request.addCardIds().size()) {
                throw new EntityNotFoundException("존재하지 않는 작업이 포함되어 있습니다.");
            }
            for (Card card : toAdd) {
                if (!release.getWorkspaceId().equals(card.getWorkspaceId())) {
                    throw new IllegalArgumentException("다른 프로젝트의 작업은 포함할 수 없습니다.");
                }
                if (card.getReleaseId() != null) {
                    throw new IllegalArgumentException("이미 배포된 작업이 포함되어 있습니다.");
                }
            }
            toAdd.forEach(card -> {
                card.setReleaseId(release.getId());
                if (request.completedStatusId() != null) card.setStatusId(request.completedStatusId());
            });
        }

        List<ReleasedCard> cards = cardRepository
                .findByReleaseIdInOrderByCreatedAtAsc(List.of(releaseId)).stream()
                .map(this::toReleasedCard)
                .toList();
        return toResponse(release, cards);
    }

    /** 배포를 취소하면 포함됐던 작업은 미배포 상태로 복귀. 작업 상태는 되돌리지 않음 */
    @Transactional
    public void delete(Long userId, Long releaseId) {
        Release release = getRelease(releaseId);
        workspaceService.requireMember(userId, release.getWorkspaceId());
        cardRepository.findByReleaseIdInOrderByCreatedAtAsc(List.of(releaseId))
                .forEach(card -> card.setReleaseId(null));
        releaseRepository.delete(release);
    }

    private Release getRelease(Long releaseId) {
        return releaseRepository.findById(releaseId)
                .orElseThrow(() -> new EntityNotFoundException("배포를 찾을 수 없습니다."));
    }

    private ReleasedCard toReleasedCard(Card card) {
        return new ReleasedCard(card.getId(), card.getTitle(), card.getType());
    }

    private ReleaseResponse toResponse(Release release, List<ReleasedCard> cards) {
        return new ReleaseResponse(release.getId(), release.getWorkspaceId(), release.getVersion(),
                release.getNotes(), release.getReleasedAt(), cards);
    }
}
