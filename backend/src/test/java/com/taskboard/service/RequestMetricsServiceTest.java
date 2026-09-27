package com.taskboard.service;

import com.taskboard.dto.MetricsDtos.MetricsResponse;
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
    void 합계는_최근_구간의_요청만_센다() {
        metricsService.record(10, 200);
        metricsService.record(30, 500);

        MetricsResponse snapshot = metricsService.snapshot();

        assertThat(snapshot.totalRequests()).isEqualTo(2L);
        assertThat(snapshot.totalErrors()).isEqualTo(1L);
        assertThat(snapshot.avgResponseMs()).isEqualTo(20.0);
    }

    @Test
    void WebSocket_세션_수를_함께_보고한다() {
        when(sessionCounter.active()).thenReturn(3);

        assertThat(metricsService.snapshot().webSocketSessions()).isEqualTo(3);
    }
}
