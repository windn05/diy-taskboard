package com.taskboard.controller;

import com.taskboard.dto.MetricsDtos.MetricsResponse;
import com.taskboard.service.RequestMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/metrics")
@RequiredArgsConstructor
public class AdminMetricsController {

    private final RequestMetricsService requestMetricsService;

    @GetMapping
    public MetricsResponse metrics() {
        return requestMetricsService.snapshot();
    }
}
