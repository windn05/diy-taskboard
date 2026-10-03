package com.taskboard.status;

import com.taskboard.status.StatusDtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 작업 상태 관리 API (관리자 전용) */
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
