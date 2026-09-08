package com.taskboard.dto;

import java.util.List;

public class MetricsDtos {

    /** minute는 ISO-8601 분 단위 시각. */
    public record MinutePoint(String minute, long requests, long errors, double avgResponseMs) {
    }

    public record MetricsResponse(
            List<MinutePoint> series,
            long totalRequests,
            long totalErrors,
            double avgResponseMs,
            int webSocketSessions) {
    }

    /** 서버(VM/컨테이너) 자원 현황. JDK 내장 MXBean만 사용— 별도 의존성 없음. */
    public record SystemStatsResponse(
            double cpuLoadPercent,
            int availableProcessors,
            long heapUsedMb,
            long heapMaxMb,
            long systemMemUsedMb,
            long systemMemTotalMb,
            long diskUsedGb,
            long diskTotalGb,
            long uptimeSeconds) {
    }
}
