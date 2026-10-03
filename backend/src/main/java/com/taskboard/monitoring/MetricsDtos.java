package com.taskboard.monitoring;

import java.util.List;

/** 모니터링 화면 응답 */
public class MetricsDtos {

    /** 최근 60분 합계. 분 단위 추이는 화면에서 쓰지 않아 응답에 담지 않음 */
    public record MetricsResponse(
            long totalRequests,
            long totalErrors,
            double avgResponseMs,
            int webSocketSessions) {
    }

    /** 자원 현황을 "서버 전체"와 "백엔드 프로세스"로 분리. 섞으면 컨테이너 한도가 서버 메모리처럼 보임 */
    public record SystemStatsResponse(HostStats host, BackendStats backend, DbStats db) {
    }

    /**
     * DB. 커넥션 풀은 CPU·메모리가 멀쩡해도 요청이 멈추는 대표적인 경로라 별도 확인
     * ({@code waiting}이 0보다 크면 이미 대기 발생).
     *
     * <p>풀이 HikariCP가 아니거나 Postgres가 아닌 환경(테스트의 H2)에서는 해당 항목이 null
     */
    public record DbStats(
            Integer poolActive,
            Integer poolIdle,
            Integer poolMax,
            Integer poolWaiting,
            Long sizeMb) {
    }

    /** 서버(VM) 전체. 백엔드·DB·프록시가 함께 쓰는 자원 */
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
     * 로컬처럼 컨테이너 밖에서 돌면 null
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
