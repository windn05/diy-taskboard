package com.taskboard.status;

import com.taskboard.status.StatusDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 작업 상태(칸반 컬럼) 관리. 모든 프로젝트가 공유하는 전역 설정이라 관리자 전용. 조회는 StatusController */
@RestController
@RequestMapping("/admin/statuses")
@RequiredArgsConstructor
public class AdminStatusController {

    private final StatusService statusService;

    @PostMapping
    public ResponseEntity<StatusResponse> create(@Valid @RequestBody StatusRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(statusService.create(request));
    }

    @PatchMapping("/{statusId}")
    public ResponseEntity<StatusResponse> rename(@PathVariable Long statusId,
                                                 @Valid @RequestBody StatusRequest request) {
        return ResponseEntity.ok(statusService.rename(statusId, request));
    }

    @DeleteMapping("/{statusId}")
    public ResponseEntity<Void> delete(@PathVariable Long statusId) {
        statusService.delete(statusId);
        return ResponseEntity.noContent().build();
    }
}
