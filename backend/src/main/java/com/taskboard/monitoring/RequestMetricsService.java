package com.taskboard.monitoring;

import com.taskboard.monitoring.MetricsDtos.MetricsResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/** 최근 60분 요청 지표를 분 단위로 메모리에 집계 */
@Service
public class RequestMetricsService {

    private static final int WINDOW_MINUTES = 60;

    private final Map<Long, Bucket> buckets = new ConcurrentHashMap<>();
    private final WebSocketSessionCounter sessionCounter;

    public RequestMetricsService(WebSocketSessionCounter sessionCounter) {
        this.sessionCounter = sessionCounter;
    }

    private static class Bucket {
        final LongAdder requests = new LongAdder();
        final LongAdder totalDurationMs = new LongAdder();
        final LongAdder errors = new LongAdder();
    }

    public void record(long durationMs, int status) {
        long minute = currentMinute();
        Bucket bucket = buckets.computeIfAbsent(minute, key -> new Bucket());
        bucket.requests.increment();
        bucket.totalDurationMs.add(durationMs);
        if (status >= 400) bucket.errors.increment();
        // 별도 스케줄러 없이 기록할 때마다 오래된 버킷 정리
        buckets.keySet().removeIf(key -> key < minute - WINDOW_MINUTES);
    }

    /*************************************************************************
     * 목적 : 최근 60분 버킷을 합산해 요청 지표 생성
     * 이유 : -
     * 파라미터
     * -
     * 반환
     * - 요청 수·에러 수·평균 응답 시간·WebSocket 세션 수
     *************************************************************************/
    public MetricsResponse snapshot() {
        long oldest = currentMinute() - WINDOW_MINUTES + 1;
        long totalRequests = 0;
        long totalErrors = 0;
        long totalDuration = 0;

        for (Map.Entry<Long, Bucket> entry : buckets.entrySet()) {
            if (entry.getKey() < oldest) continue;
            totalRequests += entry.getValue().requests.sum();
            totalErrors += entry.getValue().errors.sum();
            totalDuration += entry.getValue().totalDurationMs.sum();
        }

        return new MetricsResponse(totalRequests, totalErrors,
                average(totalDuration, totalRequests), sessionCounter.active());
    }

    private double average(long total, long count) {
        return count == 0 ? 0 : Math.round((double) total / count * 10) / 10.0;
    }

    /*************************************************************************
     * 목적 : 현재 시각의 분 번호 계산 (버킷 키)
     * 이유 : -
     * 파라미터
     * -
     * 반환
     * - epoch 기준 분 번호
     *************************************************************************/
    private long currentMinute() {
        return Instant.now().getEpochSecond() / 60;
    }
}
