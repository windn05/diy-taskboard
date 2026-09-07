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
}
