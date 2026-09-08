package com.taskboard.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class DashboardDtos {

    /** 내 담당 작업 한 줄 — 어느 프로젝트인지 함께 보여줘야 바로 이동할 수 있다. */
    public record MyTask(
            Long cardId,
            Long workspaceId,
            String workspaceName,
            String title,
            String statusName,
            String priority,
            LocalDate dueDate) {
    }

    /** 홈 화면 달력에 바 형태로 그릴 작업. 시작일/마감일이 하나라도 있어야 후보가 된다. */
    public record CalendarTask(
            Long cardId,
            Long workspaceId,
            String workspaceName,
            String title,
            String priority,
            LocalDate startDate,
            LocalDate dueDate) {
    }

    public record StatusCount(Long statusId, String statusName, long count) {
    }

    public record ProjectSummary(
            Long workspaceId,
            String name,
            long totalCards,
            List<StatusCount> statusCounts) {
    }

    public record RecentRelease(
            Long workspaceId,
            String workspaceName,
            String version,
            LocalDateTime releasedAt,
            int cardCount) {
    }

    public record DashboardResponse(
            long myTaskCount,
            long dueSoonCount,
            long unreadNotificationCount,
            List<MyTask> myTasks,
            List<MyTask> dueSoon,
            List<ProjectSummary> projects,
            List<RecentRelease> recentReleases,
            List<CalendarTask> calendarTasks) {
    }
}
