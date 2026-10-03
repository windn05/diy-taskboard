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

/** 배포 생성·수정·취소 */
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

    /*************************************************************************
     * 목적 : 아직 배포되지 않은 작업 조회 (statusId로 거르기 가능)
     * 이유 : -
     * 파라미터
     * - user : 로그인 사용자
     * - workspaceId : 프로젝트 id
     * - statusId : 이 상태만 조회 (null이면 전체)
     * 반환
     * - 배포 후보 작업 목록
     *************************************************************************/
    @Transactional(readOnly = true)
    public List<CardResponse> candidates(CurrentUser user, Long workspaceId, Long statusId) {
        workspaceService.requireReadAccess(user, workspaceId);
        return cardRepository.findByWorkspaceIdAndReleaseIdIsNullOrderByCreatedAtAsc(workspaceId).stream()
                .filter(card -> statusId == null || statusId.equals(card.getStatusId()))
                .map(cardService::toResponse)
                .toList();
    }

    /*************************************************************************
     * 목적 : 지정한 작업을 묶어 새 배포 생성 (completedStatusId가 있으면 작업 상태도 변경)
     * 이유 : -
     * 파라미터
     * - userId : 요청한 사용자 id
     * - workspaceId : 프로젝트 id
     * - request : 버전·패치노트·작업 id 목록
     * 반환
     * - 생성된 배포
     *************************************************************************/
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

    /*************************************************************************
     * 목적 : 배포의 버전·패치노트 수정과 작업 추가 (이미 묶인 작업 빼기는 미지원)
     * 이유 : -
     * 파라미터
     * - userId : 요청한 사용자 id
     * - releaseId : 배포 id
     * - request : 바꿀 내용
     * 반환
     * - 수정된 배포
     *************************************************************************/
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

    /*************************************************************************
     * 목적 : 배포 취소 (포함됐던 작업은 미배포로 복귀, 작업 상태는 유지)
     * 이유 : -
     * 파라미터
     * - userId : 요청한 사용자 id
     * - releaseId : 배포 id
     * 반환
     * -
     *************************************************************************/
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
