package com.taskboard.dashboard;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 홈 화면(대시보드) 응답 */
public class DashboardDtos {

    /** 마감 임박 작업 한 줄 */
    public record DueTask(
            Long cardId,
            Long workspaceId,
            String workspaceName,
            String title,
            String type,
            LocalDate dueDate) {
    }

    /** 홈 달력에 막대로 그릴 작업 */
    public record CalendarTask(
            Long cardId,
            Long workspaceId,
            String workspaceName,
            String title,
            LocalDate startDate,
            LocalDate dueDate) {
    }

    /** 최근 등록된 작업 한 줄 */
    public record RecentCard(
            Long cardId,
            Long workspaceId,
            String workspaceName,
            String title,
            String statusName,
            String type,
            LocalDateTime createdAt) {
    }

    /** 최근 배포 한 줄 */
    public record RecentRelease(
            Long workspaceId,
            String workspaceName,
            String version,
            LocalDateTime releasedAt,
            int cardCount) {
    }

    /** 홈 화면 전체 응답 */
    public record DashboardResponse(
            List<DueTask> dueSoon,
            List<RecentCard> recentCards,
            List<RecentRelease> recentReleases,
            List<CalendarTask> calendarTasks) {
    }
}
