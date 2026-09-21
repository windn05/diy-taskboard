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

    /** 자원 현황을 "서버 전체"와 "백엔드 프로세스"로 나눠 준다. 섞으면 컨테이너 한도가 서버 메모리처럼 보인다. */
    public record SystemStatsResponse(HostStats host, BackendStats backend, DbStats db) {
    }

    /**
     * DB. 커넥션 풀은 CPU·메모리가 멀쩡해도 요청이 멈추는 대표적인 경로라 따로 본다
     * ({@code waiting}이 0보다 크면 이미 대기가 걸린 것이다).
     *
     * <p>풀이 HikariCP가 아니거나 Postgres가 아닌 환경(테스트의 H2)에서는 해당 항목이 null이다.
     */
    public record DbStats(
            Integer poolActive,
            Integer poolIdle,
            Integer poolMax,
            Integer poolWaiting,
            Long sizeMb) {
    }

    /** 서버(VM) 전체. 백엔드·DB·프록시가 함께 쓰는 자원이다. */
    public record HostStats(
            double cpuPercent,
            int cpuCores,
            long memUsedMb,
            long memTotalMb,
            long diskUsedGb,
            long diskTotalGb) {
    }

    /**
     * 백엔드 프로세스. 컨테이너 메모리는 cgroup으로 제한된 컨테이너 안에서만 값이 있고,
     * 로컬처럼 컨테이너 밖에서 돌면 null이다.
     */
    public record BackendStats(
            double processCpuPercent,
            Long containerMemUsedMb,
            Long containerMemLimitMb,
            long heapUsedMb,
            long heapMaxMb,
            long uptimeSeconds) {
    }
}
