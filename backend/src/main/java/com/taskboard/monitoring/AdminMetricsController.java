package com.taskboard.monitoring;

import com.taskboard.monitoring.MetricsDtos.MetricsResponse;
import com.taskboard.monitoring.MetricsDtos.SystemStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 모니터링 화면의 지표 조회. 화면이 주기적으로 폴링 */
@RestController
@RequestMapping("/admin/metrics")
@RequiredArgsConstructor
public class AdminMetricsController {

    private final RequestMetricsService requestMetricsService;
    private final SystemStatsService systemStatsService;
    private final DbStatsService dbStatsService;

    /** HTTP 요청 수·응답 시간·에러율 */
    @GetMapping
    public ResponseEntity<MetricsResponse> metrics() {
        return ResponseEntity.ok(requestMetricsService.snapshot());
    }

    /**
     * 호스트·백엔드 지표와 DB 지표는 출처가 달라 서비스가 분리돼 있음.
     * 화면은 한 번에 그리므로 여기서 합쳐서 응답
     */
    @GetMapping("/system")
    public ResponseEntity<SystemStatsResponse> system() {
        SystemStatsResponse base = systemStatsService.snapshot();
        return ResponseEntity.ok(new SystemStatsResponse(base.host(), base.backend(), dbStatsService.snapshot()));
    }
}
