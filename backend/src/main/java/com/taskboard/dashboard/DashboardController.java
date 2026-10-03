package com.taskboard.dashboard;

import com.taskboard.dashboard.DashboardDtos.DashboardResponse;
import com.taskboard.global.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 홈 화면 데이터 API */
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardResponse> load(@AuthenticationPrincipal CurrentUser user) {
        return ResponseEntity.ok(dashboardService.load(user));
    }
}
