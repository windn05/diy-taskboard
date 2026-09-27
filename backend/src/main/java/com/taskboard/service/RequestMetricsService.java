package com.taskboard.service;

import com.taskboard.dto.MetricsDtos.MetricsResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/** 최근 WINDOW_MINUTES 분의 요청 지표를 분 단위 버킷으로 메모리에 집계. 단일 서버 전제 */
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

    /** 최근 WINDOW_MINUTES 분의 버킷만 합산 — 그보다 오래된 버킷은 record에서 이미 정리됨 */
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

    /** epoch 기준 분 번호. 버킷 키로 사용 */
    private long currentMinute() {
        return Instant.now().getEpochSecond() / 60;
    }
}
