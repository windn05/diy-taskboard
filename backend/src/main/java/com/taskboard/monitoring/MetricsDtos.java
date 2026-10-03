package com.taskboard.monitoring;

import java.util.List;

/** 모니터링 화면 응답 */
public class MetricsDtos {

    /** 요청 지표 응답 (최근 60분 합계) */
    public record MetricsResponse(
            long totalRequests,
            long totalErrors,
            double avgResponseMs,
            int webSocketSessions) {
    }

    /** 자원 현황 응답 (서버 전체·백엔드·DB) */
    public record SystemStatsResponse(HostStats host, BackendStats backend, DbStats db) {
    }

    /** DB 지표 (조회할 수 없는 항목은 null) */
    public record DbStats(
            Integer poolActive,
            Integer poolIdle,
            Integer poolMax,
            Integer poolWaiting,
            Long sizeMb) {
    }

    /** 서버(VM) 전체 자원 */
    public record HostStats(
            double cpuPercent,
            int cpuCores,
            long memUsedMb,
            long memTotalMb,
            long diskUsedGb,
            long diskTotalGb) {
    }

    /** 백엔드 프로세스 자원 (컨테이너 밖에서는 컨테이너 메모리가 null) */
    public record BackendStats(
            double processCpuPercent,
            Long containerMemUsedMb,
            Long containerMemLimitMb,
            long heapUsedMb,
            long heapMaxMb,
            long uptimeSeconds) {
    }
}
