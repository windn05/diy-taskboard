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

    /** 최근에 등록된 작업. 마감이 임박한 일이 없을 때도 홈이 비어 보이지 않게 하는 용도다. */
    public record RecentCard(
            Long cardId,
            Long workspaceId,
            String workspaceName,
            String title,
            String statusName,
            String priority,
            LocalDate createdDate) {
    }

    public record RecentRelease(
            Long workspaceId,
            String workspaceName,
            String version,
            LocalDateTime releasedAt,
            int cardCount) {
    }

    /**
     * 홈 화면이 그리는 것만 담는다. 예전에는 화면에서 쓰지 않는 집계 수치도 함께 보냈는데,
     * 달력을 넣으면서 해당 패널이 빠진 뒤로도 응답에만 남아 있었다.
     */
    public record DashboardResponse(
            List<MyTask> myTasks,
            List<MyTask> dueSoon,
            List<RecentCard> recentCards,
            List<RecentRelease> recentReleases,
            List<CalendarTask> calendarTasks) {
    }
}
