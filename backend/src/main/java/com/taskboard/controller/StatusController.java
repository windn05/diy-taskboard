package com.taskboard.controller;

import com.taskboard.dto.StatusDtos.*;
import com.taskboard.service.StatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 작업 상태 조회. 변경은 AdminStatusController */
@RestController
@RequiredArgsConstructor
public class StatusController {

    private final StatusService statusService;

    @GetMapping("/statuses")
    public List<StatusResponse> list() {
        return statusService.list();
    }
}
