package com.taskboard.dashboard;

import com.taskboard.card.Card;
import com.taskboard.card.CardRepository;
import com.taskboard.dashboard.DashboardDtos.*;
import com.taskboard.global.config.TimeConfig;
import com.taskboard.global.security.CurrentUser;
import com.taskboard.release.ReleaseRepository;
import com.taskboard.status.Status;
import com.taskboard.status.StatusRepository;
import com.taskboard.workspace.WorkspaceDtos.WorkspaceResponse;
import com.taskboard.workspace.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 홈 화면 데이터 조회 */
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
            return new DashboardResponse(List.of(), List.of(), List.of(), List.of());
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

        LocalDate threshold = LocalDate.now(TimeConfig.BUSINESS_ZONE).plusDays(DUE_SOON_DAYS);
        List<Card> dueSoon = openCards.stream()
                .filter(card -> card.getDueDate() != null && !card.getDueDate().isAfter(threshold))
                .filter(card -> developedOrder == null || statusOrders.getOrDefault(card.getStatusId(), 0) < developedOrder)
                .sorted(byDueDate())
                .toList();

        return new DashboardResponse(
                toTasks(dueSoon, workspaceNames),
                recentCards(cards, workspaceNames, statusNames),
                recentReleases(workspaceIds, workspaceNames, cards),
                calendarTasks(cards, workspaceNames));
    }

    /*************************************************************************
     * 목적 : 달력에 그릴 작업 목록 생성 (배포된 작업 포함)
     * 이유 : 한 달 치를 모두 보여야 해서 다른 목록과 달리 개수 제한 없음
     * 파라미터
     * - cards : 볼 수 있는 프로젝트의 작업
     * - workspaceNames : 프로젝트 id → 이름
     * 반환
     * - 달력용 작업 목록
     *************************************************************************/
    private List<CalendarTask> calendarTasks(List<Card> cards, Map<Long, String> workspaceNames) {
        return cards.stream()
                .filter(card -> card.getStartDate() != null || card.getDueDate() != null)
                .map(card -> new CalendarTask(
                        card.getId(),
                        card.getWorkspaceId(),
                        workspaceNames.get(card.getWorkspaceId()),
                        card.getTitle(),
                        card.getStartDate(),
                        card.getDueDate()))
                .toList();
    }

    private Comparator<Card> byDueDate() {
        return Comparator.comparing(Card::getDueDate, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private List<DueTask> toTasks(List<Card> cards, Map<Long, String> workspaceNames) {
        return cards.stream()
                .limit(LIST_LIMIT)
                .map(card -> new DueTask(
                        card.getId(),
                        card.getWorkspaceId(),
                        workspaceNames.get(card.getWorkspaceId()),
                        card.getTitle(),
                        card.getType(),
                        card.getDueDate()))
                .toList();
    }

    /*************************************************************************
     * 목적 : 최근 등록한 작업 목록 생성 (배포된 작업 포함)
     * 이유 : 배포된 작업을 빼면 오래된 프로젝트에서는 목록이 비어버림
     * 파라미터
     * - cards : 볼 수 있는 프로젝트의 작업
     * - workspaceNames : 프로젝트 id → 이름
     * - statusNames : 상태 id → 이름
     * 반환
     * - 최근 등록순 작업 목록
     *************************************************************************/
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
                        card.getType(),
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
