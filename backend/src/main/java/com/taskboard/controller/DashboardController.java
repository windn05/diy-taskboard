package com.taskboard.controller;

import com.taskboard.dto.DashboardDtos.DashboardResponse;
import com.taskboard.security.CurrentUser;
import com.taskboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardResponse load(@AuthenticationPrincipal CurrentUser user) {
        return dashboardService.load(user);
    }
}
