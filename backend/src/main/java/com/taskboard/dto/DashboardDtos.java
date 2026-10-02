package com.taskboard.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 홈 화면(대시보드) 응답 */
public class DashboardDtos {

    /** 마감 임박 작업 한 줄 — 프로젝트 정보를 함께 담아 바로 이동 가능 */
    public record DueTask(
            Long cardId,
            Long workspaceId,
            String workspaceName,
            String title,
            String type,
            LocalDate dueDate) {
    }

    /** 홈 화면 달력에 막대로 그릴 작업. 시작일/마감일 중 하나라도 있어야 대상 */
    public record CalendarTask(
            Long cardId,
            Long workspaceId,
            String workspaceName,
            String title,
            LocalDate startDate,
            LocalDate dueDate) {
    }

    /** 최근 등록된 작업. 마감 임박 작업이 없을 때도 홈이 비어 보이지 않게 하는 용도 */
    public record RecentCard(
            Long cardId,
            Long workspaceId,
            String workspaceName,
            String title,
            String statusName,
            String type,
            LocalDateTime createdAt) {
    }

    /** cardCount는 그 배포에 묶인 작업 수 */
    public record RecentRelease(
            Long workspaceId,
            String workspaceName,
            String version,
            LocalDateTime releasedAt,
            int cardCount) {
    }

    /**
     * 홈 화면이 그리는 데이터만 포함.
     * 화면에서 쓰지 않는 집계 수치는 제외
     */
    public record DashboardResponse(
            List<DueTask> dueSoon,
            List<RecentCard> recentCards,
            List<RecentRelease> recentReleases,
            List<CalendarTask> calendarTasks) {
    }
}
