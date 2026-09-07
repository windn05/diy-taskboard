package com.taskboard.service;

import com.taskboard.dto.MetricsDtos.MetricsResponse;
import com.taskboard.dto.MetricsDtos.MinutePoint;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

/** 최근 WINDOW_MINUTES 분의 요청 지표를 분 단위 버킷으로 메모리에 집계한다. 단일 서버 전제. */
@Service
public class RequestMetricsService {

    private static final int WINDOW_MINUTES = 60;
    private static final DateTimeFormatter MINUTE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

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
        buckets.keySet().removeIf(key -> key < minute - WINDOW_MINUTES);
    }

    public MetricsResponse snapshot() {
        long now = currentMinute();
        List<MinutePoint> series = new ArrayList<>();
        long totalRequests = 0;
        long totalErrors = 0;
        long totalDuration = 0;

        for (long minute = now - WINDOW_MINUTES + 1; minute <= now; minute++) {
            Bucket bucket = buckets.get(minute);
            long requests = bucket != null ? bucket.requests.sum() : 0;
            long errors = bucket != null ? bucket.errors.sum() : 0;
            long duration = bucket != null ? bucket.totalDurationMs.sum() : 0;
            series.add(new MinutePoint(formatMinute(minute), requests, errors, average(duration, requests)));
            totalRequests += requests;
            totalErrors += errors;
            totalDuration += duration;
        }

        return new MetricsResponse(series, totalRequests, totalErrors,
                average(totalDuration, totalRequests), sessionCounter.active());
    }

    private double average(long total, long count) {
        return count == 0 ? 0 : Math.round((double) total / count * 10) / 10.0;
    }

    private long currentMinute() {
        return Instant.now().getEpochSecond() / 60;
    }

    private String formatMinute(long minute) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(minute * 60), ZoneId.systemDefault()).format(MINUTE_FORMAT);
    }
}
