package com.taskboard.service;

import com.taskboard.config.TimeConfig;
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
 * 홈 화면 데이터. 프로젝트별로 나눠 요청하지 않도록 한 번에 모아서 응답.
 *
 * <p>완료 여부는 상태 이름이 아니라 <b>배포 여부</b>(releaseId)로 판단 — 상태 이름은 관리자가 바꿀 수 있음.
 * 예외로 "마감 임박"에서는 "개발완료" 상태(와 그 뒤 순서)도 제외. 이름 매칭이라 해당 상태가 없으면 미적용
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
            return new DashboardResponse(List.of(), List.of(), List.of(), List.of(), List.of());
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

        LocalDate threshold = LocalDate.now(TimeConfig.BUSINESS_ZONE).plusDays(DUE_SOON_DAYS);
        List<Card> dueSoon = openCards.stream()
                .filter(card -> card.getDueDate() != null && !card.getDueDate().isAfter(threshold))
                .filter(card -> developedOrder == null || statusOrders.getOrDefault(card.getStatusId(), 0) < developedOrder)
                .sorted(byDueDate())
                .toList();

        return new DashboardResponse(
                toTasks(mine, workspaceNames, statusNames),
                toTasks(dueSoon, workspaceNames, statusNames),
                recentCards(cards, workspaceNames, statusNames),
                recentReleases(workspaceIds, workspaceNames, cards),
                calendarTasks(cards, workspaceNames));
    }

    /** 달력용. 한 달 치를 모두 보여야 해서 개수 제한 없음, 배포된 작업도 포함 */
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

    /** 최근 등록 순. 배포된 작업도 포함 — 빼면 오래된 프로젝트에서는 목록이 비어버림 */
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
                        card.getCreatedAt()))
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
