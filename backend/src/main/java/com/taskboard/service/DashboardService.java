package com.taskboard.service;

import com.taskboard.domain.Card;
import com.taskboard.domain.Release;
import com.taskboard.domain.Status;
import com.taskboard.dto.DashboardDtos.*;
import com.taskboard.dto.WorkspaceDtos.WorkspaceResponse;
import com.taskboard.repository.CardRepository;
import com.taskboard.repository.NotificationRepository;
import com.taskboard.repository.ReleaseRepository;
import com.taskboard.repository.StatusRepository;
import com.taskboard.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 첫 화면에서 쓸 요약. 프로젝트마다 따로 조회하면 요청이 N개가 되므로 한 번에 모아서 내려준다.
 *
 * <p>"끝난 일"은 상태 이름이 아니라 <b>배포 여부</b>로 판단한다. 상태 이름은 관리자가 언제든
 * 바꿀 수 있어서 코드가 의존하면 조용히 깨지지만, 배포된 작업은 정의상 마무리된 일이다.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final int LIST_LIMIT = 8;
    private static final int RELEASE_LIMIT = 5;
    private static final int DUE_SOON_DAYS = 7;

    private final WorkspaceService workspaceService;
    private final CardRepository cardRepository;
    private final StatusRepository statusRepository;
    private final ReleaseRepository releaseRepository;
    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public DashboardResponse load(CurrentUser user) {
        List<WorkspaceResponse> workspaces = workspaceService.listMine(user);
        long unread = notificationRepository.countByUserIdAndReadFalse(user.getId());
        if (workspaces.isEmpty()) {
            return new DashboardResponse(0, 0, unread, List.of(), List.of(), List.of(), List.of());
        }

        List<Long> workspaceIds = workspaces.stream().map(WorkspaceResponse::id).toList();
        Map<Long, String> workspaceNames = workspaces.stream()
                .collect(Collectors.toMap(WorkspaceResponse::id, WorkspaceResponse::name));
        Map<Long, String> statusNames = statusRepository.findAll().stream()
                .collect(Collectors.toMap(Status::getId, Status::getName));

        List<Card> cards = cardRepository.findByWorkspaceIdIn(workspaceIds);
        List<Card> openCards = cards.stream().filter(card -> card.getReleaseId() == null).toList();

        List<Card> mine = openCards.stream()
                .filter(card -> user.getId().equals(card.getAssigneeId()))
                .sorted(byDueDate())
                .toList();

        LocalDate threshold = LocalDate.now().plusDays(DUE_SOON_DAYS);
        List<Card> dueSoon = openCards.stream()
                .filter(card -> card.getDueDate() != null && !card.getDueDate().isAfter(threshold))
                .sorted(byDueDate())
                .toList();

        return new DashboardResponse(
                mine.size(),
                dueSoon.size(),
                unread,
                toTasks(mine, workspaceNames, statusNames),
                toTasks(dueSoon, workspaceNames, statusNames),
                projectSummaries(workspaces, cards, statusNames),
                recentReleases(workspaceIds, workspaceNames, cards));
    }

    private Comparator<Card> byDueDate() {
        return Comparator.comparing(Card::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private List<MyTask> toTasks(List<Card> cards, Map<Long, String> workspaceNames, Map<Long, String> statusNames) {
        return cards.stream()
                .limit(LIST_LIMIT)
                .map(card -> new MyTask(
                        card.getId(),
                        card.getWorkspaceId(),
                        workspaceNames.get(card.getWorkspaceId()),
                        card.getTitle(),
                        statusNames.getOrDefault(card.getStatusId(), "-"),
                        card.getPriority().name(),
                        card.getDueDate()))
                .toList();
    }

    private List<ProjectSummary> projectSummaries(List<WorkspaceResponse> workspaces, List<Card> cards,
                                                   Map<Long, String> statusNames) {
        Map<Long, List<Card>> byWorkspace = cards.stream().collect(Collectors.groupingBy(Card::getWorkspaceId));

        return workspaces.stream().map(workspace -> {
            List<Card> workspaceCards = byWorkspace.getOrDefault(workspace.id(), List.of());
            List<StatusCount> counts = workspaceCards.stream()
                    .collect(Collectors.groupingBy(Card::getStatusId, Collectors.counting()))
                    .entrySet().stream()
                    .map(entry -> new StatusCount(entry.getKey(),
                            statusNames.getOrDefault(entry.getKey(), "-"), entry.getValue()))
                    .sorted(Comparator.comparing(StatusCount::statusName))
                    .toList();
            return new ProjectSummary(workspace.id(), workspace.name(), workspaceCards.size(), counts);
        }).toList();
    }

    private List<RecentRelease> recentReleases(List<Long> workspaceIds, Map<Long, String> workspaceNames,
                                                List<Card> cards) {
        Map<Long, Long> cardCountByRelease = cards.stream()
                .filter(card -> card.getReleaseId() != null)
                .collect(Collectors.groupingBy(Card::getReleaseId, Collectors.counting()));

        return releaseRepository.findByWorkspaceIdInOrderByIdDesc(workspaceIds).stream()
                .limit(RELEASE_LIMIT)
                .map(release -> new RecentRelease(
                        release.getWorkspaceId(),
                        workspaceNames.get(release.getWorkspaceId()),
                        release.getVersion(),
                        release.getReleasedAt(),
                        cardCountByRelease.getOrDefault(release.getId(), 0L).intValue()))
                .toList();
    }
}
