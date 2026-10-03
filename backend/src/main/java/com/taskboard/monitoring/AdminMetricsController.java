package com.taskboard.monitoring;

import com.taskboard.monitoring.MetricsDtos.MetricsResponse;
import com.taskboard.monitoring.MetricsDtos.SystemStatsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 모니터링 지표 조회 API */
@RestController
@RequestMapping("/admin/metrics")
@RequiredArgsConstructor
public class AdminMetricsController {

    private final RequestMetricsService requestMetricsService;
    private final SystemStatsService systemStatsService;
    private final DbStatsService dbStatsService;

    /*************************************************************************
     * 목적 : 최근 60분 HTTP 요청 수·에러 수·평균 응답 시간 조회
     * 이유 : -
     * 파라미터
     * -
     * 반환
     * - 200 + 최근 60분 요청 지표
     *************************************************************************/
    @GetMapping
    public ResponseEntity<MetricsResponse> metrics() {
        return ResponseEntity.ok(requestMetricsService.snapshot());
    }

    /*************************************************************************
     * 목적 : 서버·백엔드·DB 자원 지표를 한 번에 조회
     * 이유 : 출처가 달라 서비스는 나뉘어 있지만 화면은 한 번에 그리므로 여기서 합침
     * 파라미터
     * -
     * 반환
     * - 200 + 자원 지표
     *************************************************************************/
    @GetMapping("/system")
    public ResponseEntity<SystemStatsResponse> system() {
        SystemStatsResponse base = systemStatsService.snapshot();
        return ResponseEntity.ok(new SystemStatsResponse(base.host(), base.backend(), dbStatsService.snapshot()));
    }
}
