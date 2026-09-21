package com.taskboard.controller;

import com.taskboard.dto.MetricsDtos.MetricsResponse;
import com.taskboard.dto.MetricsDtos.SystemStatsResponse;
import com.taskboard.service.DbStatsService;
import com.taskboard.service.RequestMetricsService;
import com.taskboard.service.SystemStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/metrics")
@RequiredArgsConstructor
public class AdminMetricsController {

    private final RequestMetricsService requestMetricsService;
    private final SystemStatsService systemStatsService;
    private final DbStatsService dbStatsService;

    @GetMapping
    public MetricsResponse metrics() {
        return requestMetricsService.snapshot();
    }

    /**
     * 호스트·백엔드 지표와 DB 지표는 출처가 달라 서비스가 나뉘어 있다.
     * 화면은 한 번에 그리므로 여기서 합쳐 내보낸다.
     */
    @GetMapping("/system")
    public SystemStatsResponse system() {
        SystemStatsResponse base = systemStatsService.snapshot();
        return new SystemStatsResponse(base.host(), base.backend(), dbStatsService.snapshot());
    }
}
