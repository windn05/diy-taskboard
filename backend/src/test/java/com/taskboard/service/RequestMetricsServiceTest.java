package com.taskboard.service;

import com.taskboard.dto.MetricsDtos.MetricsResponse;
import com.taskboard.dto.MetricsDtos.MinutePoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestMetricsServiceTest {

    private WebSocketSessionCounter sessionCounter;
    private RequestMetricsService metricsService;

    @BeforeEach
    void setUp() {
        sessionCounter = mock(WebSocketSessionCounter.class);
        metricsService = new RequestMetricsService(sessionCounter);
    }

    @Test
    void 요청_수와_평균_응답시간을_집계한다() {
        metricsService.record(10, 200);
        metricsService.record(30, 200);

        MetricsResponse snapshot = metricsService.snapshot();

        assertThat(snapshot.totalRequests()).isEqualTo(2);
        assertThat(snapshot.avgResponseMs()).isEqualTo(20.0);
    }

    @Test
    void 상태코드_400_이상만_에러로_센다() {
        metricsService.record(5, 200);
        metricsService.record(5, 301);
        metricsService.record(5, 400);
        metricsService.record(5, 403);
        metricsService.record(5, 500);

        MetricsResponse snapshot = metricsService.snapshot();

        assertThat(snapshot.totalRequests()).isEqualTo(5);
        assertThat(snapshot.totalErrors()).isEqualTo(3);
    }

    @Test
    void 요청이_없으면_평균은_0이다() {
        assertThat(metricsService.snapshot().avgResponseMs()).isZero();
    }

    @Test
    void 시계열은_항상_60분치를_돌려준다() {
        metricsService.record(10, 200);

        MetricsResponse snapshot = metricsService.snapshot();

        assertThat(snapshot.series()).hasSize(60);
        assertThat(snapshot.series()).last().extracting(MinutePoint::requests).isEqualTo(1L);
        // 트래픽이 없던 구간은 0으로 채워져 그래프가 끊기지 않음
        assertThat(snapshot.series()).first().extracting(MinutePoint::requests).isEqualTo(0L);
    }

    @Test
    void WebSocket_세션_수를_함께_보고한다() {
        when(sessionCounter.active()).thenReturn(3);

        assertThat(metricsService.snapshot().webSocketSessions()).isEqualTo(3);
    }
}
