package com.taskboard.service;

import com.taskboard.domain.Card;
import com.taskboard.domain.Release;
import com.taskboard.domain.Status;
import com.taskboard.dto.DashboardDtos.*;
import com.taskboard.dto.WorkspaceDtos.WorkspaceResponse;
import com.taskboard.repository.CardRepository;
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
 * <p>"끝난 일"은 기본적으로 상태 이름이 아니라 <b>배포 여부</b>로 판단한다. 상태 이름은 관리자가 언제든
 * 바꿀 수 있어서 코드가 의존하면 조용히 깨지지만, 배포된 작업은 정의상 마무리된 일이다.
 *
 * <p>다만 "마감 임박·지난 작업"만은 예외로 <b>"개발완료" 상태(그 이후 순서 포함)도 제외</b>한다 —
 * 개발은 끝나고 배포만 기다리는 작업까지 "마감이 급하다"고 띄우는 건 실제로 유용하지 않다는 요청 때문.
 * 이름 매칭이라 "개발완료"라는 상태가 없으면 이 예외는 적용되지 않는다(기존 배포 여부 기준만 남음).
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

    @Transactional(readOnly = true)
    public DashboardResponse load(CurrentUser user) {
        List<WorkspaceResponse> workspaces = workspaceService.listMine(user);
        if (workspaces.isEmpty()) {
            return new DashboardResponse(List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
        }

        List<Long> workspaceIds = workspaces.stream().map(WorkspaceResponse::id).toList();
        Map<Long, String> workspaceNames = workspaces.stream()
                .collect(Collectors.toMap(WorkspaceResponse::id, WorkspaceResponse::name));
        List<Status> statuses = statusRepository.findAll();
        Map<Long, String> statusNames = statuses.stream()
                .collect(Collectors.toMap(Status::getId, Status::getName));
        Map<Long, Integer> statusOrders = statuses.stream()
                .collect(Collectors.toMap(Status::getId, Status::getOrder));
        Integer developedOrder = statuses.stream()
                .filter(s -> "개발완료".equals(s.getName().trim()))
                .map(Status::getOrder)
                .min(Integer::compareTo)
                .orElse(null);

        List<Card> cards = cardRepository.findByWorkspaceIdIn(workspaceIds);
        List<Card> openCards = cards.stream().filter(card -> card.getReleaseId() == null).toList();

        List<Card> mine = openCards.stream()
                .filter(card -> user.getId().equals(card.getAssigneeId()))
                .sorted(byDueDate())
                .toList();

        LocalDate threshold = LocalDate.now().plusDays(DUE_SOON_DAYS);
        List<Card> dueSoon = openCards.stream()
                .filter(card -> card.getDueDate() != null && !card.getDueDate().isAfter(threshold))
                .filter(card -> developedOrder == null || statusOrders.getOrDefault(card.getStatusId(), 0) < developedOrder)
                .sorted(byDueDate())
                .toList();

        return new DashboardResponse(
                toTasks(mine, workspaceNames, statusNames),
                toTasks(dueSoon, workspaceNames, statusNames),
                recentCards(cards, workspaceNames, statusNames),
                projectSummaries(workspaces, cards, statusNames),
                recentReleases(workspaceIds, workspaceNames, cards),
                calendarTasks(cards, workspaceNames));
    }

    /**
     * 달력 바에는 개수 제한을 두지 않는다 — 한 달 치를 다 봐야 하는 화면이라 8개로 자르면 의미가 없다.
     * 배포된 작업도 시작일·마감일은 지난 일정이니 그대로 보여준다(openCards로 거르지 않는다).
     */
    private List<CalendarTask> calendarTasks(List<Card> cards, Map<Long, String> workspaceNames) {
        return cards.stream()
                .filter(card -> card.getStartDate() != null || card.getDueDate() != null)
                .map(card -> new CalendarTask(
                        card.getId(),
                        card.getWorkspaceId(),
                        workspaceNames.get(card.getWorkspaceId()),
                        card.getTitle(),
                        card.getPriority().name(),
                        card.getStartDate(),
                        card.getDueDate()))
                .toList();
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

    /**
     * 최근 등록된 작업. 마감이 임박한 일이 없는 시기에도 홈이 비어 보이지 않게 채우는 자리다.
     * 배포된 작업도 뺀다면 오래된 프로젝트에서는 또 비어버리므로 거르지 않는다.
     */
    private List<RecentCard> recentCards(List<Card> cards, Map<Long, String> workspaceNames,
                                          Map<Long, String> statusNames) {
        return cards.stream()
                .sorted(Comparator.comparing(Card::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(LIST_LIMIT)
                .map(card -> new RecentCard(
                        card.getId(),
                        card.getWorkspaceId(),
                        workspaceNames.get(card.getWorkspaceId()),
                        card.getTitle(),
                        statusNames.getOrDefault(card.getStatusId(), "-"),
                        card.getPriority().name(),
                        card.getCreatedAt() == null ? null : card.getCreatedAt().toLocalDate()))
                .toList();
    }

    /** 프로젝트가 늘어날수록 화면이 길어지므로, 화면에서 위쪽 몇 개만 보여줄 수 있게 미결 작업이 많은 순으로 정렬해 둔다. */
    private List<ProjectSummary> projectSummaries(List<WorkspaceResponse> workspaces, List<Card> cards,
                                                   Map<Long, String> statusNames) {
        Map<Long, List<Card>> byWorkspace = cards.stream().collect(Collectors.groupingBy(Card::getWorkspaceId));
        Map<Long, Long> openCountByWorkspace = cards.stream()
                .filter(card -> card.getReleaseId() == null)
                .collect(Collectors.groupingBy(Card::getWorkspaceId, Collectors.counting()));

        return workspaces.stream()
                .sorted(Comparator.comparing((WorkspaceResponse w) -> openCountByWorkspace.getOrDefault(w.id(), 0L))
                        .reversed()
                        .thenComparing(WorkspaceResponse::name))
                .map(workspace -> {
                    List<Card> workspaceCards = byWorkspace.getOrDefault(workspace.id(), List.of());
                    List<StatusCount> counts = workspaceCards.stream()
                            .collect(Collectors.groupingBy(Card::getStatusId, Collectors.counting()))
                            .entrySet().stream()
                            .map(entry -> new StatusCount(entry.getKey(),
                                    statusNames.getOrDefault(entry.getKey(), "-"), entry.getValue()))
                            // 이름순이 아니라 상태 id순 — 막대를 업무 흐름 순서대로 쌓기 위함이다.
                            .sorted(Comparator.comparing(StatusCount::statusId))
                            .toList();
                    return new ProjectSummary(workspace.id(), workspace.name(), workspaceCards.size(), counts);
                })
                .toList();
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
