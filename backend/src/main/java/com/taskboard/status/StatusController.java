package com.taskboard.status;

import com.taskboard.status.StatusDtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 작업 상태 조회 API */
@RestController
@RequiredArgsConstructor
public class StatusController {

    private final StatusService statusService;

    @GetMapping("/statuses")
    public ResponseEntity<List<StatusResponse>> list() {
        return ResponseEntity.ok(statusService.list());
    }
}
